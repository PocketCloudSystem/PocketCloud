<?php

namespace pocketcloud\cloud\http\server\socket;

use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\http\server\HttpServer;
use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\Response;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\io\ResponseCache;
use pocketcloud\cloud\http\server\util\HttpConstants;
use pocketcloud\cloud\http\server\util\PendingResponse;
use pocketcloud\cloud\http\server\util\StatusCode;
use pocketcloud\cloud\http\server\util\UnhandledHttpRequest;
use pocketcloud\cloud\http\util\HttpUtils;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\thread\Thread;
use pocketcloud\cloud\traffic\impl\HttpTrafficMonitor;
use pocketcloud\cloud\traffic\TrafficMonitor;
use pocketcloud\cloud\traffic\TrafficMonitorManager;
use pocketcloud\cloud\util\net\Address;
use pocketmine\snooze\SleeperHandlerEntry;
use Socket;
use Throwable;

final class SocketServer extends Thread {

    /**
     * How many requests the main thread processes per sleeper tick.
     * handleRequest() runs on the main thread and blocks PocketMine's entire
     * tick loop. Keep this low enough that one batch does not consume more than
     * ~10ms of tick budget. Tune downward if ticks still lag.
     */
    private const MAX_REQUESTS_PER_TICK = 20;

    /**
     * socket_select() timeout in microseconds.
     * 500µs keeps the response-write loop responsive without spinning at 100%.
     */
    private const SELECT_TIMEOUT_US = 500;

    private ?Socket $socket = null;

    /** @var ThreadSafeArray<string, Socket> */
    private ThreadSafeArray $clients;
    /** @var ThreadSafeArray<string, ThreadSafeArray> */
    private ThreadSafeArray $clientBuffers;
    /** @var ThreadSafeArray<int, Socket> */
    private ThreadSafeArray $pendingClose;
    /**
     * Main thread pushes PendingResponse here after handling a request.
     * Worker thread drains this, writes the response to the socket, then closes it.
     * @var ThreadSafeArray<int, PendingResponse>
     */
    private ThreadSafeArray $responseQueue;

    /**
     * True while the main thread's notifier callback is executing.
     * The worker checks this before calling wakeupSleeper() — if the main thread
     * is already busy, the new buffer item will be picked up by the current or
     * next batch, or via needsReWake if the cap is hit.
     * @var ThreadSafeArray<int, bool>
     */
    private ThreadSafeArray $mainThreadBusy;

    /**
     * Set by the notifier callback when it exits after hitting MAX_REQUESTS_PER_TICK
     * with items still in the buffer. The worker sees this on its next iteration
     * and calls wakeupSleeper() so the remainder is processed.
     * We cannot call wakeupSleeper() from inside the callback — it is not re-entrant.
     * @var ThreadSafeArray<int, bool>
     */
    private ThreadSafeArray $needsReWake;

    private SleeperHandlerEntry $entry;

    public function __construct(
        private readonly Address $address,
        private ThreadSafeArray $buffer
    ) {
        $this->clients = new ThreadSafeArray();
        $this->clientBuffers = new ThreadSafeArray();
        $this->pendingClose = new ThreadSafeArray();
        $this->responseQueue = new ThreadSafeArray();
        $this->mainThreadBusy = new ThreadSafeArray();
        $this->needsReWake = new ThreadSafeArray();

        $this->entry = PocketCloud::getInstance()->getSleeperHandler()->addNotifier(function (): void {
            $this->mainThreadBusy[0] = true;

            $processed = 0;

            /** @var UnhandledHttpRequest $unhandledRequest */
            while ($processed < self::MAX_REQUESTS_PER_TICK && ($unhandledRequest = $this->buffer->shift()) !== null) {
                $rawBuffer = $unhandledRequest->getBuffer();
                $address = $unhandledRequest->getAddress();
                $clientId = $unhandledRequest->getClientId();
                $bufferSize = $unhandledRequest->getBufferSize();

                TrafficMonitorManager::getInstance()->pushBytes(TrafficMonitorManager::TRAFFIC_HTTP, $bufferSize, TrafficMonitor::REGULAR_MODE_IN);
                TrafficMonitorManager::getInstance()->callHandlers(
                    TrafficMonitorManager::TRAFFIC_HTTP,
                    TrafficMonitor::REGULAR_MODE_IN,
                    $rawBuffer, $bufferSize, $address
                );

                [$response, $request] = $this->handleRequest($address, $rawBuffer);

                $httpResponseString = $response->buildResponseString();
                $responseSize = strlen($httpResponseString);

                TrafficMonitorManager::getInstance()->pushBytes(TrafficMonitorManager::TRAFFIC_HTTP, $responseSize, TrafficMonitor::REGULAR_MODE_OUT);
                TrafficMonitorManager::getInstance()->callHandlers(
                    TrafficMonitorManager::TRAFFIC_NETWORK,
                    TrafficMonitor::REGULAR_MODE_OUT,
                    $httpResponseString, $responseSize, $address
                );
                if ($request !== null) {
                    TrafficMonitorManager::getInstance()->callHandlers(
                        TrafficMonitorManager::TRAFFIC_HTTP,
                        HttpTrafficMonitor::HTTP_MODE_RESPONSE_OUT,
                        $request, $response, $address
                    );
                }

                $this->responseQueue[] = new PendingResponse($clientId, $httpResponseString);
                $processed++;
            }

            $this->mainThreadBusy[0] = false;

            // If we hit the cap and items remain, ask the worker to re-wake us.
            // We cannot call wakeupSleeper() here directly — the notifier is not re-entrant.
            if ($processed === self::MAX_REQUESTS_PER_TICK && $this->buffer->count() > 0) {
                $this->needsReWake[0] = true;
            }
        });
    }

    /**
     * Builds and returns the response for a raw HTTP request buffer.
     * Must only be called from the main thread (uses cloud singletons).
     * @return array{Response, ?Request}
     */
    public function handleRequest(Address $address, string $buffer): array {
        $request = HttpUtils::parseHttpRequest($address, $buffer);
        if ($request instanceof StatusCode) {
            return [ResponseBuilder::create()->code($request)->build(), null];
        }

        TrafficMonitorManager::getInstance()->callHandlers(
            TrafficMonitorManager::TRAFFIC_HTTP,
            HttpTrafficMonitor::HTTP_MODE_REQUEST_IN,
            $request, $address
        );

        $path = $request->getPath();

        if ($path->getApiVersion() !== null) {
            $ver = HttpServer::getInstance()->getVersion($path->getApiVersion());
            if ($ver !== null && !$ver->getAuthentication()->authenticate($request)) {
                return [$path->handleFailedAuth($request), $request];
            }
        }

        if (!$path->getAuthentication()->authenticate($request)) {
            return [$path->handleFailedAuth($request), $request];
        }

        if (!HttpServer::getInstance()->getRateLimiter()->checkRequest($address, $endTimestamp)) {
            return [HttpServer::getInstance()->getRateLimitResponse($request, $endTimestamp), $request];
        }

        if ($path->isBadRequest($request, $badRequestResponse = ResponseBuilder::create()->code(StatusCode::BAD_REQUEST))) {
            return [$badRequestResponse->build(), $request];
        }

        if ($path->willCauseError($request, $serverErrorResponse = ResponseBuilder::create()->code(StatusCode::INTERNAL_SERVER_ERROR))) {
            return [$serverErrorResponse->build(), $request];
        }

        $response = ResponseCache::check($request);
        if ($response === null) {
            $response = $path->handle($request);
            if ($response->getStatusCode() === 200) {
                ResponseCache::cache($request, $response);
            }
        }

        return [$response, $request];
    }

    public function create(): bool {
        if ($this->socket !== null) return false;

        $this->socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
        if ($this->socket === false) return false;

        socket_set_option($this->socket, SOL_SOCKET, SO_REUSEADDR, 1);
        socket_set_option($this->socket, SOL_TCP, TCP_NODELAY, 1);
        socket_set_option($this->socket, SOL_SOCKET, SO_RCVBUF, HttpConstants::MAX_REQUEST_SIZE);
        socket_set_nonblock($this->socket);

        if (socket_bind($this->socket, $this->address->getAddress(), $this->address->getPort())) {
            return socket_listen($this->socket, 128);
        }

        return false;
    }

    public function close(): void {
        if ($this->socket !== null) {
            socket_shutdown($this->socket);
            socket_close($this->socket);
            $this->quit();
        }
    }

    protected function onRun(): void {
        $notifier = $this->entry->createNotifier();

        while ($this->socket !== null && $this->isAlive()) {

            // Re-wake the main thread if the previous batch hit the cap.
            // This is the only safe place to call wakeupSleeper() for continuations
            // since the notifier callback itself is not re-entrant.
            if ($this->needsReWake[0] ?? false) {
                $this->needsReWake[0] = false;
                $notifier->wakeupSleeper();
            }

            // Drain sockets queued for closing.
            while (($sock = $this->pendingClose->shift()) !== null) {
                try {
                    socket_close($sock);
                } catch (Throwable $e) {
                    $this->logger->exception($e);
                }
            }

            // Write all pending responses before doing more socket I/O so
            // clients receive their replies as quickly as possible.
            while (($pending = $this->responseQueue->shift()) !== null) {
                $clientId = $pending->getClientId();
                $sock = $this->clients[$clientId] ?? null;
                if ($sock !== null) {
                    unset($this->clients[$clientId], $this->clientBuffers[$clientId]);
                    $this->writeResponse($sock, $pending->getHttpResponse());
                    $this->pendingClose[] = $sock;
                }
            }

            // Snapshot counts once — each ->count() acquires the ThreadSafeArray
            // mutex. Calling it per-socket inside the foreach below causes N
            // mutex acquisitions per select() iteration under load.
            $clientCount = $this->clients->count();
            $bufferCount = $this->buffer->count();

            $read = [$this->socket];
            foreach ($this->clients as $clientId => $clientSocket) {
                if (!($clientSocket instanceof Socket)) {
                    $this->closeClient($clientId);
                    continue;
                }
                $read[] = $clientSocket;
            }

            $write = null;
            $except = null;

            try {
                $result = socket_select($read, $write, $except, 0, self::SELECT_TIMEOUT_US);

                if ($result === false) {
                    $err = socket_last_error();
                    socket_clear_error();
                    $this->logger->warn("socket_select failed: " . $err);
                    continue;
                }

                if ($result === 0) continue;

                if ($this->socket === null || !$this->isAlive()) break;

                $addedToBuffer = false;

                foreach ($read as $sock) {
                    if ($sock === $this->socket) {
                        $this->acceptNewConnection($bufferCount >= 1000 || $clientCount > 200);
                        continue;
                    }

                    if ($clientCount > 200 || $bufferCount >= 1000) {
                        foreach ($this->clients as $id => $s) {
                            if ($s === $sock) {
                                $this->closeClient($id);
                                break;
                            }
                        }
                        continue;
                    }

                    if ($this->handleClientData($sock, $data)) {
                        $this->buffer[] = new UnhandledHttpRequest(...$data);
                        $bufferCount++;
                        $addedToBuffer = true;
                    }
                }

                // Wake the main thread whenever we added something to the buffer
                // AND the main thread is not already inside its notifier callback.
                // If it is busy, the new item will either be caught by the current
                // batch (if under the cap) or by needsReWake (if the cap was hit).
                if ($addedToBuffer && !($this->mainThreadBusy[0] ?? false)) {
                    $notifier->wakeupSleeper();
                }

            } catch (Throwable $e) {
                $this->logger->exception($e);
            }
        }
    }

    /**
     * Writes a response string to a socket.
     * Switches to blocking mode with a tight send timeout so the write
     * completes without busy-waiting. The socket is always closed afterward
     * via pendingClose so non-blocking mode does not need to be restored.
     * Do NOT call $this->buffer->count() here — that acquires the shared mutex
     * while the main thread is actively shifting from the same array, causing
     * lock contention on every response write under load.
     */
    private function writeResponse(Socket $socket, string $data): void {
        socket_set_block($socket);
        socket_set_option($socket, SOL_SOCKET, SO_SNDTIMEO, ["sec" => 0, "usec" => 200000]);

        $total = strlen($data);
        $written = 0;
        while ($written < $total) {
            $result = @socket_write($socket, substr($data, $written), $total - $written);
            if ($result === false) break;
            $written += $result;
        }
    }

    private function closeClient(string $clientId): void {
        if (isset($this->clients[$clientId])) {
            @socket_close($this->clients[$clientId]);
            unset($this->clients[$clientId]);
        }
        unset($this->clientBuffers[$clientId]);
    }

    private function acceptNewConnection(bool $closeImmediately = false): void {
        while ($this->isAlive()) {
            $clientSocket = @socket_accept($this->socket);
            if (!($clientSocket instanceof Socket)) break;
            if ($closeImmediately) {
                @socket_shutdown($clientSocket);
                @socket_close($clientSocket);
                continue;
            }

            socket_set_nonblock($clientSocket);
            socket_set_option($clientSocket, SOL_SOCKET, SO_RCVBUF, HttpConstants::MAX_REQUEST_SIZE);

            if (!@socket_getpeername($clientSocket, $address, $port)) {
                @socket_close($clientSocket);
                continue;
            }

            $socketAddress = new Address($address, $port);
            $clientId = "$address:$port";

            if (isset($this->clients[$clientId])) {
                @socket_close($this->clients[$clientId]);
            }

            $this->clients[$clientId] = $clientSocket;
            $this->clientBuffers[$clientId] = ThreadSafeArray::fromArray([
                "buffer" => "",
                "contentLength" => 0,
                "headersComplete" => false,
                "bodyStartPos" => 0,
                "address" => $socketAddress,
            ]);
        }
    }

    private function handleClientData(Socket $clientSocket, ?array &$data = null): bool {
        if (!@socket_getpeername($clientSocket, $address, $port)) {
            foreach ($this->clients as $id => $sock) {
                if ($sock === $clientSocket) {
                    $this->closeClient($id);
                    break;
                }
            }
            return false;
        }

        $clientId = "$address:$port";

        if (!isset($this->clients[$clientId], $this->clientBuffers[$clientId])) {
            return false;
        }

        while ($this->isAlive()) {
            $chunk = @socket_read($clientSocket, HttpConstants::CHUNK_SIZE);

            if ($chunk === false) {
                $err = socket_last_error($clientSocket);
                if ($err === SOCKET_EAGAIN || $err === SOCKET_EWOULDBLOCK) break;
                $this->closeClient($clientId);
                return false;
            }

            if ($chunk === "") {
                $this->closeClient($clientId);
                return false;
            }

            $buf = $this->clientBuffers[$clientId];
            $buf["buffer"] .= $chunk;
            $this->clientBuffers[$clientId] = $buf;

            if (strlen($buf["buffer"]) > HttpConstants::MAX_REQUEST_SIZE) {
                CloudLogger::get()->warn("Request too large from §b{}§r, §cclosing§8...", $clientId);
                $this->closeClient($clientId);
                return false;
            }

            if (!$buf["headersComplete"]) {
                $headerEndPos = strpos($buf["buffer"], "\r\n\r\n");
                if ($headerEndPos !== false) {
                    $buf["headersComplete"] = true;
                    $buf["bodyStartPos"] = $headerEndPos + 4;

                    $headerSection = substr($buf["buffer"], 0, $headerEndPos);
                    if (preg_match("/Content-Length:\s*(\d+)/i", $headerSection, $matches)) {
                        $contentLength = (int)$matches[1];
                        if ($contentLength > HttpConstants::MAX_REQUEST_SIZE) {
                            CloudLogger::get()->warn("Content-Length too large from §b{}§r, §cclosing§8...", $clientId);
                            $this->closeClient($clientId);
                            return false;
                        }
                        $buf["contentLength"] = $contentLength;
                    }

                    $this->clientBuffers[$clientId] = $buf;
                }
            }

            if ($buf["headersComplete"]) {
                $currentBodyLength = strlen($buf["buffer"]) - $buf["bodyStartPos"];
                if ($currentBodyLength >= $buf["contentLength"]) {
                    $data = [
                        "buffer" => $buf["buffer"],
                        "address" => $buf["address"],
                        "clientId" => $clientId,
                        "bufferSize" => strlen($buf["buffer"]),
                        "contentLength" => $buf["contentLength"],
                    ];

                    $this->clientBuffers[$clientId] = ThreadSafeArray::fromArray([
                        "buffer" => "",
                        "contentLength" => 0,
                        "headersComplete" => false,
                        "bodyStartPos" => 0,
                        "address" => $buf["address"],
                    ]);

                    return true;
                }
            }
        }

        return false;
    }
}
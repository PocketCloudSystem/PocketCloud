<?php

namespace pocketcloud\cloud\http\socket;

use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\http\HttpServer;
use pocketcloud\cloud\http\io\ResponseBuilder;
use pocketcloud\cloud\http\io\ResponseCache;
use pocketcloud\cloud\http\util\HttpConstants;
use pocketcloud\cloud\http\util\StatusCode;
use pocketcloud\cloud\http\util\HttpUtils;
use pocketcloud\cloud\http\util\UnhandledHttpRequest;
use pocketcloud\cloud\Server;
use pocketcloud\cloud\thread\Thread;
use pocketcloud\cloud\traffic\impl\HttpTrafficMonitor;
use pocketcloud\cloud\traffic\TrafficMonitor;
use pocketcloud\cloud\traffic\TrafficMonitorManager;
use pocketcloud\cloud\util\net\Address;
use pocketmine\snooze\SleeperHandlerEntry;
use Socket;

final class SocketServer extends Thread {

    private ?Socket $socket = null;

    /** @var ThreadSafeArray<string, Socket> */
    private ThreadSafeArray $clients;

    /** @var ThreadSafeArray<string, array{buffer: string, contentLength: int, headersComplete: bool, bodyStartPos: int, address: Address}> */
    private ThreadSafeArray $clientBuffers;

    private SleeperHandlerEntry $entry;

    public function __construct(
        private readonly Address $address,
        private ThreadSafeArray $buffer,
        private readonly bool $onlyLocal
    ) {
        $this->clients = new ThreadSafeArray();
        $this->clientBuffers = new ThreadSafeArray();

        $this->entry = $Server::getInstance()->sleeperHandler->addNotifier(function (): void {
            /** @var UnhandledHttpRequest $unhandledRequest */
            while (($unhandledRequest = $this->buffer->shift()) !== null) {
                /**
                 * @var string $buffer
                 * @var Address $address
                 * @var string $clientId
                 * @var int $bufferSize
                 */
                [$buffer, $address, $clientId, $bufferSize] = [$unhandledRequest->getBuffer(), $unhandledRequest->getAddress(), $unhandledRequest->getClientId(), $unhandledRequest->getContentLength()];

                TrafficMonitorManager::getInstance()->pushBytes(TrafficMonitorManager::TRAFFIC_HTTP, $bufferSize, TrafficMonitor::REGULAR_MODE_IN);
                TrafficMonitorManager::getInstance()->callHandlers(
                    TrafficMonitorManager::TRAFFIC_HTTP,
                    TrafficMonitor::REGULAR_MODE_IN,
                    $buffer, $bufferSize, $address
                );

                $this->processCompleteRequest($clientId, $buffer, $address);
            }
        });
    }

    protected function onRun(): void {
        $notifier = $this->entry->createNotifier();
        while ($this->socket !== null && $this->isAlive()) {
            $read = [$this->socket];

            foreach ($this->clients as $clientSocket) {
                $read[] = $clientSocket;
            }

            $write = null;
            $except = null;
            if (@socket_select($read, $write, $except, 0, 50 * 1000) > 0) {
                if (!$this->isAlive() || $this->socket === null) break;
                foreach ($read as $key => $sock) {
                    if ($sock === $this->socket) {
                        $this->acceptNewConnection();
                        unset($read[$key]);
                    }
                }

                foreach ($read as $clientSocket) {
                    if ($this->handleClientData($clientSocket, $data)) {
                        $this->buffer[] = new UnhandledHttpRequest(...$data);
                        $notifier->wakeupSleeper();
                    }
                }
            }
        }
    }

    public function quit(): void {
        parent::quit();
        $this->buffer = new ThreadSafeArray();
    }

    public function create(): bool {
        if ($this->socket !== null) return false;
        $this->socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
        if ($this->socket === false) return false;

        socket_set_option($this->socket, SOL_SOCKET, SO_REUSEADDR, 1);
        socket_set_option($this->socket, SOL_TCP, TCP_NODELAY, 1);
        socket_set_nonblock($this->socket);

        if (socket_bind($this->socket, $this->address->getAddress(), $this->address->getPort())) return socket_listen($this->socket);
        return false;
    }
    
    private function acceptNewConnection(): void {
        $clientSocket = @socket_accept($this->socket);

        if ($clientSocket === false) return;
        if (!$clientSocket instanceof Socket) return;

        socket_set_nonblock($clientSocket);

        if (!@socket_getpeername($clientSocket, $address, $port)) {
            @socket_close($clientSocket);
            return;
        }

        $socketAddress = new Address($address, $port);
        if ($this->onlyLocal && !$socketAddress->isLocal()) return;

        $clientId = "$address:$port";
        $this->clients[$clientId] = $clientSocket;
        $this->clientBuffers[$clientId] = ThreadSafeArray::fromArray([
            "buffer" => "",
            "contentLength" => 0,
            "headersComplete" => false,
            "bodyStartPos" => 0,
            "address" => $socketAddress
        ]);
    }
    
    private function handleClientData(Socket $clientSocket, ?array &$data = null): bool {
        if (!@socket_getpeername($clientSocket, $address, $port)) {
            @socket_close($clientSocket);
            return false;
        }

        $clientId = "$address:$port";

        if (!isset($this->clients[$clientId])) return false;
        if (!isset($this->clientBuffers[$clientId])) return false;

        $buffer = &$this->clientBuffers[$clientId];

        $chunk = @socket_read($clientSocket, HttpConstants::CHUNK_SIZE);

        if ($chunk === false || $chunk === "") {
            $this->closeClient($clientId);
            return false;
        }

        $buffer["buffer"] .= $chunk;

        if (($bufferSize = strlen($buffer["buffer"])) > HttpConstants::MAX_REQUEST_SIZE) {
            CloudLogger::get()->warn("Request too large from §b{}§r, §cclosing§8...", $clientId);
            $this->closeClient($clientId);
            return false;
        }

        if (!$buffer["headersComplete"]) {
            if (($headerEndPos = strpos($buffer["buffer"], "\r\n\r\n")) !== false) {
                $buffer["headersComplete"] = true;
                $buffer["bodyStartPos"] = $headerEndPos + 4;

                $headerSection = substr($buffer["buffer"], 0, $headerEndPos);

                if (preg_match("/Content-Length:\s*(\d+)/i", $headerSection, $matches)) {
                    $buffer["contentLength"] = (int) $matches[1];

                    if ($buffer["contentLength"] > HttpConstants::MAX_REQUEST_SIZE) {
                        CloudLogger::get()->warn("Content-Length too large from §b{}§r, §cclosing§8...", $clientId);
                        $this->closeClient($clientId);
                        return false;
                    }
                }
            }
        }

        if ($buffer["headersComplete"]) {
            $currentBodyLength = strlen($buffer["buffer"]) - $buffer["bodyStartPos"];

            if ($currentBodyLength >= $buffer["contentLength"]) {
                $data = ["buffer" => $buffer["buffer"], "address" => $buffer["address"], "clientId" => $clientId, "bufferSize" => $bufferSize, "contentLength" => $buffer["contentLength"]];
                return true;
            }
        }

        return false;
    }
    
    private function processCompleteRequest(string $clientId, string $requestBuffer, Address $address): void {
        if (!isset($this->clients[$clientId])) return;
        $client = new SocketClient($address, $this->clients[$clientId]);

        $this->handleRequest($client, $requestBuffer);

        unset($this->clients[$clientId]);
        unset($this->clientBuffers[$clientId]);
    }

    private function closeClient(string $clientId): void {
        if (isset($this->clients[$clientId])) {
            @socket_close($this->clients[$clientId]);
            unset($this->clients[$clientId]);
        }

        if (isset($this->clientBuffers[$clientId])) {
            unset($this->clientBuffers[$clientId]);
        }
    }

    public function handleRequest(SocketClient $client, string $buffer): void {
        $request = HttpUtils::parseHttpRequest($client->getAddress(), $buffer);
        if ($request instanceof StatusCode) {
            $client->respond(ResponseBuilder::create()
                ->code($request)
                ->build()
            );
            return;
        }

        TrafficMonitorManager::getInstance()->callHandlers(
            TrafficMonitorManager::TRAFFIC_HTTP,
            HttpTrafficMonitor::HTTP_MODE_REQUEST_IN,
            $request, $client->getAddress()
        );

        $path = $request->getPath();
        if ($path->getApiVersion() !== null) {
            $ver = HttpServer::getInstance()->getVersion($path->getApiVersion());
            if ($ver !== null && !$ver->getAuthentication()->authenticate($client, $request)) {
                $client->respond($path->handleFailedAuth($request), $request);
                return;
            }
        }

        if ($path->getAuthentication()->authenticate($client, $request)) {
            if (HttpServer::getInstance()->getRateLimiter()->checkRequest($client->getAddress(), $endTimestamp)) {
                if ($path->isBadRequest($request, $badRequestResponse = ResponseBuilder::create()->code(StatusCode::BAD_REQUEST))) {
                    $client->respond($badRequestResponse->build(), $request);
                    return;
                }

                if ($path->willCauseError($request, $serverErrorResponse = ResponseBuilder::create()->code(StatusCode::INTERNAL_SERVER_ERROR))) {
                    $client->respond($serverErrorResponse->build(), $request);
                    return;
                }

                $response = ResponseCache::check($request);
                if ($response === null) {
                    $response = $path->handle($request);

                    if ($response->getStatusCode() == 200) {
                        ResponseCache::cache($request, $response);
                    }
                }

                $client->respond($response, $request);
            } else {
                $client->respond(HttpServer::getInstance()->getRateLimitResponse($client, $request, $endTimestamp), $request);
            }
        } else {
            $client->respond($path->handleFailedAuth($request), $request);
        }
    }

    public function close(): void {
        if ($this->socket !== null) {
            socket_close($this->socket);
            $this->socket = null;
        }
    }
}
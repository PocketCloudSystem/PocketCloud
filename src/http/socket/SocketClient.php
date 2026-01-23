<?php

namespace pocketcloud\cloud\http\socket;

use pmmp\thread\ThreadSafe;
use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\traffic\impl\HttpTrafficMonitor;
use pocketcloud\cloud\traffic\TrafficMonitor;
use pocketcloud\cloud\traffic\TrafficMonitorManager;
use pocketcloud\cloud\util\net\Address;
use Socket;

final class SocketClient extends ThreadSafe {

    private const int WRITE_TIMEOUT_SECONDS = 30;

    public function __construct(
        private readonly Address $address,
        private readonly Socket $socket
    ) {}

    public static function fromSocket(Socket $socket): ?SocketClient {
        if (!@socket_getpeername($socket, $address, $port)) return null;
        return new SocketClient(new Address($address, $port), $socket);
    }

    public function respond(Response $response, ?Request $request = null): void {
        $httpResponse = $response->buildResponseString();
        $total = strlen($httpResponse);
        $written = 0;
        $startTime = time();

        while ($written < $total) {
            if (time() - $startTime > self::WRITE_TIMEOUT_SECONDS) {
                $this->close();
                return;
            }

            $result = @socket_write(
                $this->socket,
                substr($httpResponse, $written),
                $total - $written
            );

            if ($result === false) break;
            $written += $result;
        }

        TrafficMonitorManager::getInstance()->pushBytes(TrafficMonitorManager::TRAFFIC_HTTP, $written, TrafficMonitor::REGULAR_MODE_OUT);
        TrafficMonitorManager::getInstance()->callHandlers(
            TrafficMonitorManager::TRAFFIC_NETWORK,
            TrafficMonitor::REGULAR_MODE_OUT,
            $httpResponse,
            $written,
            $this->address,
        );

        if ($request !== null) TrafficMonitorManager::getInstance()->callHandlers(
            TrafficMonitorManager::TRAFFIC_HTTP,
            HttpTrafficMonitor::HTTP_MODE_RESPONSE_OUT,
            $request, $response, $this->address
        );

        $this->close();
    }

    public function read(int $len): false|string {
        return socket_read($this->socket, $len);
    }

    public function close(): void {
        if ($this->socket !== null) @socket_close($this->socket);
    }

    public function getAddress(): Address {
        return $this->address;
    }
}
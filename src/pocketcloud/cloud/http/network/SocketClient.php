<?php

namespace pocketcloud\cloud\http\network;

use pmmp\thread\ThreadSafe;
use pocketcloud\cloud\traffic\TrafficMonitor;
use pocketcloud\cloud\traffic\TrafficMonitorManager;
use pocketcloud\cloud\util\net\Address;
use Socket;

final class SocketClient extends ThreadSafe {

    protected ?Socket $socket = null;

    public function __construct(protected Address $address) {}

    public static function fromSocket(Socket $socket): SocketClient {
        socket_getpeername($socket, $address, $port);
        $c = new SocketClient(new Address($address, $port));
        $c->socket = $socket;
        return $c;
    }

    public function read(int $len): false|string {
        return @socket_read($this->socket, $len);
    }

    public function write(string $buffer): bool {
        TrafficMonitorManager::getInstance()->pushBytes(TrafficMonitorManager::TRAFFIC_HTTP, strlen($buffer), TrafficMonitor::REGULAR_MODE_OUT);
        return (@socket_write($this->socket, $buffer) === strlen($buffer));
    }

    public function close(): void {
        @socket_shutdown($this->socket);
        @socket_close($this->socket);
    }

    public function getAddress(): Address {
        return $this->address;
    }
}
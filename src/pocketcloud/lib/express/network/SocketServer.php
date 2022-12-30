<?php

namespace pocketcloud\lib\express\network;

use pocketcloud\lib\snooze\SleeperNotifier;
use pocketcloud\thread\Thread;
use pocketcloud\utils\Address;
use Socket;
use function socket_accept;
use function socket_bind;
use function socket_create;
use function socket_listen;
use function socket_set_nonblock;
use const AF_INET;
use const SOCK_STREAM;
use const SOL_TCP;

class SocketServer extends Thread {

    private bool $closed = false;
    public const REQUEST_READ_LENGTH = 8192;
    protected Socket|null $socket = null;

    public function __construct(private Address $address, private SleeperNotifier $notifier, private \Volatile $buffer) {}

    public function run() {
        $this->registerClassLoader();

        while (!$this->closed) {
            if ($c = $this->accept()) {
                if ($buffer = $c->read(self::REQUEST_READ_LENGTH)) {
                    $this->buffer[] = [
                        "client" => $c,
                        "buffer" => $buffer
                    ];
                    $this->notifier->wakeupSleeper();
                }
            }
        }
    }

    public function init(bool $listen = true, bool $blocking = true): bool {
		$this->socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
		if (!socket_bind($this->socket, $this->address->getAddress(), $this->address->getPort())) return false;
		if (!$blocking) socket_set_nonblock($this->socket);
        socket_set_option($this->socket, SOL_SOCKET, SO_REUSEADDR, 1);
        if ($listen) return socket_listen($this->socket);
		return true;
	}
	
	public function accept(): ?SocketClient {
		if ($c = @socket_accept($this->socket)) return SocketClient::fromSocket($c);
		return null;
	}

    public function close() {
        if ($this->closed) return;
        $this->closed = true;
        socket_set_option($this->socket, SOL_SOCKET, SO_LINGER, ["l_onoff" => 1, "l_linger" => 1]);
        @socket_close($this->socket);
    }

    public function getAddress(): Address {
        return $this->address;
    }
}
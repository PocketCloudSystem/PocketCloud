<?php

namespace pocketcloud\lib\express\network;

use pocketcloud\utils\Address;
use Socket;
use function socket_getpeername;
use function socket_read;
use function socket_write;

class SocketClient extends \Volatile {

	protected Socket|null $socket = null;

    public function __construct(protected Address $address) { }

    public static function fromSocket(Socket $socket): SocketClient {
		socket_getpeername($socket, $address, $port);
		$c = new SocketClient(new Address($address, $port));
		$c->socket = $socket;
		return $c;
	}
	
	public function read(int $len): false|string {
		return socket_read($this->socket, $len);
	}
	
	public function write(string $buffer): bool {
		return (socket_write($this->socket, $buffer) === strlen($buffer));
	}

    public function getAddress(): Address {
        return $this->address;
    }
}
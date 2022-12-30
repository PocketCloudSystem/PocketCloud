<?php

namespace pocketcloud\lib\express\network;

use pocketcloud\lib\express\App;

class Processor {
	
	public const REQUEST_READ_LENGTH = 8192;
	
	public function __construct(protected App $app, protected SocketServer $socketServer) { }
	
	public function tick(): void {
		if ($c = $this->socketServer->accept()) {
			if ($buffer = $c->read(self::REQUEST_READ_LENGTH)) $c->write($this->app->__internalReceiveRequest($c->getAddress(), $buffer));
		}
	}
	
	/**
	 * @return SocketServer
	 */
	public function getSocketServer(): SocketServer {
		return $this->socketServer;
	}
}
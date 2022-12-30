<?php

namespace pocketcloud\lib\express;

use Closure;
use pocketcloud\lib\snooze\SleeperNotifier;
use pocketcloud\PocketCloud;
use pocketcloud\lib\express\io\Response;
use pocketcloud\lib\express\network\SocketClient;
use pocketcloud\lib\express\route\Router;
use pocketcloud\lib\express\utils\Utils;
use pocketcloud\lib\express\network\Processor;
use pocketcloud\lib\express\network\SocketServer;
use pocketcloud\utils\Address;
use pocketcloud\utils\CloudLogger;
use function spl_autoload_register;
use function str_starts_with;
use function substr;
use function usleep;

final class App {
	
	protected ?SocketServer $sock = null;
	protected Router $router;
	protected ?Closure $invalidUrlHandler = null;
	
	public function __construct() { $this->init(); }
	
	private function init(): void {
		$this->router = new Router();
	}
	
	public function listen(int $port): bool {
		$this->sock = new SocketServer(new Address("0.0.0.0", $port), $notifier = new SleeperNotifier(), $buffer = new \Volatile());

        PocketCloud::getInstance()->getSleeperHandler()->addNotifier($notifier, function() use ($buffer): void {
            while (($data = $buffer->shift()) !== null) {
                /** @var SocketClient $client */
                $client = $data["client"];
                $buf = $data["buffer"];
                if ($buf === null || $buf === false) {
                    CloudLogger::get()->warn("§cInvalid request! §8(§e" . $client->getAddress() . "§8)");
                } else {
                    $client->write($this->__internalReceiveRequest($client->getAddress(), $buf));
                }
            }
        });

        if (!$this->sock->init()) return false;
        $this->sock->start();
        return true;
	}

    public function stop() {
        $this->sock?->close();
    }
	
	public function get(string $path, Closure $closure): void {
		$this->router->add(Router::GET, $path, $closure);
	}
	
	public function post(string $path, Closure $closure): void {
		$this->router->add(Router::POST, $path, $closure);
	}
	
	public function put(string $path, Closure $closure): void {
		$this->router->add(Router::PUT, $path, $closure);
	}
	
	public function patch(string $path, Closure $closure): void {
		$this->router->add(Router::PATCH, $path, $closure);
	}
	
	public function delete(string $path, Closure $closure): void {
		$this->router->add(Router::DELETE, $path, $closure);
	}
	
	public function default(Closure $closure): void {
		$this->invalidUrlHandler = $closure;
	}
	
	/**
	 * @internal
	 *
	 * @param Address $address
	 * @param string $request
	 *
	 * @return string
	 */
	public function __internalReceiveRequest(Address $address, string $request): string {
		$request =  Utils::parseRequest($address, $request);
        if ($this->router->isRegistered($request)) {
			return $this->router->execute($request);
		}
		$response = new Response(404);
		if ($this->invalidUrlHandler !== null) ($this->invalidUrlHandler)($request, $response);
		return $response;
	}
}

<?php

namespace pocketcloud\lib\express\route;

use Closure;
use pocketcloud\lib\express\io\Request;
use pocketcloud\lib\express\io\Response;
use pocketcloud\lib\express\utils\Utils;

class Router {

	public const GET = "GET";
	
	public const POST = "POST";
	
	public const PUT = "PUT";
	
	public const PATCH = "PATCH";
	
	public const DELETE = "DELETE";
	
	/** @var Closure[][] $routes */
	protected array $routes = [];
	
	public function add(string $method, string $route, Closure $closure): void {
		$this->routes[$method][$route] = $closure;
	}
	
	public function isRegistered(Request $request): bool {
		return (bool) $this->pickRoute($request->data()->method(), $request->data()->path());
	}
	
	public function execute(Request $request): Response {
		$response = new Response();
		$d = $this->pickRoute($request->data()->method(), $request->data()->path());
		if ($d !== null) {
			[$expectedPath, $closure] = $d;
			Utils::fillRequest($request, $expectedPath);
			$closure($request, $response);
		}
		return $response;
	}
	
	public function pickRoute(string $method, string $path): ?array {
		foreach ($this->routes[$method] ?? [] as $expectedPath => $closure) {
			if (Utils::matchPath($expectedPath, $path)) return [$expectedPath, $closure];
		}
		return null;
	}
}
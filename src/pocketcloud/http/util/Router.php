<?php

namespace pocketcloud\http\util;

use Closure;
use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\util\SingletonTrait;

class Router {
    use SingletonTrait;

    public const GET = "GET";
    public const POST = "POST";
    public const PUT = "PUT";
    public const PATCH = "PATCH";
    public const DELETE = "DELETE";

    /** @var array<string, array<string, Closure>> $routes */
    protected array $routes = [];

    public function __construct() {
        self::setInstance($this);
    }

    private function add(string $method, string $route, Closure $closure): void {
        $this->routes[$method][$route] = $closure;
    }

    public function get(string $path, Closure $closure): void {
        $this->add(self::GET, $path, $closure);
    }

    public function post(string $path, Closure $closure): void {
        $this->add(self::POST, $path, $closure);
    }

    public function put(string $path, Closure $closure): void {
        $this->add(self::PUT, $path, $closure);
    }

    public function patch(string $path, Closure $closure): void {
        $this->add(self::PATCH, $path, $closure);
    }

    public function delete(string $path, Closure $closure): void {
        $this->add(self::DELETE, $path, $closure);
    }

    public function isRegistered(Request $request): bool {
        return (bool) $this->pickRoute($request->data()->method(), $request->data()->path());
    }

    public function execute(Request $request): Response {
        $response = new Response();
        $d = $this->pickRoute($request->data()->method(), $request->data()->path());
        if ($d !== null) {
            [$expectedPath, $closure] = $d;
            HttpUtils::fillRequest($request, $expectedPath);
            $closure($request, $response);
        }
        return $response;
    }

    public function pickRoute(string $method, string $path): ?array {
        foreach ($this->routes[$method] ?? [] as $expectedPath => $closure) {
            if (HttpUtils::matchPath($expectedPath, $path)) return [$expectedPath, $closure];
        }
        return null;
    }

    public static function getInstance(): self {
        return self::$instance ??= new self;
    }
}
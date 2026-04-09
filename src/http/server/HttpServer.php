<?php

namespace pocketcloud\cloud\http\server;

use Closure;
use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\Response;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\ApiPath;
use pocketcloud\cloud\http\server\route\Path;
use pocketcloud\cloud\http\server\route\RegularPath;
use pocketcloud\cloud\http\server\socket\SocketServer;
use pocketcloud\cloud\http\server\util\HttpConstants;
use pocketcloud\cloud\http\server\util\RateLimiter;
use pocketcloud\cloud\http\server\util\StatusCode;
use pocketcloud\cloud\http\server\version\ApiVersion;
use pocketcloud\cloud\http\util\RequestMethod;
use pocketcloud\cloud\util\net\Address;
use pocketcloud\cloud\util\trait\SingletonTrait;
use Throwable;

final class HttpServer {
    use SingletonTrait;

    /** @var array<ApiVersion> */
    private array $versions = [];
    /** @var array<array<Path>> */
    private array $paths = [];

    /** @var array<array<array{path: Path, pattern: string, paramNames: list<string>}>> */
    private array $parameterizedPaths = [];

    private ?SocketServer $server = null;
    private Closure $rateLimitResponse;

    public function __construct(
        private readonly Address $address,
        private readonly RateLimiter $rateLimiter,
        private readonly bool $enableVersioning,
        private readonly bool $enableResponseCaching,
        private readonly int $cachingTimeInSeconds = 60
    ) {
        self::setInstance($this);
        $this->rateLimitResponse = function (Request $request, int $endTimestamp): Response {
            return ResponseBuilder::create()
                ->code(StatusCode::TOO_MANY_REQUESTS)
                ->body(["message" => "You are being rate limited. Please try again in " .
                    ($endTimestamp - time()) .
                    " seconds.", "end_timestamp" => $endTimestamp])
                ->build();
        };
    }

    public function init(): void {
        if (!MainConfig::getInstance()->isHttpServerEnabled()) return;
        try {
            if ($this->start()) {
                CloudLogger::get()->success("§bHTTP server §rhas been §aestablished §ron §b{}§r.", $this->address);
            } else {
                CloudLogger::get()->warn("Failed to setup the HTTP server, continuing...");
            }
        } catch (Throwable $e) {
            CloudLogger::get()->warn("Failed to setup the HTTP server, continuing...");
            CloudLogger::get()->exception($e);
        }
    }

    public function start(): bool {
        if (!MainConfig::getInstance()->isHttpServerEnabled()) return false;
        $this->server = new SocketServer($this->address, new ThreadSafeArray());
        if ($this->server->create()) return $this->server->start();
        return false;
    }

    public function stop(): void {
        $this->server?->close();
    }

    public function registerPath(Path $path): bool {
        $pathRoute = "/" . trim($path->getPath(), "/");
        if ($path->getApiVersion() !== null && !$this->enableVersioning) return false;
        if (!in_array($path->getMethod()->name, HttpConstants::SUPPORTED_REQUEST_METHODS)) return false;

        if ($path instanceof RegularPath) {
            $this->paths[$path->getMethod()->name][$path->getFullPath()] = $path;
            $this->maybeRegisterParameterizedPath($path, $path->getFullPath());
        } else if ($path instanceof ApiPath) {
            if (($version = $this->getVersion($path->getApiVersion())) !== null) {
                if (!$version->isValidPath($path->getMethod()->name, $pathRoute)) $version->addPath($path->getMethod()->name, $pathRoute);
                $this->paths[$path->getMethod()->name][$path->getFullPath()] = $path;
                $this->maybeRegisterParameterizedPath($path, $path->getFullPath());
                return true;
            }
        }

        return false;
    }

    public function getPath(RequestMethod $method, string $path): ?Path {
        return $this->paths[$method->name][$path] ?? null;
    }

    private function maybeRegisterParameterizedPath(Path $path, string $fullPath): void {
        if (!preg_match("/\{([^}]+)\}/", $fullPath)) return;

        $paramNames = [];
        $pattern = preg_replace_callback("/\{([^}]+)\}/", function (array $m) use (&$paramNames): string {
            $paramNames[] = $m[1];
            return "([^/]+)";
        }, $fullPath);
        $pattern = "#^" . $pattern . "$#";

        $this->parameterizedPaths[$path->getMethod()->name][] = [
            "path" => $path,
            "pattern" => $pattern,
            "paramNames" => $paramNames
        ];
    }

    public function getVersion(string $versionOrPath, RequestMethod|string $method = "GET"): ?ApiVersion {
        $method = $method instanceof RequestMethod ? $method->name : $method;
        if (isset($this->versions[$versionOrPath])) return $this->versions[$versionOrPath];
        if (count($a = array_filter($this->versions, fn(ApiVersion $version) => $version->isValidPath($method, $versionOrPath))) >
            0) return current($a);
        return null;
    }

    public function registerVersion(ApiVersion $version): bool {
        if (!$this->enableVersioning) return false;
        if (isset($this->versions[$version->getVersion()])) return false;
        $this->versions[$version->getVersion()] = $version;
        return true;
    }

    public function getVersions(): array {
        return $this->versions;
    }

    public function findPath(RequestMethod|string $method, string $path): ?array {
        $method = $method instanceof RequestMethod ? $method->name : $method;
        if (isset($this->paths[$method][$path])) return [$this->paths[$method][$path], []];
        foreach ($this->parameterizedPaths[$method] ?? [] as $entry) {
            if (preg_match($entry["pattern"], $path, $matches)) {
                array_shift($matches);
                $params = array_combine($entry["paramNames"], $matches);
                return [$entry["path"], $params];
            }
        }

        return null;
    }

    public function getPaths(): array {
        return $this->paths;
    }

    public function getServer(): SocketServer {
        return $this->server;
    }

    public function getRateLimitResponse(Request $request, int $endTimestamp): Response {
        return ($this->rateLimitResponse)($request, $endTimestamp);
    }

    public function setRateLimitResponse(Closure $closure): void {
        $this->rateLimitResponse = $closure;
    }

    public function getAddress(): Address {
        return $this->address;
    }

    public function getRateLimiter(): RateLimiter {
        return $this->rateLimiter;
    }

    public function isEnableVersioning(): bool {
        return $this->enableVersioning;
    }

    public function isEnableResponseCaching(): bool {
        return $this->enableResponseCaching;
    }

    public function getCachingTimeInSeconds(): int {
        return $this->cachingTimeInSeconds;
    }
}
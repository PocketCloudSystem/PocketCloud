<?php

namespace pocketcloud\cloud\http;

use Closure;
use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\io\ResponseBuilder;
use pocketcloud\cloud\http\route\ApiPath;
use pocketcloud\cloud\http\route\impl\TestRoute;
use pocketcloud\cloud\http\route\Path;
use pocketcloud\cloud\http\route\RegularPath;
use pocketcloud\cloud\http\socket\auth\DefaultAuthentication;
use pocketcloud\cloud\http\socket\SocketClient;
use pocketcloud\cloud\http\socket\SocketServer;
use pocketcloud\cloud\http\util\HttpConstants;
use pocketcloud\cloud\http\util\RateLimiter;
use pocketcloud\cloud\http\util\StatusCode;
use pocketcloud\cloud\http\version\ApiVersion;
use pocketcloud\cloud\util\net\Address;
use pocketcloud\cloud\util\trait\SingletonTrait;
use Throwable;

final class HttpServer {
    use SingletonTrait;

    /** @var array<ApiVersion> */
    private array $versions = [];
    /** @var array<array<Path>> */
    private array $paths = [];

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
        $this->rateLimitResponse = function (SocketClient $client, Request $request, int $endTimestamp): Response {
            return ResponseBuilder::create()
                ->code(StatusCode::TOO_MANY_REQUESTS)
                ->body(["message" => "You are being rate limited. Please try again in " . ($endTimestamp - time()) . " seconds.", "end_timestamp" => $endTimestamp])
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

        $this->registerPath(new TestRoute("/test", HttpConstants::GET, new DefaultAuthentication()));
    }

    public function start(): bool {
        if (!MainConfig::getInstance()->isHttpServerEnabled()) return false;
        $this->server = new SocketServer($this->address, new ThreadSafeArray(), MainConfig::getInstance()->isHttpServerOnlyLocal());
        if ($this->server->create()) return $this->server->start();
        return false;
    }

    public function stop(): void {
        $this->server?->close();
    }

    public function setRateLimitResponse(Closure $closure): void {
        $this->rateLimitResponse = $closure;
    }

    public function registerPath(Path $path): bool {
        $pathRoute = "/" . trim($path->getPath(), "/");
        if ($path->getApiVersion() !== null && !$this->enableVersioning) return false;
        if (!in_array($path->getMethod(), HttpConstants::SUPPORTED_REQUEST_METHODS)) return false;

        if ($path instanceof RegularPath) {
            $this->paths[$path->getMethod()][$path->getFullPath()] = $path;
        } else if ($path instanceof ApiPath) {
            if (($version = $this->getVersion($path->getApiVersion())) !== null) {
                if (!$version->isValidPath($path->getMethod(), $pathRoute)) $version->addPath($path->getMethod(), $pathRoute);
                $this->paths[$path->getMethod()][$path->getFullPath()] = $path;
                return true;
            }
        }

        return false;
    }

    public function registerVersion(ApiVersion $version): bool {
        if (!$this->enableVersioning) return false;
        if (isset($this->versions[$version->getVersion()])) return false;
        $this->versions[$version->getVersion()] = $version;
        return true;
    }

    public function getVersion(string $versionOrPath, string $method = "GET"): ?ApiVersion {
        if (isset($this->versions[$versionOrPath])) return $this->versions[$versionOrPath];
        if (count($a = array_filter($this->versions, fn(ApiVersion $version) => $version->isValidPath($method, $versionOrPath))) > 0) return current($a);
        return null;
    }

    public function getVersions(): array {
        return $this->versions;
    }

    public function getPath(string $method, string $path): ?Path {
        return $this->paths[$method][$path] ?? null;
    }

    public function getPaths(): array {
        return $this->paths;
    }

    public function getServer(): SocketServer {
        return $this->server;
    }

    public function getRateLimitResponse(SocketClient $client, Request $request, int $endTimestamp): Response {
        return ($this->rateLimitResponse)($client, $request, $endTimestamp);
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
<?php

namespace pocketcloud\http;

use Closure;
use pmmp\thread\ThreadSafeArray;
use pocketcloud\config\impl\DefaultConfig;
use pocketcloud\event\impl\http\HttpServerInitializeEvent;
use pocketcloud\http\endpoint\EndpointRegistry;
use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\network\SocketClient;
use pocketcloud\http\util\HttpUtils;
use pocketcloud\http\util\Router;
use pocketcloud\http\util\UnhandledHttpRequest;
use pocketcloud\language\Language;
use pocketcloud\PocketCloud;
use pocketcloud\thread\Thread;
use pocketcloud\util\Address;
use pocketcloud\util\CloudLogger;
use pocketcloud\util\Reloadable;
use pocketmine\snooze\SleeperHandlerEntry;
use Socket;
use Throwable;

final class HttpServer extends Thread implements Reloadable {

    public const REQUEST_READ_LENGTH = 8192;

    private bool $connected = false;

    protected ?Socket $socket = null;
    private ThreadSafeArray $buffer;
    private SleeperHandlerEntry $entry;
    private ?Closure $invalidUrlHandler = null;

    public function __construct(private readonly Address $address) {
        $this->buffer = new ThreadSafeArray();
    }

    public function onRun(): void {
        while ($this->connected) {
            if ($c = $this->accept()) {
                if ($buffer = $c->read(self::REQUEST_READ_LENGTH)) {
                    $this->buffer[] = new UnhandledHttpRequest($buffer, $c);
                    $this->entry->createNotifier()->wakeupSleeper();
                }
            }
        }
    }

    public function default(Closure $closure): void {
        $this->invalidUrlHandler = $closure;
    }

    private function handleRequest(Address $address, string $request): string {
        $request = HttpUtils::parseRequest($address, $request);
        if (!$request instanceof Request) return new Response(500);
        if (Router::getInstance()->isRegistered($request)) return Router::getInstance()->execute($request);
        $response = new Response(404);
        if ($this->invalidUrlHandler !== null) ($this->invalidUrlHandler)($request, $response);
        return $response;
    }

    public function init(): void {
        if (DefaultConfig::getInstance()->isHttpServerEnabled()) {
            (new HttpServerInitializeEvent())->call();

            EndpointRegistry::registerDefaults();

            try {
                if ($this->bind()) {
                    CloudLogger::get()->info(Language::current()->translate("httpServer.bound", $this->address->getPort()));
                } else {
                    CloudLogger::get()->error(Language::current()->translate("httpServer.bind.failed", $this->address->getPort()));
                    return;
                }
            } catch (Throwable $exception) {
                CloudLogger::get()->error(Language::current()->translate("httpServer.bind.failed.reason", $this->address->getPort(), $exception->getMessage()));
            }

            $this->entry = PocketCloud::getInstance()->getSleeperHandler()->addNotifier(function(): void {
                /** @var UnhandledHttpRequest $data */
                while (($data = $this->buffer->shift()) !== null) {
                    $client = $data->getClient();
                    $buf = $data->getBuffer();
                    try {
                        $write = true;
                        if (DefaultConfig::getInstance()->isHttpServerOnlyLocal() && !$client->getAddress()->isLocalHost()) $write = false;
                        if ($write) $client->write($this->handleRequest($client->getAddress(), $buf));
                        $client->close();
                    } catch (Throwable $exception) {
                        CloudLogger::get()->warn(Language::current()->translate("httpServer.request.invalid", $client->getAddress()->__toString()));
                        CloudLogger::get()->debug($buf);
                        CloudLogger::get()->exception($exception);
                    }
                }
            });

            $this->start();
        }
    }

    public function bind(): bool {
        $this->socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
        socket_set_option($this->socket, SOL_SOCKET, SO_REUSEADDR, 1);
        if (!socket_bind($this->socket, $this->address->getAddress(), $this->address->getPort())) return false;
        $this->connected = true;
        return socket_listen($this->socket);
    }

    public function accept(): ?SocketClient {
        if (!$this->connected) return null;
        if (($c = @socket_accept($this->socket)) !== false && $c instanceof Socket) return SocketClient::fromSocket($c);
        return null;
    }

    public function close(): void {
        if (!$this->connected) return;
        $this->connected = false;
        @socket_shutdown($this->socket);
        @socket_close($this->socket);
    }

    public function reload(): bool {
        if (DefaultConfig::getInstance()->isHttpServerEnabled()) {
            if (!$this->connected) {
                if ($this->isRunning()) $this->quit();
                $this->init();
                $this->start();
            }
        }
        return true;
    }

    public function getAddress(): Address {
        return $this->address;
    }
}
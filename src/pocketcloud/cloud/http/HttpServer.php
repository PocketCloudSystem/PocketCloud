<?php

namespace pocketcloud\cloud\http;

use Closure;
use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\event\impl\http\HttpServerInitializeEvent;
use pocketcloud\cloud\http\endpoint\EndpointRegistry;
use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\network\SocketClient;
use pocketcloud\cloud\http\util\HttpUtils;
use pocketcloud\cloud\http\util\Router;
use pocketcloud\cloud\http\util\UnhandledHttpRequest;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\thread\Thread;
use pocketcloud\cloud\util\net\Address;
use pocketmine\snooze\SleeperHandlerEntry;
use Socket;
use Throwable;

final class HttpServer extends Thread {

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
        CloudLogger::get()->debug("Parsing HTTP request from " . $address . "...");
        if (!$request instanceof Request) {
            CloudLogger::get()->debug("HTTP request from " . $address . " could not be parsed, sending response with code 500...");
            return new Response(500);
        }

        if (Router::getInstance()->isRegistered($request)) return Router::getInstance()->execute($request);
        CloudLogger::get()->debug("No route found for " . $request->data()->method() . " HTTP request from " . $request->data()->address() . ", sending 404 response...");
        $response = new Response(404);
        if ($this->invalidUrlHandler !== null) ($this->invalidUrlHandler)($request, $response);
        return $response;
    }

    public function init(): void {
        if (MainConfig::getInstance()->isHttpServerEnabled()) {
            (new HttpServerInitializeEvent())->call();

            EndpointRegistry::registerDefaults();

            try {
                if ($this->bind()) {
                    CloudLogger::get()->success("Successfully bound the HTTP server to §b" . $this->address . "§r.");
                } else {
                    CloudLogger::get()->error("§cFailed to bind the HTTP server to §e" . $this->address . "§r.");
                    return;
                }
            } catch (Throwable $exception) {
                CloudLogger::get()->error("§cFailed to bind the HTTP server to §e" . $this->address . "§8: §e" . $exception->getMessage());
            }

            $this->entry = PocketCloud::getInstance()->getSleeperHandler()->addNotifier(function(): void {
                /** @var UnhandledHttpRequest $data */
                while (($data = $this->buffer->shift()) !== null) {
                    $client = $data->getClient();
                    $buf = $data->getBuffer();

                    CloudLogger::get()->debug("Received incoming HTTP request from " . $client->getAddress() . "...");

                    try {
                        $write = true;
                        if (MainConfig::getInstance()->isHttpServerOnlyLocal() && !$client->getAddress()->isLocal()) $write = false;
                        CloudLogger::get()->debug(!$write ? "Can't handle HTTP request from " . $client->getAddress() . "..." : "Handling HTTP request from " . $client->getAddress() . "...");
                        if ($write) $client->write($this->handleRequest($client->getAddress(), $buf));
                        $client->close();
                    } catch (Throwable $exception) {
                        CloudLogger::get()->warn("Received an invalid request from §b" . $client->getAddress() . "§r, ignoring...");
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

    public function getAddress(): Address {
        return $this->address;
    }
}
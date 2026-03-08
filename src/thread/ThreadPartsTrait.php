<?php

namespace pocketcloud\cloud\thread;

use Phar;
use pmmp\thread\Thread as NativeThread;
use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\console\handler\ExceptionHandler;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\console\log\logger\ThreadLogger;
use pocketcloud\cloud\Server;
use pocketcloud\cloud\util\loader\IClassLoader;
use pocketcloud\cloud\util\PathUtils;
use ReflectionClass;
use Throwable;
use const pocketcloud\CLOUD_PATH;
use const pocketcloud\CRASHES_PATH;
use const pocketcloud\IS_PHAR;
use const pocketcloud\STORAGE_PATH;
use const pocketcloud\TEMPLATES_PATH;
use const pocketcloud\VENDOR_AUTOLOAD_PATH;

trait ThreadPartsTrait {

    protected bool $alive = false;
    private ?string $composerAutoloadPath = null;
    private ?ThreadSafeArray $autoLoaders = null;
    protected ?ThreadLogger $logger = null;

    protected int $exitStatus = 0;
    protected ?string $exitMessage = null;

    public function setClassLoaders(?array $customAutoLoaders = null): void {
        $this->composerAutoloadPath = VENDOR_AUTOLOAD_PATH;
        $normalClassLoader = Server::getInstance()->getClassLoader();

        if ($this->autoLoaders === null) $this->autoLoaders = new ThreadSafeArray();
        if ($customAutoLoaders === null) $customAutoLoaders = [$normalClassLoader];

        foreach ($customAutoLoaders as $customAutoLoader) {
            if (is_subclass_of($customAutoLoader, IClassLoader::class)) $this->autoLoaders[] = $customAutoLoader;
        }
    }

    public function registerClassLoaders(): void {
        if ($this->composerAutoloadPath !== null) require_once $this->composerAutoloadPath;
        if ($this->autoLoaders !== null) foreach ($this->autoLoaders as $autoLoader) {
            if ($autoLoader instanceof IClassLoader) $autoLoader->init();
        }

        define("pocketcloud\VENDOR_AUTOLOAD_PATH", $this->composerAutoloadPath);
        define("pocketcloud\IS_PHAR", Phar::running() !== "");
        define("pocketcloud\SOURCE_PATH", __DIR__ . "/");

        define("pocketcloud\CLOUD_PATH", (IS_PHAR ?
            str_replace("phar://", "", dirname(__DIR__, 3) . "/") :
            dirname(__DIR__, 2) . "/"
        ));

        define("pocketcloud\STORAGE_PATH", PathUtils::join(CLOUD_PATH, "storage") . "/");
        define("pocketcloud\CRASHES_PATH", PathUtils::join(STORAGE_PATH, "crashes") . "/");
        define("pocketcloud\SERVER_CRASHES_PATH", PathUtils::join(CRASHES_PATH, "servers") . "/");
        define("pocketcloud\BINARIES_PATH", PathUtils::join(STORAGE_PATH, "binaries") . "/");
        define("pocketcloud\LIBRARIES_PATH", PathUtils::join(STORAGE_PATH, "libraries") . "/");
        define("pocketcloud\PLUGINS_PATH", PathUtils::join(STORAGE_PATH, "plugins") . "/");
        define("pocketcloud\SOFTWARE_PATH", PathUtils::join(STORAGE_PATH, "software") . "/");
        define("pocketcloud\IN_GAME_PATH", PathUtils::join(STORAGE_PATH, "inGame") . "/");
        define("pocketcloud\STATIC_SERVERS_PATH", PathUtils::join(STORAGE_PATH, "staticServers") . "/");
        define("pocketcloud\LOG_PATH", PathUtils::join(STORAGE_PATH, "cloud.log"));
        define("pocketcloud\TEMP_PATH", PathUtils::join(CLOUD_PATH, "tmp") . "/");
        define("pocketcloud\TEMPLATES_PATH", PathUtils::join(CLOUD_PATH, "templates") . "/");
        define("pocketcloud\GLOBAL_TEMPLATES_PATH", PathUtils::join(TEMPLATES_PATH, "global") . "/");
        define("pocketcloud\SERVER_GROUPS_PATH", PathUtils::join(CLOUD_PATH, "groups") . "/");
        define("pocketcloud\FIRST_RUN", !file_exists(STORAGE_PATH . "config.json"));
    }

    public function start(int $options = NativeThread::INHERIT_NONE): bool {
        $this->setClassLoaders();

        $this->logger = new ThreadLogger($buffer = new ThreadSafeArray(), Server::getInstance()->getSleeperHandler()->addNotifier(function () use($buffer): void {
            while (($logEntry = $buffer->shift()) !== null) {
                CloudLogger::get()->echo($logEntry);
            }
        }));

        ThreadManager::getInstance()->add($this);
        return parent::start($options);
    }

    final public function run(): void {
        error_reporting(-1);
        $this->registerClassLoaders();
        if ($this->logger !== null) CloudLogger::set($this->logger);

        ExceptionHandler::setErrorHandler();

        set_exception_handler($this->handleException(...));
        register_shutdown_function($this->handleShutdown(...));

        $this->alive = true;

        try {
            $this->onRun();
        } finally {
            $this->alive = false;
            CloudLogger::set(null);
            $this->autoLoaders = null;
            $this->logger = null;
        }
    }

    public function quit(): void {
        $this->synchronized(function (): void {
            $this->alive = false;
            $this->notify();
        });

        if ($this->isStarted() && !$this->isJoined()) $this->join();

        $this->autoLoaders = null;
        $this->logger = null;

        ThreadManager::getInstance()->remove($this);
    }

    abstract protected function onRun(): void;

    public function handleException(Throwable $exception): void {
        $this->synchronized(function () use ($exception): void {
            $this->logger->exception($exception);
            $this->exitStatus = ThreadExitStatus::EXCEPTION;
            $this->exitMessage = $exception->getMessage();
        });
    }

    public function handleShutdown(): void {
        $this->synchronized(function(): void {
            $error = error_get_last();
            if ($error !== null && $this->exitStatus !== ThreadExitStatus::EXCEPTION) {
                $this->exitStatus = ThreadExitStatus::FATAL_ERROR;
                $this->exitMessage = $error["message"] . " in {$error["file"]}:{$error["line"]}";

                $this->logger->error("Fatal error in thread: {}", $this->exitMessage);
            }
        });
    }

    public function isAlive(): bool {
        return $this->alive;
    }

    public function getThreadName(): string {
        return new ReflectionClass($this)->getShortName();
    }
}
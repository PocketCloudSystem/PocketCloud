<?php

namespace pocketcloud\cloud\thread;

use pmmp\thread\Thread as NativeThread;
use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\console\handler\ExceptionHandler;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\console\log\logger\ThreadLogger;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\util\loader\IClassLoader;
use ReflectionClass;
use Throwable;
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
        $normalClassLoader = PocketCloud::getInstance()->getClassLoader();

        if ($this->autoLoaders === null) $this->autoLoaders = new ThreadSafeArray();
        if ($customAutoLoaders === null) $customAutoLoaders = [$normalClassLoader];

        foreach ($customAutoLoaders as $customAutoLoader) {
            if (is_subclass_of($customAutoLoader, IClassLoader::class)) $this->autoLoaders[] = $customAutoLoader;
            else CloudLogger::get()->warn("{} cannot be set as a class loader inside a thread ({}), not inheriting from 'IClassLoader'", $customAutoLoader::class, $this::class);
        }
    }

    public function registerClassLoaders(): void {
        if ($this->composerAutoloadPath !== null) require_once $this->composerAutoloadPath;
        if ($this->autoLoaders !== null) foreach ($this->autoLoaders as $autoLoader) {
            if ($autoLoader instanceof IClassLoader) $autoLoader->init();
        }
    }

    public function start(int $options = NativeThread::INHERIT_NONE): bool {
        $this->setClassLoaders();

        $this->logger = new ThreadLogger($buffer = new ThreadSafeArray(), PocketCloud::getInstance()->getSleeperHandler()->addNotifier(function () use($buffer): void {
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
            CloudLogger::get()->exception($exception);
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

                CloudLogger::get()->error("Fatal error in thread: {}", $this->exitMessage);
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
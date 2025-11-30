<?php

namespace pocketcloud\cloud\thread;

use Phar;
use pocketcloud\cloud\exception\ExceptionHandler;
use pocketcloud\cloud\loader\ClassLoader;
use pocketcloud\cloud\PocketCloud;
use pmmp\thread\Thread as NativeThread;
use pocketcloud\cloud\terminal\log\CloudLogger;
use ReflectionClass;

abstract class Thread extends NativeThread {

    private bool $running = false;
    private ?ClassLoader $classLoader = null;

    public function start(int $options = self::INHERIT_NONE): bool {
        $this->setClassLoader(PocketCloud::getInstance()->getClassLoader());
        $this->running = true;
        ThreadManager::getInstance()->add($this);
        return parent::start($options);
    }

    public function run(): void {
        $this->registerClassLoader();
        error_reporting(-1);
        CloudLogger::set(CloudLogger::temp(true));
        ExceptionHandler::set();
        $this->onRun();
        ThreadManager::getInstance()->remove($this);
    }

    public function quit(): void {
        $this->running = false;
    }

    abstract public function onRun(): void;

    public function registerClassLoader(): void {
        define("IS_PHAR", Phar::running() !== "");

        if (IS_PHAR) {
            define("CLOUD_PATH", str_replace("phar://", "", dirname(__DIR__, 5) . DIRECTORY_SEPARATOR));
        } else {
            define("CLOUD_PATH", dirname(__DIR__, 4) . DIRECTORY_SEPARATOR);
        }

        define("SOURCE_PATH", __DIR__ . "/");
        define("STORAGE_PATH", CLOUD_PATH . "storage/");
        define("CRASH_PATH", CLOUD_PATH . "storage/crashes/");
        define("LIBRARY_PATH", STORAGE_PATH . "libraries/");
        define("PLUGINS_PATH", STORAGE_PATH . "plugins/");
        define("SERVER_PLUGINS_PATH", STORAGE_PATH . "plugins/server/");
        define("PROXY_PLUGINS_PATH", STORAGE_PATH . "plugins/proxy/");
        define("CLOUD_PLUGINS_PATH", STORAGE_PATH . "plugins/cloud/");
        define("SOFTWARE_PATH", STORAGE_PATH . "software/");
        define("IN_GAME_PATH", STORAGE_PATH . "inGame/");
        define("WEB_PATH", STORAGE_PATH . "web/");
        define("LOG_PATH", STORAGE_PATH . "cloud.log");
        define("TEMP_PATH", CLOUD_PATH . "tmp/");
        define("TEMPLATES_PATH", CLOUD_PATH . "templates/");
        define("SERVER_GROUPS_PATH", CLOUD_PATH . "groups/");
        define("FIRST_RUN", !file_exists(STORAGE_PATH));

        $this->classLoader?->init();
    }

    public function setClassLoader(ClassLoader $classLoader): void {
        $this->classLoader = $classLoader;
    }

    public function isRunning(): bool {
        return $this->running;
    }

    public function getThreadName(): string {
        return new ReflectionClass($this)->getShortName();
    }
}
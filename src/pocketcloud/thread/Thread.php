<?php

namespace pocketcloud\thread;

use pocketcloud\console\log\Logger;
use pocketcloud\util\CloudLogger;
use pocketcloud\util\ExceptionHandler;
use pmmp\thread\Thread as NativeThread;

abstract class Thread extends NativeThread {

    private bool $running = false;

    public function start(int $options = self::INHERIT_NONE): bool {
        $this->running = true;
        ThreadManager::getInstance()->add($this);
        return parent::start($options);
    }

    public function run(): void {
        $this->registerClassLoader();
        error_reporting(-1);
        CloudLogger::set(new Logger(LOG_PATH, true, true));
        ExceptionHandler::set();
        $this->onRun();
        ThreadManager::getInstance()->remove($this);
    }

    public function quit() {
        $this->running = false;
    }

    abstract public function onRun(): void;

    public function registerClassLoader() {
        if (\Phar::running()) {
            define("CLOUD_PATH", str_replace("phar://", "", dirname(__DIR__, 4) . DIRECTORY_SEPARATOR));
        } else {
            define("CLOUD_PATH", dirname(__DIR__, 3) . DIRECTORY_SEPARATOR);
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
        define("LOG_PATH", STORAGE_PATH . "cloud.log");
        define("TEMP_PATH", CLOUD_PATH . "tmp/");
        define("TEMPLATES_PATH", CLOUD_PATH . "templates/");
        define("FIRST_RUN", !file_exists(STORAGE_PATH));

        spl_autoload_register(function (string $class): void {
            if (str_starts_with($class, "pocketcloud\\")) {
                if (!class_exists($class)) require str_replace("\\", DIRECTORY_SEPARATOR, __DIR__ . DIRECTORY_SEPARATOR . ".." . DIRECTORY_SEPARATOR . substr($class, strlen("pocketcloud\\"), strlen($class))) . ".php";
            } else if (str_starts_with($class, "pocketmine\\snooze\\")) {
                if (!class_exists($class)) require LIBRARY_PATH . "snooze/" . substr($class, strlen("pocketmine\\snooze\\"), strlen($class)) . ".php";
            }
        });
    }

    public function isRunning(): bool {
        return $this->running;
    }
}
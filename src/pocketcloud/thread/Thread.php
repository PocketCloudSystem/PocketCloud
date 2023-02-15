<?php

namespace pocketcloud\thread;

use pocketcloud\exception\ExceptionHandler;

abstract class Thread extends \Thread {

    private bool $running = false;

    public function start(int $options = PTHREADS_INHERIT_NONE): bool {
        $this->running = true;
        return parent::start($options);
    }

    public function quit() {
        $this->running = false;

        if(!$this->isJoined()){
            $this->notify();
            $this->join();
        }
    }

    public function registerClassLoader() {
        spl_autoload_register(function (string $class): void {
            if (str_starts_with($class, "pocketcloud\\")) {
                if (!class_exists($class)) require str_replace("\\", DIRECTORY_SEPARATOR, __DIR__ . DIRECTORY_SEPARATOR . ".." . DIRECTORY_SEPARATOR . substr($class, strlen("pocketcloud\\"), strlen($class))) . ".php";
            }
        });

        ExceptionHandler::set();

        if (\Phar::running()) {
            define("CLOUD_PATH", str_replace("phar://", "", dirname(__DIR__, 4) . DIRECTORY_SEPARATOR));
        } else {
            define("CLOUD_PATH", dirname(__DIR__, 3) . DIRECTORY_SEPARATOR);
        }

        define("SOURCE_PATH", __DIR__ . "/");
        define("STORAGE_PATH", CLOUD_PATH . "storage/");
        define("CRASH_PATH", CLOUD_PATH . "storage/crashes/");
        define("PLUGINS_PATH", CLOUD_PATH . "storage/plugins/");
        define("SERVER_PLUGINS_PATH", CLOUD_PATH . "storage/plugins/server/");
        define("PROXY_PLUGINS_PATH", CLOUD_PATH . "storage/plugins/proxy/");
        define("CLOUD_PLUGINS_PATH", CLOUD_PATH . "storage/plugins/cloud/");
        define("SOFTWARE_PATH", CLOUD_PATH . "storage/software/");
        define("IN_GAME_PATH", CLOUD_PATH . "storage/inGame/");
        define("LOG_PATH", CLOUD_PATH . "storage/cloud.log");
        define("TEMP_PATH", CLOUD_PATH . "tmp/");
        define("TEMPLATES_PATH", CLOUD_PATH . "templates/");
    }

    public function isRunning(): bool {
        return $this->running;
    }
}
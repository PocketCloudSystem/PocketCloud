<?php

namespace pocketcloud\cloud\server\util;

use Closure;
use InvalidArgumentException;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\util\promise\Promise;
use pocketcloud\cloud\util\trait\EnumTrait;
use pocketcloud\cloud\util\Utils;
use const pocketcloud\BINARIES_PATH;
use const pocketcloud\SOFTWARE_PATH;

/**
 * @method static ServerStartMethod TMUX()
 * @method static ServerStartMethod SCREEN()
 * @method static ServerStartMethod PROC()
 */
final class ServerStartMethod {
    use EnumTrait;

    private static ?self $current = null;

    protected static function init(): void {
        self::add(new ServerStartMethod("screen", function (CloudServer $server, string $startCommand): Promise {
            $result = passthru(
                "cd " . $server->getPath() . " && " .
                "screen -dmS " . $server->getName() . " " . $startCommand
            );

            return is_null($result) ? Promise::resolved() : Promise::rejected();
        }));

        self::add(new ServerStartMethod("tmux", function (CloudServer $server, string $startCommand): Promise {
            $result = passthru(
                "cd " . $server->getPath() . " && " .
                "tmux new-session -d -s " . $server->getName() . " bash -c '" . $startCommand . "'"
            );

            return is_null($result) ? Promise::resolved() : Promise::rejected();
        }));

        self::add(new ServerStartMethod("proc", function (CloudServer $server, string $startCommand): Promise {
            $descriptors = [
                0 => ["pipe", "r"],
                1 => ["pipe", "w"],
                2 => ["pipe", "w"],
            ];

            $pipes = [];
            $process = proc_open($startCommand, $descriptors, $pipes, $server->getPath());

            if (!is_resource($process)) return Promise::rejected();

            foreach ($pipes as $pipe) fclose($pipe);

            $status = proc_get_status($process);
            if ($status["running"]) return Promise::resolved();

            proc_close($process);
            return Promise::rejected();
        }));
    }

    public static function add(ServerStartMethod $method): void {
        self::register(mb_strtoupper($method->getName()), $method);
    }

    public static function set(ServerStartMethod $method): void {
        self::$current = $method;
    }

    public static function current(): self {
        if (self::$current === null) throw new InvalidArgumentException("No default server start method found");
        return self::$current;
    }

    public function __construct(
        private readonly string $name,
        private readonly Closure $startHandler
    ) {
        Utils::validateCallbackSignature($this->startHandler, [CloudServer::class, "string"], Promise::class);
    }

    public function startServer(CloudServer $server): Promise {
        return ($this->startHandler)($server, str_replace(
            ["%BINARY_PATH%", "%SOFTWARE_PATH%"],
            [
                BINARIES_PATH . strtolower($server->getTemplate()->getTemplateType()->getName()) . DIRECTORY_SEPARATOR,
                SOFTWARE_PATH
            ], $server->getTemplate()->getTemplateType()->getSoftware()->getStartCommand()
        ));
    }

    public function getName(): string {
        return $this->name;
    }

    public function getStartHandler(): Closure {
        return $this->startHandler;
    }
}
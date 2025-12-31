<?php

namespace pocketcloud\cloud\server\util;

use Closure;
use InvalidArgumentException;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\util\promise\Promise;
use pocketcloud\cloud\util\trait\RegistryTrait;
use pocketcloud\cloud\util\Utils;
use const pocketcloud\BINARIES_PATH;
use const pocketcloud\SOFTWARE_PATH;

/**
 * @method static ServerStartMethod TMUX()
 * @method static ServerStartMethod SCREEN()
 * @method static ServerStartMethod PROC()
 */
final class ServerStartMethod {
    use RegistryTrait;

    private static ?self $current = null;

    protected static function init(): void {
        self::add(new ServerStartMethod("screen", function (CloudServer $server, string $startCommand): Promise {
            $screenName = $server->getName() . "-" . $server->getServerUuid();
            $cmd = "cd " . $server->getPath() . " && screen -dmS $screenName bash -c 'exec $startCommand' && " .
                "screen -ls | grep $screenName | awk -F '.' '{print $1}'";

            exec($cmd, $output, $returnVar);

            if ($returnVar === 0 && isset($output[0])) {
                $screenPid = (int) $output[0];
                $pid = (int) trim(shell_exec("pgrep -P $screenPid"));
                if ($pid > 0) {
                    return Promise::resolved($pid);
                }
            }
            return Promise::rejected();
        }));

        self::add(new ServerStartMethod("tmux", function (CloudServer $server, string $startCommand): Promise {
            $paneName = $server->getName() . "-" . $server->getServerUuid();
            $cmd = "cd " . $server->getPath() . " && " .
                "tmux new-session -d -s $paneName bash -c '" . $startCommand . "' && " .
                "tmux list-panes -t $paneName -F '#{pane_pid}'";

            exec($cmd, $output, $returnVar);

            if ($returnVar === 0) return Promise::resolved((int) $output[0]);
            return Promise::rejected();
        }));

        self::add(new ServerStartMethod("proc", function (CloudServer $server, string $startCommand): Promise {
            $descriptors = [
                fopen("php://temp", "r"),
                fopen("php://temp", "r"),
                fopen("php://temp", "r")
            ];

            $pipes = [];
            $process = proc_open($startCommand, $descriptors, $pipes, $server->getPath());
            if (!is_resource($process)) return Promise::rejected();

            $status = proc_get_status($process);
            if ($status["running"]) return Promise::resolved($status["pid"]);

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
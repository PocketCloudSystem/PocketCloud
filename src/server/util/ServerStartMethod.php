<?php

namespace pocketcloud\cloud\server\util;

use Closure;
use InvalidArgumentException;
use LogicException;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\util\PathUtils;
use pocketcloud\cloud\util\promise\Promise;
use pocketcloud\cloud\util\TerminalUtils;
use pocketcloud\cloud\util\trait\RegistryTrait;
use pocketcloud\cloud\util\Utils;
use ReflectionException;
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

    /**
     * @throws ReflectionException
     */
    protected static function init(): void {
        self::add(new ServerStartMethod("screen", function (CloudServer $server, string $startCommand): Promise {
            $screenName = $server->getName() . "-" . $server->getServerUuid();
            $cmd = "cd " . $server->getPath() . " && screen -dmS $screenName bash -c 'exec $startCommand' && " .
                "screen -ls | grep $screenName | awk -F '.' '{print $1}'";

            exec($cmd, $output, $returnVar);

            if ($returnVar === 0 && isset($output[0])) {
                $screenPid = (int) trim($output[0]);
                $grepOutput = shell_exec("pgrep -P $screenPid");
                if ($grepOutput === null) return Promise::rejected();
                $pid = (int) trim(shell_exec("pgrep -P $screenPid"));
                if ($pid > 0) {
                    return Promise::resolved($pid);
                }
            }
            return Promise::rejected();
        }, fn(): bool => TerminalUtils::checkCommand("screen")));

        self::add(new ServerStartMethod("tmux", function (CloudServer $server, string $startCommand): Promise {
            $paneName = $server->getName() . "-" . $server->getServerUuid();
            $cmd = "cd " . $server->getPath() . " && " .
                "tmux new-session -d -s $paneName bash -c '" . $startCommand . "' && " .
                "tmux list-panes -t $paneName -F '#{pane_pid}'";

            exec($cmd, $output, $returnVar);

            if ($returnVar === 0) return Promise::resolved((int) $output[0]);
            return Promise::rejected();
        }, fn(): bool => TerminalUtils::checkCommand("tmux")));

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
        }, fn(): bool => function_exists("proc_open") && function_exists("proc_get_status") && function_exists("proc_close")));
    }

    public static function add(ServerStartMethod $method): void {
        self::register(mb_strtoupper($method->getName()), $method);
    }

    public static function set(ServerStartMethod $method): void {
        if (!$method->isAvailable()) throw new LogicException("Start method '" . $method->getName() . "' is not available");
        self::$current = $method;
    }

    public static function current(): self {
        if (self::$current === null) throw new InvalidArgumentException("No default server start method found");
        return self::$current;
    }

    /**
     * @throws ReflectionException
     */
    public function __construct(
        private readonly string $name,
        private readonly Closure $startHandler,
        private readonly Closure $checkAvailabilityHandler
    ) {
        Utils::validateCallbackSignature($this->startHandler, [CloudServer::class, "string"], Promise::class);
        Utils::validateCallbackSignature($this->checkAvailabilityHandler, [], "bool");
    }

    public function startServer(CloudServer $server): Promise {
        return ($this->startHandler)($server, str_replace(
            ["%BINARY_PATH%", "%SOFTWARE_PATH%"],
            [
                PathUtils::join(BINARIES_PATH, strtolower($server->getTemplate()->getTemplateType()->getName())) . "/",
                SOFTWARE_PATH
            ], $server->getTemplate()->getTemplateType()->getSoftware()->getStartCommand()
        ));
    }

    public function isAvailable(): bool {
        return ($this->checkAvailabilityHandler)();
    }

    public function getName(): string {
        return $this->name;
    }

    public function getStartHandler(): Closure {
        return $this->startHandler;
    }
}
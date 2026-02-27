<?php

namespace pocketcloud\cloud\server\util;

use Closure;
use InvalidArgumentException;
use LogicException;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\util\benchmark\Benchmark;
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
            Benchmark::startTiming("screen_start");
            $screenName = $server->getName() . "-" . $server->getServerUuid();
            $workingDirectory = $server->getPath();

            $command = "cd " . escapeshellarg($workingDirectory) .
                " && screen -dmS " . escapeshellarg($screenName) .
                " bash -lc " . escapeshellarg("exec " . $startCommand);
            exec($command, $output, $returnVar);

            if ($returnVar !== 0) {
                Benchmark::stopTiming("screen_start");
                return Promise::rejected();
            }

            $screenPid = null;
            for ($i = 0; $i < 15; $i++) {
                $pidOutput = shell_exec("screen -ls | grep -F " . escapeshellarg($screenName) . " | awk -F '.' '{print \$1}' | head -n1");
                if (is_string($pidOutput) && ($parsed = (int) trim($pidOutput)) > 0) {
                    $screenPid = $parsed;
                    break;
                }

                usleep(100 * 1000);
            }

            if ($screenPid === null) {
                Benchmark::stopTiming("screen_start");
                return Promise::rejected();
            }

            for ($i = 0; $i < 15; $i++) {
                $childOutput = shell_exec("pgrep -P " . $screenPid . " | head -n1");
                if (is_string($childOutput) && ($childPid = (int) trim($childOutput)) > 0) {
                    Benchmark::stopTiming("screen_start");
                    return Promise::resolved($childPid);
                }

                usleep(100 * 1000);
            }

            Benchmark::stopTiming("screen_start");
            return Promise::rejected();
        }, fn(): bool => TerminalUtils::checkCommand("screen")));

        self::add(new ServerStartMethod("tmux", function (CloudServer $server, string $startCommand): Promise {
            Benchmark::startTiming("tmux_start");
            $paneName = $server->getName() . "-" . $server->getServerUuid();
            $workingDirectory = $server->getPath();

            $command = "cd " . escapeshellarg($workingDirectory) .
                " && tmux new-session -d -s " . escapeshellarg($paneName) .
                " bash -lc " . escapeshellarg("exec " . $startCommand);
            exec($command, $output, $returnVar);

            if ($returnVar !== 0) {
                Benchmark::stopTiming("tmux_start");
                return Promise::rejected();
            }

            for ($i = 0; $i < 15; $i++) {
                $panePidOutput = shell_exec("tmux list-panes -t " . escapeshellarg($paneName) . " -F '#{pane_pid}' 2>/dev/null | head -n1");
                if (is_string($panePidOutput) && ($panePid = (int) trim($panePidOutput)) > 0) {
                    Benchmark::stopTiming("tmux_start");
                    return Promise::resolved($panePid);
                }

                usleep(100 * 1000);
            }

            Benchmark::stopTiming("tmux_start");
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
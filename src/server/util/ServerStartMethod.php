<?php

namespace pocketcloud\cloud\server\util;

use Closure;
use InvalidArgumentException;
use LogicException;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\util\benchmark\Benchmark;
use pocketcloud\cloud\util\PathUtils;
use pocketcloud\cloud\util\ProcessUtils;
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
        self::add(new ServerStartMethod(
            "screen",
            function (CloudServer $server, string $startCommand): Promise {
                $screenName = $server->getName() . "-" . $server->getServerUuid();
                $workingDirectory = $server->getPath();
                $command = "cd " . escapeshellarg($workingDirectory) .
                    " && screen -dmS " . escapeshellarg($screenName) .
                    " bash -lc " . escapeshellarg("exec " . $startCommand);
                exec($command, $output, $returnVar);
                if ($returnVar !== 0) return Promise::rejected();
                return Promise::resolved();
            },
            fn(): bool => TerminalUtils::checkCommand("screen"),
            function (array $servers): bool {
                $commands = [];
                foreach ($servers as $server) {
                    $screenName = $server->getName() . "-" . $server->getServerUuid();
                    $startCommand = str_replace(
                        ["%BINARY_PATH%", "%SOFTWARE_PATH%"],
                        [PathUtils::join(BINARIES_PATH, strtolower($server->getTemplate()->getTemplateType()->getName())) . "/", SOFTWARE_PATH],
                        $server->getTemplate()->getTemplateType()->getSoftware()->getStartCommand()
                    );
                    $commands[] = "cd " . escapeshellarg($server->getPath()) .
                        " && screen -dmS " . escapeshellarg($screenName) .
                        " bash -lc " . escapeshellarg("exec " . $startCommand);
                }

                exec(implode(" ; ", $commands), $output, $returnVar);
                if ($returnVar !== 0) return false;
                return true;
            },
            function (CloudServer $server): ?int {
                $screenName = $server->getName() . "-" . $server->getServerUuid();
                $pidOutput = ProcessUtils::executeWithTimeout("screen -ls | grep -F " . escapeshellarg($screenName) . " | awk -F '.' '{print \$1}' | head -n1", 0.5);
                if (!is_string($pidOutput) || ($screenPid = (int) trim($pidOutput)) <= 0) return null;
                $childOutput = ProcessUtils::executeWithTimeout("pgrep -P " . $screenPid . " | head -n1", 0.5);
                if (is_string($childOutput) && ($childPid = (int) trim($childOutput)) > 0) return $childPid;
                return null;
            }
        ));

        self::add(new ServerStartMethod(
            "tmux",
            function (CloudServer $server, string $startCommand): Promise {
                $paneName = $server->getName() . "-" . $server->getServerUuid();
                $workingDirectory = $server->getPath();
                $command = "cd " . escapeshellarg($workingDirectory) .
                    " && tmux new-session -d -s " . escapeshellarg($paneName) .
                    " bash -lc " . escapeshellarg("exec " . $startCommand);
                exec($command, $output, $returnVar);
                if ($returnVar !== 0) return Promise::rejected();
                return Promise::resolved();
            },
            fn(): bool => TerminalUtils::checkCommand("tmux"),
            function (array $servers): bool {
                $commands = [];
                foreach ($servers as $server) {
                    $paneName = $server->getName() . "-" . $server->getServerUuid();
                    $startCommand = str_replace(
                        ["%BINARY_PATH%", "%SOFTWARE_PATH%"],
                        [PathUtils::join(BINARIES_PATH, strtolower($server->getTemplate()->getTemplateType()->getName())) . "/", SOFTWARE_PATH],
                        $server->getTemplate()->getTemplateType()->getSoftware()->getStartCommand()
                    );
                    $commands[] = "cd " . escapeshellarg($server->getPath()) .
                        " && tmux new-session -d -s " . escapeshellarg($paneName) .
                        " bash -lc " . escapeshellarg("exec " . $startCommand);
                }

                exec(implode(" ; ", $commands), $output, $returnVar);
                if ($returnVar !== 0) return false;
                return true;
            },
            function (CloudServer $server): ?int {
                $paneName = $server->getName() . "-" . $server->getServerUuid();
                $output = ProcessUtils::executeWithTimeout("tmux list-panes -t " . escapeshellarg($paneName) . " -F '#{pane_pid}' 2>/dev/null | head -n1", 0.5);
                if (is_string($output) && ($pid = (int) trim($output)) > 0) return $pid;
                return null;
            },
        ));

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
     * @param string $name
     * @param Closure(CloudServer $server): Promise $startHandler
     * @param Closure(): bool $checkAvailabilityHandler
     * @param Closure(array<CloudServer> $servers): bool|null $multiStartHandler
     * @param Closure(CloudServer $server): ?int|null $pidLookupHandler
     */
    public function __construct(
        private readonly string $name,
        private readonly Closure $startHandler,
        private readonly Closure $checkAvailabilityHandler,
        private readonly ?Closure $multiStartHandler = null,
        private readonly ?Closure $pidLookupHandler = null
    ) {}

    public function multiStartServer(array $servers): bool {
        if ($this->multiStartHandler === null) return false;
        Benchmark::startTiming("server_boot_multi");
        $res = ($this->multiStartHandler)($servers);
        Benchmark::stopTiming("server_boot_multi");
        return $res;
    }

    public function startServer(CloudServer $server): Promise {
        Benchmark::startTiming("server_boot");
        $res = ($this->startHandler)($server, str_replace(
            ["%BINARY_PATH%", "%SOFTWARE_PATH%"],
            [
                PathUtils::join(BINARIES_PATH, strtolower($server->getTemplate()->getTemplateType()->getName())) . "/",
                SOFTWARE_PATH
            ], $server->getTemplate()->getTemplateType()->getSoftware()->getStartCommand()
        ));
        Benchmark::stopTiming("server_boot");
        return $res;
    }

    public function lookupPid(CloudServer $server): ?int {
        if ($this->pidLookupHandler === null) return null;
        return ($this->pidLookupHandler)($server);
    }

    public function supportsMultiStart(): bool {
        return $this->multiStartHandler !== null;
    }

    public function hasPidLookup(): bool {
        return $this->pidLookupHandler !== null;
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

<?php

namespace pocketcloud\cloud\util;

use InvalidArgumentException;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\player\CloudPlayerManager;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\thread\ThreadManager;
use pocketmine\utils\AssumptionFailedError;
use Ramsey\Uuid\Uuid;
use Ramsey\Uuid\UuidInterface;
use Random\RandomException;
use ReflectionException;
use ReflectionFunction;
use ReflectionMethod;
use ReflectionNamedType;
use RuntimeException;

final class Utils {

    private static ?UuidInterface $machineUniqueId = null;

    public static function containKeys(array $array, string|int ...$keys): bool {
        return array_all($keys, fn(string|int $key) => isset($array[$key]));
    }

    public static function hasAllKeys(array $array, array $defaultArray): bool {
        foreach ($defaultArray as $key => $value) {
            if (!isset($array[$key])) return false;
            if (is_array($value) && !empty($value)) {
                if (!is_array($array[$key])) return false;
                if (!self::hasAllKeys($array[$key], $value)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static function fillMissingKeys(array &$array, array $defaultArray, ?int &$affectedKeys = 0): array {
        foreach ($defaultArray as $key => $defaultValue) {
            if (!isset($array[$key])) {
                $affectedKeys++;
                $array[$key] = $defaultValue;
            } else if (is_array($defaultValue) && is_array($array[$key])) {
                $affectedKeys++;
                $array[$key] = self::fillMissingKeys($array[$key], $defaultValue, $affectedKeys);
            } else if (is_array($defaultValue) && !is_array($array[$key])) {
                $affectedKeys++;
                $array[$key] = $defaultValue;
            }
        }

        return $array;
    }

    public static function generateString(int $length = 5, bool $uppercase = true, bool $lowercase = false, bool $numbers = true, bool $specialCharacters = false): string {
        $pool = "";
        if ($uppercase) $pool .= "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        if ($lowercase) $pool .= "abcdefghijklmnopqrstuvwxyz";
        if ($numbers) $pool .= "0123456789";
        if ($specialCharacters) $pool .= "!@#$%^&*()-_=+[]{}<>?";
        if ($pool === "") throw new InvalidArgumentException("Character pool must not be empty");

        $result = "";
        $maxIndex = strlen($pool) - 1;

        for ($i = 0; $i < $length; $i++) {
            try {
                $result .= $pool[random_int(0, $maxIndex)];
            } catch (RandomException $e) {
                CloudLogger::get()->exception($e);
                $result .= $pool[mt_rand(0, $maxIndex)];
            }
        }

        return $result;
    }

    /** @author PMMP https://github.com/pmmp/PocketMine-MP/blob/50430762cf4a93a19a5621f9d0157e8009a8c15c/src/command/utils/CommandStringHelper.php#L48 */
    public static function parseQuoteAware(string $input): array {
        $args = [];
        preg_match_all('/"((?:\\\\.|[^\\\\"])*)"|(\S+)/u', $input, $matches);
        foreach ($matches[0] as $k => $_) {
            for ($i = 1; $i <= 2; ++$i) {
                if ($matches[$i][$k] !== "") {
                    $match = $matches[$i][$k];
                    $args[] = preg_replace('/\\\\([\\\\"])/u', '$1', $match) ?? throw new RuntimeException(preg_last_error_msg());
                    break;
                }
            }
        }

        return $args;
    }

    /**
     * @throws ReflectionException
     */
    public static function validateCallbackSignature(callable $callback, array $expectedParameters, ?string $expectedReturnType = null): void {
        $ref = is_array($callback) ? new ReflectionMethod($callback[0], $callback[1]) : new ReflectionFunction($callback);

        $params = $ref->getParameters();
        if (count($params) !== count($expectedParameters)) throw new InvalidArgumentException("Invalid parameter count");

        foreach ($params as $i => $param) {
            $expected = $expectedParameters[$i];
            $type = $param->getType();

            if (!$type instanceof ReflectionNamedType) throw new InvalidArgumentException("Parameter #$i must have a type");
            if ($type->getName() !== $expected) throw new InvalidArgumentException("Parameter #$i must be of type $expected");
        }

        if ($expectedReturnType !== null) {
            $returnType = $ref->getReturnType();
            if (!$returnType instanceof ReflectionNamedType || $returnType->getName() !== $expectedReturnType) {
                throw new InvalidArgumentException("Invalid return type");
            }
        }
    }

    /** @author PMMP https://github.com/pmmp/PocketMine-MP/blob/50430762cf4a93a19a5621f9d0157e8009a8c15c/src/utils/Utils.php#L200 */
    public static function getMachineUniqueId(string $extra = ""): UuidInterface {
        if (self::$machineUniqueId !== null && $extra === "") return self::$machineUniqueId;
        $machine = php_uname();
        $cpuinfo = @file("/proc/cpuinfo");
        if ($cpuinfo !== false) {
            $cpuinfoLines = preg_grep("/(model name|Processor|Serial)/", $cpuinfo);
            if ($cpuinfoLines === false) throw new AssumptionFailedError("Pattern is valid, so this shouldn't fail ...");
            $machine .= implode("", $cpuinfoLines);
        }

        $machine .= sys_get_temp_dir();
        $machine .= $extra;

        if (file_exists("/etc/machine-id")) {
            $machine .= file_get_contents("/etc/machine-id");
        } else {
            @exec("ifconfig 2>/dev/null", $mac);
            $mac = implode("\n", $mac);
            if (preg_match_all("#HWaddr[ \t]{1,}([0-9a-f:]{17})#", $mac, $matches) > 0) {
                foreach ($matches[1] as $i => $v) {
                    if ($v === "00:00:00:00:00:00") {
                        unset($matches[1][$i]);
                    }
                }

                $machine .= implode(" ", $matches[1]); //Mac Addresses
            }
        }

        $data = $machine . PHP_MAXPATHLEN;
        $data .= PHP_INT_MAX;
        $data .= PHP_INT_SIZE;
        $data .= get_current_user();
        foreach (get_loaded_extensions() as $ext) {
            $data .= $ext . ":" . phpversion($ext);
        }

        //TODO: use of NIL as namespace is a hack; it works for now, but we should have a proper namespace UUID
        $uuid = Uuid::uuid3(Uuid::NIL, $data);

        if ($extra === "") self::$machineUniqueId = $uuid;

        return $uuid;
    }

    public static function readCloudPerformanceStatus(): array {
        $knownThreadCount = count($threads = ThreadManager::getInstance()->getAll()) + 1; // +1 -> main thread;
        [$vmRssSize, $vmRssPeak, $vmSize, $threadCount] = array_values(ProcessUtils::getProcessStatus());
        [$currentTPS, $avgTPS, $tickUsage] = array_values(PocketCloud::getInstance()->getTickPerformanceMetrics());
        $memoryLimit = ProcessUtils::getMemoryLimit();
        [$serverCount, $playerCount] = [count(CloudServerManager::getInstance()->getAll()), count(CloudPlayerManager::getInstance()->getAll())];

        return [
            "uptime" => PocketCloud::getInstance()->getUptime(),
            "known_thread_count" => $knownThreadCount,
            "os_thread_count" => $threadCount,
            "threads" => $threads,
            "vm_rss"  => $vmRssSize,
            "vm_rss_peak" => $vmRssPeak,
            "vm_size" => $vmSize,
            "memory_limit" => $memoryLimit,
            "cpu_usage" => ProcessUtils::getCpuUsage(),
            "current_tps" => $currentTPS,
            "average_tps" => $avgTPS,
            "tick_usage" => $tickUsage,
            "server_count" => $serverCount,
            "player_count" => $playerCount
        ];
    }
}
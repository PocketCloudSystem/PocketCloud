<?php

namespace pocketcloud\cloud\util;

use Exception;
use InvalidArgumentException;
use pocketcloud\cloud\console\log\CloudLogger;
use Random\RandomException;
use ReflectionException;
use ReflectionFunction;
use ReflectionMethod;
use ReflectionNamedType;

final class Utils {

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
                    $args[] = preg_replace('/\\\\([\\\\"])/u', '$1', $match) ?? throw new Exception(preg_last_error_msg());
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
            if ($type->getName() !== $expected) throw new InvalidArgumentException("Parameter #$i must be of type {$expected}");
        }

        if ($expectedReturnType !== null) {
            $returnType = $ref->getReturnType();

            if (!$returnType instanceof ReflectionNamedType || $returnType->getName() !== $expectedReturnType) {
                throw new InvalidArgumentException("Invalid return type");
            }
        }
    }
}
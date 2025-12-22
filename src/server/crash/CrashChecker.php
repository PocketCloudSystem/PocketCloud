<?php

namespace pocketcloud\cloud\server\crash;

use pocketcloud\cloud\server\CloudServer;
use const pocketcloud\SERVER_CRASHES_PATH;

/**
 * This one does only for servers which use PocketMine-MP or have the same way saving crashdumps as PMMP, meaning that WaterdogPE is not supported with this
 */
final class CrashChecker {

    private const string CRASH_DUMP_DIR = "crashdumps/";
    private const string LOG_EXTENSION = "log";
    private const int CRASH_TIME_THRESHOLD = 60;
    private const int ERROR_MESSAGE_MAX_LENGTH = 256;
    
    public static function checkCrashed(CloudServer $server, ?array &$crashData = null): bool {
        $crashDumpPath = $server->getPath() . self::CRASH_DUMP_DIR;
        if (!file_exists($crashDumpPath)) return false;

        $recentCrashFile = self::findRecentCrashFile($crashDumpPath);
        if ($recentCrashFile === null) return false;

        $reader = new CrashDumpReader($recentCrashFile);
        if (!$reader->hasRead()) return false;

        $crashData = $reader->getData();
        return true;
    }
    
    private static function findRecentCrashFile(string $crashDumpPath): ?string {
        $files = array_diff(scandir($crashDumpPath), [".", ".."]);
        
        foreach ($files as $file) {
            $filePath = $crashDumpPath . $file;
            if (!self::isRecentLogFile($filePath)) continue;
            return $filePath;
        }

        return null;
    }

    private static function isRecentLogFile(string $filePath): bool {
        if (pathinfo($filePath, PATHINFO_EXTENSION) !== self::LOG_EXTENSION) return false;
        $fileAge = time() - filectime($filePath);
        return $fileAge <= self::CRASH_TIME_THRESHOLD;
    }
    
    public static function writeCrashFile(CloudServer $server, array $crashData): void {
        $formattedData = self::formatCrashData($crashData);
        $content = self::buildCrashFileContent($formattedData);
        $fileName = self::generateCrashFileName($server->getName());

        file_put_contents(SERVER_CRASHES_PATH . $fileName, $content);
    }
    
    private static function formatCrashData(array $crashData): array {
        return [
            "Exception Class" => $crashData["error"]["type"],
            "Error" => self::truncateErrorMessage($crashData["error"]["message"] ?? "Unknown error"),
            "File" => $crashData["error"]["file"],
            "Line" => $crashData["error"]["line"],
            "Plugin involved" => $crashData["plugin_involvement"],
            "Plugin" => $crashData["plugin"] ?? "?",
            "Code" => "\n" . self::formatCodeLines($crashData["code"]) . "\n",
            "Trace" => "\n" . implode("\n", $crashData["trace"]),
            "Server Time" => self::formatServerTime($crashData["time"]),
            "Server Uptime" => $crashData["uptime"],
            "Server Git Commit" => $crashData["general"]["git"]
        ];
    }

    private static function formatCodeLines(array $code): string {
        $formattedLines = [];

        foreach ($code as $lineNumber => $codeLine) {
            $formattedLines[] = "[$lineNumber] $codeLine";
        }

        return implode("\n", $formattedLines);
    }

    private static function truncateErrorMessage(string $message): string {
        return substr($message, 0, self::ERROR_MESSAGE_MAX_LENGTH);
    }

    private static function formatServerTime(int $timestamp): string {
        return date("d.m.Y (l): H:i:s [e]", $timestamp);
    }

    private static function buildCrashFileContent(array $data): string {
        $lines = [];

        foreach ($data as $key => $value) {
            $lines[] = "$key: $value";
        }

        return implode("\n", $lines) . "\n";
    }

    private static function generateCrashFileName(string $serverName): string {
        $timestamp = date("Y-m-d_H:i:s");
        return "{$serverName}_$timestamp.log";
    }
}
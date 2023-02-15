<?php

namespace pocketcloud\server\crash;

use pocketcloud\server\CloudServer;

class CrashChecker {

    public static function checkCrashed(CloudServer $server, ?array &$crashData = null): bool {
        $filePath = "";

        if (!file_exists($server->getPath() . "crashdumps/")) return false;
        foreach (array_diff(scandir($server->getPath() . "crashdumps/"), [".", ".."]) as $file) {
            if (pathinfo($server->getPath() . "crashdumps/" . $file, PATHINFO_EXTENSION) == "log") {
                if ((time() - filectime($server->getPath() . "crashdumps/" . $file)) <= 60) {
                    $filePath = $server->getPath() . "crashdumps/" . $file;
                    break;
                }
            }
        }

        if ($filePath == "") return false;
        $reader = new CrashDumpReader($filePath);
        if (!$reader->hasRead()) return false;
        $crashData = $reader->getData();
        return true;
    }

    public static function writeCrashFile(CloudServer $server, array $crashData) {
        $codeData = [];
        foreach($crashData["code"] as $line => $code) $codeData[] = "[" . $line . "] " . $code;
        $data = [
            "Exception Class" => $crashData["error"]["type"],
            "Error" => substr($crashData["error"]["message"] ?? "Unknown error", 0, 256),
            "File" => $crashData["error"]["file"],
            "Line" => $crashData["error"]["line"],
            "Plugin involved" => $crashData["plugin_involvement"],
            "Plugin" => ($crashData["plugin"] ?? "?"),
            "Code" => "\n" . implode("\n", $codeData) . "\n",
            "Trace" => "\n" . implode("\n", $crashData["trace"]),
            "Server Time" => date("d.m.Y (l): H:i:s [e]", (int) $crashData["time"]),
            "Server Uptime" => $crashData["uptime"],
            "Server Git Commit" => $crashData["general"]["git"],
            "PHP Version" => phpversion().((function_exists('opcache_get_status') && ($opcacheStatus = opcache_get_status(false)) !== false && ($opcacheStatus["jit"]["on"] ?? false)) ? " (JIT enabled)" : " (JIT disabled)")
        ];

        $content = "";
        foreach ($data as $key => $value) $content .= $key . ": " . $value . "\n";

        file_put_contents(CRASH_PATH . $server->getName() . "_" . date("Y-m-d_H:i:s") . ".log", $content);
    }
}
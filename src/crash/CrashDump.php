<?php

namespace pocketcloud\cloud\crash;

use JsonException;
use pocketcloud\cloud\util\ErrorUtils;
use pocketcloud\cloud\util\FormatUtils;
use pocketcloud\cloud\util\PathUtils;
use ReflectionClass;
use ReflectionException;
use RuntimeException;
use const pocketcloud\CRASHES_PATH;

final class CrashDump {

    private mixed $cFile = null;
    private string $type;
    private string $message;
    private string $file;
    private int $line;
    private int $code;
    private array $trace;
    private string $encodedData;

    /**
     * @throws JsonException
     */
    public function __construct(private readonly array $crashData) {
        $this->type = $this->crashData["type"];
        $this->message = $this->crashData["message"];
        $this->file = $this->crashData["file"];
        $this->line = $this->crashData["line"];
        $this->code = $this->crashData["code"];
        $this->trace = ErrorUtils::getType($this->crashData["type"]) == E_ERROR ? [] : $this->crashData["trace"];
        $this->encodedData = zlib_encode(json_encode([
            "type" => $this->type,
            "message" => $this->message,
            "file" => $this->file,
            "line" => $this->line,
            "code" => $this->code,
            "trace" => $this->trace
        ], JSON_UNESCAPED_SLASHES | JSON_THROW_ON_ERROR), ZLIB_ENCODING_DEFLATE, 6);
    }

    public function create(): string {
        $filePath = CRASHES_PATH . date("Y-m-d_H:i:s_e") . ".log";
        $this->cFile = fopen($filePath, "w");

        $this->addLine("Type: " . $this->type);
        $this->addLine("Message: " . $this->message);
        $this->addLine("File: " . $this->file);
        $this->addLine("Line: " . $this->line);
        $this->addLine("Code: " . $this->code);
        $this->addLine("Trace: ");
        $i = 0;
        foreach ($this->trace as $trace) {
            $i++;
            $args = implode(", ", array_map(function(mixed $argument): string {
                if (is_object($argument)) {
                    try {
                        return new ReflectionClass($argument)->getShortName();
                    } catch (ReflectionException) {
                        return get_class($argument);
                    }
                } else if (is_array($argument)) {
                    return "array(" . count($argument) . ")";
                }
                return gettype($argument);
            }, ($trace["args"] ?? [])));

            $this->addLine(FormatUtils::interpolate("#{} {}({}): {}({})", [$i, PathUtils::clean($trace["file"] ?? $trace["class"]), $trace["line"] ?? "???", $trace["function"], $args]));
        }

        $this->addLine();
        $this->addLine("===BEGIN CRASH DUMP===");
        foreach(str_split(base64_encode($this->encodedData), 76) as $line){
            $this->addLine($line);
        }
        $this->addLine("===END CRASH DUMP===");

        fclose($this->cFile);
        $this->cFile = null;
        return $filePath;
    }

    private function addLine(string $line = ""): void {
        if ($this->cFile === null) return;
        fwrite($this->cFile, $line . PHP_EOL);
    }

    /**
     * @throws JsonException
     */
    public static function fromLastestError(): CrashDump {
        $latestErrorInfo = ErrorUtils::latestError(2);
        if ($latestErrorInfo === null) throw new RuntimeException("Failed to create crashdump, no latest error available");
        return new self($latestErrorInfo);
    }
}
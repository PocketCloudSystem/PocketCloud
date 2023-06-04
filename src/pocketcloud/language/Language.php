<?php

namespace pocketcloud\language;

use pocketcloud\config\DefaultConfig;
use pocketcloud\util\CloudLogger;
use pocketcloud\util\EnumTrait;

/**
 * @method static Language GERMAN()
 * @method static Language ENGLISH()
 */
final class Language {
    use EnumTrait;

    public const FALLBACK = "en";

    protected static function init(): void {
        self::register("german", new Language(
            "German",
            STORAGE_PATH . "de_DE.yml",
            ["de_DE", "ger", "Deutsch"],
            DefaultMessages::MESSAGES_DE
        ));

        self::register("english", new Language(
            "English",
            STORAGE_PATH . "en_US.yml",
            ["en_US", "en", "Englisch"],
            DefaultMessages::MESSAGES
        ));
    }

    public static function current(): Language {
        return self::getLanguage(DefaultConfig::getInstance()->getLanguage() ?? self::FALLBACK);
    }

    public static function fallback(): Language {
        return self::getLanguage(self::FALLBACK);
    }

    public static function getLanguage(string $name): ?Language {
        /** @var Language $language */
        foreach (self::getAll() as $language) {
            if ($language->getName() == $name || in_array($name, $language->getAliases())) return $language;
        }
        return null;
    }

    /** @var array<string, string> */
    private array $messages;

    public function __construct(
        private string $name,
        private string $filePath,
        private array $aliases,
        array $messages = []
    ) {
        try {
            $this->messages = @yaml_parse(@file_get_contents($this->filePath));
        } catch (\Throwable $exception) {
            $this->messages = $messages;
            @file_put_contents($this->filePath, yaml_emit($messages, YAML_UTF8_ENCODING));
            CloudLogger::get()->exception($exception);
        }
    }

    public function translate(string $key, mixed ...$params) {
        $message = str_replace("{PREFIX}", $this->messages["inGame.prefix"] ?? "", $this->messages[$key] ?? $key);
        foreach ($params as $i => $param) $message = str_replace("%" . $i . "%", $param, $message);
        return $message;
    }

    public function getName(): string {
        return $this->name;
    }

    public function getAliases(): array {
        return $this->aliases;
    }

    public function getMessages(): array {
        return $this->messages;
    }
}
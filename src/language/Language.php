<?php

namespace pocketcloud\cloud\language;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\trait\RegistryTrait;
use pocketcloud\cloud\util\Utils;
use RuntimeException;
use Throwable;
use const pocketcloud\CLOUD_PATH;
use const pocketcloud\IN_GAME_PATH;

/**
 * @method static Language GERMAN()
 * @method static Language ENGLISH()
 */
final class Language {
    use RegistryTrait;

    public const string FALLBACK = "en";

    public static function init(): void {
        self::register("german", new Language(
            "German",
            IN_GAME_PATH . "de_DE.yml",
            ["de_DE", "ger", "Deutsch"],
            DefaultMessages::MESSAGES_DE
        ));

        self::register("english", new Language(
            "English",
            IN_GAME_PATH . "en_US.yml",
            ["en_US", "en", "Englisch"],
            DefaultMessages::MESSAGES
        ));
    }

    public static function current(): Language {
        return self::get(MainConfig::getInstance()->getLanguage());
    }

    public static function fallback(): Language {
        return self::get(self::FALLBACK);
    }

    public static function get(string $name): ?Language {
        /** @var Language $language */
        return array_find(self::getAll(), fn($language) => $language->getName() == $name || in_array($name, $language->getAliases()));
    }

    /** @var array<string, string> */
    private array $messages;

    public function __construct(
        private readonly string $name,
        private readonly string $filePath,
        private readonly array $aliases,
        array $defaultMessages = []
    ) {
        if (file_exists($this->filePath)) {
            try {
                $messages = FileUtils::parseYamlFile($this->filePath);
                if ($messages === null) throw new RuntimeException("Failed to parse messages from " . str_replace(CLOUD_PATH, "", $this->filePath));
                Utils::fillMissingKeys($messages, $defaultMessages, $affectedKeys);
                if ($affectedKeys > 0) {
                    CloudLogger::get()->info("Incomplete language file found: §b{}§r, completed the file with the missing lang keys.", str_replace(CLOUD_PATH, "", $this->filePath));
                    FileUtils::emitYamlFile($this->filePath, $messages, YAML_UTF8_ENCODING);
                }

                $this->messages = $messages;
            } catch (Throwable $exception) {
                $this->messages = $defaultMessages;
                CloudLogger::get()->exception($exception);
            }
        } else {
            CloudLogger::get()->info("Language file not found: §b{}§r, generating...", str_replace(CLOUD_PATH, "", $this->filePath));
            $this->messages = $defaultMessages;
            FileUtils::emitYamlFile($this->filePath, $this->messages, YAML_UTF8_ENCODING);
        }
    }

    public function translate(string $key, array $args = []): string {
        $message = str_replace("{PREFIX}", $this->messages["inGame.prefix"] ?? "", $this->messages[$key] ?? $key);
        foreach ($args as $k => $arg) $message = str_replace("%" . $k . "%", $arg, $message);
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
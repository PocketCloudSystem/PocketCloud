<?php

namespace pocketcloud\cloud\group;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateManager;
use pocketcloud\cloud\util\misc\Writeable;
use pocketcloud\cloud\util\PathUtils;
use pocketcloud\cloud\util\Utils;
use const pocketcloud\SERVER_GROUPS_PATH;

final class ServerGroup implements Writeable {

    public function __construct(
        private readonly string $name,
        private array $templates
    ) {}

    public function add(Template $template): void {
        if (!$this->is($template)) $this->templates[] = $template->getName();
    }

    public function remove(Template|string $template): void {
        $template = $template instanceof Template ? $template->getName() : $template;
        if ($this->is($template)) unset($this->templates[array_search($template, $this->templates)]);
        $this->templates = array_values($this->templates);
    }

    public function is(Template|string $template): bool {
        $template = $template instanceof Template ? $template->getName() : $template;
        return in_array($template, $this->templates);
    }

    public function getPlayers(): array {
        return array_unique(array_merge(...array_map(fn(string $template) => TemplateManager::getInstance()->get($template)?->getPlayers() ?? [], $this->templates)));
    }

    public function getPlayerCount(): int {
        return count($this->getPlayers());
    }

    public function getName(): string {
        return $this->name;
    }

    public function getPath(): string {
        return PathUtils::join(SERVER_GROUPS_PATH, $this->name) . "/";
    }

    public function getTemplates(): array {
        return $this->templates;
    }

    public function write(): array {
        return [
            "name" => $this->name,
            "templates" => $this->templates
        ];
    }

    public static function read(array $data): ?self {
        if (!Utils::containKeys($data, "name", "templates")) return null;
        if (is_string($data["templates"])) $data["templates"] = json_decode($data["templates"], true);

        $templates = [];
        foreach ((is_array($data["templates"]) ? $data["templates"] : []) as $name) {
            if (is_string($name)) {
                if (TemplateManager::getInstance()->check($name)) $templates[] = $name;
                else CloudLogger::get()->debug("Indexing ServerGroup {}, missing template {}, skipping...", $data["name"], $name);
            }
        }

        return new self(
            $data["name"],
            $templates
        );
    }
}
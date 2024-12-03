<?php

namespace pocketcloud\cloud\template;

use pocketcloud\cloud\util\SingletonTrait;
use pocketcloud\cloud\util\tick\Tickable;

final class TemplateManager implements Tickable {
    use SingletonTrait;

    /** @var array<Template> */
    private array $templates = [];

    public function __construct() {
        self::setInstance($this);
    }

    public function load(): void {

    }

    public function create(Template $template): void {

    }

    public function remove(Template $template): void {

    }

    public function edit(Template $template, ?bool $lobby, ?bool $maintenance, ?bool $static, ?int $maxPlayerCount, ?int $minServerCount, ?int $maxServerCount, ?bool $startNewWhenFull, ?bool $autoStart): void {

    }

    public function check(string $name): bool {
        return isset($this->templates[$name]);
    }

    public function tick(int $currentTick): void {

    }

    public function get(string $name): ?Template {
        return $this->templates[$name] ?? null;
    }

    public function getAll(): array {
        return $this->templates;
    }
}
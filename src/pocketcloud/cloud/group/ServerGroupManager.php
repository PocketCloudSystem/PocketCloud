<?php

namespace pocketcloud\cloud\group;

use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\util\SingletonTrait;

final class ServerGroupManager {
    use SingletonTrait;

    /** @var array<ServerGroup> */
    private array $serverGroups = [];

    public function __construct() {
        self::setInstance($this);
    }

    public function get(Template|string $name): ?ServerGroup {
        $name = $name instanceof Template ? $name->getName() : $name;
        if (isset($this->serverGroups[$name])) return $this->serverGroups[$name];

        foreach ($this->serverGroups as $group) {
            if ($group->is($name)) return $group;
        }

        return null;
    }

    public function getAll(): array {
        return $this->serverGroups;
    }
}
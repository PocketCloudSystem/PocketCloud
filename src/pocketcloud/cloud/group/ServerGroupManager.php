<?php

namespace pocketcloud\cloud\group;

use pocketcloud\cloud\event\impl\serverGroup\ServerGroupCreateEvent;
use pocketcloud\cloud\event\impl\serverGroup\ServerGroupEditEvent;
use pocketcloud\cloud\event\impl\serverGroup\ServerGroupRemoveEvent;
use pocketcloud\cloud\provider\CloudProvider;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\SingletonTrait;

final class ServerGroupManager {
    use SingletonTrait;

    /** @var array<ServerGroup> */
    private array $serverGroups = [];
    private bool $loaded = false;

    public function __construct() {
        self::setInstance($this);
    }

    public function load(): void {
        CloudProvider::current()->getServerGroups()
            ->then(function (array $serverGroups): void {
                $this->serverGroups = $serverGroups;
                $this->loaded = true;
            });
    }

    public function create(ServerGroup $serverGroup): void {
        $startTime = microtime(true);
        CloudProvider::current()->addServerGroup($serverGroup);

        new ServerGroupCreateEvent($serverGroup)->call();

        CloudLogger::get()->debug("Creating directory: " . $serverGroup->getPath());
        if (!file_exists($serverGroup->getPath())) mkdir($serverGroup->getPath());
        $this->serverGroups[$serverGroup->getName()] = $serverGroup;
        CloudLogger::get()->success("Successfully §acreated §rthe server group §b" . $serverGroup->getName() . "§r. §8(§rTook §b" . number_format(microtime(true) - $startTime, 3) . "s§8)");
    }

    public function remove(ServerGroup $serverGroup): void {
        $startTime = microtime(true);
        CloudProvider::current()->removeServerGroup($serverGroup);

        new ServerGroupRemoveEvent($serverGroup)->call();

        if (file_exists($serverGroup->getPath())) FileUtils::removeDirectory($serverGroup->getPath());
        if (isset($this->serverGroups[$serverGroup->getName()])) unset($this->serverGroups[$serverGroup->getName()]);
        CloudLogger::get()->success("Successfully §cremoved §rthe server group §b" . $serverGroup->getName() . "§r. §8(§rTook §b" . number_format(microtime(true) - $startTime, 3) . "s§8)");
    }

    public function addTemplate(ServerGroup $serverGroup, Template $template): void {
        $startTime = microtime(true);
        $serverGroup->add($template);
        CloudProvider::current()->editServerGroup($serverGroup, $serverGroup->toArray());

        new ServerGroupEditEvent($serverGroup, $serverGroup->getTemplates())->call();

        CloudLogger::get()->success("Successfully §aadded §rthe template §b" . $template->getName() . " §rto the server group §b" . $serverGroup->getName() . "§r. §8(§rTook §b" . number_format(microtime(true) - $startTime, 3) . "s§8)");
    }

    public function removeTemplate(ServerGroup $serverGroup, Template $template): void {
        $startTime = microtime(true);
        $serverGroup->remove($template);
        CloudProvider::current()->editServerGroup($serverGroup, $serverGroup->toArray());

        new ServerGroupEditEvent($serverGroup, $serverGroup->getTemplates())->call();

        CloudLogger::get()->success("Successfully §cremoved §rthe template §b" . $template->getName() . " §rfrom the server group §b" . $serverGroup->getName() . "§r. §8(§rTook §b" . number_format(microtime(true) - $startTime, 3) . "s§8)");
    }

    public function get(Template|string $name): ?ServerGroup {
        $name = $name instanceof Template ? $name->getName() : $name;
        if (isset($this->serverGroups[$name])) return $this->serverGroups[$name];

        return array_find($this->serverGroups, fn($group) => $group->is($name));

    }

    public function isLoaded(): bool {
        return $this->loaded;
    }

    public function getAll(): array {
        return $this->serverGroups;
    }
}
<?php

namespace pocketcloud\cloud\provider;

use pocketcloud\cloud\cache\impl\InGameModuleCache;
use pocketcloud\cloud\cache\impl\MaintenanceListCache;
use pocketcloud\cloud\cache\impl\NotificationListCache;
use pocketcloud\cloud\config\Config;
use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\util\promise\Promise;
use const pocketcloud\IN_GAME_PATH;
use const pocketcloud\SERVER_GROUPS_PATH;
use const pocketcloud\TEMPLATES_PATH;

final class CloudJsonProvider extends CloudProvider {

    private Config $templatesConfig;
    private Config $serverGroupsConfig;
    private Config $modulesConfig;
    private Config $notificationsList;
    private Config $maintenanceList;

    public function __construct() {
        $this->templatesConfig = new Config(TEMPLATES_PATH . "templates.json");
        $this->serverGroupsConfig = new Config(SERVER_GROUPS_PATH . "groups.json");
        $this->modulesConfig = new Config(IN_GAME_PATH . "modules.json");
        $this->notificationsList = new Config(IN_GAME_PATH . "notifications.json");
        $this->maintenanceList = new Config(IN_GAME_PATH . "maintenanceList.json");

        foreach ($this->maintenanceList->getAll() as $player => $enabled) if ($enabled) MaintenanceListCache::add($player);
        foreach ($this->notificationsList->getAll() as $player => $enabled) if ($enabled) NotificationListCache::add($player);
        foreach ($this->modulesConfig->getAll() as $module => $enabled) InGameModuleCache::setModuleState($module, $enabled);
    }

    public function addTemplate(Template $template): Promise {
        $this->templatesConfig->set($template->getName(), $template->write());
        return $this->templatesConfig->save() ? Promise::resolved() : Promise::rejected();
    }

    public function removeTemplate(Template $template): Promise {
        $this->templatesConfig->remove($template->getName());
        return $this->templatesConfig->save() ? Promise::resolved() : Promise::rejected();
    }

    public function editTemplate(Template $template, array $newData): Promise {
        $this->templatesConfig->set($template->getName(), $newData);
        return $this->templatesConfig->save() ? Promise::resolved() : Promise::rejected();
    }

    public function getTemplate(string $template): Promise {
        $promise = new Promise();

        $data = $this->templatesConfig->get($template);
        if (($template = Template::read($data)) !== null) {
            $promise->resolve($template);
        } else $promise->reject();

        return $promise;
    }

    public function checkTemplate(string $template): Promise {
        return Promise::resolved($this->templatesConfig->has($template));
    }

    public function getTemplates(): Promise {
        $promise = new Promise();

        $templates = [];
        $data = $this->templatesConfig->getAll();
        foreach ($data as $template) {
            if (($template = Template::read($template)) !== null) $templates[$template->getName()] = $template;
        }

        $promise->resolve($templates);
        return $promise;
    }

    public function addServerGroup(ServerGroup $serverGroup): Promise {
        $this->serverGroupsConfig->set($serverGroup->getName(), $serverGroup->write());
        return $this->serverGroupsConfig->save() ? Promise::resolved() : Promise::rejected();
    }

    public function removeServerGroup(ServerGroup $serverGroup): Promise {
        $this->serverGroupsConfig->remove($serverGroup->getName());
        return $this->serverGroupsConfig->save() ? Promise::resolved() : Promise::rejected();
    }

    public function editServerGroup(ServerGroup $serverGroup, array $newData): Promise {
        $this->serverGroupsConfig->set($serverGroup->getName(), $newData);
        return $this->serverGroupsConfig->save() ? Promise::resolved() : Promise::rejected();
    }

    public function getServerGroup(string $serverGroup): Promise {
        $promise = new Promise();

        $data = $this->serverGroupsConfig->get($serverGroup);
        if (($serverGroup = ServerGroup::read($data)) !== null) {
            $promise->resolve($serverGroup);
        } else $promise->reject();

        return $promise;
    }

    public function checkServerGroup(string $serverGroup): Promise {
        return Promise::resolved($this->serverGroupsConfig->has($serverGroup));
    }

    public function getServerGroups(): Promise {
        $promise = new Promise();

        $serverGroups = [];
        $data = $this->serverGroupsConfig->getAll();
        foreach ($data as $serverGroup) {
            if (($serverGroup = ServerGroup::read($serverGroup)) !== null) $serverGroups[$serverGroup->getName()] = $serverGroup;
        }

        $promise->resolve($serverGroups);
        return $promise;
    }

    public function setModuleState(string $module, bool $enabled): Promise {
        $this->modulesConfig->set($module, $enabled);
        InGameModuleCache::setModuleState($module, $enabled);
        return $this->modulesConfig->save() ? Promise::resolved() : Promise::rejected();
    }

    public function getModuleState(string $module): Promise {
        return Promise::resolved($this->modulesConfig->get($module, false));
    }

    public function enablePlayerNotifications(string $player): Promise {
        $this->notificationsList->set($player, true);
        NotificationListCache::add($player);
        return $this->notificationsList->save() ? Promise::resolved() : Promise::rejected();
    }

    public function disablePlayerNotifications(string $player): Promise {
        $this->notificationsList->remove($player);
        NotificationListCache::remove($player);
        return $this->notificationsList->save() ? Promise::resolved() : Promise::rejected();
    }

    public function hasNotificationsEnabled(string $player): Promise {
        return Promise::resolved($this->notificationsList->get($player, false));
    }

    public function getNotificationList(): Promise {
        return Promise::resolved($this->notificationsList->getAll());
    }

    public function addToWhitelist(string $player): Promise {
        $this->maintenanceList->set($player, true);
        MaintenanceListCache::add($player);
        return $this->maintenanceList->save() ? Promise::resolved() : Promise::rejected();
    }

    public function removeFromWhitelist(string $player): Promise {
        $this->maintenanceList->remove($player);
        MaintenanceListCache::remove($player);
        return $this->maintenanceList->save() ? Promise::resolved() : Promise::rejected();
    }

    public function isOnWhitelist(string $player): Promise {
        return Promise::resolved($this->notificationsList->get($player, false));
    }

    public function getWhitelist(): Promise {
        return Promise::resolved(array_filter($this->maintenanceList->getAll(true), fn(string $user) => $this->maintenanceList->get($user, false)));
    }

    public function getTemplatesConfig(): ?Config {
        return $this->templatesConfig;
    }

    public function getServerGroupsConfig(): Config {
        return $this->serverGroupsConfig;
    }

    public function getModulesConfig(): Config {
        return $this->modulesConfig;
    }

    public function getNotificationsList(): Config {
        return $this->notificationsList;
    }

    public function getMaintenanceList(): Config {
        return $this->maintenanceList;
    }
}
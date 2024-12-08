<?php

namespace pocketcloud\cloud\provider;

use pocketcloud\cloud\cache\MaintenanceList;
use pocketcloud\cloud\config\Config;
use pocketcloud\cloud\config\type\ConfigTypes;
use pocketcloud\cloud\cache\InGameModule;
use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\util\promise\Promise;

final class CloudJsonProvider extends CloudProvider {

    private Config $templatesConfig;
    private Config $serverGroupsConfig;
    private Config $modulesConfig;
    private Config $notificationsList;
    private Config $maintenanceList;

    public function __construct() {
        $this->templatesConfig = new Config(TEMPLATES_PATH . "templates.json", ConfigTypes::JSON());
        $this->serverGroupsConfig = new Config(SERVER_GROUPS_PATH . "groups.json", ConfigTypes::JSON());
        $this->modulesConfig = new Config(IN_GAME_PATH . "modules.json", ConfigTypes::JSON());
        $this->notificationsList = new Config(IN_GAME_PATH . "notifications.json", ConfigTypes::JSON());
        $this->maintenanceList = new Config(IN_GAME_PATH . "maintenanceList.json", ConfigTypes::JSON());

        foreach ($this->maintenanceList->getAll() as $player => $enabled) {
            if ($enabled) MaintenanceList::add($player);
        }

        foreach ($this->modulesConfig->getAll() as $player => $enabled) {
            InGameModule::setModuleState($player, $enabled);
        }
    }

    public function addTemplate(Template $template): void {
        $this->templatesConfig->set($template->getName(), $template->toArray());
        $this->templatesConfig->save();
    }

    public function removeTemplate(Template $template): void {
        $this->templatesConfig->remove($template->getName());
        $this->templatesConfig->save();
    }

    public function editTemplate(Template $template, array $newData): void {
        $this->templatesConfig->set($template->getName(), $newData);
        $this->templatesConfig->save();
    }

    public function getTemplate(string $template): Promise {
        $promise = new Promise();

        $data = $this->templatesConfig->get($template);
        if (($template = Template::fromArray($data)) !== null) {
            $promise->resolve($template);
        } else $promise->reject();

        return $promise;
    }

    public function checkTemplate(string $template): Promise {
        $promise = new Promise();
        $promise->resolve($this->templatesConfig->has($template));
        return $promise;
    }

    public function getTemplates(): Promise {
        $promise = new Promise();

        $templates = [];
        $data = $this->templatesConfig->getAll();
        foreach ($data as $template) {
            if (($template = Template::fromArray($template)) !== null) $templates[$template->getName()] = $template;
        }

        $promise->resolve($templates);
        return $promise;
    }

    public function addServerGroup(ServerGroup $serverGroup): void {
        $this->serverGroupsConfig->set($serverGroup->getName(), $serverGroup->toArray());
        $this->serverGroupsConfig->save();
    }

    public function removeServerGroup(ServerGroup $serverGroup): void {
        $this->serverGroupsConfig->remove($serverGroup->getName());
        $this->serverGroupsConfig->save();
    }

    public function editServerGroup(ServerGroup $serverGroup, array $newData): void {
        $this->serverGroupsConfig->set($serverGroup->getName(), $newData);
        $this->serverGroupsConfig->save();
    }

    public function getServerGroup(string $serverGroup): Promise {
        $promise = new Promise();

        $data = $this->serverGroupsConfig->get($serverGroup);
        if (($serverGroup = ServerGroup::fromArray($data)) !== null) {
            $promise->resolve($serverGroup);
        } else $promise->reject();

        return $promise;
    }

    public function checkServerGroup(string $serverGroup): Promise {
        $promise = new Promise();
        $promise->resolve($this->serverGroupsConfig->has($serverGroup));
        return $promise;
    }

    public function getServerGroups(): Promise {
        $promise = new Promise();

        $serverGroups = [];
        $data = $this->templatesConfig->getAll();
        foreach ($data as $serverGroup) {
            if (($serverGroup = ServerGroup::fromArray($serverGroup)) !== null) $serverGroups[$serverGroup->getName()] = $serverGroup;
        }

        $promise->resolve($serverGroups);
        return $promise;
    }

    public function setModuleState(string $module, bool $enabled): void {
        $this->modulesConfig->set($module, $enabled);
        $this->modulesConfig->save();
        InGameModule::setModuleState($module, $enabled);
    }

    public function getModuleState(string $module): Promise {
        $promise = new Promise();
        $promise->resolve($this->modulesConfig->get($module, false));
        return $promise;
    }

    public function enablePlayerNotifications(string $player): void {
        $this->notificationsList->set($player, true);
        $this->notificationsList->save();
    }

    public function disablePlayerNotifications(string $player): void {
        $this->notificationsList->remove($player);
        $this->notificationsList->save();
    }

    public function hasNotificationsEnabled(string $player): Promise {
        $promise = new Promise();
        $promise->resolve($this->notificationsList->get($player, false));
        return $promise;
    }

    public function addToWhitelist(string $player): void {
        $this->maintenanceList->set($player, true);
        $this->maintenanceList->save();
        MaintenanceList::add($player);
    }

    public function removeFromWhitelist(string $player): void {
        $this->maintenanceList->remove($player);
        $this->maintenanceList->save();
        MaintenanceList::remove($player);
    }

    public function isOnWhitelist(string $player): Promise {
        $promise = new Promise();
        $promise->resolve($this->maintenanceList->get($player, false));
        return $promise;
    }

    public function getWhitelist(): Promise {
        $promise = new Promise();
        $promise->resolve(array_filter($this->maintenanceList->getAll(true), fn(string $user) => $this->maintenanceList->get($user, false)));
        return $promise;
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
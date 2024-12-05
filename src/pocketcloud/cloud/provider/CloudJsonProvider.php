<?php

namespace pocketcloud\cloud\provider;

use pocketcloud\cloud\config\Config;
use pocketcloud\cloud\config\type\ConfigTypes;
use pocketcloud\cloud\module\InGameModule;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\util\promise\Promise;

final class CloudJsonProvider extends CloudProvider {

    private Config $templatesConfig;
    private Config $modulesConfig;
    private Config $notificationsList;
    private Config $maintenanceList;

    public function __construct() {
        $this->templatesConfig = new Config(TEMPLATES_PATH . "templates.json", ConfigTypes::JSON());
        $this->modulesConfig = new Config(IN_GAME_PATH . "modules.json", ConfigTypes::JSON());
        $this->notificationsList = new Config(IN_GAME_PATH . "notifications.json", ConfigTypes::JSON());
        $this->maintenanceList = new Config(IN_GAME_PATH . "maintenanceList.json", ConfigTypes::JSON());
    }

    public function addTemplate(Template $template): void {
        $this->templatesConfig->set($template->getName(), $template->toArray());
        $this->templatesConfig->save();
    }

    public function removeTemplate(Template $template): void {
        $this->templatesConfig->remove($template->getName());
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
    }

    public function removeFromWhitelist(string $player): void {
        $this->maintenanceList->remove($player);
        $this->maintenanceList->save();
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
}
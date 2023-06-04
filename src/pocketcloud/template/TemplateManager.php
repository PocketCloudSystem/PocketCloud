<?php

namespace pocketcloud\template;

use pocketcloud\config\MaintenanceList;
use pocketcloud\event\impl\template\TemplateCreateEvent;
use pocketcloud\event\impl\template\TemplateDeleteEvent;
use pocketcloud\event\impl\template\TemplateEditEvent;
use pocketcloud\language\Language;
use pocketcloud\network\Network;
use pocketcloud\network\packet\impl\normal\TemplateSyncPacket;
use pocketcloud\player\CloudPlayer;
use pocketcloud\player\CloudPlayerManager;
use pocketcloud\PocketCloud;
use pocketcloud\server\CloudServer;
use pocketcloud\server\CloudServerManager;
use pocketcloud\server\status\ServerStatus;
use pocketcloud\server\utils\PropertiesMaker;
use pocketcloud\util\CloudLogger;
use pocketcloud\util\Config;
use pocketcloud\util\Reloadable;
use pocketcloud\util\SingletonTrait;
use pocketcloud\util\Tickable;
use pocketcloud\util\Utils;

class TemplateManager implements Reloadable, Tickable {
    use SingletonTrait;

    /** @var array<Template> */
    private array $templates = [];
    private Config $templatesConfig;

    public function __construct() {
        self::setInstance($this);
        $this->templatesConfig = new Config(TEMPLATES_PATH . "templates.json", 1);
    }

    public function loadTemplates() {
        CloudLogger::get()->info(Language::current()->translate("template.loading"));
        foreach ($this->templatesConfig->getAll() as $name => $data) {
            CloudLogger::get()->debug("Loading template " . ($data["name"] ?? $name));
            if (($template = Template::fromArray($data)) instanceof Template) {
                $this->templates[$template->getName()] = $template;
            }
        }

        if (count($this->templates) == 0) {
            CloudLogger::get()->info(Language::current()->translate("template.loaded.none"));
        } else {
            CloudLogger::get()->info(Language::current()->translate("template.loaded", count($this->templates)));
        }
    }

    public function createTemplate(Template $template) {
        $startTime = microtime(true);
        CloudLogger::get()->info(Language::current()->translate("template.create", $template->getName()));
        $this->templatesConfig->set($template->getName(), $template->toArray());
        $this->templatesConfig->save();

        (new TemplateCreateEvent($template))->call();

        CloudLogger::get()->debug("Creating directory: " . $template->getPath());
        if (!file_exists($template->getPath())) mkdir($template->getPath());
        PropertiesMaker::makeProperties($template);
        $this->templates[$template->getName()] = $template;
        CloudLogger::get()->info(Language::current()->translate("template.created", $template->getName(), number_format((microtime(true) - $startTime), 3)));
        Network::getInstance()->broadcastPacket(new TemplateSyncPacket($template));
    }

    public function deleteTemplate(Template $template) {
        $startTime = microtime(true);
        CloudLogger::get()->info(Language::current()->translate("template.delete", $template->getName()));
        $this->templatesConfig->remove($template->getName());
        $this->templatesConfig->save();

        (new TemplateDeleteEvent($template))->call();

        CloudServerManager::getInstance()->stopTemplate($template);

        if (file_exists($template->getPath())) Utils::deleteDir($template->getPath());
        if (isset($this->templates[$template->getName()])) unset($this->templates[$template->getName()]);
        CloudLogger::get()->info(Language::current()->translate("template.deleted", $template->getName(), number_format((microtime(true) - $startTime), 3)));
        Network::getInstance()->broadcastPacket(new TemplateSyncPacket($template, true));
    }

    public function editTemplate(Template $template, ?bool $lobby, ?bool $maintenance, ?bool $static, ?int $maxPlayerCount, ?int $minServerCount, ?int $maxServerCount, ?bool $startNewWhenFull, ?bool $autoStart) {
        $startTime = microtime(true);
        CloudLogger::get()->info(Language::current()->translate("template.edit", $template->getName()));
        $template->setLobby(($lobby === null ? $template->isLobby() : $lobby));
        $template->setMaintenance(($maintenance === null ? $template->isMaintenance() : $maintenance));
        $template->setStatic(($static === null ? $template->isStatic() : $static));
        $template->setMaxPlayerCount(($maxPlayerCount === null ? $template->getMaxPlayerCount() : $maxPlayerCount));
        $template->setMinServerCount(($minServerCount === null ? $template->getMinServerCount() : $minServerCount));
        $template->setMaxServerCount(($maxServerCount === null ? $template->getMaxServerCount() : $maxServerCount));
        $template->setStartNewWhenFull(($startNewWhenFull === null ? $template->isStartNewWhenFull() : $startNewWhenFull));
        $template->setAutoStart(($autoStart === null ? $template->isAutoStart() : $autoStart));

        (new TemplateEditEvent($template, $lobby, $maintenance, $static, $maxPlayerCount, $minServerCount, $maxServerCount, $startNewWhenFull, $autoStart))->call();

        $this->templatesConfig->set($template->getName(), $template->toArray());
        $this->templatesConfig->save();
        CloudLogger::get()->info(Language::current()->translate("template.edited", $template->getName(), number_format((microtime(true) - $startTime), 3)));
        Network::getInstance()->broadcastPacket(new TemplateSyncPacket($template));

        if ($template->toArray()["maintenance"]) {
            foreach (array_filter(CloudPlayerManager::getInstance()->getPlayers(), function(CloudPlayer $player) use($template): bool {
                return ($player->getCurrentServer() !== null && $player->getCurrentServer()->getTemplate() === $template) && !MaintenanceList::is($player->getName());
            }) as $player) {
                $player->kick("MAINTENANCE");
            }
        }
    }

    public function reload(): bool {
        $this->templatesConfig->reload();
        foreach ($this->templatesConfig->getAll() as $name => $templateData) {
            if (isset($this->templates[$templateData["name"] ?? $name])) {
                $this->templates[$templateData["name"] ?? $name]->apply($templateData);
            } else {
                if (($template = Template::fromArray($templateData)) !== null) {
                    $this->createTemplate($template);
                }
            }
        }

        foreach ($this->templates as $template) {
            if (!$this->templatesConfig->exists($template->getName())) $this->deleteTemplate($template);
        }
        return true;
    }

    public function checkTemplate(string $name): bool {
        return $this->templatesConfig->exists($name);
    }

    public function tick(int $currentTick): void {
        if (PocketCloud::getInstance()->isReloading()) return;
        foreach (TemplateManager::getInstance()->getTemplates() as $template) {
            if ($template->isAutoStart()) {
                if (($running = count(CloudServerManager::getInstance()->getServersByTemplate($template))) < $template->getMaxServerCount()) {
                    CloudServerManager::getInstance()->startServer($template, ($template->getMinServerCount() - $running));
                }
            }

            if ($template->isStartNewWhenFull()) {
                if (($running = count(($servers = CloudServerManager::getInstance()->getServersByTemplate($template)))) < $template->getMaxServerCount()) {
                    if ($running > 0) {
                        $full = count(array_filter($servers, fn(CloudServer $server) => $server->getServerStatus() === ServerStatus::IN_GAME() || $server->getServerStatus() === ServerStatus::FULL()));
                        if ($full == $running) {
                            CloudServerManager::getInstance()->startServer($template);
                        }
                    }
                }
            }
        }
    }

    public function getTemplateByName(string $name): ?Template {
        return $this->templates[$name] ?? null;
    }

    public function getTemplates(): array {
        return $this->templates;
    }
}
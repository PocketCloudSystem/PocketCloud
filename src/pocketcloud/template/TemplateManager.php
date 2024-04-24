<?php

namespace pocketcloud\template;

use pocketcloud\config\impl\MaintenanceList;
use pocketcloud\config\type\ConfigTypes;
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
use pocketcloud\config\Config;
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
        $this->templatesConfig = new Config(TEMPLATES_PATH . "templates.json", ConfigTypes::JSON());
    }

    public function loadTemplates(): void {
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

    public function createTemplate(Template $template): void {
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

    public function deleteTemplate(Template $template): void {
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

    public function editTemplate(Template $template, ?bool $lobby, ?bool $maintenance, ?bool $static, ?int $maxPlayerCount, ?int $minServerCount, ?int $maxServerCount, ?bool $startNewWhenFull, ?bool $autoStart): void {
        $startTime = microtime(true);
        CloudLogger::get()->info(Language::current()->translate("template.edit", $template->getName()));
        $template->getSettings()->setLobby(($lobby === null ? $template->getSettings()->isLobby() : $lobby));
        $template->getSettings()->setMaintenance(($maintenance === null ? $template->getSettings()->isMaintenance() : $maintenance));
        $template->getSettings()->setStatic(($static === null ? $template->getSettings()->isStatic() : $static));
        $template->getSettings()->setMaxPlayerCount(($maxPlayerCount === null ? $template->getSettings()->getMaxPlayerCount() : $maxPlayerCount));
        $template->getSettings()->setMinServerCount(($minServerCount === null ? $template->getSettings()->getMinServerCount() : $minServerCount));
        $template->getSettings()->setMaxServerCount(($maxServerCount === null ? $template->getSettings()->getMaxServerCount() : $maxServerCount));
        $template->getSettings()->setStartNewWhenFull(($startNewWhenFull === null ? $template->getSettings()->isStartNewWhenFull() : $startNewWhenFull));
        $template->getSettings()->setAutoStart(($autoStart === null ? $template->getSettings()->isAutoStart() : $autoStart));

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
                ($template = $this->templates[$templateData["name"]])->getSettings()->setLobby($templateData["lobby"]);
                $template->getSettings()->setMaintenance($templateData["maintenance"]);
                $template->getSettings()->setStatic($templateData["static"]);
                $template->getSettings()->setMaxPlayerCount($templateData["maxPlayerCount"]);
                $template->getSettings()->setMinServerCount($templateData["minServerCount"]);
                $template->getSettings()->setMaxServerCount($templateData["maxServerCount"]);
                $template->getSettings()->setStartNewWhenFull($templateData["startNewWhenFull"]);
                $template->getSettings()->setAutoStart($templateData["autoStart"]);
            } else {
                if (($template = Template::fromArray($templateData)) !== null) {
                    $this->createTemplate($template);
                }
            }
        }

        foreach ($this->templates as $template) {
            if (!$this->templatesConfig->has($template->getName())) $this->deleteTemplate($template);
        }
        return true;
    }

    public function checkTemplate(string $name): bool {
        return $this->templatesConfig->has($name);
    }

    public function tick(int $currentTick): void {
        if (PocketCloud::getInstance()->isReloading()) return;
        foreach (TemplateManager::getInstance()->getTemplates() as $template) {
            if ($template->getSettings()->isAutoStart()) {
                if (($running = count(CloudServerManager::getInstance()->getServersByTemplate($template))) < $template->getSettings()->getMaxServerCount()) {
                    CloudServerManager::getInstance()->startServer($template, ($template->getSettings()->getMinServerCount() - $running));
                }
            }

            if ($template->getSettings()->isStartNewWhenFull()) {
                if (($running = count(($servers = CloudServerManager::getInstance()->getServersByTemplate($template)))) < $template->getSettings()->getMaxServerCount()) {
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

    public static function getInstance(): self {
        return self::$instance ??= new self;
    }
}
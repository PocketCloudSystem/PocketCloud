<?php

namespace pocketcloud\template;

use pocketcloud\config\MaintenanceConfig;
use pocketcloud\event\impl\template\TemplateCreateEvent;
use pocketcloud\event\impl\template\TemplateDeleteEvent;
use pocketcloud\event\impl\template\TemplateEditEvent;
use pocketcloud\network\Network;
use pocketcloud\network\packet\impl\normal\LocalTemplateRegisterPacket;
use pocketcloud\network\packet\impl\normal\LocalTemplateUnregisterPacket;
use pocketcloud\network\packet\impl\normal\LocalTemplateUpdatePacket;
use pocketcloud\player\CloudPlayer;
use pocketcloud\player\CloudPlayerManager;
use pocketcloud\server\CloudServerManager;
use pocketcloud\server\utils\PropertiesMaker;
use pocketcloud\utils\CloudLogger;
use pocketcloud\utils\Config;
use pocketcloud\utils\SingletonTrait;
use pocketcloud\utils\Utils;

class TemplateManager {
    use SingletonTrait;

    /** @var array<Template> */
    private array $templates = [];

    public function loadTemplates() {
        foreach ($this->getTemplatesConfig()->getAll() as $name => $data) {
            CloudLogger::get()->debug("Loading template " . ($data["name"] ?? $name));
            if (($template = Template::fromArray($data)) instanceof Template) {
                $this->templates[$template->getName()] = $template;
            }
        }

        CloudLogger::get()->info("Successfully loaded §e" . count($this->templates) . " template" . (count($this->templates) == 1 ? "" : "s") . "§r!");
    }

    public function createTemplate(Template $template) {
        $startTime = microtime(true);
        CloudLogger::get()->info("§aCreating §rtemplate §e" . $template->getName() . "§r...");
        $cfg = $this->getTemplatesConfig();
        $cfg->set($template->getName(), $template->toArray());
        $cfg->save();

        (new TemplateCreateEvent($template))->call();

        CloudLogger::get()->debug("Creating directory: " . $template->getPath());
        if (!file_exists($template->getPath())) mkdir($template->getPath());
        PropertiesMaker::makeProperties($template);
        $this->templates[$template->getName()] = $template;
        CloudLogger::get()->info("Successfully §acreated §rthe §e" . $template->getName() . " §rtemplate in §e" . number_format((microtime(true) - $startTime), 3) . "s§r!");
        Network::getInstance()->broadcastPacket(new LocalTemplateRegisterPacket($template->toArray()));
    }

    public function deleteTemplate(Template $template) {
        $startTime = microtime(true);
        CloudLogger::get()->info("§cDeleting §rtemplate §e" . $template->getName() . "§r...");
        $cfg = $this->getTemplatesConfig();
        $cfg->remove($template->getName());
        $cfg->save();

        (new TemplateDeleteEvent($template))->call();

        CloudServerManager::getInstance()->stopTemplate($template);

        if (file_exists($template->getPath())) Utils::deleteDir($template->getPath());
        if (isset($this->templates[$template->getName()])) unset($this->templates[$template->getName()]);
        CloudLogger::get()->info("Successfully §cdeleted §rthe §e" . $template->getName() . " §rtemplate in §e" . number_format((microtime(true) - $startTime), 3) . "s§r!");
        Network::getInstance()->broadcastPacket(new LocalTemplateUnregisterPacket($template->getName()));
    }

    public function editTemplate(Template $template, ?bool $lobby, ?bool $maintenance, ?bool $static, ?int $maxPlayerCount, ?int $minServerCount, ?int $maxServerCount, ?bool $startNewWhenFull, ?bool $autoStart) {
        $startTime = microtime(true);
        CloudLogger::get()->info("Editing template §e" . $template->getName() . "§r...");
        $cfg = $this->getTemplatesConfig();
        $template->setLobby(($lobby === null ? $template->isLobby() : $lobby));
        $template->setMaintenance(($maintenance === null ? $template->isMaintenance() : $maintenance));
        $template->setStatic(($static === null ? $template->isStatic() : $static));
        $template->setMaxPlayerCount(($maxPlayerCount === null ? $template->getMaxPlayerCount() : $maxPlayerCount));
        $template->setMinServerCount(($minServerCount === null ? $template->getMinServerCount() : $minServerCount));
        $template->setMaxServerCount(($maxServerCount === null ? $template->getMaxServerCount() : $maxServerCount));
        $template->setStartNewWhenFull(($startNewWhenFull === null ? $template->isStartNewWhenFull() : $startNewWhenFull));
        $template->setAutoStart(($autoStart === null ? $template->isAutoStart() : $autoStart));

        (new TemplateEditEvent($template, $lobby, $maintenance, $static, $maxPlayerCount, $minServerCount, $maxServerCount, $startNewWhenFull, $autoStart))->call();

        $cfg->set($template->getName(), $template->toArray());
        $cfg->save();
        CloudLogger::get()->info("Successfully §aedited §rthe §e" . $template->getName() . " §rtemplate in §e" . number_format((microtime(true) - $startTime), 3) . "s§r!");
        Network::getInstance()->broadcastPacket(new LocalTemplateUpdatePacket($template->getName(), $template->toArray()));

        if ($template->toArray()["maintenance"]) {
            foreach (array_filter(CloudPlayerManager::getInstance()->getPlayers(), function(CloudPlayer $player) use($template): bool {
                return ($player->getCurrentServer() !== null && $player->getCurrentServer()->getTemplate() === $template) && !MaintenanceConfig::getInstance()->is($player->getName());
            }) as $player) {
                $player->kick("MAINTENANCE");
            }
        }
    }

    public function checkTemplate(string $name): bool {
        return $this->getTemplatesConfig()->exists($name);
    }

    public function getTemplateByName(string $name): ?Template {
        return $this->templates[$name] ?? null;
    }

    private function getTemplatesConfig(): Config {
        return new Config(TEMPLATES_PATH . "templates.json", 1);
    }

    public function getTemplates(): array {
        return $this->templates;
    }
}
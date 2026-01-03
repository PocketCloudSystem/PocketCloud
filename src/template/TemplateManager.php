<?php

namespace pocketcloud\cloud\template;

use pocketcloud\cloud\cache\MaintenanceListCache;
use pocketcloud\cloud\event\impl\template\TemplateCreateEvent;
use pocketcloud\cloud\event\impl\template\TemplateEditEvent;
use pocketcloud\cloud\event\impl\template\TemplateRemoveEvent;
use pocketcloud\cloud\group\ServerGroupManager;
use pocketcloud\cloud\network\packet\impl\TemplateSyncPacket;
use pocketcloud\cloud\player\CloudPlayer;
use pocketcloud\cloud\player\CloudPlayerManager;
use pocketcloud\cloud\provider\CloudProvider;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\server\prepare\ServerPreparator;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\misc\Loadable;
use pocketcloud\cloud\util\misc\Tickable;
use pocketcloud\cloud\util\trait\SingletonTrait;

final class TemplateManager implements Loadable, Tickable {
    use SingletonTrait;

    /** @var array<Template> */
    private array $templates = [];

    public function __construct() {
        self::setInstance($this);
    }

    public function load(): void {
        foreach (TemplateType::getAll() as $type) FileUtils::createDir($type->getGlobalTemplatePath());

        CloudProvider::current()->getTemplates()
            ->then(function(array $templates): void {
                $this->templates = $templates;

                if (array_sum(array_map(fn(Template $template) => $template->getSettings()->getMinServerCount(), array_filter($this->templates, fn(Template $template) => $template->getSettings()->isAutoStart()))) >= 9 && count(ServerPreparator::getInstance()->getThreads()) == 0) {
                    CloudLogger::get()->warn("Your total active server count exceeds §b9§8, §rtherefore you should set §8'§bserverPrepareThreads§8' §rinside your §bconfig.json §rto at least §b1 §ror §b2 §rand restart the the §bcloud§r.");
                }

                ServerGroupManager::getInstance()->load();
            });
    }

    public function create(Template $template): void {
        $startTime = microtime(true);
        CloudProvider::current()->addTemplate($template);

        new TemplateCreateEvent($template)->call();

        CloudLogger::get()->debug("Creating directory: " . $template->getPath());
        if (!file_exists($template->getPath())) mkdir($template->getPath());
        $this->templates[$template->getName()] = $template;
        CloudLogger::get()->success("Successfully §acreated §rthe template §b" . $template->getName() . "§r. §8(§rTook §b" . number_format(microtime(true) - $startTime, 3) . "s§8)");
        TemplateSyncPacket::create($template, false)->broadcastPacket();
    }

    public function remove(Template $template): void {
        $startTime = microtime(true);
        CloudProvider::current()->removeTemplate($template);

        new TemplateRemoveEvent($template)->call();

        CloudServerManager::getInstance()->stop($template, true);

        if (file_exists($template->getPath())) FileUtils::removeDirectory($template->getPath());
        if (isset($this->templates[$template->getName()])) unset($this->templates[$template->getName()]);
        CloudLogger::get()->success("Successfully §cremoved §rthe template §b" . $template->getName() . "§r. §8(§rTook §b" . number_format(microtime(true) - $startTime, 3) . "s§8)");
        TemplateSyncPacket::create($template, true)->broadcastPacket();
    }

    public function edit(Template $template, ?bool $lobby, ?bool $maintenance, ?bool $static, ?int $maxPlayerCount, ?int $minServerCount, ?int $maxServerCount, ?float $startNewPercentage, ?bool $autoStart): void {
        $startTime = microtime(true);
        $template->getSettings()->setLobby(($lobby === null ? $template->getSettings()->isLobby() : $lobby));
        $template->getSettings()->setMaintenance(($maintenance === null ? $template->getSettings()->isMaintenance() : $maintenance));
        $template->getSettings()->setStatic(($static === null ? $template->getSettings()->isStatic() : $static));
        $template->getSettings()->setMaxPlayerCount(($maxPlayerCount === null ? $template->getSettings()->getMaxPlayerCount() : $maxPlayerCount));
        $template->getSettings()->setMinServerCount(($minServerCount === null ? $template->getSettings()->getMinServerCount() : $minServerCount));
        $template->getSettings()->setMaxServerCount(($maxServerCount === null ? $template->getSettings()->getMaxServerCount() : $maxServerCount));
        $template->getSettings()->setStartNewPercentage(($startNewPercentage === null ? $template->getSettings()->getStartNewPercentage() : $startNewPercentage));
        $template->getSettings()->setAutoStart(($autoStart === null ? $template->getSettings()->isAutoStart() : $autoStart));

        new TemplateEditEvent($template, $lobby, $maintenance, $static, $maxPlayerCount, $minServerCount, $maxServerCount, $startNewPercentage, $autoStart)->call();

        CloudProvider::current()->editTemplate($template, $template->write());

        CloudLogger::get()->success("Successfully §eedited §rthe template §b" . $template->getName() . "§r. §8(§rTook §b" . number_format(microtime(true) - $startTime, 3) . "s§8)");
        TemplateSyncPacket::create($template, false)->broadcastPacket();

        if ($template->isMaintenance()) {
            foreach (array_filter(CloudPlayerManager::getInstance()->getAll($template), fn (CloudPlayer $player): bool => !MaintenanceListCache::is($player->getName())) as $player) {
                $player->kick("MAINTENANCE");
            }
        }
    }

    public function check(string $name): bool {
        return isset($this->templates[$name]);
    }

    public function tick(int $currentTick): void {
        if (!ServerGroupManager::getInstance()->isLoaded()) return;
        foreach (TemplateManager::getInstance()->getAll() as $template) {
            if ($template->getSettings()->isAutoStart()) {
                if (($running = count(CloudServerManager::getInstance()->getAll($template))) < $template->getSettings()->getMaxServerCount()) {
                    CloudServerManager::getInstance()->start($template, ($template->getSettings()->getMinServerCount() - $running));
                }
            }

            if (($latest = CloudServerManager::getInstance()->getLatest($template)) !== null) {
                $players = $latest->getPlayerCount();
                $requiredPercentage = $template->getStartNewPercentage();
                if ($requiredPercentage < 1) $requiredPercentage = $requiredPercentage * 100;
                $percentage = 100 * $players / $requiredPercentage;
                if ($percentage >= $requiredPercentage && CloudServerManager::getInstance()->checkCapacity($template)) {
                    CloudServerManager::getInstance()->start($template);
                }
            }
        }
    }

    public function get(string $name): ?Template {
        return $this->templates[$name] ?? null;
    }

    public function getAll(): array {
        return $this->templates;
    }
}
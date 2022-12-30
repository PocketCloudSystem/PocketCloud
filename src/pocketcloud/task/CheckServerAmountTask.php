<?php

namespace pocketcloud\task;

use pocketcloud\PocketCloud;
use pocketcloud\scheduler\Task;
use pocketcloud\server\CloudServer;
use pocketcloud\server\CloudServerManager;
use pocketcloud\server\status\ServerStatus;
use pocketcloud\template\TemplateManager;

class CheckServerAmountTask extends Task {

    public function onRun() {
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
}
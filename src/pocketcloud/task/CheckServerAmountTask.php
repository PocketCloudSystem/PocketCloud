<?php

namespace pocketcloud\task;

use pocketcloud\scheduler\Task;
use pocketcloud\server\CloudServerManager;
use pocketcloud\template\TemplateManager;

class CheckServerAmountTask extends Task {

    public function onRun() {
        foreach (TemplateManager::getInstance()->getTemplates() as $template) {
            if ($template->isAutoStart()) {
                if (($running = count(CloudServerManager::getInstance()->getServersByTemplate($template))) < $template->getMaxServerCount()) {
                    CloudServerManager::getInstance()->startServer($template, ($template->getMinServerCount() - $running));
                }
            }
        }
    }
}
<?php

namespace pocketcloud\update;

use pocketcloud\scheduler\AsyncTask;
use pocketcloud\utils\CloudLogger;
use pocketcloud\utils\VersionInfo;

class AsyncUpdateCheckTask extends AsyncTask {

    public function onRun(): void {
        try {
            $ch = curl_init("https://raw.githubusercontent.com/PocketCloudSystem/PocketCloud/main/src/pocketcloud/resources/version.yml");
            curl_setopt_array($ch, [
                    CURLOPT_SSL_VERIFYPEER => false,
                    CURLOPT_SSL_VERIFYHOST => false,
                    CURLOPT_RETURNTRANSFER => true,
                    CURLOPT_HEADER => false
                ]
            );

            $result = curl_exec($ch);
            $data = @yaml_parse($result);
            if ($data == false) {
                $this->setResult([false]);
            } else {
                if (isset($data["version"])) {
                    $this->setResult([$data["version"]]);
                } else {
                    $this->setResult([false]);
                }
            }
        } catch (\Exception $e) {
            $this->setResult([false]);
        }
    }

    public function onCompletion(): void {
        if ($this->getResult()[0] == false) {
            CloudLogger::get()->error("§cError occurred while checking for new updates!");
        } else {
            $current = explode(".", UpdateChecker::getInstance()->getCurrentVersion());
            $latest = explode(".", $this->getResult()[0]);
            $outdated = false;

            $i = 0;
            foreach ($current as $number) {
                if (intval($latest[$i]) > intval($number)) {
                    $outdated = true;
                    break;
                }
                $i++;
            }

            UpdateChecker::getInstance()->setData(["outdated" => $outdated, "newest_version" => $this->getResult()[0]]);

            if ($outdated) {
                CloudLogger::get()->warn("§cYour version of §bPocket§3Cloud §cis outdated! Please install the newest version from §8'§bgithub.com/PocketCloudSystem/PocketCloud/releases/latest§8'§c!");
                CloudLogger::get()->warn("§cYour Version: §e" . VersionInfo::VERSION . " §8| §cLatest Version: §e" . $this->getResult()[0]);
                CloudLogger::get()->warn("§cAlso make sure that the plugins are up to date!");
            } else {
                CloudLogger::get()->info("§aYour version of §bPocket§3Cloud §ais up to date!");
            }
        }
    }
}
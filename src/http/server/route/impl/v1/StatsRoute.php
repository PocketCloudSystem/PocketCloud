<?php

namespace pocketcloud\cloud\http\server\route\impl\v1;

use pocketcloud\cloud\group\ServerGroupManager;
use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\Response;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\ApiPath;
use pocketcloud\cloud\http\server\socket\auth\NoAuthRequiredAuthentication;
use pocketcloud\cloud\http\server\util\HttpConstants;
use pocketcloud\cloud\http\server\util\StatusCode;
use pocketcloud\cloud\http\server\version\ApiVersion;
use pocketcloud\cloud\player\CloudPlayerManager;
use pocketcloud\cloud\plugin\CloudPluginManager;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\template\TemplateManager;
use pocketcloud\cloud\traffic\TrafficMonitorManager;
use pocketcloud\cloud\util\VersionInfo;

final class StatsRoute extends ApiPath {

    public function __construct() {
        parent::__construct("/stats", ApiVersion::V1, HttpConstants::GET, new NoAuthRequiredAuthentication());
    }

    public function handle(Request $request): Response {
        $currentVersion = VersionInfo::VERSION;
        $isBeta = VersionInfo::BETA;
        $serverCount = count(CloudServerManager::getInstance()->getAll());
        $playerCount = count(CloudPlayerManager::getInstance()->getAll());
        $templateCount = count(TemplateManager::getInstance()->getAll());
        $serverGroupCount = count(ServerGroupManager::getInstance()->getAll());
        $pluginCount = count(CloudPluginManager::getInstance()->getAll());
        $uptime = PocketCloud::getInstance()->getUptime();
        $totalAvgTraffic = TrafficMonitorManager::getInstance()->getTotalAllAverageTimeTraffic();
        $totalTraffic = TrafficMonitorManager::getInstance()->getTotalTraffic();
        return ResponseBuilder::create()
            ->code(StatusCode::OK)
            ->body([
                "version" => $currentVersion,
                "beta" => $isBeta,
                "server_count" => $serverCount,
                "player_count" => $playerCount,
                "template_count" => $templateCount,
                "server_group_count" => $serverGroupCount,
                "plugin_count" => $pluginCount,
                "uptime" => $uptime,
                "total_avg_traffic" => [
                    "in" => $totalAvgTraffic[0],
                    "out" => $totalAvgTraffic[1],
                ],
                "total_traffic" => [
                    "in" => $totalTraffic[0],
                    "out" => $totalTraffic[1],
                ]
            ])
            ->build();
    }

    public function isBadRequest(Request $request, ResponseBuilder $response): bool {
        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
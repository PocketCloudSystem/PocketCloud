<?php

namespace pocketcloud\cloud\http\server\route\impl\v1\server;

use pocketcloud\cloud\group\ServerGroupManager;
use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\util\RequestMethod;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\template\TemplateManager;

final class ListServersRoute extends ApiV1JsonPath {

    public function __construct() {
        parent::__construct(
            "/servers",
            RequestMethod::GET,
            0
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $template = $request->getQuery("template");
        $group = $request->getQuery("group");
        $availableFilters = [];
        if ($template !== null) $availableFilters[] = TemplateManager::getInstance()->get($template);
        if ($group !== null) $availableFilters[] = ServerGroupManager::getInstance()->get($group);

        $serverList = [];
        foreach (CloudServerManager::getInstance()->getAll(...$availableFilters) as $server) {
            $serverList[$server->getName()] = [
                "name" => $server->getName(),
                "uuid" => $server->getServerUuid(),
                "player_count" => $server->getPlayerCount(),
                "max_players" => $server->getServerData()->getMaxPlayers(),
                "status" => $server->getServerStatus()?->getName()
            ];
        }

        $builder->body($serverList);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        $template = $request->getQuery("template");
        $group = $request->getQuery("group");

        if ($template !== null && !TemplateManager::getInstance()->check($template)) {
            $response->body(["message" => "Template does not exist."]);
            return true;
        }

        if ($group !== null && !ServerGroupManager::getInstance()->check($group)) {
            $response->body(["message" => "ServerGroup does not exist."]);
            return true;
        }

        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
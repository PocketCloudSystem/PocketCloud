<?php

namespace pocketcloud\cloud\http\server\route\impl\v1\player;

use pocketcloud\cloud\group\ServerGroupManager;
use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\util\RequestMethod;
use pocketcloud\cloud\player\CloudPlayerManager;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\template\TemplateManager;

final class ListPlayerRoute extends ApiV1JsonPath {

    public function __construct() {
        parent::__construct(
            "/players",
            RequestMethod::GET,
            0
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $server = $request->getQuery("server");
        $template = $request->getQuery("template");
        $group = $request->getQuery("group");
        $availableFilters = [];
        if ($server !== null) $availableFilters[] = CloudServerManager::getInstance()->get($server);
        if ($template !== null) $availableFilters[] = TemplateManager::getInstance()->get($template);
        if ($group !== null) $availableFilters[] = ServerGroupManager::getInstance()->get($group);
        $availableFilter = array_values(array_slice(array_filter($availableFilters, fn(mixed $v) => $v !==
            null), 0, 1));

        $playerList = [];
        foreach (CloudPlayerManager::getInstance()->getAll($availableFilter[0] ?? null) as $player) {
            $playerList[$player->getName()] = [
                "name" => $player->getName(),
                "xbox_id" => $player->getXboxUserId(),
                "server" => $player->getCurrentServerName(),
                "proxy" => $player->getCurrentProxyName()
            ];
        }

        $builder->body($playerList);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        $server = $request->getQuery("server");
        $template = $request->getQuery("template");
        $group = $request->getQuery("group");

        if ($server !== null && !CloudServerManager::getInstance()->get($server)) {
            $response->body(["message" => "Server does not exist."]);
            return true;
        }

        if ($template !== null && !TemplateManager::getInstance()->check($template)) {
            $response->body(["message" => "Template does not exist."]);
            return true;
        }

        if ($group !== null && !ServerGroupManager::getInstance()->check($group)) {
            $response->body(["message" => "ServerGroup does not exist."]);
            return true;
        }

        $array = [$server, $template, $group];
        $alreadyGiven = false;
        foreach ($array as $v) {
            if ($alreadyGiven && $v !== null) {
                $response->body(["message" => "You can only apply one of the following filters: server, template, group."]);
                return true;
            }

            if ($v !== null) $alreadyGiven = true;
        }

        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
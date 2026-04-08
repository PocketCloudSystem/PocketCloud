<?php

namespace pocketcloud\cloud\http\server\route\impl\v1\group;

use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\group\ServerGroupManager;
use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\server\util\HttpConstants;

final class ListGroupsRoute extends ApiV1JsonPath {

    public function __construct() {
        parent::__construct(
            "/groups",
            HttpConstants::GET,
            0
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $groups = array_map(fn(ServerGroup $group) => ["name" => $group->getName(), "player_count" => $group->getPlayerCount()], ServerGroupManager::getInstance()->getAll());
        $builder->body($groups);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
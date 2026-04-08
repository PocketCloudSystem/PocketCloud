<?php

namespace pocketcloud\cloud\http\server\route\impl\v1\group;

use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\group\ServerGroupManager;
use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\server\util\HttpConstants;

final class CreateGroupRoute extends ApiV1JsonPath {

    public const array EXAMPLE_PAYLOAD = [
        "name" => "BedWars",
        "templates" => ["BW-2x1"]
    ];

    public function __construct() {
        parent::__construct(
            "/groups/",
            HttpConstants::POST,
            2**9,
            [
                "name" => "string",
                "templates" => "array"
            ]
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $group = ServerGroup::read($requestBody);
        ServerGroupManager::getInstance()->create($group);
        $builder->body(["message" => "Created the group."]);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        $group = ServerGroup::read($body);
        if ($group === null) {
            $response->body(["message" => "Invalid group object."]);
            return true;
        }

        if (ServerGroupManager::getInstance()->check($group->getName())) {
            $response->body(["message" => "Group already exists."]);
            return true;
        }

        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
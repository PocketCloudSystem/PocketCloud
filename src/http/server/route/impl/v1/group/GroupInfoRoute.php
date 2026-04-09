<?php

namespace pocketcloud\cloud\http\server\route\impl\v1\group;

use pocketcloud\cloud\group\ServerGroupManager;
use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\util\RequestMethod;

final class GroupInfoRoute extends ApiV1JsonPath {

    public function __construct() {
        parent::__construct(
            "/groups/{name}",
            RequestMethod::GET,
            0
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $group = ServerGroupManager::getInstance()->get($request->getParameter("name"));
        $builder->body($group->write());
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        $group = $request->getParameter("name");
        if ($group === null) {
            $response->body(["message" => "Please specify a group name."]);
            return true;
        }

        if (ServerGroupManager::getInstance()->get($group) === null) {
            $response->body(["message" => "Group not found."]);
            return true;
        }

        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
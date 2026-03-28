<?php

namespace pocketcloud\cloud\http\route\impl\v1\server;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\ResponseBuilder;
use pocketcloud\cloud\http\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\util\HttpConstants;
use pocketcloud\cloud\server\CloudServerManager;

final class ServerInfoRoute extends ApiV1JsonPath {

    public function __construct() {
        parent::__construct(
            "/servers/{name}",
            HttpConstants::GET,
            0
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $server = CloudServerManager::getInstance()->get($request->getParameter("name"));
        $builder->body($server->writeDetailed());
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        $server = $request->getParameter("name");
        if ($server === null) {
            $response->body(["message" => "Please specify a server name or uuid."]);
            return true;
        }

        if (CloudServerManager::getInstance()->get($server) === null) {
            $response->body(["message" => "Server not found."]);
            return true;
        }

        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
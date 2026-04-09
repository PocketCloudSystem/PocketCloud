<?php

namespace pocketcloud\cloud\http\server\route\impl\v1\server;

use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\server\util\StatusCode;
use pocketcloud\cloud\http\util\RequestMethod;
use pocketcloud\cloud\server\CloudServerManager;

final class ServerLogsRoute extends ApiV1JsonPath {

    public function __construct() {
        parent::__construct(
            "/servers/{name}/logs",
            RequestMethod::GET,
            0
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $server = CloudServerManager::getInstance()->get($request->getParameter("name"));
        $logs = $server->retrieveLogs();
        if ($logs === null) {
            $builder->code(StatusCode::INTERNAL_SERVER_ERROR);
            $builder->body(["message" => "Failed to retrieve server logs."]);
            return;
        }

        $plainLogs = implode("\n", $logs);
        $builder->contentType("text/plain; charset=utf-8");
        $builder->body($plainLogs);
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
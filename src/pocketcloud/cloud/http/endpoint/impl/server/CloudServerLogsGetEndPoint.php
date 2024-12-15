<?php

namespace pocketcloud\cloud\http\endpoint\impl\server;

use pocketcloud\cloud\http\endpoint\EndPoint;
use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;
use pocketcloud\cloud\server\CloudServerManager;

final class CloudServerLogsGetEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/server/logs/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $server = $request->data()->queries()->get("server");

        if (($server = CloudServerManager::getInstance()->get($server)) === null) {
            return ["error" => "The server doesn't exists!"];
        }

        if (($logs = $server->retrieveLogs()) === null) {
            return ["error" => "No logs were found!"];
        }

        return [
            "server" => $server->getName(),
            "logs" => $logs
        ];
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("server");
    }
}
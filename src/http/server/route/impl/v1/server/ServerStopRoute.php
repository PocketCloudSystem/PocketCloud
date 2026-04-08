<?php

namespace pocketcloud\cloud\http\server\route\impl\v1\server;

use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\server\util\HttpConstants;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\CloudServerManager;

final class ServerStopRoute extends ApiV1JsonPath {

    public const array EXAMPLE_PAYLOAD = [
        "force" => true
    ];

    public function __construct() {
        parent::__construct(
            "/servers/{name}/stop",
            HttpConstants::POST,
            32,
            ["force" => "boolean"]
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $force = $requestBody["force"] ?? false;
        $affectedServers = CloudServerManager::getInstance()->stop($request->getParameter("name"), $force);
        $servers = array_map(fn(CloudServer $server) => ["name" => $server->getName(), "uuid" => $server->getServerUuid()], $affectedServers);
        $builder->body($servers);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        $server = $request->getParameter("name");
        if ($server === null) {
            $response->body(["message" => "Please specify a server name or uuid, a template name or a server group name."]);
            return true;
        }

        if (count(CloudServerManager::getInstance()->getAll($server)) == 0) {
            $response->body(["message" => "Server(s) not found."]);
            return true;
        }

        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
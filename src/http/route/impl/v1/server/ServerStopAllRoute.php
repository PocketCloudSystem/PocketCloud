<?php

namespace pocketcloud\cloud\http\route\impl\v1\server;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\ResponseBuilder;
use pocketcloud\cloud\http\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\util\HttpConstants;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\CloudServerManager;

final class ServerStopAllRoute extends ApiV1JsonPath {

    public const array EXAMPLE_PAYLOAD = [
        "force" => true
    ];

    public function __construct() {
        parent::__construct(
            "/servers/stopAll",
            HttpConstants::POST,
            32,
            ["force" => "boolean"]
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $force = $requestBody["force"] ?? false;
        $servers = array_map(fn(CloudServer $server) => ["name" => $server->getName(), "uuid" => $server->getServerUuid()], CloudServerManager::getInstance()->stopAll($force));
        $builder->body($servers);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
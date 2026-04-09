<?php

namespace pocketcloud\cloud\http\server\route\impl\v1\server;

use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\util\RequestMethod;
use pocketcloud\cloud\server\CloudServerManager;

final class ServerSendCommandRoute extends ApiV1JsonPath {

    public const array EXAMPLE_PAYLOAD = [
        "command" => "op r3pt1s"
    ];

    public function __construct() {
        parent::__construct(
            "/servers/{name}/execute",
            RequestMethod::POST,
            2 ** 8,
            ["command" => "string"]
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $server = CloudServerManager::getInstance()->get($request->getParameter("name"));
        $command = $requestBody["command"];
        $server->executeCommand($command);
        $builder->body(["message" => "Attempted to execute the command on the server."]);
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
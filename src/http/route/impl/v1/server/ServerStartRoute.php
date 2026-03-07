<?php

namespace pocketcloud\cloud\http\route\impl\v1\server;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\ResponseBuilder;
use pocketcloud\cloud\http\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\util\HttpConstants;
use pocketcloud\cloud\http\util\StatusCode;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\template\TemplateManager;

final class ServerStartRoute extends ApiV1JsonPath {

    public const array EXAMPLE_PAYLOAD = [
        "template" => "Lobby",
        "count" => 2
    ];

    public function __construct() {
        parent::__construct(
            "/servers/start",
            HttpConstants::POST,
            2**8,
            ["template" => "string", "count" => "integer"]
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $template = TemplateManager::getInstance()->get($requestBody["template"]);
        $count = $requestBody["count"];
        if (!CloudServerManager::getInstance()->checkCapacity($template)) {
            $builder->code(StatusCode::CONFLICT);
            $builder->body(["message" => "The maximum amount of servers for this template has already been reached."]);
            return;
        }

        $startedServers = CloudServerManager::getInstance()->start($template, $count);
        $builder->body(["message" => "Attempted to start " . $count . " server(s).", "started_servers" => $startedServers]);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        $template = $body["template"];
        $count = $body["count"];

        if (!TemplateManager::getInstance()->check($template)) {
            $response->body(["message" => "Template does not exist."]);
            return true;
        }

        if ($count < 1) {
            $response->body(["message" => "The requested amount cannot be less than 1."]);
            return true;
        }

        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
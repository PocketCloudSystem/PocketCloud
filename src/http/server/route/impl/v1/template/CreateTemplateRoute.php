<?php

namespace pocketcloud\cloud\http\server\route\impl\v1\template;

use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\util\RequestMethod;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateManager;

final class CreateTemplateRoute extends ApiV1JsonPath {

    public const array EXAMPLE_PAYLOAD = [
        "name" => "Lobby",
        "lobby" => true,
        "maintenance" => false,
        "static" => false,
        "alwaysCopyToStaticServers" => false,
        "maxPlayerCount" => 20,
        "minServerCount" => 1,
        "maxServerCount" => 2,
        "startNewPercentage" => 30.0,
        "autoStart" => true,
        "templateType" => "SERVER"
    ];

    public function __construct() {
        parent::__construct(
            "/templates/",
            RequestMethod::POST,
            2 ** 9,
            [
                "name" => "string",
                "lobby" => "boolean",
                "maintenance" => "boolean",
                "static" => "boolean",
                "alwaysCopyToStaticServers" => "boolean",
                "maxPlayerCount" => "integer",
                "minServerCount" => "integer",
                "maxServerCount" => "integer",
                "startNewPercentage" => "double|integer",
                "autoStart" => "boolean",
                "templateType" => "string"
            ]
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $template = Template::read($requestBody);
        TemplateManager::getInstance()->create($template);
        $builder->body(["message" => "Created the template."]);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        $template = Template::read($body);
        if ($template === null) {
            $response->body(["message" => "Invalid template object."]);
            return true;
        }

        if (TemplateManager::getInstance()->check($template->getName())) {
            $response->body(["message" => "Template already exists."]);
            return true;
        }

        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
<?php

namespace pocketcloud\cloud\http\route\impl\v1\template;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\ResponseBuilder;
use pocketcloud\cloud\http\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\util\HttpConstants;
use pocketcloud\cloud\template\TemplateHelper;
use pocketcloud\cloud\template\TemplateManager;

final class EditTemplateRoute extends ApiV1JsonPath {

    public const array EXAMPLE_PAYLOAD = [
        "lobby" => true,
        "maintenance" => false,
        "static" => false,
        "alwaysCopyToStaticServers" => false,
        "maxPlayerCount" => 20,
        "minServerCount" => 1,
        "maxServerCount" => 2,
        "startNewPercentage" => 30,
        "autoStart" => true,
        "templateType" => "SERVER"
    ];

    public function __construct() {
        parent::__construct(
            "/templates/{name}",
            HttpConstants::PATCH,
            2**9
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $template = TemplateManager::getInstance()->get($request->getParameter("name"));
        TemplateManager::getInstance()->edit(
            $template,
            $requestBody["lobby"] ?? null,
            $requestBody["maintenance"] ?? null,
            $requestBody["static"] ?? null,
            $requestBody["alwaysCopyToStaticServers"] ?? null,
            $requestBody["maxPlayerCount"] ?? null,
            $requestBody["minServerCount"] ?? null,
            $requestBody["maxServerCount"] ?? null,
            $requestBody["startNewPercentage"] ?? null,
            $requestBody["autoStart"] ?? null
        );
        $builder->body(["message" => "Edited the template."]);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        $template = $request->getParameter("name");
        if ($template === null) {
            $response->body(["message" => "Please specify a template name."]);
            return true;
        }

        if (!TemplateManager::getInstance()->check($template)) {
            $response->body(["message" => "Template not found."]);
            return true;
        }

        $detectedKey = null;
        foreach (array_keys($body) as $key) {
            if (!in_array($key, TemplateHelper::EDITABLE_KEYS)) {
                $detectedKey = $key;
                break;
            }

            if (!TemplateHelper::checkRawValue($body[$key], $key, $expectedValue)) {
                $response->body(["message" => "Invalid value for key: $key, expected: $expectedValue, got " . gettype($body[$key])]);
                return true;
            }
        }

        if ($detectedKey !== null) {
            $response->body(["message" => "The key: $detectedKey is not allowed inside the request body."]);
            return true;
        }

        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
<?php

namespace pocketcloud\cloud\http\route\impl\v1\plugin;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\ResponseBuilder;
use pocketcloud\cloud\http\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\util\HttpConstants;
use pocketcloud\cloud\plugin\CloudPluginManager;

final class EnableAllPluginsRoute extends ApiV1JsonPath {

    public function __construct() {
        parent::__construct(
            "/plugins/enableAll",
            HttpConstants::POST,
            0
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        CloudPluginManager::getInstance()->enableAll();
        $builder->body(["message" => "All plugins have been enabled."]);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
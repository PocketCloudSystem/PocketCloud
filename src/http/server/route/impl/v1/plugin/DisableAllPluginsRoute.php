<?php

namespace pocketcloud\cloud\http\server\route\impl\v1\plugin;

use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\util\RequestMethod;
use pocketcloud\cloud\plugin\CloudPluginManager;

final class DisableAllPluginsRoute extends ApiV1JsonPath {

    public function __construct() {
        parent::__construct(
            "/plugins/disableAll",
            RequestMethod::POST,
            0
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        CloudPluginManager::getInstance()->disableAll();
        $builder->body(["message" => "All plugins have been disabled."]);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
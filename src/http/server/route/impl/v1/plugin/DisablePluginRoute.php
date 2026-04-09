<?php

namespace pocketcloud\cloud\http\server\route\impl\v1\plugin;

use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\util\RequestMethod;
use pocketcloud\cloud\plugin\CloudPluginManager;

final class DisablePluginRoute extends ApiV1JsonPath {

    public function __construct() {
        parent::__construct(
            "/plugins/{name}/disable",
            RequestMethod::POST,
            0
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $plugin = CloudPluginManager::getInstance()->get($request->getQuery("name"));
        if ($plugin->isDisabled()) {
            $builder->body(["message" => "Plugin is already disabled."]);
            return;
        }

        CloudPluginManager::getInstance()->disablePlugin($plugin);
        $builder->body(["message" => "Plugin has been disabled."]);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        $plugin = $request->getParameter("name");
        if ($plugin === null) {
            $response->body(["message" => "Please specify a plugin name."]);
            return true;
        }

        if (CloudPluginManager::getInstance()->get($plugin) === null) {
            $response->body(["message" => "Plugin not found."]);
            return true;
        }

        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
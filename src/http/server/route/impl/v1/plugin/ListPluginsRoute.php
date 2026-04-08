<?php

namespace pocketcloud\cloud\http\server\route\impl\v1\plugin;

use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\server\util\HttpConstants;
use pocketcloud\cloud\plugin\CloudPlugin;
use pocketcloud\cloud\plugin\CloudPluginManager;

final class ListPluginsRoute extends ApiV1JsonPath {

    public const array EXAMPLE_PAYLOAD = [
        "enabled" => false // -> only "enabled" plugins will be shown
    ];

    public function __construct() {
        parent::__construct(
            "/plugins",
            HttpConstants::GET,
            32,
            ["enabled" => "boolean"]
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $plugins = array_map(fn(CloudPlugin $plugin) => [
            "name" => $plugin->getDescription()->getFullName(),
            "authors" => $plugin->getDescription()->getAuthors(),
            "version" => $plugin->getDescription()->getVersion()
        ], CloudPluginManager::getInstance()->getAll($requestBody["enabled"]));

        $builder->body($plugins);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
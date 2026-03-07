<?php

namespace pocketcloud\cloud\http\route\impl\v1\plugin;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\ResponseBuilder;
use pocketcloud\cloud\http\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\util\HttpConstants;
use pocketcloud\cloud\plugin\CloudPluginManager;

final class PluginInfoRoute extends ApiV1JsonPath {

    public function __construct() {
        parent::__construct(
            "/plugins/{name}",
            HttpConstants::GET,
            0
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $plugin = CloudPluginManager::getInstance()->get($request->getParameter("name"));
        $builder->body([
            "name" => $plugin->getDescription()->getName(),
            "version" => $plugin->getDescription()->getVersion(),
            "full_name" => $plugin->getDescription()->getFullName(),
            "authors" => $plugin->getDescription()->getAuthors(),
            "main" => $plugin->getDescription()->getMain(),
            "src_namespace_prefix" => $plugin->getDescription()->getsrcNamespacePrefix(),
            "data_folder" => $plugin->getDataFolder()
        ]);
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
<?php

namespace pocketcloud\cloud\http\endpoint\impl\maintenance;

use pocketcloud\cloud\cache\MaintenanceList;
use pocketcloud\cloud\http\endpoint\EndPoint;
use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;
use pocketcloud\cloud\provider\CloudProvider;

class MaintenanceRemoveEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::DELETE, "/maintenance/remove/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $player = $request->data()->queries()->get("player");

        if (!MaintenanceList::is($player)) {
            return ["error" => "The player is not on the maintenance list!"];
        }

        CloudProvider::current()->removeFromWhitelist($player);
        return ["success" => "The player was removed from the maintenance list!"];
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("player");
    }
}
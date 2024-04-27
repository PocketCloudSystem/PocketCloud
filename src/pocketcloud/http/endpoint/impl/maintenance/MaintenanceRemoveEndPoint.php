<?php

namespace pocketcloud\http\endpoint\impl\maintenance;

use pocketcloud\config\impl\MaintenanceList;
use pocketcloud\http\endpoint\EndPoint;
use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\util\Router;

class MaintenanceRemoveEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::DELETE, "/maintenance/remove/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $player = $request->data()->queries()->get("player");

        if (!MaintenanceList::is($player)) {
            return ["error" => "The player is not on the maintenance list!"];
        }

        MaintenanceList::remove($player);
        return ["success" => "The player was removed from the maintenance list!"];
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("player");
    }
}
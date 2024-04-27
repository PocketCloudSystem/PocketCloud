<?php

namespace pocketcloud\http\endpoint\impl\maintenance;

use pocketcloud\config\impl\MaintenanceList;
use pocketcloud\http\endpoint\EndPoint;
use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\util\Router;

class MaintenanceGetEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/maintenance/get/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $player = $request->data()->queries()->get("player");
        return ["player" => $player, "status" => MaintenanceList::is($player)];
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("player");
    }
}
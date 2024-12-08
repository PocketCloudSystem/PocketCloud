<?php

namespace pocketcloud\cloud\http\endpoint\impl\maintenance;

use pocketcloud\cloud\cache\MaintenanceList;
use pocketcloud\cloud\http\endpoint\EndPoint;
use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;

final class MaintenanceGetEndPoint extends EndPoint {

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
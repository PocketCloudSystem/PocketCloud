<?php

namespace pocketcloud\cloud\http\endpoint\impl\maintenance;

use pocketcloud\cloud\cache\MaintenanceList;
use pocketcloud\cloud\http\endpoint\EndPoint;
use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;

final class MaintenanceListEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/maintenance/list/");
    }

    public function handleRequest(Request $request, Response $response): array {
        return MaintenanceList::getAll();
    }

    public function isBadRequest(Request $request): bool {
        return false;
    }
}
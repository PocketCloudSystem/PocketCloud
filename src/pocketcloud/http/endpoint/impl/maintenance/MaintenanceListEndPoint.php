<?php

namespace pocketcloud\http\endpoint\impl\maintenance;

use pocketcloud\config\impl\MaintenanceList;
use pocketcloud\http\endpoint\EndPoint;
use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\util\Router;

class MaintenanceListEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/maintenance/list/");
    }

    public function handleRequest(Request $request, Response $response): array {
        return MaintenanceList::all();
    }

    public function isBadRequest(Request $request): bool {
        return false;
    }
}
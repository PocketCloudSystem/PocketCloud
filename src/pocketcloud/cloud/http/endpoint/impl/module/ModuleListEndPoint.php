<?php

namespace pocketcloud\cloud\http\endpoint\impl\module;

use pocketcloud\cloud\cache\InGameModule;
use pocketcloud\cloud\http\endpoint\EndPoint;
use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;

final class ModuleListEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/module/list/");
    }

    public function handleRequest(Request $request, Response $response): array {
        return InGameModule::getAll();
    }

    public function isBadRequest(Request $request): bool {
        return false;
    }
}
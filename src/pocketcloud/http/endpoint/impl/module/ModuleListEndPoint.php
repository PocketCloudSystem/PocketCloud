<?php

namespace pocketcloud\http\endpoint\impl\module;

use pocketcloud\http\endpoint\EndPoint;
use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\util\Router;

class ModuleListEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/module/list/");
    }

    public function handleRequest(Request $request, Response $response): array {
        return ["signModule", "npcModule", "globalChatModule", "hubCommandModule"];
    }

    public function isBadRequest(Request $request): bool {
        return false;
    }
}
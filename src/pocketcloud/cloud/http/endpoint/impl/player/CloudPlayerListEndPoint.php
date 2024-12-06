<?php

namespace pocketcloud\cloud\http\endpoint\impl\player;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;
use pocketcloud\cloud\player\CloudPlayer;
use pocketcloud\cloud\player\CloudPlayerManager;
use pocketcloud\cloud\http\endpoint\EndPoint;

class CloudPlayerListEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/player/list/");
    }

    public function handleRequest(Request $request, Response $response): array {
        return array_values(array_map(fn(CloudPlayer $player) => $player->getName(), CloudPlayerManager::getInstance()->getAll()));
    }

    public function isBadRequest(Request $request): bool {
        return false;
    }
}
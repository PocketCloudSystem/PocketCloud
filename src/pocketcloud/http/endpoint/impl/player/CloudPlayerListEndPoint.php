<?php

namespace pocketcloud\http\endpoint\impl\player;

use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\util\Router;
use pocketcloud\player\CloudPlayer;
use pocketcloud\player\CloudPlayerManager;
use pocketcloud\http\endpoint\EndPoint;

class CloudPlayerListEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/player/list/");
    }

    public function handleRequest(Request $request, Response $response): array {
        return array_values(array_map(fn(CloudPlayer $player) => $player->getName(), CloudPlayerManager::getInstance()->getPlayers()));
    }

    public function isBadRequest(Request $request): bool {
        return false;
    }
}
<?php

namespace pocketcloud\rest\endpoint\impl\player;

use pocketcloud\lib\express\io\Request;
use pocketcloud\lib\express\io\Response;
use pocketcloud\lib\express\route\Router;
use pocketcloud\player\CloudPlayer;
use pocketcloud\player\CloudPlayerManager;
use pocketcloud\rest\endpoint\EndPoint;

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
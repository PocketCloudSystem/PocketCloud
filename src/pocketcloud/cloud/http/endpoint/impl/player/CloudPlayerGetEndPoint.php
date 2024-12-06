<?php

namespace pocketcloud\cloud\http\endpoint\impl\player;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;
use pocketcloud\cloud\player\CloudPlayerManager;
use pocketcloud\cloud\http\endpoint\EndPoint;

class CloudPlayerGetEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/player/get/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $identifier = $request->data()->queries()->get("identifier");
        $player = CloudPlayerManager::getInstance()->get($identifier);
        if ($player === null) {
            return ["error" => "Player is not online!"];
        }

        return $player->toArray();
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("identifier");
    }
}
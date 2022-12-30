<?php

namespace pocketcloud\rest\endpoint\impl\player;

use pocketcloud\lib\express\io\Request;
use pocketcloud\lib\express\io\Response;
use pocketcloud\lib\express\route\Router;
use pocketcloud\player\CloudPlayerManager;
use pocketcloud\rest\endpoint\EndPoint;

class CloudPlayerKickEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::POST, "/player/kick/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $identifier = $request->data()->queries()->get("identifier");
        $player = CloudPlayerManager::getInstance()->getPlayerByName($identifier) ?? CloudPlayerManager::getInstance()->getPlayerByUniqueId($identifier) ?? CloudPlayerManager::getInstance()->getPlayerByXboxUserId($identifier);
        if ($player === null) {
            return ["error" => "Player is not online!"];
        }

        $player->kick($request->data()->queries()->get("reason") ?? "");
        return ["success" => "Player was successfully kicked!"];
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("identifier");
    }
}
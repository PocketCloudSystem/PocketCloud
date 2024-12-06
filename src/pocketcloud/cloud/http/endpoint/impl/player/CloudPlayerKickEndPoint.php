<?php

namespace pocketcloud\cloud\http\endpoint\impl\player;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;
use pocketcloud\cloud\player\CloudPlayerManager;
use pocketcloud\cloud\http\endpoint\EndPoint;

class CloudPlayerKickEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::POST, "/player/kick/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $identifier = $request->data()->queries()->get("identifier");
        $player = CloudPlayerManager::getInstance()->get($identifier) ?? CloudPlayerManager::getInstance()->getByUniqueId($identifier) ?? CloudPlayerManager::getInstance()->getByXboxId($identifier);
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
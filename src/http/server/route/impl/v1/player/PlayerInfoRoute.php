<?php

namespace pocketcloud\cloud\http\server\route\impl\v1\player;

use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\server\util\HttpConstants;
use pocketcloud\cloud\player\CloudPlayerManager;

final class PlayerInfoRoute extends ApiV1JsonPath {

    public function __construct() {
        parent::__construct(
            "/players/{name}",
            HttpConstants::GET,
            0
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $player = CloudPlayerManager::getInstance()->get($request->getParameter("name"));
        $builder->body($player->write());
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        $player = $request->getParameter("name");
        if ($player === null) {
            $response->body(["message" => "Please specify a player name."]);
            return true;
        }

        if (CloudPlayerManager::getInstance()->get($player) === null) {
            $response->body(["message" => "Player not found."]);
            return true;
        }

        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
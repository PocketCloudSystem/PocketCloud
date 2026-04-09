<?php

namespace pocketcloud\cloud\http\server\route\impl\v1\player;

use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\util\RequestMethod;
use pocketcloud\cloud\player\CloudPlayerManager;
use pocketcloud\cloud\server\CloudServerManager;

final class TransferPlayerRoute extends ApiV1JsonPath {

    public const array EXAMPLE_PAYLOAD = [
        "server" => "Lobby-1"
    ];

    public function __construct() {
        parent::__construct(
            "/players/{name}/transfer",
            RequestMethod::POST,
            64,
            ["server" => "string"]
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $player = CloudPlayerManager::getInstance()->get($request->getParameter("name"));
        $server = CloudServerManager::getInstance()->get($requestBody["server"]);
        $player->transfer($server);
        $builder->body(["message" => "Attempted to transfer the player."]);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        $player = $request->getParameter("name");
        $server = $body["server"];
        if ($player === null) {
            $response->body(["message" => "Please specify a player name."]);
            return true;
        }

        if (CloudPlayerManager::getInstance()->get($player) === null) {
            $response->body(["message" => "Player not found."]);
            return true;
        }

        if (CloudServerManager::getInstance()->get($server) === null) {
            $response->body(["message" => "Server not found."]);
            return true;
        }

        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
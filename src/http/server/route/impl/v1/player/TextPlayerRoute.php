<?php

namespace pocketcloud\cloud\http\server\route\impl\v1\player;

use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\server\util\HttpConstants;
use pocketcloud\cloud\network\packet\data\TextType;
use pocketcloud\cloud\player\CloudPlayerManager;

final class TextPlayerRoute extends ApiV1JsonPath {

    public const array EXAMPLE_PAYLOAD = [
        "type" => "MESSAGE",
        "message" => "How are you?"
    ];

    public function __construct() {
        parent::__construct(
            "/players/{name}/text",
            HttpConstants::POST,
            2**10,
            ["type" => "string", "message" => "string"]
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $player = CloudPlayerManager::getInstance()->get($request->getParameter("name"));
        $type = TextType::fromName($requestBody["type"]);
        $message = $requestBody["message"];

        $player->send($message, $type);
        $builder->body(["message" => "Attempted to text the player."]);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        $player = $request->getParameter("name");
        $type = $body["type"];
        if ($player === null) {
            $response->body(["message" => "Please specify a player name."]);
            return true;
        }

        if (CloudPlayerManager::getInstance()->get($player) === null) {
            $response->body(["message" => "Player not found."]);
            return true;
        }

        if (TextType::fromName($type) === null) {
            $response->body(["message" => "TextType not found."]);
            return true;
        }

        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
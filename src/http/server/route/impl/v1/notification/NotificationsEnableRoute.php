<?php

namespace pocketcloud\cloud\http\server\route\impl\v1\notification;

use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\server\util\HttpConstants;
use pocketcloud\cloud\provider\CloudProvider;

final class NotificationsEnableRoute extends ApiV1JsonPath {

    public function __construct() {
        parent::__construct(
            "/notifications",
            HttpConstants::POST,
            32,
            ["player" => "string"]
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $player = $requestBody["player"];
        CloudProvider::current()->enablePlayerNotifications($player);
        $builder->body(["message" => "Player's notifications have been enabled."]);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
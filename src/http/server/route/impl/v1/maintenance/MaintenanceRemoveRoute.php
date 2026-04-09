<?php

namespace pocketcloud\cloud\http\server\route\impl\v1\maintenance;

use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\util\RequestMethod;
use pocketcloud\cloud\provider\CloudProvider;

final class MaintenanceRemoveRoute extends ApiV1JsonPath {

    public function __construct() {
        parent::__construct(
            "/maintenance",
            RequestMethod::DELETE,
            32,
            ["player" => "string"]
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $player = $requestBody["player"];
        CloudProvider::current()->removeFromWhitelist($player);
        $builder->body(["message" => "Player has been removed from the whitelist."]);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
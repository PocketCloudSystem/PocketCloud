<?php

namespace pocketcloud\cloud\http\route\impl\v1\maintenance;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\ResponseBuilder;
use pocketcloud\cloud\http\route\util\ApiJsonPath;
use pocketcloud\cloud\http\socket\auth\NoAuthRequiredAuthentication;
use pocketcloud\cloud\http\util\HttpConstants;
use pocketcloud\cloud\http\version\ApiVersion;
use pocketcloud\cloud\provider\CloudProvider;

final class MaintenanceAddRoute extends ApiJsonPath {

    public function __construct() {
        parent::__construct(
            "/maintenance",
            ApiVersion::V1,
            HttpConstants::POST,
            32,
            ["player" => "string"],
            new NoAuthRequiredAuthentication()
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $player = $requestBody["player"];
        CloudProvider::current()->addToWhitelist($player);
        $builder->body(["message" => "Player has been added to the whitelist."]);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
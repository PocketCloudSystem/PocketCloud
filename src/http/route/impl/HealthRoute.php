<?php

namespace pocketcloud\cloud\http\route\impl;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\io\ResponseBuilder;
use pocketcloud\cloud\http\route\RegularPath;
use pocketcloud\cloud\http\socket\auth\NoAuthRequiredAuthentication;
use pocketcloud\cloud\http\util\HttpConstants;
use pocketcloud\cloud\http\util\StatusCode;

final class HealthRoute extends RegularPath {

    public function __construct() {
        parent::__construct("/health", HttpConstants::GET, new NoAuthRequiredAuthentication());
    }

    public function handle(Request $request): Response {
        return ResponseBuilder::create()
            ->code(StatusCode::OK)
            ->body(["status" => "ok"])
            ->build();
    }

    public function isBadRequest(Request $request, ResponseBuilder $response): bool {
        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
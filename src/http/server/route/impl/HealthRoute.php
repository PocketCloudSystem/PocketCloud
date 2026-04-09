<?php

namespace pocketcloud\cloud\http\server\route\impl;

use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\Response;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\RegularPath;
use pocketcloud\cloud\http\server\socket\auth\NoAuthRequiredAuthentication;
use pocketcloud\cloud\http\server\util\StatusCode;
use pocketcloud\cloud\http\util\RequestMethod;

final class HealthRoute extends RegularPath {

    public function __construct() {
        parent::__construct("/health", RequestMethod::GET, new NoAuthRequiredAuthentication());
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
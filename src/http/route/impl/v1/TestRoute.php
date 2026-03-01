<?php

namespace pocketcloud\cloud\http\route\impl\v1;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\io\ResponseBuilder;
use pocketcloud\cloud\http\route\ApiPath;
use pocketcloud\cloud\http\socket\auth\NoAuthRequiredAuthentication;
use pocketcloud\cloud\http\util\HttpConstants;
use pocketcloud\cloud\http\version\ApiVersion;

final class TestRoute extends ApiPath {

    public function __construct() {
        parent::__construct("/test", ApiVersion::V1, HttpConstants::GET, new NoAuthRequiredAuthentication());
    }

    public function handle(Request $request): Response {
        return ResponseBuilder::create()
            ->code(200)
            ->body(["just" => "whatever"])
            ->build();
    }

    public function isBadRequest(Request $request, ResponseBuilder $response): bool {
        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
<?php

namespace pocketcloud\cloud\http\route\impl;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\io\ResponseBuilder;
use pocketcloud\cloud\http\route\RegularPath;

final class TestRoute extends RegularPath {

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
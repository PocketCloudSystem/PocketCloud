<?php

namespace pocketcloud\cloud\http\server\route\impl\v1\maintenance;

use pocketcloud\cloud\cache\MaintenanceListCache;
use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\server\util\HttpConstants;

final class ListMaintenanceRoute extends ApiV1JsonPath {

    public function __construct() {
        parent::__construct(
            "/maintenance",
            HttpConstants::GET,
            0
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $builder->body(MaintenanceListCache::getAll());
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
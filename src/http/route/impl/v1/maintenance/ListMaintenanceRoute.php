<?php

namespace pocketcloud\cloud\http\route\impl\v1\maintenance;

use pocketcloud\cloud\cache\MaintenanceListCache;
use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\ResponseBuilder;
use pocketcloud\cloud\http\route\util\ApiJsonPath;
use pocketcloud\cloud\http\socket\auth\NoAuthRequiredAuthentication;
use pocketcloud\cloud\http\util\HttpConstants;
use pocketcloud\cloud\http\version\ApiVersion;

final class ListMaintenanceRoute extends ApiJsonPath {

    public function __construct() {
        parent::__construct(
            "/maintenance",
            ApiVersion::V1,
            HttpConstants::GET,
            0,
            [],
            new NoAuthRequiredAuthentication()
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
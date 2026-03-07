<?php

namespace pocketcloud\cloud\http\route\impl\v1;

use pocketcloud\cloud\http\route\util\ApiJsonPath;
use pocketcloud\cloud\http\socket\auth\Authentication;
use pocketcloud\cloud\http\socket\auth\NoAuthRequiredAuthentication;
use pocketcloud\cloud\http\version\ApiVersion;

abstract class ApiV1JsonPath extends ApiJsonPath {

    public function __construct(string $path, string $requestMethod, int $maxPayloadLength, array $requiredBodyStructure = [], ?Authentication $authentication = null) {
        parent::__construct($path, ApiVersion::V1, $requestMethod, $maxPayloadLength, $requiredBodyStructure, $authentication ?? new NoAuthRequiredAuthentication());
    }
}
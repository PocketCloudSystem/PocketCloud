<?php

namespace pocketcloud\cloud\http\server\route\impl\v1;

use pocketcloud\cloud\http\server\route\util\ApiJsonPath;
use pocketcloud\cloud\http\server\socket\auth\Authentication;
use pocketcloud\cloud\http\server\socket\auth\NoAuthRequiredAuthentication;
use pocketcloud\cloud\http\server\version\ApiVersion;
use pocketcloud\cloud\http\util\RequestMethod;

abstract class ApiV1JsonPath extends ApiJsonPath {

    public function __construct(string $path, RequestMethod $requestMethod, int $maxPayloadLength, array $requiredBodyStructure = [], ?Authentication $authentication = null) {
        parent::__construct($path, ApiVersion::V1, $requestMethod, $maxPayloadLength, $requiredBodyStructure, $authentication
            ??
            new NoAuthRequiredAuthentication());
    }
}
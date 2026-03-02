<?php

namespace pocketcloud\cloud\http\route\util;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\io\ResponseBuilder;
use pocketcloud\cloud\http\route\ApiPath;
use pocketcloud\cloud\http\socket\auth\Authentication;
use pocketcloud\cloud\http\util\StatusCode;
use pocketcloud\cloud\util\Utils;
use Throwable;

abstract class ApiJsonPath extends ApiPath {

    /**
     * For developers
     */
    public const array EXAMPLE_PAYLOAD = [];

    private ?array $requestBody = null;

    /**
     * @param string $path
     * @param string $version
     * @param string $requestMethod
     * @param int $maxPayloadLength
     * @param array $requiredBodyStructure
     * @see Utils::validateArraySignature()
     * @param Authentication $authentication
     */
    public function __construct(
        string $path,
        string $version,
        string $requestMethod,
        private int $maxPayloadLength,
        private readonly array $requiredBodyStructure,
        Authentication $authentication
    ) {
        if ($this->maxPayloadLength < 0) $this->maxPayloadLength = 0;
        parent::__construct($path, $version, $requestMethod, $authentication);
    }

    final public function handle(Request $request): Response {
        $this->onHandle($request, $resBuilder = ResponseBuilder::create()->code(StatusCode::OK), $this->requestBody ?? []);
        $this->requestBody = null;
        return $resBuilder->build();
    }

    abstract public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void;

    final public function isBadRequest(Request $request, ResponseBuilder $response): bool {
        if ($request->getHeader("Content-Type") !== "application/json" && $this->maxPayloadLength > 0) return true;
        $body = substr($request->getBody(), 0, $this->maxPayloadLength + 1);
        if (strlen($body) > $this->maxPayloadLength) {
            $response->code(StatusCode::PAYLOAD_TOO_LARGE);
            return true;
        }

        if ($this->maxPayloadLength == 0) return false;

        try {
            $this->requestBody = json_decode($body, true, 512, JSON_THROW_ON_ERROR);
            Utils::validateArraySignature($this->requestBody, $this->requiredBodyStructure);
            if ($this->checkForBadRequest($request, $response, $this->requestBody)) return true;
            return false;
        } catch (Throwable $e) {
            $response->body(["message" => "Exception occurred while processing your request.", "exception" => $e->getMessage()]);
            $this->requestBody = null;
        }

        return true;
    }

    abstract public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool;
}
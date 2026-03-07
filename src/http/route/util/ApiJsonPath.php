<?php

namespace pocketcloud\cloud\http\route\util;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\io\ResponseBuilder;
use pocketcloud\cloud\http\route\ApiPath;
use pocketcloud\cloud\http\socket\auth\Authentication;
use pocketcloud\cloud\http\util\StatusCode;
use pocketcloud\cloud\util\PathUtils;
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
        try {
            if ($request->getHeader("Content-Type") !== "application/json" && $this->maxPayloadLength > 0) return true;
            $body = substr($request->getBody(), 0, $this->maxPayloadLength + 1);
            if (($bodyPayloadLength = strlen($body)) > $this->maxPayloadLength) {
                $response->code(StatusCode::PAYLOAD_TOO_LARGE);
                return true;
            }

            if ($bodyPayloadLength == 0 && $this->maxPayloadLength > 0) {
                $response->code(StatusCode::BAD_REQUEST);
                $response->body(["message" => "A json body is required to run this request."]);
                return true;
            }

            if ($this->maxPayloadLength == 0) {
                return $this->checkForBadRequest($request, $response, []);
            }

            $this->requestBody = json_decode($body, true, 512, JSON_THROW_ON_ERROR);
            Utils::validateArraySignature($this->requestBody, $this->requiredBodyStructure);
            return $this->checkForBadRequest($request, $response, $this->requestBody);
        } catch (Throwable $e) {
            $this->requestBody = null;
            $response->body([
                "message" => "Exception occurred while processing your request.",
                "exception_type" => $e::class,
                "exception" => $e->getMessage(),
                "code" => $e->getCode(),
                "file" => PathUtils::clean($e->getFile()),
                "line" => $e->getLine()
            ]);
        }

        return true;
    }

    abstract public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool;
}
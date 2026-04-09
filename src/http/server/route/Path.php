<?php

namespace pocketcloud\cloud\http\server\route;

use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\Response;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\socket\auth\Authentication;
use pocketcloud\cloud\http\util\RequestMethod;

interface Path {

    public function handle(Request $request): Response;

    public function handleFailedAuth(Request $request): Response;

    public function isBadRequest(Request $request, ResponseBuilder $response): bool;

    public function willCauseError(Request $request, ResponseBuilder $response): bool;

    public function getApiVersion(): ?string;

    public function getPath(): string;

    public function getFullPath(): string;

    public function getMethod(): RequestMethod;

    public function getAuthentication(): Authentication;
}
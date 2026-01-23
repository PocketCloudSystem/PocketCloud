<?php

namespace pocketcloud\cloud\http\route;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\io\ResponseBuilder;
use pocketcloud\cloud\http\socket\auth\Authentication;

interface Path {

    public function handle(Request $request): Response;

    public function handleFailedAuth(Request $request): Response;

    public function isBadRequest(Request $request, ResponseBuilder $response): bool;

    public function willCauseError(Request $request, ResponseBuilder $response): bool;

    public function getApiVersion(): ?string;

    public function getPath(): string;

    public function getFullPath(): string;

    public function getMethod(): string;

    public function getAuthentication(): Authentication;
}
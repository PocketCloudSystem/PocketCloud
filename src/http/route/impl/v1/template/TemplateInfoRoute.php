<?php

namespace pocketcloud\cloud\http\route\impl\v1\template;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\ResponseBuilder;
use pocketcloud\cloud\http\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\util\HttpConstants;
use pocketcloud\cloud\template\TemplateManager;

final class TemplateInfoRoute extends ApiV1JsonPath {

    public function __construct() {
        parent::__construct(
            "/templates/{name}",
            HttpConstants::GET,
            0
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $template = TemplateManager::getInstance()->get($request->getParameter("name"));
        $builder->body($template->write());
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        $template = $request->getParameter("name");
        if (!TemplateManager::getInstance()->check($template)) {
            $response->body(["message" => "Template not found."]);
            return true;
        }

        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
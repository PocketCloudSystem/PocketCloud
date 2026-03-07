<?php

namespace pocketcloud\cloud\http\route\impl\v1\template;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\ResponseBuilder;
use pocketcloud\cloud\http\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\util\HttpConstants;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateManager;
use pocketcloud\cloud\template\TemplateType;

final class ListTemplatesRoute extends ApiV1JsonPath {

    public function __construct() {
        parent::__construct(
            "/templates",
            HttpConstants::GET,
            0
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $templateType = $request->hasQuery("type") ? TemplateType::get($request->getQuery("type")) : TemplateType::getAll();
        if ($templateType instanceof TemplateType) $templateType = [$templateType];
        $templates = array_map(fn(Template $template) => ["name" => $template->getName(), "player_count" => $template->getPlayerCount(), "maintenance" => $template->isMaintenance()], TemplateManager::getInstance()->getAll(...($templateType ?? [])));
        $builder->body($templates);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        $templateType = $request->getQuery("type");
        if ($templateType !== null && !TemplateType::get($templateType)) {
            $response->body(["message" => "The specified template type does not exist."]);
            return true;
        }

        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
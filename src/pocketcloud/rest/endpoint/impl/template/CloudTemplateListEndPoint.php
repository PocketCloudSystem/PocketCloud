<?php

namespace pocketcloud\rest\endpoint\impl\template;

use pocketcloud\lib\express\io\Request;
use pocketcloud\lib\express\io\Response;
use pocketcloud\lib\express\route\Router;
use pocketcloud\rest\endpoint\EndPoint;
use pocketcloud\template\Template;
use pocketcloud\template\TemplateManager;

class CloudTemplateListEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/template/list/");
    }

    public function handleRequest(Request $request, Response $response): array {
        return array_values(array_map(fn(Template $template) => $template->getName(), TemplateManager::getInstance()->getTemplates()));
    }

    public function isBadRequest(Request $request): bool {
        return false;
    }
}
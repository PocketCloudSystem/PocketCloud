<?php

namespace pocketcloud\http\endpoint\impl\template;

use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\util\Router;
use pocketcloud\http\endpoint\EndPoint;
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
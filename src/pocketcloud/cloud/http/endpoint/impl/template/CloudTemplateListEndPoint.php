<?php

namespace pocketcloud\cloud\http\endpoint\impl\template;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;
use pocketcloud\cloud\http\endpoint\EndPoint;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateManager;

class CloudTemplateListEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/template/list/");
    }

    public function handleRequest(Request $request, Response $response): array {
        return array_values(array_map(fn(Template $template) => $template->getName(), TemplateManager::getInstance()->getAll()));
    }

    public function isBadRequest(Request $request): bool {
        return false;
    }
}
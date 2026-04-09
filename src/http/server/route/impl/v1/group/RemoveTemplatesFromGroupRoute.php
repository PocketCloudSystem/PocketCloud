<?php

namespace pocketcloud\cloud\http\server\route\impl\v1\group;

use pocketcloud\cloud\group\ServerGroupManager;
use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\ResponseBuilder;
use pocketcloud\cloud\http\server\route\impl\v1\ApiV1JsonPath;
use pocketcloud\cloud\http\util\RequestMethod;
use pocketcloud\cloud\provider\CloudProvider;
use pocketcloud\cloud\template\TemplateManager;

final class RemoveTemplatesFromGroupRoute extends ApiV1JsonPath {

    public const array EXAMPLE_PAYLOAD = [
        "templates" => ["BW-2x1", "BW-2x4"]
    ];

    public function __construct() {
        parent::__construct(
            "/groups/{name}/templates",
            RequestMethod::DELETE,
            2 ** 9,
            ["templates" => "array"]
        );
    }

    public function onHandle(Request $request, ResponseBuilder $builder, array $requestBody): void {
        $group = ServerGroupManager::getInstance()->get($request->getParameter("name"));
        $i = 0;
        foreach ($requestBody["templates"] as $template) {
            if (is_string($template)) {
                if (($template = TemplateManager::getInstance()->get($template)) !== null) {
                    $i++;
                    ServerGroupManager::getInstance()->removeTemplate($group, $template, false);
                }
            }
        }

        if ($i > 0) CloudProvider::current()->editServerGroup($group, $group->write());

        $builder->body(["message" => "Removed the templates from the group."]);
    }

    public function checkForBadRequest(Request $request, ResponseBuilder $response, array $body): bool {
        $group = $request->getParameter("name");
        if ($group === null) {
            $response->body(["message" => "Please specify a group name."]);
            return true;
        }

        if (ServerGroupManager::getInstance()->get($group) === null) {
            $response->body(["message" => "Group not found."]);
            return true;
        }

        return false;
    }

    public function willCauseError(Request $request, ResponseBuilder $response): bool {
        return false;
    }
}
<?php

namespace pocketcloud\rest;

use pocketcloud\config\CloudConfig;
use pocketcloud\event\impl\rest\RestAPIInitializeEvent;
use pocketcloud\lib\express\App;
use pocketcloud\lib\express\io\Request;
use pocketcloud\lib\express\io\Response;
use pocketcloud\rest\endpoint\EndPoint;
use pocketcloud\rest\endpoint\impl\cloud\CloudInfoEndPoint;
use pocketcloud\rest\endpoint\impl\player\CloudPlayerGetEndPoint;
use pocketcloud\rest\endpoint\impl\player\CloudPlayerKickEndPoint;
use pocketcloud\rest\endpoint\impl\player\CloudPlayerListEndPoint;
use pocketcloud\rest\endpoint\impl\player\CloudPlayerTextEndPoint;
use pocketcloud\rest\endpoint\impl\plugin\CloudPluginDisableEndPoint;
use pocketcloud\rest\endpoint\impl\plugin\CloudPluginEnableEndPoint;
use pocketcloud\rest\endpoint\impl\plugin\CloudPluginGetEndPoint;
use pocketcloud\rest\endpoint\impl\plugin\CloudPluginListEndPoint;
use pocketcloud\rest\endpoint\impl\server\CloudServerExecuteEndPoint;
use pocketcloud\rest\endpoint\impl\server\CloudServerGetEndPoint;
use pocketcloud\rest\endpoint\impl\server\CloudServerListEndPoint;
use pocketcloud\rest\endpoint\impl\server\CloudServerSaveEndPoint;
use pocketcloud\rest\endpoint\impl\server\CloudServerStartEndPoint;
use pocketcloud\rest\endpoint\impl\server\CloudServerStopEndPoint;
use pocketcloud\rest\endpoint\impl\template\CloudTemplateCreateEndPoint;
use pocketcloud\rest\endpoint\impl\template\CloudTemplateEditEndPoint;
use pocketcloud\rest\endpoint\impl\template\CloudTemplateGetEndPoint;
use pocketcloud\rest\endpoint\impl\template\CloudTemplateListEndPoint;
use pocketcloud\rest\endpoint\impl\template\CloudTemplateRemoveEndPoint;
use pocketcloud\utils\CloudLogger;
use pocketcloud\utils\SingletonTrait;

class RestAPI {
    use SingletonTrait;

    private ?App $app = null;
    /** @var array<EndPoint> */
    private array $endpoints = [];

    public function __construct() {
        self::setInstance($this);
        if (CloudConfig::getInstance()->isRestAPIEnabled()) {
            $this->app = new App();
            $this->endpoints = [
                new CloudInfoEndPoint(),
                new CloudPlayerGetEndPoint(), new CloudPlayerTextEndPoint(), new CloudPlayerKickEndPoint(), new CloudPlayerListEndPoint(),
                new CloudPluginGetEndPoint(), new CloudPluginEnableEndPoint(), new CloudPluginDisableEndPoint(), new CloudPluginListEndPoint(),
                new CloudTemplateCreateEndPoint(), new CloudTemplateRemoveEndPoint(), new CloudTemplateGetEndPoint(), new CloudTemplateListEndPoint(), new CloudTemplateEditEndPoint(),
                new CloudServerStartEndPoint(), new CloudServerStopEndPoint(), new CloudServerSaveEndPoint(), new CloudServerExecuteEndPoint(), new CloudServerGetEndPoint(), new CloudServerListEndPoint()
            ];

            (new RestAPIInitializeEvent())->call();

            foreach ($this->endpoints as $endpoint) {
                $this->app->{strtolower($endpoint->getRequestMethod())}($endpoint->getPath(), function(Request $request, Response $response) use($endpoint): void {
                    $response->contentType("application/json");
                    if (!RestUtils::checkAuthorized($request)) {
                        $response->code(401);
                        return;
                    }

                    if ($endpoint->isBadRequest($request)) {
                        $response->code(400);
                        return;
                    }

                    $response->body($endpoint->handleRequest($request, $response));
                });
            }

            try {
                if ($this->app->listen(CloudConfig::getInstance()->getRestAPIPort())) {
                    CloudLogger::get()->info("§rRestAPI was §asuccessfully §rinitialized on port §e" . CloudConfig::getInstance()->getRestAPIPort() . "§r!");
                } else {
                    CloudLogger::get()->error("§cRestAPI can't be initialized on port §e" . CloudConfig::getInstance()->getRestAPIPort() . "§c!");
                }
            } catch (\Throwable $exception) {
                CloudLogger::get()->error("§cRestAPI can't be initialized on port §e" . CloudConfig::getInstance()->getRestAPIPort() . "§c: §e" . $exception->getMessage());
            }
        }
    }

    public function addEndPoint(EndPoint $endPoint) {
        if (in_array(strtoupper($endPoint->getRequestMethod()), Request::SUPPORTED_REQUEST_METHODS)) {
            $this->endpoints[$endPoint->getPath()] = $endPoint;
        } else CloudLogger::get()->error("§cCan't add the endpoint §e" . $endPoint->getPath() . " §cbecause the request method isn't supported! §8(§cSupported: §e" . implode("§8, §e", Request::SUPPORTED_REQUEST_METHODS) . "§r§8)");
    }

    public function removeEndPoint(EndPoint $endPoint) {
        if (isset($this->endpoints[$endPoint->getPath()])) unset($this->endpoints[$endPoint->getPath()]);
    }

    public function getEndPoint(string $path): ?EndPoint {
        return $this->endpoints[$path] ?? null;
    }

    public function getEndPoints(): array {
        return $this->endpoints;
    }

    public function getApp(): ?App {
        return $this->app;
    }
}
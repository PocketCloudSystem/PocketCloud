<?php

namespace pocketcloud\http\endpoint;

use pocketcloud\http\endpoint\impl\cloud\CloudInfoEndPoint;
use pocketcloud\http\endpoint\impl\maintenance\MaintenanceAddEndPoint;
use pocketcloud\http\endpoint\impl\maintenance\MaintenanceGetEndPoint;
use pocketcloud\http\endpoint\impl\maintenance\MaintenanceListEndPoint;
use pocketcloud\http\endpoint\impl\maintenance\MaintenanceRemoveEndPoint;
use pocketcloud\http\endpoint\impl\module\ModuleEditEndPoint;
use pocketcloud\http\endpoint\impl\module\ModuleGetEndPoint;
use pocketcloud\http\endpoint\impl\module\ModuleListEndPoint;
use pocketcloud\http\endpoint\impl\player\CloudPlayerGetEndPoint;
use pocketcloud\http\endpoint\impl\player\CloudPlayerKickEndPoint;
use pocketcloud\http\endpoint\impl\player\CloudPlayerListEndPoint;
use pocketcloud\http\endpoint\impl\player\CloudPlayerTextEndPoint;
use pocketcloud\http\endpoint\impl\plugin\CloudPluginDisableEndPoint;
use pocketcloud\http\endpoint\impl\plugin\CloudPluginEnableEndPoint;
use pocketcloud\http\endpoint\impl\plugin\CloudPluginGetEndPoint;
use pocketcloud\http\endpoint\impl\plugin\CloudPluginListEndPoint;
use pocketcloud\http\endpoint\impl\server\CloudServerExecuteEndPoint;
use pocketcloud\http\endpoint\impl\server\CloudServerGetEndPoint;
use pocketcloud\http\endpoint\impl\server\CloudServerListEndPoint;
use pocketcloud\http\endpoint\impl\server\CloudServerSaveEndPoint;
use pocketcloud\http\endpoint\impl\server\CloudServerStartEndPoint;
use pocketcloud\http\endpoint\impl\server\CloudServerStopEndPoint;
use pocketcloud\http\endpoint\impl\template\CloudTemplateCreateEndPoint;
use pocketcloud\http\endpoint\impl\template\CloudTemplateEditEndPoint;
use pocketcloud\http\endpoint\impl\template\CloudTemplateGetEndPoint;
use pocketcloud\http\endpoint\impl\template\CloudTemplateListEndPoint;
use pocketcloud\http\endpoint\impl\template\CloudTemplateRemoveEndPoint;
use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\util\Router;
use pocketcloud\language\Language;
use pocketcloud\util\CloudLogger;

final class EndpointRegistry {

    /** @var array<EndPoint> */
    private static array $endPoints = [];

    public static function registerDefaults(): void {
        $endPoints = [
            new CloudInfoEndPoint(),
            new CloudPlayerGetEndPoint(), new CloudPlayerTextEndPoint(), new CloudPlayerKickEndPoint(), new CloudPlayerListEndPoint(),
            new CloudPluginGetEndPoint(), new CloudPluginEnableEndPoint(), new CloudPluginDisableEndPoint(), new CloudPluginListEndPoint(),
            new CloudTemplateCreateEndPoint(), new CloudTemplateRemoveEndPoint(), new CloudTemplateGetEndPoint(), new CloudTemplateListEndPoint(), new CloudTemplateEditEndPoint(),
            new CloudServerStartEndPoint(), new CloudServerStopEndPoint(), new CloudServerSaveEndPoint(), new CloudServerExecuteEndPoint(), new CloudServerGetEndPoint(), new CloudServerListEndPoint(),
            new ModuleGetEndPoint(), new ModuleListEndPoint(), new ModuleEditEndPoint(),
            new MaintenanceAddEndPoint(), new MaintenanceRemoveEndPoint(), new MaintenanceGetEndPoint(), new MaintenanceListEndPoint()
        ];

        foreach ($endPoints as $endPoint) {
            self::addEndPoint($endPoint);
        }
    }

    public static function addEndPoint(EndPoint $endPoint): void {
        if (in_array(strtoupper($endPoint->getRequestMethod()), Request::SUPPORTED_REQUEST_METHODS)) {
            self::$endPoints[$endPoint->getPath()] = $endPoint;
            Router::getInstance()->{strtolower($endPoint->getRequestMethod())}($endPoint->getPath(), function(Request $request, Response $response) use ($endPoint): void {
                $response->contentType("application/json");
                if (!$request->authorized()) {
                    $response->code(401);
                    CloudLogger::get()->warn(Language::current()->translate("httpServer.request.unauthorized", $request->data()->address()->__toString()));
                    return;
                }

                if ($endPoint->isBadRequest($request)) {
                    $response->code(400);
                    return;
                }

                $response->body($endPoint->handleRequest($request, $response));
            });
        } else CloudLogger::get()->error(Language::current()->translate("httpServer.endPoint.add.failed", $endPoint->getPath(), implode("§8, §e", Request::SUPPORTED_REQUEST_METHODS)));
    }

    public static function removeEndPoint(EndPoint $endPoint): void {
        if (isset(self::$endPoints[$endPoint->getPath()])) unset(self::$endPoints[$endPoint->getPath()]);
    }

    public static function getEndPoint(string $path): ?EndPoint {
        return self::$endPoints[$path] ?? null;
    }
}
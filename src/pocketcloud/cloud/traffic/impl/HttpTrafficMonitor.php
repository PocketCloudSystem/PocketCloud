<?php

namespace pocketcloud\cloud\traffic\impl;

use Closure;
use pocketcloud\cloud\traffic\TrafficMonitor;
use pocketcloud\cloud\traffic\TrafficMonitorManager;

final class HttpTrafficMonitor extends TrafficMonitor {

    public const string HTTP_MODE_REQUEST_IN = "http_request_in";
    public const string HTTP_MODE_RESPONSE_OUT = "http_response_out";

    public function __construct() {
        parent::__construct(TrafficMonitorManager::TRAFFIC_HTTP);
    }

    /**
     * @param Closure $handler function (Request $request, Address $source)
     * @return self
     */
    public function monitorRequestIn(Closure $handler): self {
        $this->addHandler(self::HTTP_MODE_REQUEST_IN, $handler);
        return $this;
    }

    /**
     * @param Closure $handler function (Request $request, Response $response, Address $destination)
     * @return self
     */
    public function monitorResponseOut(Closure $handler): self {
        $this->addHandler(self::HTTP_MODE_RESPONSE_OUT, $handler);
        return $this;
    }
}
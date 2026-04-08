<?php

namespace pocketcloud\cloud\traffic\impl;

use Closure;
use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\io\Response;
use pocketcloud\cloud\traffic\TrafficMonitor;
use pocketcloud\cloud\traffic\TrafficMonitorManager;
use pocketcloud\cloud\util\net\Address;

final class HttpTrafficMonitor extends TrafficMonitor {

    public const string HTTP_MODE_REQUEST_IN = "http_request_in";
    public const string HTTP_MODE_RESPONSE_OUT = "http_response_out";

    public function __construct() {
        parent::__construct(TrafficMonitorManager::TRAFFIC_HTTP);
    }

    /**
     * @param Closure(Request $request, Address $source): void $handler function
     * @return self
     */
    public function monitorRequestIn(Closure $handler): self {
        $this->addHandler(self::HTTP_MODE_REQUEST_IN, $handler);
        return $this;
    }

    /**
     * @param Closure(Request $request, Response $response, Address $destination): void $handler
     * @return self
     */
    public function monitorResponseOut(Closure $handler): self {
        $this->addHandler(self::HTTP_MODE_RESPONSE_OUT, $handler);
        return $this;
    }
}
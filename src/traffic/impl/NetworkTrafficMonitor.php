<?php

namespace pocketcloud\cloud\traffic\impl;

use Closure;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\traffic\TrafficMonitor;
use pocketcloud\cloud\traffic\TrafficMonitorManager;
use pocketcloud\cloud\util\net\Address;
use pocketcloud\cloud\util\PathUtils;

final class NetworkTrafficMonitor extends TrafficMonitor {

    public const string NETWORK_MODE_PACKET_IN = "packet_in";
    public const string NETWORK_MODE_PACKET_OUT = "packet_out";

    public function __construct() {
        parent::__construct(TrafficMonitorManager::TRAFFIC_NETWORK);
    }

    /**
     * @param string $packetClass
     * @param Closure(CloudPacket $packet, Address $address): void $handler
     * @return self
     */
    public function monitorPacketIn(string $packetClass, Closure $handler): self {
        $this->addHandler(self::parsePacketMode(self::NETWORK_MODE_PACKET_IN, $packetClass), $handler);
        return $this;
    }

    /**
     * @param string $packetClass
     * @param Closure(CloudPacket $packet, Address $destination, bool $success): void $handler
     * @return self
     */
    public function monitorPacketOut(string $packetClass, Closure $handler): self {
        $this->addHandler(self::parsePacketMode(self::NETWORK_MODE_PACKET_OUT, $packetClass), $handler);
        return $this;
    }

    public static function parsePacketMode(string $normalMode, string $packetClass): string {
        return $normalMode . "-" . basename(PathUtils::normalize($packetClass));
    }
}
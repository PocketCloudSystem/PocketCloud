<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class KeepAlivePacket extends CloudPacket implements ClientboundPacket, CloudboundPacket {

    public function __construct(
        private float $tps = -1,
        private float $avgTps = -1,
        private float $memoryUsage = -1,
        private float $memoryPeak = -1,
        private float $memoryLimit = -1,
        private float $cpuUsage = -1
    ) {}

    public function handle(ServerClient $client): void {
        if (($server = $client->getServer()) !== null) {
            $server->setLastCheckTime(time());
            $server->getServerData()->setPerformanceStats($this->tps, $this->avgTps, $this->memoryUsage, $this->memoryPeak, $this->memoryLimit, $this->cpuUsage);
        }
    }

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeAll($this->tps, $this->avgTps, $this->memoryUsage, $this->memoryPeak, $this->memoryLimit, $this->cpuUsage);
    }

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAll($this->tps, $this->avgTps, $this->memoryUsage, $this->memoryPeak, $this->memoryLimit, $this->cpuUsage);
    }

    public static function create(float $tps, float $avgTps, float $memoryUsage, float $memoryPeak, float $memoryLimit, float $cpuUsage): self {
        return new self($tps, $avgTps, $memoryUsage, $memoryPeak, $memoryLimit, $cpuUsage);
    }
}
<?php

namespace pocketcloud\cloud\traffic;

use LogicException;
use pocketcloud\cloud\traffic\impl\HttpTrafficMonitor;
use pocketcloud\cloud\traffic\impl\NetworkTrafficMonitor;
use pocketcloud\cloud\util\SingletonTrait;
use pocketcloud\cloud\util\tick\Tickable;

final class TrafficMonitorManager implements Tickable {
    use SingletonTrait;

    public const string TRAFFIC_NETWORK = "network";
    public const string TRAFFIC_HTTP = "http";

    /** @var array<string> */
    private array $trafficMonitorTypes = [];
    /** @var array<array<TrafficMonitor>> */
    private array $trafficMonitors = [];
    private array $allTimeTraffic = [];
    private array $byteHistory = [];

    public function __construct() {
        self::setInstance($this);

        $this->registerTrafficMonitor(self::TRAFFIC_NETWORK, NetworkTrafficMonitor::class);
        $this->registerTrafficMonitor(self::TRAFFIC_HTTP, HttpTrafficMonitor::class);
    }

    public function tick(int $currentTick): void {
        if ($currentTick % 20 === 0) {
            $this->cleanupHistory();
            foreach ($this->trafficMonitors as $trafficMonitors) {
                foreach ($trafficMonitors as $trafficMonitor) $trafficMonitor->cleanupHistory();
            }
        }
    }

    protected function cleanupHistory(): void {
        $now = microtime(true);
        $threshold = $now - 1;
        foreach ($this->byteHistory as $typeModeIdentifier => $history) {
            // type_mode
            $type = implode("_", array_slice($parts = explode("_", $typeModeIdentifier), 0, count($parts) - 1));
            $mode = substr($typeModeIdentifier, strlen($type) + 1) . TrafficMonitor::SUFFIX_AVG;
            foreach ($history as $i => $data) {
                if ($data[0] < $threshold) unset($this->byteHistory[$typeModeIdentifier][$i]);
            }

            $this->allTimeTraffic[$type][$mode] = array_sum(array_map(fn(array $data) => $data[1], $this->byteHistory[$typeModeIdentifier]));
        }
    }

    public function registerTrafficMonitor(string $type, string $monitorClass, bool $override = false): void {
        if (isset($this->trafficMonitorTypes[$type]) && !$override) return;
        $this->trafficMonitorTypes[$type] = $monitorClass;
        $this->allTimeTraffic[$type] = [
            TrafficMonitor::REGULAR_MODE_IN => 0,
            TrafficMonitor::REGULAR_MODE_OUT => 0,
            TrafficMonitor::REGULAR_MODE_IN . TrafficMonitor::SUFFIX_AVG => 0,
            TrafficMonitor::REGULAR_MODE_OUT . TrafficMonitor::SUFFIX_AVG => 0
        ];

        $this->byteHistory[$type . "_" . TrafficMonitor::REGULAR_MODE_IN] = [];
        $this->byteHistory[$type . "_" . TrafficMonitor::REGULAR_MODE_OUT] = [];
    }

    public function createNetworkMonitor(): NetworkTrafficMonitor {
        $monitor = $this->createTrafficMonitor(self::TRAFFIC_NETWORK);
        if (!$monitor instanceof NetworkTrafficMonitor) throw new LogicException("Registered monitor class for traffic type " . self::TRAFFIC_NETWORK . " is not a 'NetworkTrafficMonitor'");
        return $monitor;
    }

    public function createHttpMonitor(): HttpTrafficMonitor {
        $monitor = $this->createTrafficMonitor(self::TRAFFIC_HTTP);
        if (!$monitor instanceof HttpTrafficMonitor) throw new LogicException("Registered monitor class for traffic type " . self::TRAFFIC_HTTP . " is not a 'HttpTrafficMonitor'");
        return $monitor;
    }

    public function createTrafficMonitor(string $customType): ?TrafficMonitor {
        if (!isset($this->trafficMonitorTypes[$customType])) return null;
        if (!isset($this->trafficMonitors[$customType])) $this->trafficMonitors[$customType] = [];
        $monitor = new ($this->trafficMonitorTypes[$customType])($customType);
        $this->trafficMonitors[$customType][spl_object_id($monitor)] = $monitor;
        return $monitor;
    }

    public function removeTrafficMonitor(TrafficMonitor $monitor): void {
        if (!isset($this->trafficMonitors[$monitor->getMonitorType()]) || !isset($this->trafficMonitors[$monitor->getMonitorType()][spl_object_id($monitor)])) return;
        unset($this->trafficMonitors[$monitor->getMonitorType()][spl_object_id($monitor)]);
    }

    public function pushBytes(string $type, int $bytes, string $mode): void {
        if (!isset($this->allTimeTraffic[$type]) || !isset($this->allTimeTraffic[$type][$mode])) return;
        $this->allTimeTraffic[$type][$mode] += $bytes;
        $this->byteHistory[$type . "_" . $mode][] = [microtime(true), $bytes];
        foreach (($this->trafficMonitors[$type] ?? []) as $monitor) {
            $monitor->pushBytes($mode, $bytes);
        }
    }

    public function callHandlers(string $type, string $mode, mixed ...$args): void {
        foreach (($this->trafficMonitors[$type] ?? []) as $monitor) {
            $monitor->callHandlers($mode, ...$args);
        }
    }

    public function getTrafficMonitorTypes(): array {
        return $this->trafficMonitorTypes;
    }

    public function getTrafficMonitors(?string $type = null): ?array {
        if ($type !== null) return $this->trafficMonitors[$type] ?? null;
        return $this->trafficMonitors;
    }

    public function getAllTimeTraffic(?string $type = null): ?array {
        if ($type !== null) return $this->allTimeTraffic[$type] ?? null;
        return $this->allTimeTraffic;
    }
}
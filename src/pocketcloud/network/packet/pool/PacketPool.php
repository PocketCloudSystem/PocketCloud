<?php

namespace pocketcloud\network\packet\pool;

use pocketcloud\network\packet\impl\normal\CloudNotifyPacket;
use pocketcloud\network\packet\impl\normal\CommandSendAnswerPacket;
use pocketcloud\network\packet\impl\normal\CommandSendPacket;
use pocketcloud\network\packet\impl\normal\ConsoleTextPacket;
use pocketcloud\network\packet\impl\normal\LibrarySyncPacket;
use pocketcloud\network\packet\impl\normal\ModuleSyncPacket;
use pocketcloud\util\Utils;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\impl\normal\CloudServerSavePacket;
use pocketcloud\network\packet\impl\normal\CloudServerStatusChangePacket;
use pocketcloud\network\packet\impl\normal\DisconnectPacket;
use pocketcloud\network\packet\impl\normal\KeepAlivePacket;
use pocketcloud\network\packet\impl\normal\PlayerConnectPacket;
use pocketcloud\network\packet\impl\normal\PlayerDisconnectPacket;
use pocketcloud\network\packet\impl\normal\PlayerKickPacket;
use pocketcloud\network\packet\impl\normal\PlayerNotifyUpdatePacket;
use pocketcloud\network\packet\impl\normal\PlayerSwitchServerPacket;
use pocketcloud\network\packet\impl\normal\PlayerSyncPacket;
use pocketcloud\network\packet\impl\normal\PlayerTextPacket;
use pocketcloud\network\packet\impl\normal\ProxyRegisterServerPacket;
use pocketcloud\network\packet\impl\normal\ProxyUnregisterServerPacket;
use pocketcloud\network\packet\impl\normal\ServerSyncPacket;
use pocketcloud\network\packet\impl\normal\TemplateSyncPacket;
use pocketcloud\network\packet\impl\request\CheckPlayerMaintenanceRequestPacket;
use pocketcloud\network\packet\impl\request\CheckPlayerNotifyRequestPacket;
use pocketcloud\network\packet\impl\request\CloudServerStartRequestPacket;
use pocketcloud\network\packet\impl\request\CloudServerStopRequestPacket;
use pocketcloud\network\packet\impl\request\LoginRequestPacket;
use pocketcloud\network\packet\impl\response\CheckPlayerMaintenanceResponsePacket;
use pocketcloud\network\packet\impl\response\CheckPlayerNotifyResponsePacket;
use pocketcloud\network\packet\impl\response\CloudServerStartResponsePacket;
use pocketcloud\network\packet\impl\response\CloudServerStopResponsePacket;
use pocketcloud\network\packet\impl\response\LoginResponsePacket;
use pocketcloud\util\SingletonTrait;

class PacketPool {
    use SingletonTrait;

    /** @var array<CloudPacket> */
    private array $packets = [];

    public function __construct() {
        self::setInstance($this);
        $this->registerPacket(LoginRequestPacket::class);
        $this->registerPacket(LoginResponsePacket::class);
        $this->registerPacket(DisconnectPacket::class);
        $this->registerPacket(KeepAlivePacket::class);
        $this->registerPacket(CommandSendPacket::class);
        $this->registerPacket(CommandSendAnswerPacket::class);
        $this->registerPacket(ConsoleTextPacket::class);
        $this->registerPacket(PlayerConnectPacket::class);
        $this->registerPacket(PlayerDisconnectPacket::class);
        $this->registerPacket(PlayerTextPacket::class);
        $this->registerPacket(PlayerKickPacket::class);
        $this->registerPacket(PlayerNotifyUpdatePacket::class);
        $this->registerPacket(ProxyRegisterServerPacket::class);
        $this->registerPacket(ProxyUnregisterServerPacket::class);
        $this->registerPacket(CloudServerSavePacket::class);
        $this->registerPacket(CloudServerStatusChangePacket::class);
        $this->registerPacket(PlayerSwitchServerPacket::class);
        $this->registerPacket(TemplateSyncPacket::class);
        $this->registerPacket(ServerSyncPacket::class);
        $this->registerPacket(PlayerSyncPacket::class);
        $this->registerPacket(CloudServerStartRequestPacket::class);
        $this->registerPacket(CloudServerStartResponsePacket::class);
        $this->registerPacket(CloudServerStopRequestPacket::class);
        $this->registerPacket(CloudServerStopResponsePacket::class);
        $this->registerPacket(CheckPlayerMaintenanceRequestPacket::class);
        $this->registerPacket(CheckPlayerMaintenanceResponsePacket::class);
        $this->registerPacket(CheckPlayerNotifyRequestPacket::class);
        $this->registerPacket(CheckPlayerNotifyResponsePacket::class);
        $this->registerPacket(CloudNotifyPacket::class);
        $this->registerPacket(ModuleSyncPacket::class);
        $this->registerPacket(LibrarySyncPacket::class);
    }

    public function registerPacket(string $packetClass): void {
        if (!is_subclass_of($packetClass, CloudPacket::class)) return;
        $this->packets[Utils::cleanPath($packetClass, true)] = $packetClass;
    }

    public function getPacketById(string $pid): ?CloudPacket {
        $get = $this->packets[$pid] ?? null;
        return ($get == null ? null : new $get());
    }

    public function getPackets(): array {
        return $this->packets;
    }

    public static function getInstance(): ?self {
        return self::$instance;
    }
}
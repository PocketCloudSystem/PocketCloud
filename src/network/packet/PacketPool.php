<?php

namespace pocketcloud\cloud\network\packet;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\network\packet\impl\BulkSyncPacket;
use pocketcloud\cloud\network\packet\impl\CloudNotificationPacket;
use pocketcloud\cloud\network\packet\impl\CloudSyncServerStoragePacket;
use pocketcloud\cloud\network\packet\impl\ConsoleLogPacket;
use pocketcloud\cloud\network\packet\impl\DisconnectPacket;
use pocketcloud\cloud\network\packet\impl\KeepAlivePacket;
use pocketcloud\cloud\network\packet\impl\LanguageSyncPacket;
use pocketcloud\cloud\network\packet\impl\LibrarySyncPacket;
use pocketcloud\cloud\network\packet\impl\MaintenanceListSyncPacket;
use pocketcloud\cloud\network\packet\impl\ModuleSyncPacket;
use pocketcloud\cloud\network\packet\impl\NotificationListSyncPacket;
use pocketcloud\cloud\network\packet\impl\PlayerConnectPacket;
use pocketcloud\cloud\network\packet\impl\PlayerDisconnectPacket;
use pocketcloud\cloud\network\packet\impl\PlayerKickPacket;
use pocketcloud\cloud\network\packet\impl\PlayerSwitchServerPacket;
use pocketcloud\cloud\network\packet\impl\PlayerSyncPacket;
use pocketcloud\cloud\network\packet\impl\PlayerTextPacket;
use pocketcloud\cloud\network\packet\impl\PlayerUpdateNotificationStatePacket;
use pocketcloud\cloud\network\packet\impl\PlayerTransferPacket;
use pocketcloud\cloud\network\packet\impl\ProxyRegisterServerPacket;
use pocketcloud\cloud\network\packet\impl\ProxyUnregisterServerPacket;
use pocketcloud\cloud\network\packet\impl\request\client\CommandExecuteRequestPacket;
use pocketcloud\cloud\network\packet\impl\request\PlayerNotificationCheckRequestPacket;
use pocketcloud\cloud\network\packet\impl\request\PlayerWhitelistCheckRequestPacket;
use pocketcloud\cloud\network\packet\impl\request\ServerHandshakeRequestPacket;
use pocketcloud\cloud\network\packet\impl\request\ServerSaveRequestPacket;
use pocketcloud\cloud\network\packet\impl\request\ServerStartRequestPacket;
use pocketcloud\cloud\network\packet\impl\request\ServerStopRequestPacket;
use pocketcloud\cloud\network\packet\impl\response\client\CommandExecuteResponsePacket;
use pocketcloud\cloud\network\packet\impl\response\PlayerNotificationCheckResponsePacket;
use pocketcloud\cloud\network\packet\impl\response\PlayerWhitelistCheckResponsePacket;
use pocketcloud\cloud\network\packet\impl\response\ServerHandshakeResponsePacket;
use pocketcloud\cloud\network\packet\impl\response\ServerSaveResponsePacket;
use pocketcloud\cloud\network\packet\impl\response\ServerStartResponsePacket;
use pocketcloud\cloud\network\packet\impl\response\ServerStopResponsePacket;
use pocketcloud\cloud\network\packet\impl\ServerChangeStatusPacket;
use pocketcloud\cloud\network\packet\impl\ServerGroupSyncPacket;
use pocketcloud\cloud\network\packet\impl\ServerSyncPacket;
use pocketcloud\cloud\network\packet\impl\TemplateSyncPacket;
use pocketcloud\cloud\util\trait\SingletonTrait;
use ReflectionClass;
use ReflectionException;

final class PacketPool {
    use SingletonTrait;

    /** @var array<CloudPacket> */
    private array $packets = [];

    public static function init(): void {
        self::setInstance(new self());
    }

    public function __construct() {
        self::setInstance($this);
        $this->register(ServerHandshakeRequestPacket::class);
        $this->register(ServerHandshakeResponsePacket::class);
        $this->register(DisconnectPacket::class);
        $this->register(KeepAlivePacket::class);
        $this->register(CloudNotificationPacket::class);
        $this->register(CommandExecuteRequestPacket::class);
        $this->register(CommandExecuteResponsePacket::class);
        $this->register(LanguageSyncPacket::class);
        $this->register(LibrarySyncPacket::class);
        $this->register(ModuleSyncPacket::class);
        $this->register(TemplateSyncPacket::class);
        $this->register(ServerSyncPacket::class);
        $this->register(ServerGroupSyncPacket::class);
        $this->register(PlayerSyncPacket::class);
        $this->register(PlayerConnectPacket::class);
        $this->register(PlayerDisconnectPacket::class);
        $this->register(ProxyRegisterServerPacket::class);
        $this->register(ProxyUnregisterServerPacket::class);
        $this->register(PlayerKickPacket::class);
        $this->register(PlayerSwitchServerPacket::class);
        $this->register(PlayerWhitelistCheckRequestPacket::class);
        $this->register(PlayerWhitelistCheckResponsePacket::class);
        $this->register(PlayerNotificationCheckRequestPacket::class);
        $this->register(PlayerNotificationCheckResponsePacket::class);
        $this->register(PlayerUpdateNotificationStatePacket::class);
        $this->register(MaintenanceListSyncPacket::class);
        $this->register(NotificationListSyncPacket::class);
        $this->register(ServerChangeStatusPacket::class);
        $this->register(CloudSyncServerStoragePacket::class);
        $this->register(PlayerTransferPacket::class);
        $this->register(PlayerTextPacket::class);
        $this->register(ServerStartRequestPacket::class);
        $this->register(ServerStartResponsePacket::class);
        $this->register(ServerStopRequestPacket::class);
        $this->register(ServerStopResponsePacket::class);
        $this->register(ServerSaveRequestPacket::class);
        $this->register(ServerSaveResponsePacket::class);
        $this->register(ConsoleLogPacket::class);
        $this->register(BulkSyncPacket::class);
    }

    public function register(string $packetClass): void {
        if (!is_subclass_of($packetClass, CloudPacket::class)) return;
        try {
            CloudLogger::get()->debug("Registering packet " . ($packetName = new ReflectionClass($packetClass)->getShortName()) . " (" . $packetClass . ")");
            $this->packets[$packetName] = $packetClass;
        } catch (ReflectionException $e) {
            CloudLogger::get()->error("§cFailed to register packet §e{}§c.", $packetClass);
            CloudLogger::get()->exception($e);
        }
    }

    public function get(string $pid): ?CloudPacket {
        $get = $this->packets[$pid] ?? null;
        return ($get == null ? null : new $get());
    }

    public function getAll(): array {
        return $this->packets;
    }
}
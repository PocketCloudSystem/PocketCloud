<?php

namespace pocketcloud\cloud\network\packet;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\network\packet\impl\CloudNotificationPacket;
use pocketcloud\cloud\network\packet\impl\CommandAnswerPacket;
use pocketcloud\cloud\network\packet\impl\CommandExecutePacket;
use pocketcloud\cloud\network\packet\impl\DisconnectPacket;
use pocketcloud\cloud\network\packet\impl\KeepAlivePacket;
use pocketcloud\cloud\network\packet\impl\LanguageSyncPacket;
use pocketcloud\cloud\network\packet\impl\LibrarySyncPacket;
use pocketcloud\cloud\network\packet\impl\ModuleSyncPacket;
use pocketcloud\cloud\network\packet\impl\PlayerSyncPacket;
use pocketcloud\cloud\network\packet\impl\request\ServerHandshakeRequestPacket;
use pocketcloud\cloud\network\packet\impl\response\ServerHandshakeResponsePacket;
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
        $this->register(CommandExecutePacket::class);
        $this->register(CommandAnswerPacket::class);
        $this->register(LanguageSyncPacket::class);
        $this->register(LibrarySyncPacket::class);
        $this->register(ModuleSyncPacket::class);
        $this->register(TemplateSyncPacket::class);
        $this->register(ServerSyncPacket::class);
        $this->register(ServerGroupSyncPacket::class);
        $this->register(PlayerSyncPacket::class);
    }

    public function register(string $packetClass): void {
        if (!is_subclass_of($packetClass, CloudPacket::class)) return;
        try {
            CloudLogger::get()->debug("Registering packet " . ($packetName = new ReflectionClass($packetClass)->getShortName()) . " (" . $packetClass . ")");
            $this->packets[$packetName] = $packetClass;
        } catch (ReflectionException $e) {
            CloudLogger::get()->error("§cFailed to register packet §e{}§c." . $packetClass);
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
<?php

namespace pocketcloud\network\packet\handler;

use pocketcloud\config\DefaultConfig;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\pool\PacketPool;
use pocketcloud\network\packet\utils\PacketData;
use pocketcloud\util\CloudLogger;

class PacketSerializer {

    public static function encode(CloudPacket $packet): string {
        $packet->encode($buffer = new PacketData());
        try {
            return DefaultConfig::getInstance()->isNetworkEncryptionEnabled() ? base64_encode(json_encode($buffer, JSON_THROW_ON_ERROR)) : json_encode($buffer, JSON_THROW_ON_ERROR);
        } catch (\Exception $exception) {
            CloudLogger::get()->error("§cFailed to encode packet: §e" . (new \ReflectionClass($packet))->getShortName());
            CloudLogger::get()->exception($exception);
        }
        return "";
    }

    public static function decode(string $buffer): ?CloudPacket {
        if (trim($buffer) == "") return null;
        $data = json_decode((DefaultConfig::getInstance()->isNetworkEncryptionEnabled() ? base64_decode($buffer) : $buffer),  true, flags: JSON_THROW_ON_ERROR);
        if (is_array($data)) {
            if (isset($data[0])) {
                if (($packet = PacketPool::getInstance()->getPacketById($data[0])) !== null) {
                    $packet->decode(new PacketData($data));
                    return $packet;
                }
            }
        }
        return null;
    }
}
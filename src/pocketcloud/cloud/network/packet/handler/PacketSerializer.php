<?php

namespace pocketcloud\cloud\network\packet\handler;

use Exception;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\exception\ExceptionHandler;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\pool\PacketPool;
use pocketcloud\cloud\network\packet\data\PacketData;
use pocketcloud\cloud\terminal\log\CloudLogger;
use ReflectionClass;

final class PacketSerializer {

    public static function encode(CloudPacket $packet): string {
        try {
            $packet->encode($buffer = new PacketData());
            return MainConfig::getInstance()->isNetworkEncryptionEnabled() ? base64_encode(json_encode($buffer, JSON_THROW_ON_ERROR)) : json_encode($buffer, JSON_THROW_ON_ERROR);
        } catch (Exception $exception) {
            CloudLogger::get()->error("§cFailed to encode packet: §e" . new ReflectionClass($packet)->getShortName());
            CloudLogger::get()->exception($exception);
        }
        return "";
    }

    public static function decode(string $buffer): ?CloudPacket {
        if (trim($buffer) == "") return null;
        $data = ExceptionHandler::tryCatch(fn() => json_decode((MainConfig::getInstance()->isNetworkEncryptionEnabled() ? base64_decode($buffer) : $buffer),  true, flags: JSON_THROW_ON_ERROR), "Failed to decode packet", onExceptionClosure: fn() => CloudLogger::get()->debug("Buffer: " . $buffer));
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
<?php

namespace pocketcloud\cloud\network\packet\util;

use ErrorException;
use JsonException;
use pocketcloud\cloud\console\handler\ExceptionHandler;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\exception\PacketException;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\PacketPool;

final class PacketSerializer {

    /**
     * @throws ErrorException
     */
    public static function encode(ClientboundPacket $packet, bool $encryptionEnabled, string $authenticationKey): ?string {
        return ExceptionHandler::tryCatch(function (ClientboundPacket $packet, bool $encryptionEnabled, string $authenticationKey): string {
            $packet->encode($buffer = new PacketData());
            $buffer->write($authenticationKey);
            $stringBuffer = json_encode($buffer, JSON_THROW_ON_ERROR);
            if ($encryptionEnabled) $stringBuffer = zlib_encode($stringBuffer, ZLIB_ENCODING_DEFLATE, 3);
            return $stringBuffer;
        }, "Failed to encode packet: " . $packet->getName(), null, $packet, $encryptionEnabled, $authenticationKey);
    }

    /**
     * @throws JsonException|PacketException
     */
    public static function decode(string $buffer, bool $encryptionEnabled, string $authenticationKey): ?CloudboundPacket {
        if ($buffer == "") return null;
        $data = json_decode($encryptionEnabled ? zlib_decode($buffer) : $buffer, true, flags: JSON_THROW_ON_ERROR);
        if (!is_array($data)) throw new PacketException("Received buffer is not an array");
        $packetName = $data[0] ?? null;
        if ($packetName === null) throw new PacketException("Received buffer does not contain a valid packet name");
        if (($packet = PacketPool::getInstance()->get($packetName)) !== null) {
            if (!$packet instanceof CloudboundPacket) throw new PacketException("Received packet is not a CloudboundPacket");
            $packet->decode($buffer = new PacketData($data));
            if ($buffer->isEmpty()) throw new PacketException("Received packet does not contain an authentication key");
            if (($givenKey = $buffer->readString()) === null) throw new PacketException("Received packet does not contain an authentication key");
            if ($givenKey !== $authenticationKey) throw new PacketException("Received packet does not contain a valid authentication key");
            return $packet;
        }

        return null;
    }
}
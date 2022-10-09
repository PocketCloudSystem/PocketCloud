<?php

namespace pocketcloud\network\packet\handler\decoder;

use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\content\PacketContent;
use pocketcloud\network\packet\pool\PacketPool;

class PacketDecoder {

    public static function decode(string $buffer): ?CloudPacket {
        $contents = json_decode($buffer, true);
        if (is_array($contents)) {
            if (isset($contents[0])) {
                $packet = PacketPool::getInstance()->getPacketById($contents[0]);
                if ($packet !== null) {
                    $packet->decode(new PacketContent($contents));
                    return $packet;
                }
            }
        }
        return null;
    }
}
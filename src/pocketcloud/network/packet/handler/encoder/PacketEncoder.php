<?php

namespace pocketcloud\network\packet\handler\encoder;

use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\content\PacketContent;

class PacketEncoder {

    public static function encode(CloudPacket $packet): false|string {
        $content = new PacketContent([]);
        $packet->encode($content);
        return json_encode($content->getContent());
    }
}
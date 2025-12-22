<?php

namespace pocketcloud\cloud\network\packet\util;

use JsonSerializable;

final class PacketData implements JsonSerializable {

    public function __construct(private array $data = []) {}

    public function write(mixed $v): self {
        $this->data[] = $v;
        return $this;
    }

    public function read(): mixed {
        return array_shift($this->data);
    }

    public function readString(): ?string {
        $read = $this->read();
        if ($read === null) return null;
        return (string) $read;
    }

    public function readInt(): ?int {
        $read = $this->read();
        if ($read === null) return null;
        return intval($read);
    }

    public function readFloat(): ?float {
        $read = $this->read();
        if ($read === null) return null;
        return floatval($read);
    }

    public function readBool(): ?bool {
        $read = $this->read();
        if ($read === null) return null;
        return boolval($read);
    }

    public function readArray(): ?array {
        $read = $this->read();
        if (is_array($read)) return $read;
        return null;
    }

    public function jsonSerialize(): array {
        return $this->data;
    }
}
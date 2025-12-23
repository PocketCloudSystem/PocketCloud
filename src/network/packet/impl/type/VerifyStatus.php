<?php

namespace pocketcloud\cloud\network\packet\impl\type;

enum VerifyStatus: string {

    case DENIED = "DENIED";
    case VERIFIED = "VERIFIED";
    case NOT_APPLIED = "NOT_APPLIED";

    public function getName(): string {
        return $this->value;
    }
}
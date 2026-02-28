<?php

namespace pocketcloud\cloud\network\packet;

enum RequestPacketFailureReason {

    case THEN_CRASHED;
    case REQUEST_TIMEOUT;
}
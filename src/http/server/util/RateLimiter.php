<?php

namespace pocketcloud\cloud\http\server\util;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\util\net\Address;

final class RateLimiter {

    public const int DEFAULT_TIMEOUT = 120;
    public const int DEFAULT_MAX_REQUESTS = 10;
    public const int DEFAULT_TIME_FRAME = 10;


    public static function configure(bool $enabled, int $timeout, int $maxRequests, int $timeFrame): self {
        return new self($enabled, $timeout, $maxRequests, $timeFrame);
    }

    private array $requests = [];
    private array $rateLimits = [];

    public function __construct(
        private readonly bool $enabled,
        private int $timeout,
        private int $maxRequests,
        private int $timeFrame
    ) {
        if ($this->maxRequests < 0) $this->maxRequests = self::DEFAULT_MAX_REQUESTS;
        if ($this->timeout < 0) $this->timeout = self::DEFAULT_TIMEOUT;
        if ($this->timeFrame < 0) $this->timeFrame = self::DEFAULT_TIME_FRAME;
    }

    public function rateLimit(Address $address, ?int $timeout = null): int {
        if (!$this->enabled) return -1;
        $timeout = $timeout ?? $this->timeout;
        if ($timeout < 0) $timeout = self::DEFAULT_TIMEOUT;
        $this->rateLimits[$address->getAddress()] = time() + $timeout;
        CloudLogger::get()->info("Rate limited §b{} §rfor §b{}§rs", $address, $timeout);
        return $this->rateLimits[$address->getAddress()];
    }

    /**
     * @param Address $address
     * @param int|null $endTimestamp
     * @return bool return true means everything is ok and false means address is rate limited
     */
    public function checkRequest(Address $address, ?int &$endTimestamp = null): bool {
        if (!$this->enabled) return true;
        if ($this->checkRateLimit($address)) {
            $endTimestamp = $this->rateLimits[$address->getAddress()];
            return false;
        }

        if (!isset($this->requests[$address->getAddress()]) || $this->requests[$address->getAddress()]["timestamp"] <= time())
            $this->requests[$address->getAddress()] = [
                "count" => 0,
                "timestamp" => time() + $this->timeFrame
            ];

        $this->requests[$address->getAddress()]["count"] = $this->requests[$address->getAddress()]["count"] + 1;
        if ($this->requests[$address->getAddress()]["count"] > $this->maxRequests) {
            $endTimestamp = $this->rateLimit($address);
            return false;
        }

        return true;
    }

    public function checkRateLimit(Address $address): bool {
        if (isset($this->rateLimits[$address->getAddress()])) {
            if (time() < $this->rateLimits[$address->getAddress()]) return true;
            unset($this->rateLimits[$address->getAddress()]);
        }

        return false;
    }

    public function isEnabled(): bool {
        return $this->enabled;
    }

    public function getTimeout(): int {
        return $this->timeout;
    }

    public function getMaxRequests(): int {
        return $this->maxRequests;
    }

    public function getTimeFrame(): int {
        return $this->timeFrame;
    }
}
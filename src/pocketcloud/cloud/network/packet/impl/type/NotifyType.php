<?php

namespace pocketcloud\cloud\network\packet\impl\type;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\util\enum\EnumTrait;

/**
 * @method static NotifyType STARTING()
 * @method static NotifyType STOPPING()
 * @method static NotifyType TIMED()
 * @method static NotifyType CRASHED()
 * @method static NotifyType START_FAILED()
 */
final class NotifyType {
    use EnumTrait;

    protected static function init(): void {
        self::register("starting", new NotifyType("STARTING", "§aStarting §fthe server §b%server%§f..."));
        self::register("stopping", new NotifyType("STOPPING", "§cStopping §fthe server §b%server%§f..."));
        self::register("timed", new NotifyType("TIMED", "§fThe server §b%server% §fhas §ctimed out§f."));
        self::register("crashed", new NotifyType("CRASHED", "§fThe server §b%server% §ccrashed§f."));
        self::register("start_failed", new NotifyType("START_FAILED", "§fFailed to start the server §b%server%§f..."));
    }

    public function __construct(
        private readonly string $name,
        private readonly string $message
    ) {}

    public function send(array $params): void {
        $message = MainConfig::getInstance()->getInGamePrefix() . str_replace(array_keys($params), array_values($params), $this->message);
        //todo: send notify
    }

    public function getName(): string {
        return $this->name;
    }

    public function getMessage(): string {
        return $this->message;
    }
}
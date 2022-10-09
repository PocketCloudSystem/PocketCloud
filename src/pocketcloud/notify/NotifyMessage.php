<?php

namespace pocketcloud\notify;

use pocketcloud\config\MessagesConfig;
use pocketcloud\utils\EnumTrait;

/**
 * @method static NotifyMessage SERVER_START()
 * @method static NotifyMessage SERVER_COULD_NOT_START()
 * @method static NotifyMessage SERVER_TIMED_OUT()
 * @method static NotifyMessage SERVER_STOP()
 * @method static NotifyMessage SERVER_CRASHED()
 */
class NotifyMessage {
    use EnumTrait;

    protected static function init(): void {
        self::register("server_start", new NotifyMessage("server-start", "{PREFIX}§7The server §e%server% §7is §astarting§7..."));
        self::register("server_could_not_start", new NotifyMessage("server-could-not-start", "{PREFIX}§7The server §e%server% §ccouldn't §7be started§7!"));
        self::register("server_timed_out", new NotifyMessage("server-timed-out", "{PREFIX}§7The server §e%server% §7is §ctimed out§7!"));
        self::register("server_stop", new NotifyMessage("server-stop", "{PREFIX}§7The server §e%server% §7is §cstopping§7..."));
        self::register("server_crashed", new NotifyMessage("server-crashed", "{PREFIX}§7The server §e%server% §7was §ccrashed§7!"));
    }

    private array $replacements = [];

    public function __construct(private string $key, private string $default) {}

    public function withReplacements(array $replacements): self {
        $this->replacements = $replacements;
        return $this;
    }

    public function parse(array $replacements = []): string {
        $message = str_replace("{PREFIX}", MessagesConfig::getInstance()->getPrefix(), MessagesConfig::getInstance()->getConfig()->get($this->key, $this->default));
        foreach ($replacements as $key => $replacement) $message = str_replace("%" . $key . "%", $replacement, $message);
        return $message;
    }

    public function resetReplacements(): self {
        $this->replacements = [];
        return $this;
    }

    public function getReplacements(): array {
        return $this->replacements;
    }

    public function getKey(): string {
        return $this->key;
    }

    public function getDefault(): string {
        return $this->default;
    }

    public function hasReplacements(): bool {
        return count($this->replacements) > 0;
    }
}
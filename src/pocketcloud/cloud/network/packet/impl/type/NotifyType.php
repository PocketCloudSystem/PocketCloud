<?php

namespace pocketcloud\cloud\network\packet\impl\type;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\language\Language;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\client\ServerClientCache;
use pocketcloud\cloud\network\packet\impl\normal\CloudNotifyPacket;
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
        self::register("starting", new NotifyType("STARTING", Language::current()->translate("inGame.notify.message.starting")));
        self::register("stopping", new NotifyType("STOPPING", Language::current()->translate("inGame.notify.message.stopping")));
        self::register("timed", new NotifyType("TIMED", Language::current()->translate("inGame.notify.message.timed")));
        self::register("crashed", new NotifyType("CRASHED", Language::current()->translate("inGame.notify.message.crashed")));
        self::register("start_failed", new NotifyType("START_FAILED", Language::current()->translate("inGame.notify.message.start_failed")));
    }

    public function __construct(
        private readonly string $name,
        private readonly string $message
    ) {}

    public function send(array $params): void {
        CloudNotifyPacket::create(str_replace(array_keys($params), array_values($params), $this->message))->broadcastPacket(
            ...ServerClientCache::getInstance()->pick(fn(ServerClient $client) => $client->getServer() !== null && $client->getServer()->getTemplate()->getTemplateType()->isProxy())
        );
    }

    public function getName(): string {
        return $this->name;
    }

    public function getMessage(): string {
        return $this->message;
    }
}
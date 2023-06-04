<?php

namespace pocketcloud\network\packet\impl\types;

use pocketcloud\language\Language;
use pocketcloud\network\client\ServerClient;
use pocketcloud\network\client\ServerClientManager;
use pocketcloud\network\Network;
use pocketcloud\network\packet\impl\normal\CloudNotifyPacket;
use pocketcloud\template\TemplateType;
use pocketcloud\util\EnumTrait;

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
        private string $name,
        private string $message
    ) {}

    public function notify(array $params) {
        Network::getInstance()->broadcastPacket(new CloudNotifyPacket(str_replace(array_keys($params), array_values($params), $this->message)), ...ServerClientManager::getInstance()->pickClients(fn(ServerClient $client) => $client->getServer() !== null && $client->getServer()->getTemplate()->getTemplateType() === TemplateType::PROXY()));
    }

    public function getName(): string {
        return $this->name;
    }

    public function getMessage(): string {
        return $this->message;
    }
}
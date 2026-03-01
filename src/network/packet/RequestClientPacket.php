<?php

namespace pocketcloud\cloud\network\packet;

use Closure;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\util\PacketData;
use pocketcloud\cloud\network\request\RequestManager;
use pocketcloud\cloud\server\CloudServer;
use RuntimeException;
use Throwable;

/**
 * A different version from the regular RequestClientPacket
 * This logic is reversed, means the cloud sends this RequestClientPacket and the sub-servers answer via ResponseClientPacket
 * @see RequestClientPacket
 * @see ResponseClientPacket
 */
abstract class RequestClientPacket extends CloudPacket implements ClientboundPacket {

    private ?string $requestId = null;
    /** @var array<Closure> */
    private array $thenClosures = [];
    private ?Closure $failure = null;

    /** @internal */
    public function prepare(): void {
        if ($this->requestId !== null) return;
        $this->requestId = uniqid();
    }

    final public function encode(PacketData $packetData): void {
        parent::encode($packetData);
        $packetData->write($this->requestId);
    }

    final public function decode(PacketData $packetData): void {
        parent::decode($packetData);
        $this->requestId = $packetData->readString();
    }

    final public function decodePayload(PacketData $packetData): void {}

    /**
     * Should not be used for RequestPackets, use @see RequestClientPacket::sendRequest() instead
     * @param CloudServer|ServerClient $client
     * @return bool
     * @deprecated
     */
    public function sendPacket(CloudServer|ServerClient $client): bool {
        throw new RuntimeException("Use sendRequest() instead of sendPacket()");
    }

    public function sendRequest(CloudServer|ServerClient $client): RequestClientPacket|false {
        $client = $client instanceof ServerClient ? $client : $client->getServerClient();
        if ($client === null) throw new RuntimeException("Server has not been verified yet");
        return RequestManager::getInstance()->send($this, $client);
    }

    final public function invokeClosures(bool $failed, ?ResponseClientPacket $responseClientPacket, ?RequestPacketFailureReason $reason = null): void {
        if ($failed) {
            if ($this->failure !== null) {
                ($this->failure)($this, null, $reason);
            }
            return;
        }

        $value = null;

        try {
            foreach ($this->thenClosures as $thenClosure) {
                $value = ($thenClosure)($responseClientPacket, $value);
            }
        } catch (Throwable $exception) {
            if ($this->failure !== null) {
                ($this->failure)($this, $exception, RequestPacketFailureReason::THEN_CRASHED);
            }
        }
    }

    /**
     * @param Closure(ResponseClientPacket $packet, mixed $initialValue): mixed $closure
     * @return $this
     */
    public function then(Closure $closure): self {
        $this->thenClosures[] = $closure;
        return $this;
    }

    /**
     * @param Closure(RequestClientPacket $packet, ?Throwable $exception, ?RequestPacketFailureReason $failureReason): mixed $closure
     * @return $this
     */
    public function failure(Closure $closure): self {
        $this->failure = $closure;
        return $this;
    }

    public function isPrepared(): bool {
        return $this->requestId !== null;
    }

    public function getRequestId(): ?string {
        return $this->requestId;
    }

    final public function handle(ServerClient $client): void {}

    public static function dynamic(ServerClient|CloudServer $client, mixed ...$args): static {
        return new static(...$args)->sendRequest($client);
    }
}
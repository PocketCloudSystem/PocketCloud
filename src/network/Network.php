<?php

namespace pocketcloud\cloud\network;

use JsonException;
use LogicException;
use pmmp\thread\ConnectionException;
use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\event\impl\network\NetworkPacketPreSendEvent;
use pocketcloud\cloud\event\impl\network\NetworkPacketReceiveEvent;
use pocketcloud\cloud\event\impl\network\NetworkPacketReceivePreProcessEvent;
use pocketcloud\cloud\event\impl\network\NetworkPacketSentEvent;
use pocketcloud\cloud\event\impl\network\NetworkPacketTooLargeEvent;
use pocketcloud\cloud\exception\PacketException;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\client\ServerClientCache;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\PacketPool;
use pocketcloud\cloud\network\packet\ResponseClientPacket;
use pocketcloud\cloud\network\packet\UnhandledPacket;
use pocketcloud\cloud\network\packet\util\PacketSerializer;
use pocketcloud\cloud\network\request\RequestManager;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\thread\Thread;
use pocketcloud\cloud\traffic\impl\NetworkTrafficMonitor;
use pocketcloud\cloud\traffic\TrafficMonitor;
use pocketcloud\cloud\traffic\TrafficMonitorManager;
use pocketcloud\cloud\util\benchmark\Benchmark;
use pocketcloud\cloud\util\net\Address;
use pocketcloud\cloud\util\promise\Promise;
use pocketcloud\cloud\util\trait\SingletonTrait;
use pocketcloud\cloud\util\Utils;
use pocketmine\snooze\SleeperHandlerEntry;
use RuntimeException;
use Socket;
use Throwable;

final class Network extends Thread {
    use SingletonTrait;

    private int $packetSizeLimit;
    private ThreadSafeArray $buffer;
    private ThreadSafeArray $sendBuffer;
    private ThreadSafeArray $disconnectBuffer;
    private SleeperHandlerEntry $handlerEntry;
    private Socket $serverSocket;
    private bool $established = false;
    private string $authenticationKey;

    public function __construct(private readonly Address $address) {
        self::setInstance($this);
        $this->packetSizeLimit = MainConfig::getInstance()->getNetworkPacketSizeLimit();
        $this->buffer = new ThreadSafeArray();
        $this->sendBuffer = new ThreadSafeArray();
        $this->disconnectBuffer = new ThreadSafeArray();
        $this->authenticationKey = Utils::generateString(mt_rand(32, 64));

        PacketPool::init();

        $this->handlerEntry = PocketCloud::getInstance()->getSleeperHandler()->addNotifier(function (): void {
            try {
                $deadline = microtime(true) + 0.010;
                while (microtime(true) < $deadline && ($unhandledPacketData = $this->buffer->shift()) !== null) {
                    if (!$this->established) return;
                    Benchmark::startTiming("packet_receive_handling");
                    [$address, $port, $buffer, $bytes] = $unhandledPacketData;
                    $unhandledPacket = new UnhandledPacket($buffer, Address::create($address, $port), $bytes);

                    TrafficMonitorManager::getInstance()->pushBytes(TrafficMonitorManager::TRAFFIC_NETWORK, $bytes = $unhandledPacket->getBytes(), TrafficMonitor::REGULAR_MODE_IN);
                    TrafficMonitorManager::getInstance()->callHandlers(
                        TrafficMonitorManager::TRAFFIC_NETWORK,
                        TrafficMonitor::REGULAR_MODE_IN,
                        $unhandledPacket->getBuffer(), $bytes, $unhandledPacket->getAddress()
                    );

                    $client = ServerClientCache::getInstance()->getByAddress($unhandledPacket->getAddress()) ?? new ServerClient($unhandledPacket->getAddress());

                    ($ev = new NetworkPacketReceivePreProcessEvent($this, $client, $unhandledPacket->getBuffer(), $encryption = MainConfig::getInstance()->isNetworkEncryptionEnabled()))->call();
                    if ($ev->isCancelled()) return;

                    try {
                        if (($packet = $unhandledPacket->buildCloudPacket($encryption, $this->authenticationKey)) !== null) {
                            TrafficMonitorManager::getInstance()->callHandlers(
                                TrafficMonitorManager::TRAFFIC_NETWORK,
                                NetworkTrafficMonitor::parsePacketMode(NetworkTrafficMonitor::NETWORK_MODE_PACKET_IN, $packet::class),
                                $packet, $unhandledPacket->getAddress()
                            );

                            ($ev = new NetworkPacketReceiveEvent($this, $client, $packet))->call();
                            if ($ev->isCancelled()) return;
                            $packet->handle($client);

                            if ($packet instanceof ResponseClientPacket) {
                                RequestManager::getInstance()->resolve($packet);
                                RequestManager::getInstance()->remove($packet->getRequestId());
                            }
                        } else CloudLogger::get()->debug("Received an unknown packet from §b{}§r, ignoring...", $unhandledPacket->getAddress())->debug("Packet buffer: " . $unhandledPacket->getBuffer());
                    } catch (PacketException|JsonException $e) {
                        CloudLogger::get()->warn("§cFailed to decode packet from §b{}§8: §e{}", $client->getAddress(), $e->getMessage())
                            ->debug($unhandledPacket->getBuffer());
                        CloudLogger::get()->exception($e);
                    }

                    Benchmark::stopTiming("packet_receive_handling");
                }

                while (($disconnectData = $this->disconnectBuffer->shift()) !== null) {
                    if (!$this->established) return;
                    [$address, $port] = $disconnectData;
                    $addr = Address::create($address, $port);
                    $client = ServerClientCache::getInstance()->getByAddress($addr);
                    if ($client !== null) {
                        CloudLogger::get()->debug("TCP client §b{}§r disconnected.", $addr);
                        $client->getServer()?->handleDisconnect();
                    }
                }

            } catch (ConnectionException $e) {
                $this->established = false;
                CloudLogger::get()->exception($e);
            } catch (Throwable $e) {
                CloudLogger::get()->exception($e);
            }
        });
    }

    public function init(): void {
        if ($this->established) throw new LogicException("Socket has already been established");
        $socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
        if (!$socket) throw new RuntimeException(socket_strerror(socket_last_error()));
        $this->serverSocket = $socket;
        socket_set_option($socket, SOL_SOCKET, SO_REUSEADDR, 1);
        socket_set_option($socket, SOL_TCP, TCP_NODELAY, 1);
        if (socket_bind($socket, $this->address->getAddress(), $this->address->getPort())) {
            if (!socket_listen($socket)) {
                throw new RuntimeException(socket_strerror(socket_last_error()));
            }
            $this->established = true;
        } else throw new RuntimeException(socket_strerror(socket_last_error()));

        CloudLogger::get()->success("§bNetwork connection §rhas been §aestablished §ron §b{}§r.", $this->address);
        $this->start();
    }

    protected function onRun(): void {
        $notifier = $this->handlerEntry->createNotifier();
        $receiveBuffer = $this->buffer;
        $sendBuffer = $this->sendBuffer;
        $disconnectBuffer = $this->disconnectBuffer;
        $serverSocket = $this->serverSocket;
        /** @var array<Socket> $clientSockets  address_string => Socket */
        $clientSockets = [];
        /** @var array<string> $clientBuffers  address_string => raw bytes awaiting framing */
        $clientBuffers = [];


        while (true) {
            try {
                if (!$this->established || !$this->isAlive()) break;
                while (($sendData = $sendBuffer->shift()) !== null) {
                    if ($sendData[0] === "__broadcast__") {
                        [, $buffer, $targets] = $sendData;
                        foreach ($targets as $addressStr) {
                            if (isset($clientSockets[$addressStr])) {
                                $this->tcpWrite($clientSockets[$addressStr], $buffer);
                            }
                        }
                    } else {
                        [$addressStr, $buffer] = $sendData;
                        if (isset($clientSockets[$addressStr])) {
                            $this->tcpWrite($clientSockets[$addressStr], $buffer);
                        }
                    }
                }

                $read = [$serverSocket, ...array_values($clientSockets)];
                $write = $except = [];

                if (@socket_select($read, $write, $except, 0, 50 * 1000) < 1) continue;

                if (in_array($serverSocket, $read, true)) {
                    $clientSocket = @socket_accept($serverSocket);
                    if ($clientSocket !== false && $clientSocket !== null) {
                        @socket_getpeername($clientSocket, $peerAddr, $peerPort);
                        $addrStr = $peerAddr . ":" . $peerPort;
                        @socket_set_option($clientSocket, SOL_SOCKET, SO_SNDTIMEO, ["sec" => 0, "usec" => 200000]);
                        $clientSockets[$addrStr] = $clientSocket;
                        $clientBuffers[$addrStr] = "";
                        CloudLogger::get()->debug("New TCP connection from §b{}§r.", $addrStr);
                    }
                }

                $wakeNeeded = false;
                foreach ($clientSockets as $addrStr => $clientSocket) {
                    if (!in_array($clientSocket, $read, true)) continue;

                    $chunk = "";
                    $result = @socket_recv($clientSocket, $chunk, 65535, 0);

                    if ($result === 0 || $result === false) {
                        [$addr, $port] = explode(":", $addrStr, 2);
                        $disconnectBuffer[] = ThreadSafeArray::fromArray([$addr, (int) $port]);
                        @socket_close($clientSocket);
                        unset($clientSockets[$addrStr], $clientBuffers[$addrStr]);
                        $notifier->wakeupSleeper();
                        continue;
                    }

                    $clientBuffers[$addrStr] .= $chunk;

                    // Parse length-prefixed frames: [4-byte big-endian uint][payload]
                    while (strlen($clientBuffers[$addrStr]) >= 4) {
                        $length = unpack("N", substr($clientBuffers[$addrStr], 0, 4))[1];
                        if ($length > $this->packetSizeLimit || $length < 0) {
                            CloudLogger::get()->debug("Client §b$addrStr §rsent a packet exceeding the limit ($length bytes). Discarding...");

                            [$addr, $port] = explode(":", $addrStr, 2);
                            $disconnectBuffer[] = ThreadSafeArray::fromArray([$addr, (int) $port]);
                            @socket_close($clientSocket);
                            unset($clientSockets[$addrStr], $clientBuffers[$addrStr]);
                            $notifier->wakeupSleeper();
                            continue 2;
                        }

                        if (strlen($clientBuffers[$addrStr]) < (4 + $length)) break;

                        $buffer = substr($clientBuffers[$addrStr], 4, $length);
                        $clientBuffers[$addrStr] = substr($clientBuffers[$addrStr], 4 + $length);

                        [$addr, $port] = explode(":", $addrStr, 2);
                        $receiveBuffer[] = ThreadSafeArray::fromArray([$addr, (int) $port, $buffer, $length]);
                        $wakeNeeded = true;
                    }
                }

                if ($wakeNeeded) $notifier->wakeupSleeper();
            } catch (ConnectionException $e) {
                $this->established = false;
                $this->logThreadException($e);
                break;
            } catch (Throwable $e) {
                $this->logThreadException($e);
            }
        }

        foreach ($clientSockets as $sock) {
            @socket_close($sock);
        }
    }

    private function tcpWrite(Socket $socket, string $buffer): bool {
        $framed = pack("N", strlen($buffer)) . $buffer;
        $total = strlen($framed);
        $sent = 0;
        while ($sent < $total) {
            $result = @socket_write($socket, substr($framed, $sent), $total - $sent);
            if ($result === false || $result === 0) return false;
            $sent += $result;
        }

        return true;
    }

    private function logThreadException(Throwable $e): void {
        try {
            $this->logger?->exception($e);
        } catch (Throwable) {}
    }

    public function sendPacket(ClientboundPacket $packet, ServerClient $client): bool {
        if (!$this->established) return false;
        ($ev = new NetworkPacketPreSendEvent($this, $client, $packet))->call();
        if ($ev->isCancelled()) return false;
        Benchmark::startTiming("packet_send_handling");
        $buffer = PacketSerializer::encode($packet, MainConfig::getInstance()->isNetworkEncryptionEnabled(), $this->authenticationKey);
        if ($buffer === null) {
            Benchmark::stopTiming("packet_send_handling");
            return false;
        }

        if (($size = strlen($buffer)) > $this->packetSizeLimit) {
            Benchmark::stopTiming("packet_send_handling");
            new NetworkPacketTooLargeEvent($this, $client, $packet, $size, $buffer)->call();
            return false;
        }

        $success = $this->write($buffer, $client->getAddress());

        TrafficMonitorManager::getInstance()->callHandlers(
            TrafficMonitorManager::TRAFFIC_NETWORK,
            NetworkTrafficMonitor::parsePacketMode(NetworkTrafficMonitor::NETWORK_MODE_PACKET_OUT, $packet::class),
            $packet, $client->getAddress(), $success
        );

        new NetworkPacketSentEvent($this, $client, $packet, $success)->call();
        Benchmark::stopTiming("packet_send_handling");
        return $success;
    }

    public function broadcastPacket(ClientboundPacket $packet, ServerClient|TemplateType ...$exclusions): Promise {
        if (!$this->established) return Promise::all([]);
        Benchmark::startTiming("packet_broadcast_send_handling");

        $buffer = PacketSerializer::encode($packet, MainConfig::getInstance()->isNetworkEncryptionEnabled(), $this->authenticationKey);
        if ($buffer === null) {
            Benchmark::stopTiming("packet_broadcast_send_handling");
            return Promise::rejected("Buffer null");
        }

        $targets = [];
        $promises = [];
        foreach (ServerClientCache::getInstance()->getAll() as $client) {
            if ($client->getServer() === null) continue;
            if (in_array($client, $exclusions) || in_array($client->getServer()->getTemplate()->getTemplateType(), $exclusions)) continue;

            ($ev = new NetworkPacketPreSendEvent($this, $client, $packet))->call();
            if ($ev->isCancelled()) {
                $promises[] = Promise::rejected();
                continue;
            }

            $targets[] = $client->getAddress()->toString();
            $promises[] = Promise::resolved();
        }

        if (!empty($targets)) {
            $this->sendBuffer[] = ThreadSafeArray::fromArray(['__broadcast__', $buffer, ThreadSafeArray::fromArray($targets)]);
            $totalBytes = strlen($buffer) * count($targets);
            TrafficMonitorManager::getInstance()->pushBytes(TrafficMonitorManager::TRAFFIC_NETWORK, $totalBytes, TrafficMonitor::REGULAR_MODE_OUT);
        }

        Benchmark::stopTiming("packet_broadcast_send_handling");
        return Promise::all($promises);
    }

    public function write(string $buffer, Address $dst, ?int &$bytes = null): bool {
        if (!$this->established) return false;
        $bytes = strlen($buffer);
        $this->sendBuffer[] = ThreadSafeArray::fromArray([$dst->toString(), $buffer]);

        TrafficMonitorManager::getInstance()->pushBytes(TrafficMonitorManager::TRAFFIC_NETWORK, $bytes, TrafficMonitor::REGULAR_MODE_OUT);
        TrafficMonitorManager::getInstance()->callHandlers(
            TrafficMonitorManager::TRAFFIC_NETWORK,
            TrafficMonitor::REGULAR_MODE_OUT,
            $buffer, $bytes, $dst
        );

        return true;
    }

    public function close(): void {
        if ($this->established) {
            $this->established = false;
            PocketCloud::getInstance()->getSleeperHandler()->removeNotifier($this->handlerEntry->getNotifierId());
            @socket_shutdown($this->serverSocket);
            @socket_close($this->serverSocket);
        }

        if ($this->isStarted() && !$this->isJoined()) $this->quit();
    }

    public function getSocket(): Socket {
        return $this->serverSocket;
    }

    public function getPacketSizeLimit(): int {
        return $this->packetSizeLimit;
    }

    public function isEstablished(): bool {
        return $this->established;
    }

    public function getAuthenticationKey(): string {
        return $this->authenticationKey;
    }

    public function getAddress(): Address {
        return $this->address;
    }

    public static function getInstance(): self {
        return self::$instance;
    }
}
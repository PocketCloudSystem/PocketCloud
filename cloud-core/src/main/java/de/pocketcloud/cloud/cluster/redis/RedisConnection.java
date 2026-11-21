package de.pocketcloud.cloud.cluster.redis;

import de.pocketcloud.cloud.config.sub.RedisConfiguration;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;

import java.time.Duration;

/**
 * One shared Redis connection of this node. Lettuce connections are thread-safe,
 * so all components use the same one.
 */
public final class RedisConnection implements AutoCloseable {

    private final String keyPrefix;
    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;

    private RedisConnection(String keyPrefix, RedisClient client, StatefulRedisConnection<String, String> connection) {
        this.keyPrefix = keyPrefix;
        this.client = client;
        this.connection = connection;
    }

    /**
     * Connects and checks the connection with a PING.
     *
     * @param cloudName used as namespace, keys look like {@code pc:<cloudName>:servers}
     */
    public static RedisConnection open(RedisConfiguration config, String cloudName) {
        RedisURI.Builder uri = RedisURI.builder()
                .withHost(config.address())
                .withPort(config.port())
                .withDatabase(config.database())
                .withSsl(config.ssl())
                .withTimeout(Duration.ofMillis(config.timeout()));

        if (!config.password().isEmpty()) {
            if (config.username().isEmpty()) uri.withPassword(config.password().toCharArray());
            else uri.withAuthentication(config.username(), config.password());
        }

        RedisClient client = RedisClient.create(uri.build());
        try {
            StatefulRedisConnection<String, String> connection = client.connect();
            connection.sync().ping();
            return new RedisConnection("pc:" + cloudName + ":", client, connection);
        } catch (RuntimeException e) {
            client.shutdown();
            throw e;
        }
    }

    public String key(String logicalKey) {
        return keyPrefix + logicalKey;
    }

    public String keyPrefix() {
        return keyPrefix;
    }

    public RedisCommands<String, String> sync() {
        return connection.sync();
    }

    public RedisAsyncCommands<String, String> async() {
        return connection.async();
    }

    public boolean isOpen() {
        return connection.isOpen();
    }

    @Override
    public void close() {
        connection.close();
        client.shutdown();
    }
}

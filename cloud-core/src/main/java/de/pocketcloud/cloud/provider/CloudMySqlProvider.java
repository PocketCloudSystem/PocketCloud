package de.pocketcloud.cloud.provider;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cache.NotificationListCache;
import de.pocketcloud.cloud.cache.WhitelistCache;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.provider.database.DatabaseQueries;
import de.pocketcloud.cloud.provider.database.MySqlSettings;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.common.cache.LocalCache;
import de.pocketcloud.common.concurrent.Promise;
import de.pocketcloud.common.util.FileUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class CloudMySqlProvider extends CloudProvider {

    private final DataSource connectionPool;

    public CloudMySqlProvider() {
        this.connectionPool = buildConnectionPool(PocketCloud.instance().config().mysqlSettings().asSettings());

        executeAsync(DatabaseQueries.createTables()).thenSuccess(_ -> {
            getWhitelist().thenSuccess(list -> LocalCache.get(WhitelistCache.class).syncIn(list.stream()
                    .collect(Collectors.toMap(Function.identity(), _ -> true))));

            getNotificationList().thenSuccess(list -> LocalCache.get(NotificationListCache.class).syncIn(list.stream()
                    .collect(Collectors.toMap(Function.identity(), _ -> true))));
        });
    }

    @Override
    public Promise<Void> addTemplate(ITemplate template) {
        Map<String, Object> data = template.write();
        return executeAsync(DatabaseQueries.addTemplate(), data.values().toArray());
    }

    @Override
    public Promise<Void> removeTemplate(ITemplate template) {
        return executeAsync(DatabaseQueries.removeTemplate(), template.name());
    }

    @Override
    public Promise<Void> editTemplate(ITemplate template, Map<String, Object> newData) {
        Object[] params = buildUpdateParams(newData, template.name());
        return executeAsync(DatabaseQueries.editTemplate(newData), params);
    }

    @Override
    public Promise<Optional<Template>> getTemplate(String templateName) {
        Promise<Optional<Template>> promise = new Promise<>();
        queryAsync(DatabaseQueries.getTemplate(), templateName).thenSuccess(rows -> {
            if (rows.isEmpty()) {
                promise.resolve(Optional.empty());
                return;
            }

            try {
                promise.resolve(Optional.of(Template.read(rows.getFirst())));
            } catch (Exception e) {
                Promise.rejected(e);
            }
        }).failure(promise::reject);

        return promise;
    }

    @Override
    public Promise<Boolean> checkTemplate(String template) {
        Promise<Boolean> promise = new Promise<>();
        countAsync(DatabaseQueries.checkTemplate(), template)
                .thenSuccess(count -> promise.resolve(count > 0))
                .failure(promise::reject);

        return promise;
    }

    @Override
    public Promise<Map<String, Template>> getTemplates() {
        Promise<Map<String, Template>> promise = new Promise<>();
        queryAsync(DatabaseQueries.getTemplates()).thenSuccess(rows -> {
            Map<String, Template> templates = new HashMap<>();
            rows.forEach(row -> {
                try {
                    Template t = Template.read(row);
                    templates.put(t.name(), t);
                } catch (Exception _) {}
            });

            promise.resolve(templates);
        }).failure(promise::reject);

        return promise;
    }

    @Override
    public Promise<Void> addServerGroup(IServerGroup serverGroup) {
        Map<String, Object> data = serverGroup.write();
        if (data.get("templates") instanceof List) data.put("templates", FileUtils.encodeJson(data.get("templates")));
        return executeAsync(DatabaseQueries.addServerGroup(), data.values().toArray());
    }

    @Override
    public Promise<Void> removeServerGroup(IServerGroup serverGroup) {
        return executeAsync(DatabaseQueries.removeServerGroup(), serverGroup.name());
    }

    @Override
    public Promise<Void> editServerGroup(IServerGroup serverGroup, Map<String, Object> newData) {
        if (newData.get("templates") instanceof List)
            newData.put("templates", FileUtils.encodeJson(newData.get("templates")));
        Object[] params = buildUpdateParams(newData, serverGroup.name());
        return executeAsync(DatabaseQueries.editServerGroup(newData), params);
    }

    @Override
    public Promise<Optional<ServerGroup>> getServerGroup(String serverGroupName) {
        Promise<Optional<ServerGroup>> promise = new Promise<>();
        queryAsync(DatabaseQueries.getServerGroup(), serverGroupName).thenSuccess(rows -> {
            if (rows.isEmpty()) {
                promise.resolve(Optional.empty());
                return;
            }

            try {
                ServerGroup sg = ServerGroup.read(rows.getFirst());
                promise.resolve(Optional.of(sg));
            } catch (Exception e) {
                promise.reject(e);
            }
        }).failure(promise::reject);

        return promise;
    }

    @Override
    public Promise<Boolean> checkServerGroup(String serverGroup) {
        Promise<Boolean> promise = new Promise<>();
        countAsync(DatabaseQueries.checkServerGroup(), serverGroup)
                .thenSuccess(count -> promise.resolve(count > 0))
                .failure(promise::reject);

        return promise;
    }

    @Override
    public Promise<Map<String, ServerGroup>> getServerGroups() {
        Promise<Map<String, ServerGroup>> promise = new Promise<>();
        queryAsync(DatabaseQueries.getServerGroups()).thenSuccess(rows -> {
            Map<String, ServerGroup> serverGroups = new HashMap<>();
            rows.forEach(row -> {
                try {
                    ServerGroup sg = ServerGroup.read(row);
                    serverGroups.put(sg.name(), sg);
                } catch (Exception _) {}
            });
            promise.resolve(serverGroups);
        }).failure(promise::reject);

        return promise;
    }

    @Override
    public Promise<Void> enablePlayerNotifications(String player) {
        LocalCache.get(NotificationListCache.class).add(player, true);
        return executeAsync(DatabaseQueries.enablePlayerNotifications(), player);
    }

    @Override
    public Promise<Void> disablePlayerNotifications(String player) {
        LocalCache.get(NotificationListCache.class).remove(player);
        return executeAsync(DatabaseQueries.disablePlayerNotifications(), player);
    }

    @Override
    public Promise<Boolean> hasNotificationsEnabled(String player) {
        Promise<Boolean> promise = new Promise<>();
        countAsync(DatabaseQueries.hasNotificationsEnabled(), player)
                .thenSuccess(count -> promise.resolve(count > 0))
                .failure(promise::reject);

        return promise;
    }

    @Override
    public Promise<List<String>> getNotificationList() {
        Promise<List<String>> promise = new Promise<>();
        queryAsync(DatabaseQueries.getNotificationList()).
                thenSuccess(rows -> {
                    List<String> list = rows.stream().map(r -> (String) r.get("player")).toList();
                    promise.resolve(list);
                }).failure(promise::reject);

        return promise;
    }

    @Override
    public Promise<Void> addToWhitelist(String player) {
        LocalCache.get(WhitelistCache.class).add(player, true);
        return executeAsync(DatabaseQueries.addToWhitelist(), player);
    }

    @Override
    public Promise<Void> removeFromWhitelist(String player) {
        LocalCache.get(WhitelistCache.class).remove(player);
        return executeAsync(DatabaseQueries.removeFromWhitelist(), player);
    }

    @Override
    public Promise<Boolean> isOnWhitelist(String player) {
        Promise<Boolean> promise = new Promise<>();
        countAsync(DatabaseQueries.isOnWhitelist(), player)
                .thenSuccess(count -> promise.resolve(count > 0))
                .failure(promise::reject);

        return promise;
    }

    @Override
    public Promise<List<String>> getWhitelist() {
        Promise<List<String>> promise = new Promise<>();
        queryAsync(DatabaseQueries.getWhitelist()).thenSuccess(rows -> {
            List<String> list = rows.stream().map(r -> (String) r.get("player")).toList();
            promise.resolve(list);
        }).failure(promise::reject);

        return promise;
    }

    private Promise<Void> executeAsync(String sql, Object... params) {
        return Promise.runAsync(() -> {
            try (Connection conn = connectionPool.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                bindParams(ps, params);
                ps.executeUpdate();
            } catch (SQLException e) {
                CloudLogger.get().exception("MySQL execute rejecteded: {}", e, sql);
                throw new RuntimeException(e);
            }
        });
    }

    private Promise<List<Map<String, Object>>> queryAsync(String sql, Object... params) {
        return Promise.supplyAsync(() -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                bindParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int cols = meta.getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= cols; i++) {
                            row.put(meta.getColumnLabel(i), rs.getObject(i));
                        }
                        rows.add(row);
                    }
                }
            } catch (SQLException e) {
                CloudLogger.get().exception("MySQL query rejecteded: {}", e, sql);
                throw new RuntimeException(e);
            }
            return rows;
        });
    }

    private Promise<Integer> countAsync(String sql, Object... params) {
        return queryAsync(sql, params).thenApply(rows -> {
            if (rows.isEmpty()) return 0;
            Object v = rows.getFirst().values().iterator().next();
            return v instanceof Number ? ((Number) v).intValue() : 0;
        });
    }

    private static void bindParams(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    private static Object[] buildUpdateParams(Map<String, Object> data, String pkValue) {
        List<Object> params = new ArrayList<>(data.values());
        params.add(pkValue);
        return params.toArray();
    }

    private static DataSource buildConnectionPool(MySqlSettings mysqlSettings) {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl("jdbc:mysql://" + mysqlSettings.address() + ":" + mysqlSettings.port() + "/" + mysqlSettings.database());
        config.setUsername(mysqlSettings.user());
        config.setPassword(mysqlSettings.password());

        return new HikariDataSource(config);
    }
}
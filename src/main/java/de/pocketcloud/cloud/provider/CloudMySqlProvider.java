package de.pocketcloud.cloud.provider;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cache.*;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.provider.database.DatabaseQueries;
import de.pocketcloud.cloud.provider.database.MySqlSettings;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.cloud.util.FileUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class CloudMySqlProvider extends CloudProvider {

    private final DataSource connectionPool;

    public CloudMySqlProvider() {
        this.connectionPool = buildConnectionPool(PocketCloud.getInstance().config().getMysqlSettings());

        executeAsync(DatabaseQueries.createTables()).thenRun(() -> {
            //TODO migration
//            MigratorManager.getInstance().getAll(
//                (IMigrator m) -> m.id().startsWith("migrate-json-")
//            ).forEach(migrator -> {
//                if (migrator.requiresMigration()) {
//                    MigratorManager.getInstance().migrate(migrator);
//                }
//            });
        });

        LocalCache.get(ActiveInGameModuleCache.class).getAll().forEach(module -> {
            getModuleState(module).thenAccept(enabled -> {
                Boolean realEnabled = enabled.orElse(false);
                if (enabled.isEmpty()) {
                    executeAsync(DatabaseQueries.insertModuleState(), module, false);
                }

                LocalCache.get(ActiveInGameModuleCache.class).set(module, realEnabled);
            });
        });

        getWhitelist().thenAccept(LocalCache.get(WhitelistCache.class)::syncIn);
        getNotificationList().thenAccept(LocalCache.get(NotificationListCache.class)::syncIn);
    }

    @Override
    public CompletableFuture<Void> addTemplate(Template template) {
        Map<String, Object> data = template.write();
        return executeAsync(DatabaseQueries.addTemplate(), data.values().toArray());
    }

    @Override
    public CompletableFuture<Void> removeTemplate(Template template) {
        return executeAsync(DatabaseQueries.removeTemplate(), template.name());
    }

    @Override
    public CompletableFuture<Void> editTemplate(Template template, Map<String, Object> newData) {
        Object[] params = buildUpdateParams(newData, template.name());
        return executeAsync(DatabaseQueries.editTemplate(newData), params);
    }

    @Override
    public CompletableFuture<Optional<Template>> getTemplate(String templateName) {
        CompletableFuture<Optional<Template>> promise = new CompletableFuture<>();
        queryAsync(DatabaseQueries.getTemplate(), templateName).thenAccept(rows -> {
            if (rows.isEmpty()) {
                promise.complete(Optional.empty());
                return;
            }

            try {
                promise.complete(Optional.of(Template.read(rows.getFirst())));
            } catch (Exception e) {
                promise.completeExceptionally(e);
            }
        }).exceptionally(ex -> {
            promise.completeExceptionally(ex);
            return null;
        });

        return promise;
    }

    @Override
    public CompletableFuture<Boolean> checkTemplate(String template) {
        CompletableFuture<Boolean> promise = new CompletableFuture<>();
        countAsync(DatabaseQueries.checkTemplate(), template)
                .thenAccept(count -> promise.complete(count > 0))
                .exceptionally(ex -> {
                    promise.completeExceptionally(ex);
                    return null;
                });
        return promise;
    }

    @Override
    public CompletableFuture<Map<String, Template>> getTemplates() {
        CompletableFuture<Map<String, Template>> promise = new CompletableFuture<>();
        queryAsync(DatabaseQueries.getTemplates()).thenAccept(rows -> {
            Map<String, Template> templates = new HashMap<>();
            rows.forEach(row -> {
                try {
                    Template t = Template.read(row);
                    templates.put(t.name(), t);
                } catch (Exception _) {}
            });

            promise.complete(templates);
        }).exceptionally(ex -> {
            promise.completeExceptionally(ex);
            return null;
        });

        return promise;
    }

    @Override
    public CompletableFuture<Void> addServerGroup(ServerGroup serverGroup) {
        Map<String, Object> data = serverGroup.write();
        if (data.get("templates") instanceof List) data.put("templates", FileUtils.encodeJson(data.get("templates")));
        return executeAsync(DatabaseQueries.addServerGroup(), data.values().toArray());
    }

    @Override
    public CompletableFuture<Void> removeServerGroup(ServerGroup serverGroup) {
        return executeAsync(DatabaseQueries.removeServerGroup(), serverGroup.name());
    }

    @Override
    public CompletableFuture<Void> editServerGroup(ServerGroup serverGroup, Map<String, Object> newData) {
        if (newData.get("templates") instanceof List) newData.put("templates", FileUtils.encodeJson(newData.get("templates")));
        Object[] params = buildUpdateParams(newData, serverGroup.name());
        return executeAsync(DatabaseQueries.editServerGroup(newData), params);
    }

    @Override
    public CompletableFuture<Optional<ServerGroup>> getServerGroup(String serverGroupName) {
        CompletableFuture<Optional<ServerGroup>> promise = new CompletableFuture<>();
        queryAsync(DatabaseQueries.getServerGroup(), serverGroupName).thenAccept(rows -> {
            if (rows.isEmpty()) {
                promise.complete(Optional.empty());
                return;
            }

            try {
                ServerGroup sg = ServerGroup.read(rows.getFirst());
                promise.complete(Optional.of(sg));
            } catch (Exception e) {
                promise.completeExceptionally(e);
            }
        }).exceptionally(ex -> {
            promise.completeExceptionally(ex);
            return null;
        });
        return promise;
    }

    @Override
    public CompletableFuture<Boolean> checkServerGroup(String serverGroup) {
        CompletableFuture<Boolean> promise = new CompletableFuture<>();
        countAsync(DatabaseQueries.checkServerGroup(), serverGroup)
                .thenAccept(count -> promise.complete(count > 0))
                .exceptionally(ex -> {
                    promise.completeExceptionally(ex);
                    return null;
                });
        return promise;
    }

    @Override
    public CompletableFuture<Map<String, ServerGroup>> getServerGroups() {
        CompletableFuture<Map<String, ServerGroup>> promise = new CompletableFuture<>();
        queryAsync(DatabaseQueries.getServerGroups()).thenAccept(rows -> {
            Map<String, ServerGroup> serverGroups = new HashMap<>();
            rows.forEach(row -> {
                try {
                    ServerGroup sg = ServerGroup.read(row);
                    serverGroups.put(sg.name(), sg);
                } catch (Exception _) {}
            });
            promise.complete(serverGroups);
        }).exceptionally(ex -> {
            promise.completeExceptionally(ex);
            return null;
        });
        return promise;
    }

    @Override
    public CompletableFuture<Void> setModuleState(String module, boolean enabled) {
        CompletableFuture<Void> promise = new CompletableFuture<>();
        LocalCache.get(ActiveInGameModuleCache.class).set(module, enabled);
        countAsync(DatabaseQueries.checkModuleState(), module).thenAccept(count -> {
            String sql = count > 0 ? DatabaseQueries.setModuleState() : DatabaseQueries.insertModuleState();
            executeAsync(sql, enabled, module)
                    .thenRun(() -> promise.complete(null))
                    .exceptionally(ex -> {
                        promise.completeExceptionally(ex);
                        return null;
                    });
        });
        return promise;
    }

    @Override
    public CompletableFuture<Optional<Boolean>> getModuleState(String module) {
        CompletableFuture<Optional<Boolean>> promise = new CompletableFuture<>();
        queryAsync(DatabaseQueries.getModuleState(), module).thenAccept(rows -> {
            if (rows.isEmpty()) {
                promise.complete(Optional.empty());
            } else {
                Object enabled = rows.getFirst().get("enabled");
                promise.complete(Optional.of(Integer.valueOf(1).equals(enabled) || Boolean.TRUE.equals(enabled)));
            }
        }).exceptionally(ex -> {
            promise.completeExceptionally(ex);
            return null;
        });
        return promise;
    }

    @Override
    public CompletableFuture<Void> enablePlayerNotifications(String player) {
        return executeAsync(DatabaseQueries.enablePlayerNotifications(), player);
    }

    @Override
    public CompletableFuture<Void> disablePlayerNotifications(String player) {
        return executeAsync(DatabaseQueries.disablePlayerNotifications(), player);
    }

    @Override
    public CompletableFuture<Boolean> hasNotificationsEnabled(String player) {
        CompletableFuture<Boolean> promise = new CompletableFuture<>();
        countAsync(DatabaseQueries.hasNotificationsEnabled(), player)
                .thenAccept(count -> promise.complete(count > 0))
                .exceptionally(ex -> {
                    promise.completeExceptionally(ex);
                    return null;
                });
        return promise;
    }

    @Override
    public CompletableFuture<List<String>> getNotificationList() {
        CompletableFuture<List<String>> promise = new CompletableFuture<>();
        queryAsync(DatabaseQueries.getNotificationList()).
                thenAccept(rows -> {
                    List<String> list = rows.stream().map(r -> (String) r.get("player")).toList();
                    promise.complete(list);
                }).exceptionally(ex -> {
                    promise.completeExceptionally(ex);
                    return null;
                });
        return promise;
    }

    @Override
    public CompletableFuture<Void> addToWhitelist(String player) {
        LocalCache.get(WhitelistCache.class).add(player);
        return executeAsync(DatabaseQueries.addToWhitelist(), player);
    }

    @Override
    public CompletableFuture<Void> removeFromWhitelist(String player) {
        LocalCache.get(WhitelistCache.class).remove(player);
        return executeAsync(DatabaseQueries.removeFromWhitelist(), player);
    }

    @Override
    public CompletableFuture<Boolean> isOnWhitelist(String player) {
        CompletableFuture<Boolean> promise = new CompletableFuture<>();
        countAsync(DatabaseQueries.isOnWhitelist(), player)
                .thenAccept(count -> promise.complete(count > 0))
                .exceptionally(ex -> {
                    promise.completeExceptionally(ex);
                    return null;
                });
        return promise;
    }

    @Override
    public CompletableFuture<List<String>> getWhitelist() {
        CompletableFuture<List<String>> promise = new CompletableFuture<>();
        queryAsync(DatabaseQueries.getWhitelist()).thenAccept(rows -> {
            List<String> list = rows.stream().map(r -> (String) r.get("player")).toList();
            promise.complete(list);
        }).exceptionally(ex -> {
            promise.completeExceptionally(ex);
            return null;
        });
        return promise;
    }

    private CompletableFuture<Void> executeAsync(String sql, Object... params) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = connectionPool.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                bindParams(ps, params);
                ps.executeUpdate();
            } catch (SQLException e) {
                CloudLogger.get().exception("MySQL execute failed: {}", e, sql);
                throw new RuntimeException(e);
            }
        });
    }

    private CompletableFuture<List<Map<String, Object>>> queryAsync(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
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
                CloudLogger.get().exception("MySQL query failed: {}", e, sql);
                throw new RuntimeException(e);
            }
            return rows;
        });
    }

    private CompletableFuture<Integer> countAsync(String sql, Object... params) {
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

        config.setJdbcUrl("jdbc:mysql://" + mysqlSettings.address() + ":" + mysqlSettings.port()  + "/" + mysqlSettings.database());
        config.setUsername(mysqlSettings.user());
        config.setPassword(mysqlSettings.password());

        return new HikariDataSource(config);
    }
}

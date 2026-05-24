package de.pocketcloud.cloud.provider.database;

import de.pocketcloud.cloud.template.util.TemplateHelper;

import java.util.Map;

public final class DatabaseQueries {

    public static String createTables() {
        return
            "CREATE TABLE IF NOT EXISTS " + DatabaseTables.TEMPLATES + " (" +
                "name VARCHAR(50) PRIMARY KEY," +
                "lobby BOOL," +
                "maintenance BOOL," +
                "`staticServers` BOOL," +
                "maxPlayerCount INTEGER," +
                "minServerCount INTEGER," +
                "maxServerCount INTEGER," +
                "startNewPercentage FLOAT," +
                "autoStart BOOL," +
                "alwaysCopyToStaticServers BOOL," +
                "templateType VARCHAR(20)" +
                "serverSoftware VARCHAR(30)" +
            "); " +
            "CREATE TABLE IF NOT EXISTS " + DatabaseTables.SERVER_GROUPS + " (" +
                "name VARCHAR(50) PRIMARY KEY," +
                "templates TEXT" +
            "); " +
            "CREATE TABLE IF NOT EXISTS " + DatabaseTables.MODULES + " (" +
                "module VARCHAR(100) PRIMARY KEY," +
                "enabled BOOL" +
            "); " +
            "CREATE TABLE IF NOT EXISTS " + DatabaseTables.NOTIFICATIONS + " (" +
                "player VARCHAR(16) PRIMARY KEY" +
            "); " +
            "CREATE TABLE IF NOT EXISTS " + DatabaseTables.MAINTENANCE_LIST + " (" +
                "player VARCHAR(16) PRIMARY KEY" +
            ");";
    }

    public static String addTemplate() {
        return buildInsert(DatabaseTables.TEMPLATES, TemplateHelper.KEYS.toArray(new String[0]));
    }

    public static String removeTemplate() {
        return "DELETE FROM " + DatabaseTables.TEMPLATES + " WHERE name = ?";
    }

    public static String editTemplate(Map<String, Object> newData) {
        return buildUpdate(DatabaseTables.TEMPLATES, newData, "name");
    }

    public static String getTemplate() {
        return "SELECT " + String.join(", ", TemplateHelper.KEYS) + " FROM " + DatabaseTables.TEMPLATES + " WHERE name = ?";
    }

    public static String checkTemplate() {
        return "SELECT COUNT(*) FROM " + DatabaseTables.TEMPLATES + " WHERE name = ?";
    }

    public static String getTemplates() {
        return "SELECT " + String.join(", ", TemplateHelper.KEYS) + " FROM " + DatabaseTables.TEMPLATES;
    }

    public static String addServerGroup() {
        return buildInsert(DatabaseTables.SERVER_GROUPS, new String[]{"name", "templates"});
    }

    public static String removeServerGroup() {
        return "DELETE FROM " + DatabaseTables.SERVER_GROUPS + " WHERE name = ?";
    }

    public static String editServerGroup(Map<String, Object> newData) {
        return buildUpdate(DatabaseTables.SERVER_GROUPS, newData, "name");
    }

    public static String getServerGroup() {
        return "SELECT name, templates FROM " + DatabaseTables.SERVER_GROUPS + " WHERE name = ?";
    }

    public static String checkServerGroup() {
        return "SELECT COUNT(*) FROM " + DatabaseTables.SERVER_GROUPS + " WHERE name = ?";
    }

    public static String getServerGroups() {
        return "SELECT name, templates FROM " + DatabaseTables.SERVER_GROUPS;
    }

    public static String insertModuleState() {
        return "INSERT INTO " + DatabaseTables.MODULES + " (module, enabled) VALUES (?, ?)";
    }

    public static String setModuleState() {
        return "UPDATE " + DatabaseTables.MODULES + " SET enabled = ? WHERE module = ?";
    }

    public static String checkModuleState() {
        return "SELECT COUNT(*) FROM " + DatabaseTables.MODULES + " WHERE module = ?";
    }

    public static String getModuleState() {
        return "SELECT enabled FROM " + DatabaseTables.MODULES + " WHERE module = ?";
    }

    public static String enablePlayerNotifications() {
        return "INSERT IGNORE INTO " + DatabaseTables.NOTIFICATIONS + " (player) VALUES (?)";
    }

    public static String disablePlayerNotifications() {
        return "DELETE FROM " + DatabaseTables.NOTIFICATIONS + " WHERE player = ?";
    }

    public static String hasNotificationsEnabled() {
        return "SELECT COUNT(*) FROM " + DatabaseTables.NOTIFICATIONS + " WHERE player = ?";
    }

    public static String getNotificationList() {
        return "SELECT player FROM " + DatabaseTables.NOTIFICATIONS;
    }

    public static String addToWhitelist() {
        return "INSERT IGNORE INTO " + DatabaseTables.MAINTENANCE_LIST + " (player) VALUES (?)";
    }

    public static String removeFromWhitelist() {
        return "DELETE FROM " + DatabaseTables.MAINTENANCE_LIST + " WHERE player = ?";
    }

    public static String isOnWhitelist() {
        return "SELECT COUNT(*) FROM " + DatabaseTables.MAINTENANCE_LIST + " WHERE player = ?";
    }

    public static String getWhitelist() {
        return "SELECT player FROM " + DatabaseTables.MAINTENANCE_LIST;
    }

    private static String buildInsert(String table, String[] columns) {
        String cols = String.join(", ", columns);
        String placeholders = "?,".repeat(columns.length);
        placeholders = placeholders.substring(0, placeholders.length() - 1);
        return "INSERT INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")";
    }

    private static String buildUpdate(String table, Map<String, Object> data, String whereKey) {
        StringBuilder sb = new StringBuilder("UPDATE ").append(table).append(" SET ");
        data.keySet().stream()
            .filter(k -> !k.equals(whereKey))
            .forEach(k -> sb.append(k).append(" = ?, "));

        sb.setLength(sb.length() - 2);
        sb.append(" WHERE ").append(whereKey).append(" = ?");
        return sb.toString();
    }
}
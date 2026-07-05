package de.pocketcloud.cloud.provider.database;

public record MySqlSettings(String address, Integer port, String database, String user, String password) {}
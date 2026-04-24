package com.magiclink.core.repository;

import lombok.extern.slf4j.Slf4j;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Slf4j
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:magiclink.db";

    public Connection getConnection() throws Exception {
        return DriverManager.getConnection(DB_URL);
    }

    public void initialize() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Nodes table
            stmt.execute("CREATE TABLE IF NOT EXISTS nodes (" +
                    "id TEXT PRIMARY KEY, " +
                    "type TEXT, " +
                    "host TEXT, " +
                    "port INTEGER, " +
                    "country TEXT, " +
                    "source TEXT, " +
                    "latency INTEGER, " +
                    "params TEXT)"); // JSON stored as TEXT

            // Sources table
            stmt.execute("CREATE TABLE IF NOT EXISTS sources (" +
                    "id TEXT PRIMARY KEY, " +
                    "name TEXT, " +
                    "url TEXT, " +
                    "type TEXT, " +
                    "pattern TEXT, " +
                    "last_etag TEXT, " +
                    "last_update TEXT, " +
                    "ttl_hours INTEGER)");

            log.info("Database initialized successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize database", e);
        }
    }
}

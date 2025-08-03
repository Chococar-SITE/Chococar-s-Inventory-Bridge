package site.chococar.inventorybridge.common.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import site.chococar.inventorybridge.common.config.ConfigurationManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseConnection.class);
    private final ConfigurationManager config;
    private HikariDataSource dataSource;
    private String tablePrefix;
    
    public DatabaseConnection(ConfigurationManager config) {
        this.config = config;
    }
    
    public void initialize() {
        this.tablePrefix = config.getString("database.tablePrefix", "ib_");
        
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                config.getString("database.host", "localhost"),
                config.getInt("database.port", 3306),
                config.getString("database.database", "inventory_bridge"),
                config.getBoolean("database.useSSL", false)));
        
        hikariConfig.setUsername(config.getString("database.username", "minecraft"));
        hikariConfig.setPassword(config.getString("database.password", "password"));
        hikariConfig.setMaximumPoolSize(config.getInt("database.maxPoolSize", 10));
        hikariConfig.setConnectionTimeout(config.getInt("database.connectionTimeout", 30000));
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // 連接池優化
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
        
        dataSource = new HikariDataSource(hikariConfig);
        
        createTables();
        LOGGER.info("資料庫連接初始化完成");
    }
    
    private void createTables() {
        createInventoriesTable();
        createVersionMappingsTable();
        createSyncLogTable();
    }
    
    private void createInventoriesTable() {
        String sql = String.format("""
            CREATE TABLE IF NOT EXISTS `%sinventories` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `player_uuid` VARCHAR(36) NOT NULL,
                `server_id` VARCHAR(64) NOT NULL,
                `inventory_data` LONGTEXT NOT NULL,
                `ender_chest_data` LONGTEXT,
                `experience` INT DEFAULT 0,
                `experience_level` INT DEFAULT 0,
                `health` FLOAT DEFAULT 20.0,
                `hunger` INT DEFAULT 20,
                `minecraft_version` VARCHAR(16) NOT NULL,
                `data_version` INT NOT NULL,
                `last_updated` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                UNIQUE KEY `unique_player_server` (`player_uuid`, `server_id`),
                INDEX `idx_player_uuid` (`player_uuid`),
                INDEX `idx_last_updated` (`last_updated`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """, tablePrefix);
        
        executeUpdate(sql, "背包資料表");
    }
    
    private void createVersionMappingsTable() {
        String sql = String.format("""
            CREATE TABLE IF NOT EXISTS `%sversion_mappings` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `from_version` VARCHAR(16) NOT NULL,
                `to_version` VARCHAR(16) NOT NULL,
                `item_id` VARCHAR(128) NOT NULL,
                `mapping_data` TEXT,
                `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY `unique_mapping` (`from_version`, `to_version`, `item_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """, tablePrefix);
        
        executeUpdate(sql, "版本映射表");
    }
    
    private void createSyncLogTable() {
        String sql = String.format("""
            CREATE TABLE IF NOT EXISTS `%ssync_log` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `player_uuid` VARCHAR(36) NOT NULL,
                `server_id` VARCHAR(64) NOT NULL,
                `sync_type` ENUM('JOIN', 'LEAVE', 'MANUAL', 'AUTO') NOT NULL,
                `status` ENUM('SUCCESS', 'FAILED', 'PARTIAL') NOT NULL,
                `error_message` TEXT,
                `sync_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                INDEX `idx_player_uuid` (`player_uuid`),
                INDEX `idx_sync_time` (`sync_time`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """, tablePrefix);
        
        executeUpdate(sql, "同步日誌表");
    }
    
    private void executeUpdate(String sql, String tableName) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
            LOGGER.info("{}創建成功", tableName);
        } catch (SQLException e) {
            LOGGER.error("創建{}失敗", tableName, e);
        }
    }
    
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public String getTablePrefix() {
        return tablePrefix;
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("資料庫連接關閉");
        }
    }
}
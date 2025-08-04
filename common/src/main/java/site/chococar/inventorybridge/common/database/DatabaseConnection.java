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
    private boolean standbyMode = false;
    private String lastConnectionError = null;
    
    public DatabaseConnection(ConfigurationManager config) {
        this.config = config;
    }
    
    public void initialize() {
        this.tablePrefix = config.getString("database.tablePrefix", "ib_");
        
        try {
            attemptConnection();
            LOGGER.info("資料庫連接初始化完成");
        } catch (Exception e) {
            this.standbyMode = true;
            this.lastConnectionError = e.getMessage();
            LOGGER.warn("╔══════════════════════════════════════════════════════════════════════════════════════╗");
            LOGGER.warn("║                             🚧 進入待機模式 🚧                                      ║");
            LOGGER.warn("║                                                                                      ║");
            LOGGER.warn("║  無法連接到資料庫，模組/插件將在待機模式下運行                                         ║");
            LOGGER.warn("║  在此模式下，背包同步功能將被暫停                                                      ║");
            LOGGER.warn("║                                                                                      ║");
            LOGGER.warn("║  錯誤原因: {}                                      ║", String.format("%-58s", e.getMessage()));
            LOGGER.warn("║                                                                                      ║");
            LOGGER.warn("║  請檢查資料庫設定並使用以下指令重新連線:                                                ║");
            LOGGER.warn("║  - /inventorybridge reload (重新載入設定)                                           ║");
            LOGGER.warn("║  - /inventorybridge reconnect (重新連接資料庫)                                      ║");
            LOGGER.warn("║                                                                                      ║");
            LOGGER.warn("╚══════════════════════════════════════════════════════════════════════════════════════╝");
        }
    }
    
    private void attemptConnection() throws Exception {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                config.getString("database.host", "localhost"),
                config.getInt("database.port", 3306),
                config.getString("database.database", "inventory_bridge"),
                config.getBoolean("database.useSSL", false)));
        
        hikariConfig.setUsername(config.getString("database.username", "minecraft"));
        hikariConfig.setPassword(config.getString("database.password", "password"));
        hikariConfig.setMaximumPoolSize(config.getInt("database.maxPoolSize", 10));
        hikariConfig.setConnectionTimeout(config.getInt("database.connectionTimeout", 10000)); // 縮短超時時間
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
        
        // 關閉舊連接（如果存在）
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        
        dataSource = new HikariDataSource(hikariConfig);
        
        // 測試連接
        try (Connection testConn = dataSource.getConnection()) {
            testConn.isValid(5);
        }
        
        createTables();
        this.standbyMode = false;
        this.lastConnectionError = null;
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
        if (standbyMode) {
            throw new SQLException("資料庫處於待機模式，無法獲取連接。請使用 /inventorybridge reconnect 重新連接。");
        }
        return dataSource.getConnection();
    }
    
    public String getTablePrefix() {
        return tablePrefix;
    }
    
    public boolean isStandbyMode() {
        return standbyMode;
    }
    
    public String getLastConnectionError() {
        return lastConnectionError;
    }
    
    public boolean reconnect() {
        LOGGER.info("嘗試重新連接資料庫...");
        
        try {
            attemptConnection();
            LOGGER.info("╔══════════════════════════════════════════════════════════════════════════════════════╗");
            LOGGER.info("║                             ✅ 資料庫重新連接成功 ✅                                  ║");
            LOGGER.info("║                                                                                      ║");
            LOGGER.info("║  背包同步功能已恢復正常運作                                                           ║");
            LOGGER.info("║  所有待處理的同步操作將會被執行                                                        ║");
            LOGGER.info("║                                                                                      ║");
            LOGGER.info("╚══════════════════════════════════════════════════════════════════════════════════════╝");
            return true;
        } catch (Exception e) {
            this.standbyMode = true;
            this.lastConnectionError = e.getMessage();
            LOGGER.error("╔══════════════════════════════════════════════════════════════════════════════════════╗");
            LOGGER.error("║                             ❌ 資料庫重新連接失敗 ❌                                  ║");
            LOGGER.error("║                                                                                      ║");
            LOGGER.error("║  錯誤原因: {}                                      ║", String.format("%-58s", e.getMessage()));
            LOGGER.error("║                                                                                      ║");
            LOGGER.error("║  請檢查以下項目:                                                                     ║");
            LOGGER.error("║  1. 資料庫伺服器是否正在運行                                                          ║");
            LOGGER.error("║  2. 網路連接是否正常                                                                 ║");
            LOGGER.error("║  3. 資料庫設定是否正確                                                               ║");
            LOGGER.error("║  4. 使用者權限是否足夠                                                               ║");
            LOGGER.error("║                                                                                      ║");
            LOGGER.error("╚══════════════════════════════════════════════════════════════════════════════════════╝");
            return false;
        }
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("資料庫連接關閉");
        }
    }
}
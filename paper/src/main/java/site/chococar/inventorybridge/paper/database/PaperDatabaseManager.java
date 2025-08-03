package site.chococar.inventorybridge.paper.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import site.chococar.inventorybridge.paper.config.PaperConfigManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

public class PaperDatabaseManager {
    private final PaperConfigManager configManager;
    private final Logger logger;
    private HikariDataSource dataSource;
    private String tablePrefix;
    
    public PaperDatabaseManager(PaperConfigManager configManager) {
        this.configManager = configManager;
        this.logger = Logger.getLogger("ChococarsInventoryBridge");
    }
    
    public void initialize() {
        this.tablePrefix = configManager.getTablePrefix();
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                configManager.getDatabaseHost(), 
                configManager.getDatabasePort(), 
                configManager.getDatabaseName(), 
                configManager.useSSL()));
        config.setUsername(configManager.getDatabaseUsername());
        config.setPassword(configManager.getDatabasePassword());
        config.setMaximumPoolSize(configManager.getMaxPoolSize());
        config.setConnectionTimeout(configManager.getConnectionTimeout());
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        dataSource = new HikariDataSource(config);
        
        createTables();
        logger.info("Paper database connection initialized");
    }
    
    private void createTables() {
        String inventoryTable = String.format("""
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
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(inventoryTable)) {
            stmt.executeUpdate();
            logger.info("Paper database tables created successfully");
        } catch (SQLException e) {
            logger.severe("Failed to create database tables: " + e.getMessage());
        }
    }
    
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public void saveInventory(UUID playerUuid, String serverId, String inventoryData, 
                             String enderChestData, int experience, int experienceLevel,
                             double health, int hunger, String minecraftVersion, int dataVersion) {
        String sql = String.format("""
            INSERT INTO `%sinventories` 
            (`player_uuid`, `server_id`, `inventory_data`, `ender_chest_data`, 
             `experience`, `experience_level`, `health`, `hunger`, `minecraft_version`, `data_version`)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
            `inventory_data` = VALUES(`inventory_data`),
            `ender_chest_data` = VALUES(`ender_chest_data`),
            `experience` = VALUES(`experience`),
            `experience_level` = VALUES(`experience_level`),
            `health` = VALUES(`health`),
            `hunger` = VALUES(`hunger`),
            `minecraft_version` = VALUES(`minecraft_version`),
            `data_version` = VALUES(`data_version`)
            """, tablePrefix);
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, serverId);
            stmt.setString(3, inventoryData);
            stmt.setString(4, enderChestData);
            stmt.setInt(5, experience);
            stmt.setInt(6, experienceLevel);
            stmt.setDouble(7, health);
            stmt.setInt(8, hunger);
            stmt.setString(9, minecraftVersion);
            stmt.setInt(10, dataVersion);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Failed to save inventory for player " + playerUuid + ": " + e.getMessage());
        }
    }
    
    public PaperInventoryData loadInventory(UUID playerUuid, String serverId) {
        String sql = String.format("""
            SELECT `inventory_data`, `ender_chest_data`, `experience`, `experience_level`,
                   `health`, `hunger`, `minecraft_version`, `data_version`, `last_updated`
            FROM `%sinventories`
            WHERE `player_uuid` = ? AND `server_id` = ?
            """, tablePrefix);
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, serverId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new PaperInventoryData(
                        rs.getString("inventory_data"),
                        rs.getString("ender_chest_data"),
                        rs.getInt("experience"),
                        rs.getInt("experience_level"),
                        rs.getDouble("health"),
                        rs.getInt("hunger"),
                        rs.getString("minecraft_version"),
                        rs.getInt("data_version"),
                        rs.getTimestamp("last_updated")
                    );
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to load inventory for player " + playerUuid + ": " + e.getMessage());
        }
        
        return null;
    }
    
    public void logSync(UUID playerUuid, String serverId, String syncType, String status, String errorMessage) {
        String sql = String.format("""
            INSERT INTO `%ssync_log` (`player_uuid`, `server_id`, `sync_type`, `status`, `error_message`)
            VALUES (?, ?, ?, ?, ?)
            """, tablePrefix);
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, serverId);
            stmt.setString(3, syncType);
            stmt.setString(4, status);
            stmt.setString(5, errorMessage);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.warning("Failed to log sync for player " + playerUuid + ": " + e.getMessage());
        }
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Paper database connection closed");
        }
    }
}
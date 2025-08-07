package site.chococar.inventorybridge.paper.database;

import site.chococar.inventorybridge.common.database.CommonDatabaseManager;
import site.chococar.inventorybridge.common.database.DatabaseConnection;
import site.chococar.inventorybridge.common.database.InventoryDataRecord;
import site.chococar.inventorybridge.paper.config.PaperConfigManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

public class PaperDatabaseManager implements CommonDatabaseManager {
    private final DatabaseConnection databaseConnection;
    private final Logger logger;
    
    public PaperDatabaseManager(PaperConfigManager configManager) {
        this.logger = Logger.getLogger("ChococarsInventoryBridge");
        // 直接使用Paper配置管理器的Common ConfigurationManager
        this.databaseConnection = new DatabaseConnection(configManager.getConfigurationManager());
    }
    
    @Override
    public void initialize() {
        databaseConnection.initialize();
        logger.info("Paper database initialized through Common module");
    }
    
    public Connection getConnection() throws SQLException {
        return databaseConnection.getConnection();
    }
    
    public String getTablePrefix() {
        return databaseConnection.getTablePrefix();
    }
    
    @Override
    public boolean isStandbyMode() {
        return databaseConnection.isStandbyMode();
    }
    
    @Override
    public String getLastConnectionError() {
        return databaseConnection.getLastConnectionError();
    }
    
    @Override
    public boolean reconnect() {
        boolean success = databaseConnection.reconnect();
        if (success) {
            logger.info("✅ 資料庫重新連接成功");
        } else {
            logger.severe("❌ 資料庫重新連接失敗");
        }
        return success;
    }
    
    @Override
    public void close() {
        databaseConnection.close();
        logger.info("Paper database connection closed");
    }
    
    // Paper特有的便利方法
    @Override
    public void saveInventory(UUID playerUuid, String serverId, String inventoryData, 
                            String enderChestData, int experience, int experienceLevel, 
                            double health, int hunger, String minecraftVersion, int dataVersion) {
        String sql = String.format("""
            INSERT INTO `%sinventories` (`player_uuid`, `server_id`, `inventory_data`, `ender_chest_data`, 
                                       `experience`, `experience_level`, `health`, `hunger`, 
                                       `minecraft_version`, `data_version`)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                `inventory_data` = VALUES(`inventory_data`),
                `ender_chest_data` = VALUES(`ender_chest_data`),
                `experience` = VALUES(`experience`),
                `experience_level` = VALUES(`experience_level`),
                `health` = VALUES(`health`),
                `hunger` = VALUES(`hunger`),
                `minecraft_version` = VALUES(`minecraft_version`),
                `data_version` = VALUES(`data_version`),
                `last_updated` = CURRENT_TIMESTAMP
            """, getTablePrefix());
        
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
            logger.severe("保存背包資料失敗: " + e.getMessage());
        }
    }
    
    @Override
    public InventoryDataRecord loadInventory(UUID playerUuid, String serverId) {
        // 首先嘗試從當前伺服器載入
        String currentServerSql = String.format("""
            SELECT `inventory_data`, `ender_chest_data`, `experience`, `experience_level`, 
                   `health`, `hunger`, `minecraft_version`, `data_version`, `last_updated`
            FROM `%sinventories`
            WHERE `player_uuid` = ? AND `server_id` = ?
            """, getTablePrefix());
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(currentServerSql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, serverId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    logger.info("從當前伺服器 " + serverId + " 載入玩家 " + playerUuid + " 的資料");
                    return new InventoryDataRecord(
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
            logger.severe("從當前伺服器載入背包資料失敗: " + e.getMessage());
        }
        
        // 如果當前伺服器沒有資料，嘗試從其他伺服器載入最新資料
        String crossServerSql = String.format("""
            SELECT `inventory_data`, `ender_chest_data`, `experience`, `experience_level`, 
                   `health`, `hunger`, `minecraft_version`, `data_version`, `last_updated`, `server_id`
            FROM `%sinventories`
            WHERE `player_uuid` = ?
            ORDER BY `last_updated` DESC
            LIMIT 1
            """, getTablePrefix());
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(crossServerSql)) {
            stmt.setString(1, playerUuid.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String sourceServerId = rs.getString("server_id");
                    logger.info("從其他伺服器 " + sourceServerId + " 載入玩家 " + playerUuid + " 的資料至 " + serverId);
                    return new InventoryDataRecord(
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
            logger.severe("從其他伺服器載入背包資料失敗: " + e.getMessage());
        }
        
        return null;
    }
    
    @Override
    public boolean hasInventory(UUID playerUuid, String serverId) {
        String sql = String.format("""
            SELECT 1 FROM `%sinventories` 
            WHERE `player_uuid` = ? AND `server_id` = ? 
            LIMIT 1
            """, getTablePrefix());
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, serverId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.warning("檢查背包資料失敗: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public void logSync(UUID playerUuid, String serverId, String syncType, String status, String errorMessage) {
        String sql = String.format("""
            INSERT INTO `%ssync_log` (`player_uuid`, `server_id`, `sync_type`, `status`, `error_message`)
            VALUES (?, ?, ?, ?, ?)
            """, getTablePrefix());
        
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
    
}
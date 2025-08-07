package site.chococar.inventorybridge.fabric.database;

import site.chococar.inventorybridge.common.database.CommonDatabaseManager;
import site.chococar.inventorybridge.common.database.DatabaseConnection;
import site.chococar.inventorybridge.common.database.InventoryDataRecord;
import site.chococar.inventorybridge.fabric.config.FabricConfigManager;
import site.chococar.inventorybridge.fabric.util.FabricLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class FabricDatabaseManager implements CommonDatabaseManager {
    private static final FabricLogger LOGGER = new FabricLogger("FabricDatabaseManager");
    private final DatabaseConnection databaseConnection;
    
    public FabricDatabaseManager(FabricConfigManager configManager) {
        this.databaseConnection = new DatabaseConnection(configManager.getConfigurationManager());
    }
    
    @Override
    public void initialize() {
        databaseConnection.initialize();
    }
    
    @Override
    public void close() {
        databaseConnection.close();
    }
    
    public Connection getConnection() throws SQLException {
        return databaseConnection.getConnection();
    }
    
    @Override
    public boolean isStandbyMode() {
        return databaseConnection.isStandbyMode();
    }
    
    public String getTablePrefix() {
        return databaseConnection.getTablePrefix();
    }
    
    @Override
    public boolean hasInventory(UUID playerUuid, String serverId) {
        String sql = String.format("""
            SELECT COUNT(*) FROM `%sinventories`
            WHERE `player_uuid` = ? AND `server_id` = ?
            """, getTablePrefix());
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, serverId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.error("檢查玩家資料庫記錄失敗 - 玩家: " + playerUuid, e);
        }
        
        return false;
    }
    
    @Override
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
            LOGGER.error("保存背包到資料庫失敗 - 玩家: " + playerUuid, e);
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
                    LOGGER.info("從當前伺服器 " + serverId + " 載入玩家 " + playerUuid + " 的資料");
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
            LOGGER.error("從當前伺服器載入背包失敗 - 玩家: " + playerUuid, e);
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
                    LOGGER.info("從其他伺服器 " + sourceServerId + " 載入玩家 " + playerUuid + " 的資料至 " + serverId);
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
            LOGGER.error("從其他伺服器載入背包失敗 - 玩家: " + playerUuid, e);
        }
        
        return null;
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
            LOGGER.error("記錄同步日誌失敗 - 玩家: " + playerUuid, e);
        }
    }
    
    @Override
    public boolean reconnect() {
        LOGGER.info("管理員請求重新連接資料庫");
        boolean success = databaseConnection.reconnect();
        
        if (success) {
            LOGGER.info("✅ 資料庫重新連接成功");
            LOGGER.info("資料庫連接已恢復");
            LOGGER.info("所有同步功能恢復正常運作");
        } else {
            LOGGER.error("❌ 資料庫重新連接失敗");
            LOGGER.error("錯誤原因: {}", databaseConnection.getLastConnectionError());
            LOGGER.error("插件將繼續以待機模式運行");
            LOGGER.error("請檢查資料庫設定和連接狀態");
        }
        
        return success;
    }
    
    @Override
    public String getLastConnectionError() {
        return databaseConnection.getLastConnectionError();
    }
    
    // Internal access to DatabaseConnection for sync manager
    public DatabaseConnection getDatabaseConnection() {
        return databaseConnection;
    }
}
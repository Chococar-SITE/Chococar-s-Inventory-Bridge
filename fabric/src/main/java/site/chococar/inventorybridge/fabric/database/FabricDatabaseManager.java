package site.chococar.inventorybridge.fabric.database;

import site.chococar.inventorybridge.common.database.DatabaseConnection;
import site.chococar.inventorybridge.fabric.config.FabricConfigManager;
import site.chococar.inventorybridge.fabric.util.FabricLogger;

import java.sql.Connection;
import java.sql.SQLException;

public class FabricDatabaseManager {
    private static final FabricLogger LOGGER = new FabricLogger("FabricDatabaseManager");
    private final DatabaseConnection databaseConnection;
    private final FabricConfigManager configManager;
    
    public FabricDatabaseManager(FabricConfigManager configManager) {
        this.configManager = configManager;
        this.databaseConnection = new DatabaseConnection(configManager.getConfigurationManager());
    }
    
    public void initialize() {
        databaseConnection.initialize();
    }
    
    public void close() {
        databaseConnection.close();
    }
    
    public Connection getConnection() throws SQLException {
        return databaseConnection.getConnection();
    }
    
    public boolean isStandbyMode() {
        return databaseConnection.isStandbyMode();
    }
    
    public boolean hasInventory(java.util.UUID playerUuid, String serverId) {
        // Since DatabaseConnection doesn't have this method, we need to implement it here
        // For now, we'll delegate this responsibility to the sync manager
        // This is a placeholder method to maintain compatibility with Paper
        return false;
    }
    
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
    
    public String getLastConnectionError() {
        return databaseConnection.getLastConnectionError();
    }
    
    // Internal access to DatabaseConnection for sync manager
    public DatabaseConnection getDatabaseConnection() {
        return databaseConnection;
    }
}
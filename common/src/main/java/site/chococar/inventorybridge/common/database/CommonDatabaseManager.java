package site.chococar.inventorybridge.common.database;

import java.util.UUID;

/**
 * 通用資料庫管理器介面
 * 定義平台無關的資料庫操作
 */
public interface CommonDatabaseManager {
    
    /**
     * 初始化資料庫連接
     */
    void initialize();
    
    /**
     * 關閉資料庫連接
     */
    void close();
    
    /**
     * 檢查是否為待機模式
     */
    boolean isStandbyMode();
    
    /**
     * 重新連接資料庫
     */
    boolean reconnect();
    
    /**
     * 獲取最後連接錯誤
     */
    String getLastConnectionError();
    
    /**
     * 檢查玩家是否有背包資料
     */
    boolean hasInventory(UUID playerUuid, String serverId);
    
    /**
     * 保存背包資料
     */
    void saveInventory(UUID playerUuid, String serverId, String inventoryData, 
                      String enderChestData, int experience, int experienceLevel,
                      double health, int hunger, String minecraftVersion, int dataVersion);
    
    /**
     * 載入背包資料
     */
    InventoryDataRecord loadInventory(UUID playerUuid, String serverId);
    
    /**
     * 記錄同步日誌
     */
    void logSync(UUID playerUuid, String serverId, String syncType, String status, String errorMessage);
}
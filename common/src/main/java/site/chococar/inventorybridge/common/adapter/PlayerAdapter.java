package site.chococar.inventorybridge.common.adapter;

import java.util.UUID;

/**
 * 通用玩家適配器介面
 * 抽象化不同平台的玩家操作
 */
public interface PlayerAdapter {
    /**
     * 獲取玩家UUID
     */
    UUID getUniqueId();
    
    /**
     * 獲取玩家名稱
     */
    String getName();
    
    /**
     * 獲取主背包適配器
     */
    InventoryAdapter getInventory();
    
    /**
     * 獲取終界箱適配器
     */
    InventoryAdapter getEnderChest();
    
    /**
     * 獲取總經驗值
     */
    int getTotalExperience();
    
    /**
     * 獲取等級
     */
    int getLevel();
    
    /**
     * 獲取生命值
     */
    double getHealth();
    
    /**
     * 獲取飢餓值
     */
    int getFoodLevel();
    
    /**
     * 設置生命值
     */
    void setHealth(double health);
    
    /**
     * 設置飢餓值
     */
    void setFoodLevel(int foodLevel);
    
    /**
     * 設置經驗
     */
    void setExperience(int totalExperience, int level);
    
    /**
     * 更新背包顯示
     */
    void updateInventory();
}
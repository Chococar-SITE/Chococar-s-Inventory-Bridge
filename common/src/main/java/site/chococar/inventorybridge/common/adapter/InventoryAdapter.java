package site.chococar.inventorybridge.common.adapter;

/**
 * 通用背包適配器介面
 * 抽象化不同平台的背包操作
 */
public interface InventoryAdapter {
    /**
     * 獲取背包大小
     */
    int size();
    
    /**
     * 清空背包
     */
    void clear();
    
    /**
     * 序列化背包內容
     */
    String serialize();
    
    /**
     * 反序列化背包內容
     */
    void deserialize(String data);
}
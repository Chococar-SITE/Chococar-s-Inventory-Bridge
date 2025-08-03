package site.chococar.inventorybridge.common.database;

import java.sql.Timestamp;

/**
 * 背包數據記錄
 */
public record InventoryDataRecord(
    String inventoryData,
    String enderChestData,
    int experience,
    int experienceLevel,
    double health,
    int hunger,
    String minecraftVersion,
    int dataVersion,
    Timestamp lastUpdated
) {}
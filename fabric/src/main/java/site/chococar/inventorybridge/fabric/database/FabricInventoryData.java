package site.chococar.inventorybridge.fabric.database;

import java.sql.Timestamp;

public record FabricInventoryData(
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
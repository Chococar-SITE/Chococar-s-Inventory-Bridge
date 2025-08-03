package site.chococar.inventorybridge.paper.database;

import java.sql.Timestamp;

public record PaperInventoryData(
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
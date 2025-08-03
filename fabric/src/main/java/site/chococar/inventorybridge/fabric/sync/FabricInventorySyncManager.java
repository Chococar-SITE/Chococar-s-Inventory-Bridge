package site.chococar.inventorybridge.fabric.sync;

import net.minecraft.server.network.ServerPlayerEntity;
import site.chococar.inventorybridge.common.config.ConfigurationManager;
import site.chococar.inventorybridge.common.database.DatabaseConnection;
import site.chococar.inventorybridge.common.database.InventoryDataRecord;
import site.chococar.inventorybridge.fabric.ChococarsInventoryBridgeFabric;
import site.chococar.inventorybridge.fabric.serialization.FabricItemSerializer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class FabricInventorySyncManager {
    private final DatabaseConnection databaseConnection;
    private final ConfigurationManager config;
    private final Map<UUID, Long> lastSyncTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> syncInProgress = new ConcurrentHashMap<>();
    
    public FabricInventorySyncManager(DatabaseConnection databaseConnection, ConfigurationManager config) {
        this.databaseConnection = databaseConnection;
        this.config = config;
    }
    
    public void onPlayerJoin(ServerPlayerEntity player) {
        if (!config.getBoolean("sync.syncOnJoin", true)) {
            return;
        }
        
        UUID playerUuid = player.getUuid();
        
        if (syncInProgress.getOrDefault(playerUuid, false)) {
            return;
        }
        
        syncInProgress.put(playerUuid, true);
        
        CompletableFuture.runAsync(() -> {
            try {
                loadPlayerInventory(player);
                logSync(playerUuid, "JOIN", "SUCCESS", null);
                ChococarsInventoryBridgeFabric.getLogger().info(String.format("成功載入玩家 %s 的背包", player.getName().getString()));
            } catch (Exception e) {
                logSync(playerUuid, "JOIN", "FAILED", e.getMessage());
                ChococarsInventoryBridgeFabric.getLogger().error(String.format("載入玩家 %s 的背包失敗", player.getName().getString()), e);
            } finally {
                syncInProgress.put(playerUuid, false);
                lastSyncTimes.put(playerUuid, System.currentTimeMillis());
            }
        });
    }
    
    public void onPlayerLeave(ServerPlayerEntity player) {
        if (!config.getBoolean("sync.syncOnLeave", true)) {
            return;
        }
        
        UUID playerUuid = player.getUuid();
        
        if (syncInProgress.getOrDefault(playerUuid, false)) {
            return;
        }
        
        syncInProgress.put(playerUuid, true);
        
        CompletableFuture.runAsync(() -> {
            try {
                savePlayerInventory(player);
                logSync(playerUuid, "LEAVE", "SUCCESS", null);
                ChococarsInventoryBridgeFabric.getLogger().info(String.format("成功保存玩家 %s 的背包", player.getName().getString()));
            } catch (Exception e) {
                logSync(playerUuid, "LEAVE", "FAILED", e.getMessage());
                ChococarsInventoryBridgeFabric.getLogger().error(String.format("保存玩家 %s 的背包失敗", player.getName().getString()), e);
            } finally {
                syncInProgress.remove(playerUuid);
                lastSyncTimes.put(playerUuid, System.currentTimeMillis());
            }
        });
    }
    
    public void manualSync(ServerPlayerEntity player, boolean save) {
        UUID playerUuid = player.getUuid();
        
        if (syncInProgress.getOrDefault(playerUuid, false)) {
            ChococarsInventoryBridgeFabric.getLogger().warn(String.format("玩家 %s 的同步正在進行中", player.getName().getString()));
            return;
        }
        
        syncInProgress.put(playerUuid, true);
        
        CompletableFuture.runAsync(() -> {
            try {
                if (save) {
                    savePlayerInventory(player);
                } else {
                    loadPlayerInventory(player);
                }
                logSync(playerUuid, "MANUAL", "SUCCESS", null);
                ChococarsInventoryBridgeFabric.getLogger().info(String.format("手動%s完成 - 玩家: %s", 
                        save ? "保存" : "載入", player.getName().getString()));
            } catch (Exception e) {
                logSync(playerUuid, "MANUAL", "FAILED", e.getMessage());
                ChococarsInventoryBridgeFabric.getLogger().error(String.format("手動%s失敗 - 玩家: %s", 
                        save ? "保存" : "載入", player.getName().getString()), e);
            } finally {
                syncInProgress.put(playerUuid, false);
                lastSyncTimes.put(playerUuid, System.currentTimeMillis());
            }
        });
    }
    
    private void savePlayerInventory(ServerPlayerEntity player) {
        String serverId = config.getString("sync.serverId", "server1");
        
        // 序列化主背包
        String inventoryData = FabricItemSerializer.serializeInventory(player.getInventory());
        
        // 序列化終界箱（如果啟用）
        String enderChestData = null;
        if (config.getBoolean("sync.syncEnderChest", true)) {
            enderChestData = FabricItemSerializer.serializeInventory(player.getEnderChestInventory());
        }
        
        // 獲取經驗數據
        int experience = config.getBoolean("sync.syncExperience", true) ? player.totalExperience : 0;
        int experienceLevel = config.getBoolean("sync.syncExperience", true) ? player.experienceLevel : 0;
        
        // 獲取生命值和飢餓值數據
        float health = config.getBoolean("sync.syncHealth", false) ? player.getHealth() : 20.0f;
        int hunger = config.getBoolean("sync.syncHunger", false) ? player.getHungerManager().getFoodLevel() : 20;
        
        saveInventoryToDatabase(
                player.getUuid(),
                serverId,
                inventoryData,
                enderChestData,
                experience,
                experienceLevel,
                health,
                hunger,
                config.getString("compatibility.minecraftVersion", "1.21.8"),
                4082 // 1.21.8 的數據版本
        );
    }
    
    private void loadPlayerInventory(ServerPlayerEntity player) {
        String serverId = config.getString("sync.serverId", "server1");
        
        InventoryDataRecord data = loadInventoryFromDatabase(player.getUuid(), serverId);
        
        if (data == null) {
            ChococarsInventoryBridgeFabric.getLogger().info(String.format("未找到玩家 %s 的保存背包", player.getName().getString()));
            return;
        }
        
        // 載入主背包
        player.getInventory().clear();
        FabricItemSerializer.deserializeInventory(data.inventoryData(), player.getInventory());
        
        // 載入終界箱（如果啟用且數據存在）
        if (config.getBoolean("sync.syncEnderChest", true) && data.enderChestData() != null) {
            player.getEnderChestInventory().clear();
            FabricItemSerializer.deserializeInventory(data.enderChestData(), player.getEnderChestInventory());
        }
        
        // 載入經驗（如果啟用）
        if (config.getBoolean("sync.syncExperience", true)) {
            player.setExperienceLevel(data.experienceLevel());
            player.setExperiencePoints(data.experience());
        }
        
        // 載入生命值（如果啟用）
        if (config.getBoolean("sync.syncHealth", false)) {
            player.setHealth((float) data.health());
        }
        
        // 載入飢餓值（如果啟用）
        if (config.getBoolean("sync.syncHunger", false)) {
            player.getHungerManager().setFoodLevel(data.hunger());
        }
        
        // 同步到客戶端
        player.playerScreenHandler.syncState();
    }
    
    private void saveInventoryToDatabase(UUID playerUuid, String serverId, String inventoryData, 
                                       String enderChestData, int experience, int experienceLevel,
                                       float health, int hunger, String minecraftVersion, int dataVersion) {
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
            """, databaseConnection.getTablePrefix());
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, serverId);
            stmt.setString(3, inventoryData);
            stmt.setString(4, enderChestData);
            stmt.setInt(5, experience);
            stmt.setInt(6, experienceLevel);
            stmt.setFloat(7, health);
            stmt.setInt(8, hunger);
            stmt.setString(9, minecraftVersion);
            stmt.setInt(10, dataVersion);
            stmt.executeUpdate();
        } catch (SQLException e) {
            ChococarsInventoryBridgeFabric.getLogger().error("保存背包到資料庫失敗 - 玩家: " + playerUuid, e);
        }
    }
    
    private InventoryDataRecord loadInventoryFromDatabase(UUID playerUuid, String serverId) {
        String sql = String.format("""
            SELECT `inventory_data`, `ender_chest_data`, `experience`, `experience_level`,
                   `health`, `hunger`, `minecraft_version`, `data_version`, `last_updated`
            FROM `%sinventories`
            WHERE `player_uuid` = ? AND `server_id` = ?
            """, databaseConnection.getTablePrefix());
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, serverId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
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
            ChococarsInventoryBridgeFabric.getLogger().error("從資料庫載入背包失敗 - 玩家: " + playerUuid, e);
        }
        
        return null;
    }
    
    private void logSync(UUID playerUuid, String syncType, String status, String errorMessage) {
        String serverId = config.getString("sync.serverId", "server1");
        String sql = String.format("""
            INSERT INTO `%ssync_log` (`player_uuid`, `server_id`, `sync_type`, `status`, `error_message`)
            VALUES (?, ?, ?, ?, ?)
            """, databaseConnection.getTablePrefix());
        
        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, serverId);
            stmt.setString(3, syncType);
            stmt.setString(4, status);
            stmt.setString(5, errorMessage);
            stmt.executeUpdate();
        } catch (SQLException e) {
            ChococarsInventoryBridgeFabric.getLogger().error("記錄同步日誌失敗 - 玩家: " + playerUuid, e);
        }
    }
    
    public boolean isSyncInProgress(UUID playerUuid) {
        return syncInProgress.getOrDefault(playerUuid, false);
    }
    
    public long getLastSyncTime(UUID playerUuid) {
        return lastSyncTimes.getOrDefault(playerUuid, 0L);
    }
}
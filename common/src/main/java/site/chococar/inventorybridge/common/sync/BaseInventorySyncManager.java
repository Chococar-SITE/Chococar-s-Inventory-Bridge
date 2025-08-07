package site.chococar.inventorybridge.common.sync;

import site.chococar.inventorybridge.common.adapter.PlayerAdapter;
import site.chococar.inventorybridge.common.config.ConfigurationManager;
import site.chococar.inventorybridge.common.database.CommonDatabaseManager;
import site.chococar.inventorybridge.common.database.InventoryDataRecord;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用背包同步管理器基類
 * 包含平台無關的同步邏輯
 */
public abstract class BaseInventorySyncManager<T extends PlayerAdapter> {
    protected final CommonDatabaseManager databaseManager;
    protected final ConfigurationManager config;
    protected final Map<UUID, Long> lastSyncTimes = new ConcurrentHashMap<>();
    protected final Map<UUID, Boolean> syncInProgress = new ConcurrentHashMap<>();
    protected volatile boolean hasScannedPlayerFiles = false;
    
    public BaseInventorySyncManager(CommonDatabaseManager databaseManager, ConfigurationManager config) {
        this.databaseManager = databaseManager;
        this.config = config;
    }
    
    /**
     * 玩家加入時的同步邏輯
     */
    public void onPlayerJoin(T player) {
        if (!config.getBoolean("sync.syncOnJoin", true)) {
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        
        if (syncInProgress.getOrDefault(playerUuid, false)) {
            return;
        }
        
        syncInProgress.put(playerUuid, true);
        
        CompletableFuture.runAsync(() -> {
            try {
                loadPlayerInventory(player);
                databaseManager.logSync(playerUuid, getServerId(), "JOIN", "SUCCESS", null);
                getLogger().info(String.format("成功載入玩家 %s 的背包", player.getName()));
            } catch (Exception e) {
                databaseManager.logSync(playerUuid, getServerId(), "JOIN", "FAILED", e.getMessage());
                getLogger().severe(String.format("載入玩家 %s 的背包失敗", player.getName()));
                logError("載入玩家背包失敗", e);
            } finally {
                syncInProgress.put(playerUuid, false);
                lastSyncTimes.put(playerUuid, System.currentTimeMillis());
            }
        });
    }
    
    /**
     * 玩家離開時的同步邏輯
     */
    public void onPlayerLeave(T player) {
        if (!config.getBoolean("sync.syncOnLeave", true)) {
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        
        if (syncInProgress.getOrDefault(playerUuid, false)) {
            return;
        }
        
        syncInProgress.put(playerUuid, true);
        
        CompletableFuture.runAsync(() -> {
            try {
                savePlayerInventory(player);
                databaseManager.logSync(playerUuid, getServerId(), "LEAVE", "SUCCESS", null);
                getLogger().info(String.format("成功保存玩家 %s 的背包", player.getName()));
            } catch (Exception e) {
                databaseManager.logSync(playerUuid, getServerId(), "LEAVE", "FAILED", e.getMessage());
                getLogger().severe(String.format("保存玩家 %s 的背包失敗", player.getName()));
                logError("保存玩家背包失敗", e);
            } finally {
                syncInProgress.remove(playerUuid);
                lastSyncTimes.put(playerUuid, System.currentTimeMillis());
            }
        });
    }
    
    /**
     * 手動同步邏輯
     */
    public void manualSync(T player, boolean save) {
        UUID playerUuid = player.getUniqueId();
        
        if (syncInProgress.getOrDefault(playerUuid, false)) {
            getLogger().warning(String.format("玩家 %s 的同步正在進行中", player.getName()));
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
                databaseManager.logSync(playerUuid, getServerId(), "MANUAL", "SUCCESS", null);
                getLogger().info(String.format("手動%s完成 - 玩家: %s", 
                        save ? "保存" : "載入", player.getName()));
            } catch (Exception e) {
                databaseManager.logSync(playerUuid, getServerId(), "MANUAL", "FAILED", e.getMessage());
                getLogger().severe(String.format("手動%s失敗 - 玩家: %s", 
                        save ? "保存" : "載入", player.getName()));
                logError("手動同步失敗", e);
            } finally {
                syncInProgress.put(playerUuid, false);
                lastSyncTimes.put(playerUuid, System.currentTimeMillis());
            }
        });
    }
    
    /**
     * 保存玩家背包
     */
    protected void savePlayerInventory(T player) {
        String serverId = getServerId();
        
        // 序列化主背包
        String inventoryData = player.getInventory().serialize();
        
        // 序列化終界箱（如果啟用）
        String enderChestData = null;
        if (config.getBoolean("sync.syncEnderChest", true)) {
            enderChestData = player.getEnderChest().serialize();
        }
        
        // 獲取經驗數據
        int experience = config.getBoolean("sync.syncExperience", true) ? player.getTotalExperience() : 0;
        int experienceLevel = config.getBoolean("sync.syncExperience", true) ? player.getLevel() : 0;
        
        // 獲取生命值和飢餓值數據
        double health = config.getBoolean("sync.syncHealth", false) ? player.getHealth() : 20.0;
        int hunger = config.getBoolean("sync.syncHunger", false) ? player.getFoodLevel() : 20;
        
        databaseManager.saveInventory(
                player.getUniqueId(),
                serverId,
                inventoryData,
                enderChestData,
                experience,
                experienceLevel,
                health,
                hunger,
                getCurrentVersion(),
                getCurrentDataVersion()
        );
    }
    
    /**
     * 載入玩家背包
     */
    protected void loadPlayerInventory(T player) {
        String serverId = getServerId();
        
        InventoryDataRecord data = databaseManager.loadInventory(player.getUniqueId(), serverId);
        
        if (data == null) {
            getLogger().info(String.format("未找到玩家 %s 的保存背包", player.getName()));
            return;
        }
        
        // 載入主背包
        player.getInventory().clear();
        player.getInventory().deserialize(data.inventoryData());
        
        // 載入終界箱（如果啟用且數據存在）
        if (config.getBoolean("sync.syncEnderChest", true) && data.enderChestData() != null) {
            player.getEnderChest().clear();
            player.getEnderChest().deserialize(data.enderChestData());
        }
        
        // 載入經驗（如果啟用）
        if (config.getBoolean("sync.syncExperience", true)) {
            player.setExperience(data.experience(), data.experienceLevel());
        }
        
        // 載入生命值（如果啟用）
        if (config.getBoolean("sync.syncHealth", false)) {
            player.setHealth(data.health());
        }
        
        // 載入飢餓值（如果啟用）
        if (config.getBoolean("sync.syncHunger", false)) {
            player.setFoodLevel(data.hunger());
        }
        
        // 同步到客戶端
        player.updateInventory();
    }
    
    /**
     * 檢查同步是否正在進行
     */
    public boolean isSyncInProgress(UUID playerUuid) {
        return syncInProgress.getOrDefault(playerUuid, false);
    }
    
    /**
     * 獲取最後同步時間
     */
    public long getLastSyncTime(UUID playerUuid) {
        return lastSyncTimes.getOrDefault(playerUuid, 0L);
    }
    
    // 抽象方法，由子類實現
    protected abstract String getServerId();
    protected abstract String getCurrentVersion();
    protected abstract int getCurrentDataVersion();
    protected abstract Logger getLogger();
    protected abstract void logError(String message, Exception e);
    
    /**
     * 通用的日誌介面，避免平台依賴
     */
    public interface Logger {
        void info(String message);
        void warning(String message);
        void severe(String message);
    }
}
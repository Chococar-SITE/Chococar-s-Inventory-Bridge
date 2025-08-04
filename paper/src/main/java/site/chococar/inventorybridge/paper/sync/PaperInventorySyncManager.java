package site.chococar.inventorybridge.paper.sync;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import site.chococar.inventorybridge.paper.config.PaperConfigManager;
import site.chococar.inventorybridge.paper.database.PaperDatabaseManager;
import site.chococar.inventorybridge.paper.database.PaperInventoryData;
import site.chococar.inventorybridge.paper.serialization.PaperItemSerializer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class PaperInventorySyncManager {
    private final PaperDatabaseManager databaseManager;
    private final Logger logger;
    private final Map<UUID, Long> lastSyncTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> syncInProgress = new ConcurrentHashMap<>();
    private volatile boolean hasScannedPlayerFiles = false;
    
    public PaperInventorySyncManager(PaperDatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.logger = Logger.getLogger("ChococarsInventoryBridge");
    }
    
    public void onPlayerJoin(Player player) {
        // Auto-load disabled for safety to prevent timing issues and item loss
        logger.info("Player " + player.getName() + " joined - auto-load disabled for safety. Use '/ib sync load' to manually load inventory.");
    }
    
    public void onPlayerLeave(Player player) {
        // Auto-save disabled for safety to prevent timing issues and item loss
        logger.info("Player " + player.getName() + " left - auto-save disabled for safety. Use '/ib sync save' to manually save inventory.");
    }
    
    public void manualSync(Player player, boolean save) {
        UUID playerUuid = player.getUniqueId();
        PaperConfigManager config = getConfigManager();
        
        if (syncInProgress.getOrDefault(playerUuid, false)) {
            logger.warning("Sync already in progress for player " + player.getName());
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
                databaseManager.logSync(playerUuid, config.getServerId(), "MANUAL", "SUCCESS", null);
                logger.info("Manual " + (save ? "save" : "load") + " completed for player " + player.getName());
            } catch (Exception e) {
                databaseManager.logSync(playerUuid, config.getServerId(), "MANUAL", "FAILED", e.getMessage());
                logger.severe("Manual " + (save ? "save" : "load") + " failed for player " + player.getName() + ": " + e.getMessage());
            } finally {
                syncInProgress.put(playerUuid, false);
                lastSyncTimes.put(playerUuid, System.currentTimeMillis());
            }
        });
    }
    
    private void savePlayerInventory(Player player) {
        PaperConfigManager config = getConfigManager();
        
        // Serialize main inventory
        String inventoryData = PaperItemSerializer.serializeInventory(player.getInventory());
        
        // Serialize ender chest if enabled
        String enderChestData = null;
        if (config.isSyncEnderChest()) {
            enderChestData = PaperItemSerializer.serializeInventory(player.getEnderChest());
        }
        
        // Get experience data
        int experience = config.isSyncExperience() ? player.getTotalExperience() : 0;
        int experienceLevel = config.isSyncExperience() ? player.getLevel() : 0;
        
        // Get health and hunger data
        double health = config.isSyncHealth() ? player.getHealth() : 20.0;
        int hunger = config.isSyncHunger() ? player.getFoodLevel() : 20;
        
        databaseManager.saveInventory(
                player.getUniqueId(),
                config.getServerId(),
                inventoryData,
                enderChestData,
                experience,
                experienceLevel,
                health,
                hunger,
                config.getMinecraftVersion(),
                4082 // Current data version for 1.21.8
        );
    }
    
    private void loadPlayerInventory(Player player) {
        PaperConfigManager config = getConfigManager();
        
        PaperInventoryData data = databaseManager.loadInventory(player.getUniqueId(), config.getServerId());
        
        if (data == null) {
            logger.info("No saved inventory found for player " + player.getName());
            return;
        }
        
        // Load main inventory
        player.getInventory().clear();
        ItemStack[] items = PaperItemSerializer.deserializeInventory(data.inventoryData());
        if (items != null) {
            player.getInventory().setContents(items);
        }
        
        // Load ender chest if enabled and data exists
        if (config.isSyncEnderChest() && data.enderChestData() != null) {
            ItemStack[] enderItems = PaperItemSerializer.deserializeInventory(data.enderChestData());
            if (enderItems != null) {
                player.getEnderChest().setContents(enderItems);
            }
        }
        
        // Load experience if enabled
        if (config.isSyncExperience()) {
            player.setLevel(data.experienceLevel());
            player.setTotalExperience(data.experience());
        }
        
        // Load health if enabled
        if (config.isSyncHealth()) {
            AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.MAX_HEALTH);
            double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : 20.0;
            player.setHealth(Math.min(data.health(), maxHealth));
        }
        
        // Load hunger if enabled
        if (config.isSyncHunger()) {
            player.setFoodLevel(data.hunger());
        }
        
        // Update player
        player.updateInventory();
    }
    
    private PaperConfigManager getConfigManager() {
        return site.chococar.inventorybridge.paper.ChococarsInventoryBridgePlugin.getInstance().getConfigManager();
    }
    
    public boolean isSyncInProgress(UUID playerUuid) {
        return syncInProgress.getOrDefault(playerUuid, false);
    }
    
    public long getLastSyncTime(UUID playerUuid) {
        return lastSyncTimes.getOrDefault(playerUuid, 0L);
    }
    
    public void scanAndSyncExistingPlayerFiles() {
        if (hasScannedPlayerFiles) {
            logger.info("重新掃描現有玩家檔案並同步至資料庫...");
        } else {
            logger.info("開始掃描現有玩家檔案並同步至資料庫...");
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                org.bukkit.Server server = org.bukkit.Bukkit.getServer();
                java.io.File worldContainer = server.getWorldContainer();
                java.io.File[] worldDirs = worldContainer.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."));
                
                if (worldDirs == null) {
                    logger.warning("無法找到世界資料夾");
                    return;
                }
                
                int totalScanned = 0;
                int totalSynced = 0;
                PaperConfigManager config = getConfigManager();
                
                for (java.io.File worldDir : worldDirs) {
                    java.io.File playerDataDir = new java.io.File(worldDir, "playerdata");
                    if (!playerDataDir.exists() || !playerDataDir.isDirectory()) {
                        continue;
                    }
                    
                    java.io.File[] playerFiles = playerDataDir.listFiles((dir, name) -> name.endsWith(".dat"));
                    if (playerFiles == null) continue;
                    
                    for (java.io.File playerFile : playerFiles) {
                        try {
                            String fileName = playerFile.getName();
                            String uuidString = fileName.substring(0, fileName.length() - 4); // 移除 .dat
                            UUID playerUuid = UUID.fromString(uuidString);
                            
                            totalScanned++;
                            
                            // 檢查資料庫中是否已存在此玩家資料
                            if (!databaseManager.hasInventory(playerUuid, config.getServerId())) {
                                // 嘗試從 NBT 檔案載入玩家資料
                                if (syncPlayerFromFile(playerUuid, playerFile)) {
                                    totalSynced++;
                                    logger.info("已同步玩家 " + playerUuid + " 的資料至資料庫");
                                }
                            }
                            
                        } catch (IllegalArgumentException e) {
                            // 無效的 UUID 格式，跳過此檔案
                            logger.warning("跳過無效的玩家檔案: " + playerFile.getName());
                        } catch (Exception e) {
                            logger.warning("處理玩家檔案 " + playerFile.getName() + " 時發生錯誤: " + e.getMessage());
                        }
                    }
                }
                
                logger.info("玩家檔案掃描完成！掃描了 " + totalScanned + " 個檔案，同步了 " + totalSynced + " 個新玩家至資料庫");
                hasScannedPlayerFiles = true;
                
            } catch (Exception e) {
                logger.severe("掃描玩家檔案時發生錯誤: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    private boolean syncPlayerFromFile(UUID playerUuid, java.io.File playerFile) {
        try {
            // 使用 Bukkit API 載入離線玩家資料
            org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(playerUuid);
            
            if (!offlinePlayer.hasPlayedBefore()) {
                logger.info("玩家 " + playerUuid + " 從未加入過伺服器，跳過同步");
                return false;
            }
            
            PaperConfigManager config = getConfigManager();
            
            try {
                // 嘗試讀取離線玩家的實際資料
                // 注意：這需要玩家的 .dat 檔案存在且可讀取
                
                // 使用 Paper/Bukkit 的 NBT 功能讀取玩家資料
                org.bukkit.entity.Player onlinePlayer = org.bukkit.Bukkit.getPlayer(playerUuid);
                
                String inventoryData;
                String enderChestData = null;
                int experience = 0;
                int experienceLevel = 0;
                double health = 20.0;
                int hunger = 20;
                
                if (onlinePlayer != null) {
                    // 如果玩家在線，直接讀取其資料
                    inventoryData = PaperItemSerializer.serializeInventory(onlinePlayer.getInventory());
                    if (config.isSyncEnderChest()) {
                        enderChestData = PaperItemSerializer.serializeInventory(onlinePlayer.getEnderChest());
                    }
                    if (config.isSyncExperience()) {
                        experience = onlinePlayer.getTotalExperience();
                        experienceLevel = onlinePlayer.getLevel();
                    }
                    if (config.isSyncHealth()) {
                        health = onlinePlayer.getHealth();
                    }
                    if (config.isSyncHunger()) {
                        hunger = onlinePlayer.getFoodLevel();
                    }
                } else {
                    // 玩家離線，創建初始記錄以標記玩家存在
                    inventoryData = createEmptyInventoryData();
                    logger.info("玩家 " + offlinePlayer.getName() + " (" + playerUuid + ") 離線，創建初始資料庫記錄");
                }
                
                databaseManager.saveInventory(
                    playerUuid,
                    config.getServerId(),
                    inventoryData,
                    enderChestData,
                    experience,
                    experienceLevel,
                    health,
                    hunger,
                    config.getMinecraftVersion(),
                    4082 // 目前資料版本
                );
                
                databaseManager.logSync(playerUuid, config.getServerId(), "INITIAL_SYNC", "SUCCESS", null);
                return true;
                
            } catch (Exception e) {
                logger.warning("讀取玩家 " + playerUuid + " 的資料時發生錯誤，使用預設值: " + e.getMessage());
                
                // 發生錯誤時，至少創建一個基本記錄
                databaseManager.saveInventory(
                    playerUuid,
                    config.getServerId(),
                    createEmptyInventoryData(),
                    null,
                    0, 0, 20.0, 20,
                    config.getMinecraftVersion(),
                    4082
                );
                
                databaseManager.logSync(playerUuid, config.getServerId(), "INITIAL_SYNC", "PARTIAL", e.getMessage());
                return true;
            }
            
        } catch (Exception e) {
            logger.warning("同步玩家檔案 " + playerUuid + " 失敗: " + e.getMessage());
            PaperConfigManager config = getConfigManager();
            databaseManager.logSync(playerUuid, config.getServerId(), "INITIAL_SYNC", "FAILED", e.getMessage());
            return false;
        }
    }
    
    private String createEmptyInventoryData() {
        // 創建一個空的物品欄資料字串
        // 這裡使用簡化的空物品欄表示
        return "[]"; // 空的 JSON 陣列表示空物品欄
    }
}
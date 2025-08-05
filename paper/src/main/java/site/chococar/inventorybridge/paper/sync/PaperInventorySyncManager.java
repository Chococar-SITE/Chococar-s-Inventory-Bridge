package site.chococar.inventorybridge.paper.sync;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.craftbukkit.v1_21_R3.inventory.CraftItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import java.io.FileInputStream;
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
        PaperConfigManager config = getConfigManager();
        if (!config.isSyncOnJoin()) {
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
                databaseManager.logSync(playerUuid, config.getServerId(), "JOIN", "SUCCESS", null);
                logger.info("已為玩家 " + player.getName() + " 自動載入背包");
            } catch (Exception e) {
                databaseManager.logSync(playerUuid, config.getServerId(), "JOIN", "FAILED", e.getMessage());
                logger.severe("玩家 " + player.getName() + " 自動載入失敗: " + e.getMessage());
            } finally {
                syncInProgress.put(playerUuid, false);
                lastSyncTimes.put(playerUuid, System.currentTimeMillis());
            }
        });
    }
    
    public void onPlayerLeave(Player player) {
        PaperConfigManager config = getConfigManager();
        if (!config.isSyncOnLeave()) {
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
                databaseManager.logSync(playerUuid, config.getServerId(), "LEAVE", "SUCCESS", null);
                logger.info("已為玩家 " + player.getName() + " 自動保存背包");
            } catch (Exception e) {
                databaseManager.logSync(playerUuid, config.getServerId(), "LEAVE", "FAILED", e.getMessage());
                logger.severe("玩家 " + player.getName() + " 自動保存失敗: " + e.getMessage());
            } finally {
                syncInProgress.put(playerUuid, false);
                lastSyncTimes.put(playerUuid, System.currentTimeMillis());
            }
        });
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
                logger.info("玩家 " + player.getName() + " 手動" + (save ? "保存" : "載入") + "完成");
            } catch (Exception e) {
                databaseManager.logSync(playerUuid, config.getServerId(), "MANUAL", "FAILED", e.getMessage());
                logger.severe("玩家 " + player.getName() + " 手動" + (save ? "保存" : "載入") + "失敗: " + e.getMessage());
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
            logger.info("玩家 " + player.getName() + " 沒有已保存的背包資料");
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
                    // 玩家離線，嘗試從NBT檔案讀取真實背包資料
                    try {
                        PaperNBTInventoryData nbtData = readPlayerNBTData(playerFile);
                        if (nbtData != null) {
                            inventoryData = nbtData.inventoryData;
                            if (config.isSyncEnderChest() && nbtData.enderChestData != null) {
                                enderChestData = nbtData.enderChestData;
                            }
                            if (config.isSyncExperience()) {
                                experience = nbtData.experience;
                                experienceLevel = nbtData.experienceLevel;
                            }
                            if (config.isSyncHealth()) {
                                health = nbtData.health;
                            }
                            if (config.isSyncHunger()) {
                                hunger = nbtData.hunger;
                            }
                            logger.info("從檔案讀取玩家 " + playerUuid + " 的離線資料並同步至資料庫");
                        } else {
                            logger.warning("無法讀取玩家 " + playerUuid + " 的檔案資料，跳過同步");
                            databaseManager.logSync(playerUuid, config.getServerId(), "INITIAL_SYNC", "FAILED", "無法讀取玩家檔案資料");
                            return false;
                        }
                    } catch (Exception nbtException) {
                        logger.warning("讀取玩家 " + playerUuid + " 離線檔案失敗，跳過同步: " + nbtException.getMessage());
                        databaseManager.logSync(playerUuid, config.getServerId(), "INITIAL_SYNC", "FAILED", "檔案讀取異常: " + nbtException.getMessage());
                        return false;
                    }
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
                logger.warning("讀取玩家 " + playerUuid + " 的資料時發生錯誤，跳過同步: " + e.getMessage());
                databaseManager.logSync(playerUuid, config.getServerId(), "INITIAL_SYNC", "FAILED", e.getMessage());
                return false;
            }
            
        } catch (Exception e) {
            logger.warning("同步玩家檔案 " + playerUuid + " 失敗: " + e.getMessage());
            PaperConfigManager config = getConfigManager();
            databaseManager.logSync(playerUuid, config.getServerId(), "INITIAL_SYNC", "FAILED", e.getMessage());
            return false;
        }
    }
    
    
    /**
     * NBT庫存資料結構
     */
    private static class PaperNBTInventoryData {
        final String inventoryData;
        final String enderChestData;
        final int experience;
        final int experienceLevel;
        final double health;
        final int hunger;
        
        PaperNBTInventoryData(String inventoryData, String enderChestData, 
                            int experience, int experienceLevel, 
                            double health, int hunger) {
            this.inventoryData = inventoryData;
            this.enderChestData = enderChestData;
            this.experience = experience;
            this.experienceLevel = experienceLevel;
            this.health = health;
            this.hunger = hunger;
        }
    }
    
    /**
     * 從NBT檔案讀取玩家資料 (Paper實現)
     */
    private PaperNBTInventoryData readPlayerNBTData(java.io.File playerFile) {
        try {
            // 使用Paper的NBT API讀取玩家檔案
            CompoundTag playerData = NbtIo.readCompressed(new FileInputStream(playerFile), NbtAccounter.unlimitedHeap());
            
            if (playerData == null) {
                return null;
            }
            
            String inventoryData = "[]";
            String enderChestData = null;
            int experience = 0;
            int experienceLevel = 0;
            double health = 20.0;
            int hunger = 20;
            
            // 讀取背包資料
            if (playerData.contains("Inventory", 9)) {
                ListTag inventoryList = playerData.getList("Inventory", 10);
                inventoryData = serializeNbtListToPaper(inventoryList);
            }
            
            // 讀取終界箱資料
            if (playerData.contains("EnderItems", 9)) {
                ListTag enderList = playerData.getList("EnderItems", 10);
                enderChestData = serializeNbtListToPaper(enderList);
            }
            
            // 讀取經驗值
            if (playerData.contains("XpTotal")) {
                experience = playerData.getInt("XpTotal");
            }
            if (playerData.contains("XpLevel")) {
                experienceLevel = playerData.getInt("XpLevel");
            }
            
            // 讀取血量
            if (playerData.contains("Health")) {
                health = playerData.getFloat("Health");
            }
            
            // 讀取飢餓值
            if (playerData.contains("foodLevel")) {
                hunger = playerData.getInt("foodLevel");
            }
            
            return new PaperNBTInventoryData(inventoryData, enderChestData, 
                                           experience, experienceLevel, 
                                           health, hunger);
            
        } catch (Exception e) {
            logger.warning("讀取NBT檔案失敗: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 將NBT清單序列化為Paper格式的字串
     */
    private String serializeNbtListToPaper(ListTag nbtList) {
        if (nbtList == null || nbtList.isEmpty()) {
            return "[]";
        }
        
        try {
            ItemStack[] items = new ItemStack[41]; // 預設背包大小
            
            for (int i = 0; i < nbtList.size(); i++) {
                CompoundTag itemNbt = nbtList.getCompound(i);
                if (itemNbt != null && !itemNbt.isEmpty()) {
                    // 讀取槽位
                    int slot = itemNbt.getByte("Slot") & 255;
                    if (slot < items.length) {
                        // 使用Paper/CraftBukkit API轉換NBT到ItemStack
                        net.minecraft.world.item.ItemStack nmsStack = net.minecraft.world.item.ItemStack.of(itemNbt);
                        items[slot] = CraftItemStack.asBukkitCopy(nmsStack);
                    }
                }
            }
            
            return PaperItemSerializer.serializeInventoryArray(items);
            
        } catch (Exception e) {
            logger.warning("序列化NBT清單失敗: " + e.getMessage());
            return "[]";
        }
    }
}
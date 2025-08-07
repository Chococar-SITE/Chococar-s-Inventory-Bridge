package site.chococar.inventorybridge.paper.sync;

import org.bukkit.entity.Player;
import site.chococar.inventorybridge.common.config.ConfigurationManager;
import site.chococar.inventorybridge.common.sync.BaseInventorySyncManager;
import site.chococar.inventorybridge.paper.adapter.PaperPlayerAdapter;
import site.chococar.inventorybridge.paper.config.PaperConfigManager;
import site.chococar.inventorybridge.paper.database.PaperDatabaseManager;
import site.chococar.inventorybridge.paper.serialization.PaperItemSerializer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PaperInventorySyncManager extends BaseInventorySyncManager<PaperPlayerAdapter> {
    private final java.util.logging.Logger logger;
    private volatile boolean hasScannedPlayerFiles = false;
    
    public PaperInventorySyncManager(PaperDatabaseManager databaseManager, ConfigurationManager config) {
        super(databaseManager, config);
        this.logger = java.util.logging.Logger.getLogger("ChococarsInventoryBridge");
    }
    
    public void onPlayerJoin(Player player) {
        super.onPlayerJoin(new PaperPlayerAdapter(player));
    }
    
    public void onPlayerLeave(Player player) {
        super.onPlayerLeave(new PaperPlayerAdapter(player));
    }
    
    public void manualSync(Player player, boolean save) {
        super.manualSync(new PaperPlayerAdapter(player), save);
    }
    
    
    
    // 實現抽象方法
    @Override
    protected String getServerId() {
        return getConfigManager().getServerId();
    }
    
    @Override
    protected String getCurrentVersion() {
        try {
            return org.bukkit.Bukkit.getMinecraftVersion();
        } catch (Exception e) {
            logger.warning("無法獲取 Minecraft 版本，使用預設值: " + e.getMessage());
            return getConfigManager().getMinecraftVersion();
        }
    }
    
    @Override
    protected int getCurrentDataVersion() {
        try {
            String version = getCurrentVersion();
            
            // 根據版本號推斷數據版本
            if (version.startsWith("1.21.4")) {
                return 4071;
            } else if (version.startsWith("1.21.5")) {
                return 4073;
            } else if (version.startsWith("1.21.6")) {
                return 4076;
            } else if (version.startsWith("1.21.7")) {
                return 4079;
            } else if (version.startsWith("1.21.8")) {
                return 4082;
            } else if (version.startsWith("1.21")) {
                return 4071;
            } else {
                logger.warning("未知的 Minecraft 版本: " + version + "，使用預設數據版本");
                return 4071;
            }
        } catch (Exception e) {
            logger.warning("無法獲取數據版本，使用預設值: " + e.getMessage());
            return 4071;
        }
    }
    
    @Override
    protected Logger getLogger() {
        return new Logger() {
            @Override
            public void info(String message) {
                logger.info(message);
            }
            
            @Override
            public void warning(String message) {
                logger.warning(message);
            }
            
            @Override
            public void severe(String message) {
                logger.severe(message);
            }
        };
    }
    
    @Override
    protected void logError(String message, Exception e) {
        logger.severe(message + ": " + e.getMessage());
        e.printStackTrace();
    }
    
    private PaperConfigManager getConfigManager() {
        return site.chococar.inventorybridge.paper.ChococarsInventoryBridgePlugin.getInstance().getConfigManager();
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
                    getCurrentDataVersion() // 動態獲取當前數據版本
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
     * NBT 玩家資料結構（內部使用）
     */
    private static class NBTPlayerData {
        final String inventoryData;
        final String enderChestData;
        final int experience;
        final int experienceLevel;
        final double health;
        final int hunger;
        
        NBTPlayerData(String inventoryData, String enderChestData, 
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
     * 使用Paper原生API實現真實的NBT檔案讀取
     */
    private PaperNBTInventoryData readPlayerNBTData(java.io.File playerFile) {
        try {
            logger.info("正在讀取玩家NBT檔案: " + playerFile.getName());
            
            // 檢查檔案是否存在且可讀
            if (playerFile.length() > 0 && playerFile.canRead()) {
                try {
                    // 使用 Java 的 NBT 讀取功能
                    java.io.FileInputStream fis = new java.io.FileInputStream(playerFile);
                    java.io.DataInputStream dis = new java.io.DataInputStream(new java.util.zip.GZIPInputStream(fis));
                    
                    // 基本檔案驗證
                    long fileSize = playerFile.length();
                    long lastModified = playerFile.lastModified();
                    
                    logger.info("玩家檔案驗證通過 - 大小: " + fileSize + " bytes, 修改時間: " + new java.util.Date(lastModified));
                    
                    try {
                        // 使用 Bukkit 的 OfflinePlayer API 嘗試讀取資料
                        String uuidString = playerFile.getName().replace(".dat", "");
                        java.util.UUID playerUuid = java.util.UUID.fromString(uuidString);
                        org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(playerUuid);
                        
                        // 檢查玩家是否曾經遊玩過
                        if (offlinePlayer.hasPlayedBefore()) {
                            logger.info("檢測到玩家 " + playerUuid + " 曾經遊玩過，嘗試讀取NBT資料");
                            
                            // 嘗試讀取基本的 NBT 結構
                            byte tagType = dis.readByte();
                            if (tagType == 10) { // CompoundTag
                                dis.readUTF(); // 讀取根標籤名稱
                                
                                // 讀取玩家的基本資料
                                NBTPlayerData nbtData = readNBTPlayerData(dis, fileSize);
                                if (nbtData != null) {
                                    logger.info("成功讀取玩家 " + playerUuid + " 的實際NBT資料");
                                    
                                    // 關閉流
                                    dis.close();
                                    fis.close();
                                    
                                    return new PaperNBTInventoryData(
                                        nbtData.inventoryData,
                                        nbtData.enderChestData,
                                        nbtData.experience,
                                        nbtData.experienceLevel,
                                        nbtData.health,
                                        nbtData.hunger
                                    );
                                } else {
                                    logger.warning("無法讀取NBT資料，跳過此玩家的同步");
                                    dis.close();
                                    fis.close();
                                    return null;
                                }
                            } else {
                                logger.warning("NBT檔案格式錯誤，跳過此玩家的同步");
                                dis.close();
                                fis.close();
                                return null;
                            }
                        } else {
                            logger.info("玩家從未遊玩過，跳過同步");
                            dis.close();
                            fis.close();
                            return null;
                        }
                    } catch (Exception nbtReadException) {
                        logger.warning("NBT讀取失敗，跳過此玩家的同步: " + nbtReadException.getMessage());
                        dis.close();
                        fis.close();
                        return null;
                    }
                    
                } catch (java.io.IOException e) {
                    logger.warning("讀取NBT檔案時發生IO錯誤: " + e.getMessage());
                    return null;
                }
            } else {
                logger.warning("玩家檔案無法讀取或為空: " + playerFile.getName());
                return null;
            }
            
        } catch (Exception e) {
            logger.warning("讀取NBT檔案失敗: " + e.getMessage());
            return null;
        }
    }
    
    
    
    
    /**
     * 從NBT資料流讀取玩家資料
     */
    private NBTPlayerData readNBTPlayerData(java.io.DataInputStream dis, long fileSize) {
        try {
            logger.info("開始解析真實的NBT資料，檔案大小: " + fileSize + " bytes");
            
            // 初始化預設值
            String inventoryData = "[]";
            String enderChestData = "[]";
            int experience = 0;
            int experienceLevel = 0;
            double health = 20.0;
            int hunger = 20;
            
            // 讀取NBT根標籤
            NBTCompound rootTag = readNBTCompound(dis);
            if (rootTag == null) {
                logger.warning("無法讀取NBT根標籤");
                return null;
            }
            
            logger.info("成功讀取NBT根標籤，包含 " + rootTag.size() + " 個欄位");
            
            // 讀取背包資料
            if (rootTag.containsKey("Inventory")) {
                java.util.List<NBTCompound> inventoryList = rootTag.getList("Inventory");
                if (inventoryList != null && !inventoryList.isEmpty()) {
                    inventoryData = convertInventoryToJson(inventoryList);
                    logger.info("成功讀取背包資料，包含 " + inventoryList.size() + " 個物品");
                }
            }
            
            // 讀取終界箱資料
            if (rootTag.containsKey("EnderItems")) {
                java.util.List<NBTCompound> enderList = rootTag.getList("EnderItems");
                if (enderList != null && !enderList.isEmpty()) {
                    enderChestData = convertInventoryToJson(enderList);
                    logger.info("成功讀取終界箱資料，包含 " + enderList.size() + " 個物品");
                }
            }
            
            // 讀取經驗值
            if (rootTag.containsKey("XpLevel")) {
                experienceLevel = rootTag.getInt("XpLevel");
                logger.info("讀取到經驗等級: " + experienceLevel);
            }
            if (rootTag.containsKey("XpTotal")) {
                experience = rootTag.getInt("XpTotal");
                logger.info("讀取到總經驗: " + experience);
            }
            
            // 讀取生命值
            if (rootTag.containsKey("Health")) {
                health = rootTag.getFloat("Health");
                logger.info("讀取到生命值: " + health);
            }
            
            // 讀取飢餓值
            if (rootTag.containsKey("foodLevel")) {
                hunger = rootTag.getInt("foodLevel");
                logger.info("讀取到飢餓值: " + hunger);
            }
            
            logger.info("成功解析完整的NBT玩家資料");
            return new NBTPlayerData(inventoryData, enderChestData, experience, experienceLevel, health, hunger);
            
        } catch (Exception e) {
            logger.warning("讀取NBT玩家資料失敗: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 簡單的NBT Compound類別
     */
    private static class NBTCompound {
        private final java.util.Map<String, Object> data = new java.util.HashMap<>();
        
        public boolean containsKey(String key) {
            return data.containsKey(key);
        }
        
        public int getInt(String key) {
            Object value = data.get(key);
            if (value instanceof Integer) return (Integer) value;
            if (value instanceof Number) return ((Number) value).intValue();
            return 0;
        }
        
        public float getFloat(String key) {
            Object value = data.get(key);
            if (value instanceof Float) return (Float) value;
            if (value instanceof Number) return ((Number) value).floatValue();
            return 0.0f;
        }
        
        public String getString(String key) {
            Object value = data.get(key);
            return value instanceof String ? (String) value : "";
        }
        
        @SuppressWarnings("unchecked")
        public java.util.List<NBTCompound> getList(String key) {
            Object value = data.get(key);
            return value instanceof java.util.List ? (java.util.List<NBTCompound>) value : new java.util.ArrayList<>();
        }
        
        public void put(String key, Object value) {
            data.put(key, value);
        }
        
        public int size() {
            return data.size();
        }
    }
    
    /**
     * 讀取NBT Compound標籤
     */
    private NBTCompound readNBTCompound(java.io.DataInputStream dis) throws java.io.IOException {
        NBTCompound compound = new NBTCompound();
        
        while (true) {
            byte tagType = dis.readByte();
            if (tagType == 0) break; // TAG_End
            
            String name = dis.readUTF();
            Object value = readNBTValue(dis, tagType);
            
            if (value != null) {
                compound.put(name, value);
                logger.fine("讀取NBT標籤: " + name + " (類型: " + tagType + ")");
            }
        }
        
        return compound;
    }
    
    /**
     * 根據類型讀取NBT值
     */
    private Object readNBTValue(java.io.DataInputStream dis, byte tagType) throws java.io.IOException {
        switch (tagType) {
            case 1: // TAG_Byte
                return dis.readByte();
            case 2: // TAG_Short
                return dis.readShort();
            case 3: // TAG_Int
                return dis.readInt();
            case 4: // TAG_Long
                return dis.readLong();
            case 5: // TAG_Float
                return dis.readFloat();
            case 6: // TAG_Double
                return dis.readDouble();
            case 8: // TAG_String
                return dis.readUTF();
            case 9: // TAG_List
                return readNBTList(dis);
            case 10: // TAG_Compound
                return readNBTCompound(dis);
            case 7: // TAG_Byte_Array
                int byteArrayLength = dis.readInt();
                byte[] byteArray = new byte[byteArrayLength];
                dis.readFully(byteArray);
                return byteArray;
            case 11: // TAG_Int_Array
                int intArrayLength = dis.readInt();
                int[] intArray = new int[intArrayLength];
                for (int i = 0; i < intArrayLength; i++) {
                    intArray[i] = dis.readInt();
                }
                return intArray;
            case 12: // TAG_Long_Array
                int longArrayLength = dis.readInt();
                long[] longArray = new long[longArrayLength];
                for (int i = 0; i < longArrayLength; i++) {
                    longArray[i] = dis.readLong();
                }
                return longArray;
            default:
                logger.warning("未知的NBT標籤類型: " + tagType);
                return null;
        }
    }
    
    /**
     * 讀取NBT List
     */
    private java.util.List<NBTCompound> readNBTList(java.io.DataInputStream dis) throws java.io.IOException {
        byte listType = dis.readByte();
        int listLength = dis.readInt();
        
        java.util.List<NBTCompound> list = new java.util.ArrayList<>();
        
        for (int i = 0; i < listLength; i++) {
            if (listType == 10) { // TAG_Compound
                list.add(readNBTCompound(dis));
            } else {
                // 跳過非Compound類型的項目
                readNBTValue(dis, listType);
            }
        }
        
        return list;
    }
    
    /**
     * 將背包NBT轉換為JSON
     */
    private String convertInventoryToJson(java.util.List<NBTCompound> inventoryList) {
        StringBuilder json = new StringBuilder();
        json.append("{\"size\":41,\"minecraft_version\":\"").append(getCurrentVersion())
            .append("\",\"data_version\":").append(getCurrentDataVersion())
            .append(",\"items\":{");
        
        boolean first = true;
        for (NBTCompound item : inventoryList) {
            if (item.containsKey("Slot") && item.containsKey("id")) {
                if (!first) json.append(",");
                first = false;
                
                int slot = item.getInt("Slot");
                String itemId = item.getString("id");
                int count = item.containsKey("count") ? item.getInt("count") : 1;
                
                json.append("\"").append(slot).append("\":")
                    .append("\"{\\\"id\\\":\\\"").append(itemId).append("\\\",\\\"count\\\":").append(count).append("}\"");
            }
        }
        
        json.append("}}");
        return json.toString();
    }
    
}
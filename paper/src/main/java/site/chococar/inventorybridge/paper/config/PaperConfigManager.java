package site.chococar.inventorybridge.paper.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import site.chococar.inventorybridge.paper.ChococarsInventoryBridgePlugin;

import java.io.File;
import java.io.IOException;

public class PaperConfigManager {
    private static final int CURRENT_CONFIG_VERSION = 2;
    private final ChococarsInventoryBridgePlugin plugin;
    private FileConfiguration config;
    private File configFile;
    
    public PaperConfigManager(ChococarsInventoryBridgePlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }
    
    public void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // 檢查配置版本
        int configVersion = config.getInt("version", 1);
        
        if (configVersion < CURRENT_CONFIG_VERSION) {
            plugin.getLogger().info("檢測到舊版配置文件 (版本 " + configVersion + "，當前版本 " + CURRENT_CONFIG_VERSION + ")，開始遷移...");
            migrateConfig(configVersion);
            plugin.getLogger().info("配置文件遷移完成");
        } else if (configVersion > CURRENT_CONFIG_VERSION) {
            plugin.getLogger().warning("配置文件版本 (" + configVersion + ") 高於當前支持版本 (" + CURRENT_CONFIG_VERSION + ")，可能存在兼容性問題");
        } else {
            plugin.getLogger().info("配置文件加載成功 (版本 " + configVersion + ")");
        }
    }
    
    private void migrateConfig(int fromVersion) {
        plugin.getLogger().info("開始從版本 " + fromVersion + " 遷移配置到版本 " + CURRENT_CONFIG_VERSION);
        
        if (fromVersion == 1) {
            migrateFromV1ToV2();
        }
        
        // 更新版本號
        config.set("version", CURRENT_CONFIG_VERSION);
        saveConfig();
        
        plugin.getLogger().info("配置遷移完成，已保留所有自定義設置");
    }
    
    private void migrateFromV1ToV2() {
        // 版本1到版本2的遷移邏輯
        // 主要是將自動同步默認改為關閉，並添加版本號
        
        // 如果沒有明確設置 syncOnJoin，則設為 false（新的安全默認值）
        if (!config.contains("sync.syncOnJoin")) {
            config.set("sync.syncOnJoin", false);
        }
        
        // 如果沒有明確設置 syncOnLeave，則設為 false（新的安全默認值）
        if (!config.contains("sync.syncOnLeave")) {
            config.set("sync.syncOnLeave", false);
        }
        
        plugin.getLogger().info("已將自動同步設置更新為更安全的默認值");
    }
    
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save configuration: " + e.getMessage());
        }
    }
    
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    // Database configuration
    public String getDatabaseHost() {
        return config.getString("database.host", "localhost");
    }
    
    public int getDatabasePort() {
        return config.getInt("database.port", 3306);
    }
    
    public String getDatabaseName() {
        return config.getString("database.database", "inventory_bridge");
    }
    
    public String getDatabaseUsername() {
        return config.getString("database.username", "minecraft");
    }
    
    public String getDatabasePassword() {
        return config.getString("database.password", "password");
    }
    
    public String getTablePrefix() {
        return config.getString("database.tablePrefix", "ib_");
    }
    
    public int getMaxPoolSize() {
        return config.getInt("database.maxPoolSize", 10);
    }
    
    public int getConnectionTimeout() {
        return config.getInt("database.connectionTimeout", 30000);
    }
    
    public boolean useSSL() {
        return config.getBoolean("database.useSSL", false);
    }
    
    // Sync configuration
    public boolean isAutoSyncEnabled() {
        return config.getBoolean("sync.enableAutoSync", true);
    }
    
    public int getSyncIntervalTicks() {
        return config.getInt("sync.syncIntervalTicks", 200);
    }
    
    public boolean isSyncOnJoin() {
        return config.getBoolean("sync.syncOnJoin", true);
    }
    
    public boolean isSyncOnLeave() {
        return config.getBoolean("sync.syncOnLeave", true);
    }
    
    public boolean isSyncEnderChest() {
        return config.getBoolean("sync.syncEnderChest", true);
    }
    
    public boolean isSyncExperience() {
        return config.getBoolean("sync.syncExperience", true);
    }
    
    public boolean isSyncHealth() {
        return config.getBoolean("sync.syncHealth", false);
    }
    
    public boolean isSyncHunger() {
        return config.getBoolean("sync.syncHunger", false);
    }
    
    public String getServerId() {
        return config.getString("sync.serverId", "server1");
    }
    
    // Compatibility configuration
    public boolean isLegacySupportEnabled() {
        return config.getBoolean("compatibility.enableLegacySupport", true);
    }
    
    public boolean isConvertOldItems() {
        return config.getBoolean("compatibility.convertOldItems", true);
    }
    
    public boolean isPreserveCustomModelData() {
        return config.getBoolean("compatibility.preserveCustomModelData", true);
    }
    
    public boolean isHandleBundles() {
        return config.getBoolean("compatibility.handleBundles", true);
    }
    
    public boolean isHandleCopperOxidation() {
        return config.getBoolean("compatibility.handleCopperOxidation", true);
    }
    
    public boolean isHandleResinItems() {
        return config.getBoolean("compatibility.handleResinItems", true);
    }
    
    public String getMinecraftVersion() {
        return config.getString("compatibility.minecraftVersion", "1.21.8");
    }
}
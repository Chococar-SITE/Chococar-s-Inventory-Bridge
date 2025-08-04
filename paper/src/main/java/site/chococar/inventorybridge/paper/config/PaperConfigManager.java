package site.chococar.inventorybridge.paper.config;

import site.chococar.inventorybridge.common.config.ConfigurationManager;
import site.chococar.inventorybridge.paper.ChococarsInventoryBridgePlugin;

import java.nio.file.Path;

public class PaperConfigManager {
    private final ChococarsInventoryBridgePlugin plugin;
    private final ConfigurationManager configManager;
    
    public PaperConfigManager(ChococarsInventoryBridgePlugin plugin) {
        this.plugin = plugin;
        Path configPath = plugin.getDataFolder().toPath().resolve("config.yml");
        this.configManager = new ConfigurationManager(configPath);
    }
    
    public void loadConfig() {
        // 如果配置文件不存在，先創建默認配置
        Path configPath = plugin.getDataFolder().toPath().resolve("config.yml");
        if (!configPath.toFile().exists()) {
            plugin.saveDefaultConfig();
        }
        
        configManager.loadConfig();
        plugin.getLogger().info("配置文件通過Common模組載入完成");
    }
    
    public void saveConfig() {
        configManager.saveConfig();
    }
    
    public void reloadConfig() {
        configManager.loadConfig();
    }
    
    // Database configuration - 委託給Common模組
    public String getDatabaseHost() {
        return configManager.getString("database.host", "localhost");
    }
    
    public int getDatabasePort() {
        return configManager.getInt("database.port", 3306);
    }
    
    public String getDatabaseName() {
        return configManager.getString("database.database", "inventory_bridge");
    }
    
    public String getDatabaseUsername() {
        return configManager.getString("database.username", "minecraft");
    }
    
    public String getDatabasePassword() {
        return configManager.getString("database.password", "password");
    }
    
    public String getTablePrefix() {
        return configManager.getString("database.tablePrefix", "ib_");
    }
    
    public int getMaxPoolSize() {
        return configManager.getInt("database.maxPoolSize", 10);
    }
    
    public int getConnectionTimeout() {
        return configManager.getInt("database.connectionTimeout", 30000);
    }
    
    public boolean useSSL() {
        return configManager.getBoolean("database.useSSL", false);
    }
    
    // Sync configuration - 委託給Common模組
    public boolean isAutoSyncEnabled() {
        return configManager.getBoolean("sync.enableAutoSync", true);
    }
    
    public int getSyncIntervalTicks() {
        return configManager.getInt("sync.syncIntervalTicks", 200);
    }
    
    public boolean isSyncOnJoin() {
        return configManager.getBoolean("sync.syncOnJoin", true);
    }
    
    public boolean isSyncOnLeave() {
        return configManager.getBoolean("sync.syncOnLeave", true);
    }
    
    public boolean isSyncEnderChest() {
        return configManager.getBoolean("sync.syncEnderChest", true);
    }
    
    public boolean isSyncExperience() {
        return configManager.getBoolean("sync.syncExperience", true);
    }
    
    public boolean isSyncHealth() {
        return configManager.getBoolean("sync.syncHealth", false);
    }
    
    public boolean isSyncHunger() {
        return configManager.getBoolean("sync.syncHunger", false);
    }
    
    public String getServerId() {
        return configManager.getString("sync.serverId", "server1");
    }
    
    // Compatibility configuration - 委託給Common模組
    public boolean isLegacySupportEnabled() {
        return configManager.getBoolean("compatibility.enableLegacySupport", true);
    }
    
    public boolean isConvertOldItems() {
        return configManager.getBoolean("compatibility.convertOldItems", true);
    }
    
    public boolean isPreserveCustomModelData() {
        return configManager.getBoolean("compatibility.preserveCustomModelData", true);
    }
    
    public boolean isHandleBundles() {
        return configManager.getBoolean("compatibility.handleBundles", true);
    }
    
    public boolean isHandleCopperOxidation() {
        return configManager.getBoolean("compatibility.handleCopperOxidation", true);
    }
    
    public boolean isHandleResinItems() {
        return configManager.getBoolean("compatibility.handleResinItems", true);
    }
    
    public boolean isHandleGhastItems() {
        return configManager.getBoolean("compatibility.handleGhastItems", true);
    }
    
    public boolean isHandleNewMusicDiscs() {
        return configManager.getBoolean("compatibility.handleNewMusicDiscs", true);
    }
    
    public String getMinecraftVersion() {
        return configManager.getString("compatibility.minecraftVersion", "1.21.8");
    }
    
    // 內部訪問Common的ConfigurationManager (供DatabaseManager使用)
    public ConfigurationManager getConfigurationManager() {
        return configManager;
    }
}
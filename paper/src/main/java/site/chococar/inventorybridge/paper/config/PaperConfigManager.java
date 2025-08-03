package site.chococar.inventorybridge.paper.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import site.chococar.inventorybridge.paper.ChococarsInventoryBridgePlugin;

import java.io.File;
import java.io.IOException;

public class PaperConfigManager {
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
        plugin.getLogger().info("Configuration loaded successfully");
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
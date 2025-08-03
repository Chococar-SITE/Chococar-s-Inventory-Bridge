package site.chococar.inventorybridge.paper;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import site.chococar.inventorybridge.paper.config.PaperConfigManager;
import site.chococar.inventorybridge.paper.database.PaperDatabaseManager;
import site.chococar.inventorybridge.paper.sync.PaperInventorySyncManager;
import site.chococar.inventorybridge.paper.commands.InventoryBridgeCommand;

public class ChococarsInventoryBridgePlugin extends JavaPlugin implements Listener {
    
    private PaperConfigManager configManager;
    private PaperDatabaseManager databaseManager;
    private PaperInventorySyncManager syncManager;
    
    @Override
    public void onEnable() {
        getLogger().info("Enabling Chococar's Inventory Bridge Plugin");
        
        // Initialize configuration
        configManager = new PaperConfigManager(this);
        configManager.loadConfig();
        
        // Initialize database connection
        databaseManager = new PaperDatabaseManager(configManager);
        databaseManager.initialize();
        
        // Initialize sync manager
        syncManager = new PaperInventorySyncManager(databaseManager);
        
        // Register events
        getServer().getPluginManager().registerEvents(this, this);
        
        // Register commands
        getCommand("inventorybridge").setExecutor(new InventoryBridgeCommand(this));
        
        getLogger().info("Chococar's Inventory Bridge Plugin enabled successfully");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("Disabling Chococar's Inventory Bridge Plugin");
        
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info("Chococar's Inventory Bridge Plugin disabled");
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        syncManager.onPlayerJoin(event.getPlayer());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        syncManager.onPlayerLeave(event.getPlayer());
    }
    
    public PaperConfigManager getConfigManager() {
        return configManager;
    }
    
    public PaperDatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public PaperInventorySyncManager getSyncManager() {
        return syncManager;
    }
    
    public static ChococarsInventoryBridgePlugin getInstance() {
        return getPlugin(ChococarsInventoryBridgePlugin.class);
    }
}
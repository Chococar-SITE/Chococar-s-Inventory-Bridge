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
        
        try {
            // Initialize configuration
            configManager = new PaperConfigManager(this);
            configManager.loadConfig();
            getLogger().info("Configuration loaded successfully");
            
            // Initialize database connection
            databaseManager = new PaperDatabaseManager(configManager);
            databaseManager.initialize();
            
            // Initialize sync manager
            syncManager = new PaperInventorySyncManager(databaseManager);
            getLogger().info("Sync manager initialized");
            
            // Register events
            getServer().getPluginManager().registerEvents(this, this);
            getLogger().info("Events registered");
            
            // Register commands using reflection for Paper compatibility
            try {
                InventoryBridgeCommand commandExecutor = new InventoryBridgeCommand(this);
                
                // Use reflection to create PluginCommand since constructor is not public
                java.lang.reflect.Constructor<?> constructor = org.bukkit.command.PluginCommand.class.getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
                constructor.setAccessible(true);
                org.bukkit.command.PluginCommand inventoryBridgeCmd = (org.bukkit.command.PluginCommand) constructor.newInstance("inventorybridge", this);
                
                inventoryBridgeCmd.setDescription("Main command for Inventory Bridge");
                inventoryBridgeCmd.setUsage("/inventorybridge <subcommand>");
                inventoryBridgeCmd.setAliases(java.util.Arrays.asList("ib", "invbridge"));
                inventoryBridgeCmd.setPermission("chococars.inventorybridge.admin");
                inventoryBridgeCmd.setExecutor(commandExecutor);
                inventoryBridgeCmd.setTabCompleter(commandExecutor);
                
                this.getServer().getCommandMap().register("inventorybridge", inventoryBridgeCmd);
                getLogger().info("Commands registered successfully");
            } catch (Exception e) {
                getLogger().severe("Failed to register commands: " + e.getMessage());
                e.printStackTrace();
            }
            
            // Scan and sync existing player files if database is available
            if (!databaseManager.isStandbyMode()) {
                syncManager.scanAndSyncExistingPlayerFiles();
                getLogger().info("Started scanning existing player files");
            }
            
            getLogger().info("Chococar's Inventory Bridge Plugin enabled successfully");
        } catch (Exception e) {
            getLogger().severe("Plugin initialization encountered an error: " + e.getMessage());
            getLogger().severe("Plugin will continue running but some features may be limited");
            e.printStackTrace();
        }
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
        if (!databaseManager.isStandbyMode()) {
            syncManager.onPlayerJoin(event.getPlayer());
        } else {
            getLogger().warning("玩家 " + event.getPlayer().getName() + " 加入伺服器，但資料庫處於待機模式，跳過背包同步");
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!databaseManager.isStandbyMode()) {
            syncManager.onPlayerLeave(event.getPlayer());
        } else {
            getLogger().warning("玩家 " + event.getPlayer().getName() + " 離開伺服器，但資料庫處於待機模式，跳過背包同步");
        }
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
    
    public boolean reloadPluginConfig() {
        getLogger().info("管理員請求重新載入配置");
        
        try {
            // 重新載入配置文件
            configManager.loadConfig();
            getLogger().info("配置文件重新載入成功");
            
            // 重新初始化資料庫連接（使用新配置）
            databaseManager.reconnect();
            
            // 重新初始化同步管理器
            syncManager = new PaperInventorySyncManager(databaseManager);
            
            if (!databaseManager.isStandbyMode()) {
                    getLogger().info("✅ 配置重新載入完成");
                    getLogger().info("配置文件已重新載入");
                    getLogger().info("資料庫連接已重新初始化");
                    getLogger().info("同步管理器已更新");
                    getLogger().info("所有功能恢復正常運作");
                    
                    // 配置重載且資料庫連接成功後，掃描現有玩家檔案
                    syncManager.scanAndSyncExistingPlayerFiles();
                    getLogger().info("已開始掃描現有玩家檔案進行同步");
                } else {
                    getLogger().warning("⚠️ 配置重新載入部分成功");
                    getLogger().warning("配置文件已重新載入");
                    getLogger().warning("但資料庫連接失敗，進入待機模式");
                    getLogger().warning("錯誤原因: " + databaseManager.getLastConnectionError());
                    getLogger().warning("請檢查資料庫設定和連接狀態");
                }
            
            return true;
        } catch (Exception e) {
            getLogger().severe("❌ 配置重新載入失敗");
            getLogger().severe("錯誤原因: " + e.getMessage());
            getLogger().severe("請檢查配置文件格式是否正確");
            return false;
        }
    }
    
    public boolean reconnectDatabase() {
        getLogger().info("管理員請求重新連接資料庫");
        
        boolean success = databaseManager.reconnect();
        
        if (success) {
            // 重新初始化同步管理器
            syncManager = new PaperInventorySyncManager(databaseManager);
            
            // 重新連接成功後，掃描現有玩家檔案
            syncManager.scanAndSyncExistingPlayerFiles();
            getLogger().info("已開始掃描現有玩家檔案進行同步");
        }
        
        return success;
    }
    
}
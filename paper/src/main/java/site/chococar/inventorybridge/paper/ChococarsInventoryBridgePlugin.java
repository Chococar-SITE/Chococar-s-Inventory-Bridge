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
            getLogger().info("Database manager initialized");
            
            // Initialize sync manager
            syncManager = new PaperInventorySyncManager(databaseManager);
            getLogger().info("Sync manager initialized");
            
            // Register events
            getServer().getPluginManager().registerEvents(this, this);
            getLogger().info("Events registered");
            
            // Register commands
            getCommand("inventorybridge").setExecutor(new InventoryBridgeCommand(this));
            getLogger().info("Commands registered");
            
            getLogger().info("Chococar's Inventory Bridge Plugin enabled successfully");
        } catch (Exception e) {
            getLogger().severe("Failed to enable plugin: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
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
    
    public boolean reloadPluginConfig() {
        getLogger().info("管理員請求重新載入配置");
        
        try {
            // 重新載入配置文件
            configManager.loadConfig();
            getLogger().info("配置文件重新載入成功");
            
            // 重新初始化資料庫連接（使用新配置）
            if (databaseManager != null) {
                databaseManager.close();
            }
            
            databaseManager = new PaperDatabaseManager(configManager);
            
            try {
                databaseManager.initialize();
                
                // 重新初始化同步管理器
                syncManager = new PaperInventorySyncManager(databaseManager);
                
                // 測試資料庫連接
                boolean dbInitialized = testDatabaseConnection();
                
                if (dbInitialized) {
                    getLogger().info("✅ 配置重新載入完成");
                    getLogger().info("配置文件已重新載入");
                    getLogger().info("資料庫連接已重新初始化");
                    getLogger().info("同步管理器已更新");
                    getLogger().info("所有功能恢復正常運作");
                } else {
                    getLogger().warning("⚠️ 配置重新載入部分成功");
                    getLogger().warning("配置文件已重新載入");
                    getLogger().warning("但資料庫連接失敗，可能進入待機模式");
                    getLogger().warning("請檢查資料庫設定和連接狀態");
                }
            } catch (Exception e) {
                getLogger().severe("初始化資料庫時發生錯誤: " + e.getMessage());
                return false;
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
        
        try {
            if (databaseManager != null) {
                databaseManager.close();
            }
            
            databaseManager = new PaperDatabaseManager(configManager);
            databaseManager.initialize();
            
            // 重新初始化同步管理器
            syncManager = new PaperInventorySyncManager(databaseManager);
            
            boolean success = testDatabaseConnection();
            
            if (success) {
                getLogger().info("✅ 資料庫重新連接成功");
            } else {
                getLogger().warning("❌ 資料庫重新連接失敗");
            }
            
            return success;
        } catch (Exception e) {
            getLogger().severe("❌ 重新連接資料庫時發生錯誤: " + e.getMessage());
            return false;
        }
    }
    
    private boolean testDatabaseConnection() {
        try {
            if (databaseManager != null && databaseManager.getConnection() != null) {
                try (var connection = databaseManager.getConnection()) {
                    return connection.isValid(5);
                }
            }
            return false;
        } catch (Exception e) {
            getLogger().warning("資料庫連接測試失敗: " + e.getMessage());
            return false;
        }
    }
}
package site.chococar.inventorybridge.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import site.chococar.inventorybridge.fabric.config.FabricConfigManager;
import site.chococar.inventorybridge.fabric.database.FabricDatabaseManager;
import site.chococar.inventorybridge.fabric.commands.InventoryBridgeCommand;
import site.chococar.inventorybridge.fabric.sync.FabricInventorySyncManager;
import site.chococar.inventorybridge.fabric.util.FabricLogger;

public class ChococarsInventoryBridgeFabric implements ModInitializer {
    public static final String MOD_ID = "chococars_inventory_bridge";
    private static final FabricLogger LOGGER = new FabricLogger("ChococarsInventoryBridge");
    
    private static ChococarsInventoryBridgeFabric instance;
    private FabricConfigManager configManager;
    private FabricDatabaseManager databaseManager;
    private FabricInventorySyncManager syncManager;
    
    @Override
    public void onInitialize() {
        instance = this;
        LOGGER.info("正在初始化 Chococar's Inventory Bridge");
        
        // 初始化配置
        configManager = new FabricConfigManager();
        configManager.loadConfig();
        
        // 初始化資料庫連接
        databaseManager = new FabricDatabaseManager(configManager);
        
        // 初始化同步管理器
        syncManager = new FabricInventorySyncManager(databaseManager.getDatabaseConnection(), configManager.getConfigurationManager());
        
        // 註冊指令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            InventoryBridgeCommand.register(dispatcher);
        });
        
        // 註冊伺服器生命週期事件
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        
        // 註冊玩家連接事件
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!databaseManager.isStandbyMode()) {
                syncManager.onPlayerJoin(handler.getPlayer());
            } else {
                LOGGER.warn("玩家 {} 加入伺服器，但資料庫處於待機模式，跳過背包同步", 
                    handler.getPlayer().getGameProfile().getName());
            }
        });
        
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (!databaseManager.isStandbyMode()) {
                syncManager.onPlayerLeave(handler.getPlayer());
            } else {
                LOGGER.warn("玩家 {} 離開伺服器，但資料庫處於待機模式，跳過背包同步", 
                    handler.getPlayer().getGameProfile().getName());
            }
        });
        
        LOGGER.info("Chococar's Inventory Bridge 初始化完成");
    }
    
    private void onServerStarting(MinecraftServer server) {
        LOGGER.info("伺服器啟動中 - 初始化資料庫連接");
        
        // 設置伺服器實例供同步管理器使用
        FabricInventorySyncManager.setServerInstance(server);
        
        databaseManager.initialize();
    }
    
    private void onServerStarted(MinecraftServer server) {
        LOGGER.info("伺服器已完全啟動");
        
        // 如果資料庫連接成功，掃描現有玩家檔案
        if (!databaseManager.isStandbyMode()) {
            syncManager.scanAndSyncExistingPlayerFiles();
            LOGGER.info("已開始掃描現有玩家檔案進行同步");
        }
    }
    
    private void onServerStopping(MinecraftServer server) {
        LOGGER.info("伺服器關閉中 - 關閉資料庫連接");
        if (databaseManager != null) {
            databaseManager.close();
        }
    }
    
    public static ChococarsInventoryBridgeFabric getInstance() {
        return instance;
    }
    
    public FabricConfigManager getConfigManager() {
        return configManager;
    }
    
    public FabricDatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public FabricInventorySyncManager getSyncManager() {
        return syncManager;
    }
    
    public static FabricLogger getLogger() {
        return LOGGER;
    }
    
    public boolean reconnectDatabase() {
        boolean success = databaseManager.reconnect();
        
        if (success) {
            // 重新初始化同步管理器
            syncManager = new FabricInventorySyncManager(databaseManager.getDatabaseConnection(), configManager.getConfigurationManager());
            
            // 重新連接成功後，掃描現有玩家檔案
            syncManager.scanAndSyncExistingPlayerFiles();
            LOGGER.info("已開始掃描現有玩家檔案進行同步");
        }
        
        return success;
    }
    
    public boolean reloadPluginConfig() {
        LOGGER.info("管理員請求重新載入配置");
        
        try {
            // 重新載入配置文件
            configManager.loadConfig();
            LOGGER.info("配置文件重新載入成功");
            
            // 重新初始化同步管理器
            syncManager = new FabricInventorySyncManager(databaseManager.getDatabaseConnection(), configManager.getConfigurationManager());
            
            if (!databaseManager.isStandbyMode()) {
                LOGGER.info("✅ 配置重新載入完成");
                LOGGER.info("配置文件已重新載入");
                LOGGER.info("資料庫連接已重新初始化");
                LOGGER.info("同步管理器已更新");
                LOGGER.info("所有功能恢復正常運作");
                
                // 配置重載且資料庫連接成功後，掃描現有玩家檔案
                syncManager.scanAndSyncExistingPlayerFiles();
                LOGGER.info("已開始掃描現有玩家檔案進行同步");
            } else {
                LOGGER.warn("⚠️ 配置重新載入部分成功");
                LOGGER.warn("配置文件已重新載入");
                LOGGER.warn("但資料庫連接失敗，進入待機模式");
                LOGGER.warn("錯誤原因: {}", databaseManager.getLastConnectionError());
                LOGGER.warn("請檢查資料庫設定和連接狀態");
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.error("❌ 配置重新載入失敗");
            LOGGER.error("錯誤原因: {}", e.getMessage());
            LOGGER.error("請檢查配置文件格式是否正確");
            return false;
        }
    }
}
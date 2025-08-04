package site.chococar.inventorybridge.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import site.chococar.inventorybridge.common.config.ConfigurationManager;
import site.chococar.inventorybridge.common.database.DatabaseConnection;
import site.chococar.inventorybridge.fabric.sync.FabricInventorySyncManager;
import site.chococar.inventorybridge.fabric.util.FabricLogger;

public class ChococarsInventoryBridgeFabric implements ModInitializer {
    public static final String MOD_ID = "chococars_inventory_bridge";
    private static final FabricLogger LOGGER = new FabricLogger("ChococarsInventoryBridge");
    
    private static ChococarsInventoryBridgeFabric instance;
    private ConfigurationManager configManager;
    private DatabaseConnection databaseConnection;
    private FabricInventorySyncManager syncManager;
    
    @Override
    public void onInitialize() {
        instance = this;
        LOGGER.info("正在初始化 Chococar's Inventory Bridge");
        
        // 初始化配置
        configManager = new ConfigurationManager(
            FabricLoader.getInstance().getConfigDir().resolve("chococars_inventory_bridge.yml")
        );
        configManager.loadConfig();
        
        // 初始化資料庫連接
        databaseConnection = new DatabaseConnection(configManager);
        
        // 初始化同步管理器
        syncManager = new FabricInventorySyncManager(databaseConnection, configManager);
        
        // 註冊伺服器生命週期事件
        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        
        // 註冊玩家連接事件
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!databaseConnection.isStandbyMode()) {
                syncManager.onPlayerJoin(handler.getPlayer());
            } else {
                LOGGER.warn("玩家 {} 加入伺服器，但資料庫處於待機模式，跳過背包同步", 
                    handler.getPlayer().getGameProfile().getName());
            }
        });
        
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (!databaseConnection.isStandbyMode()) {
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
        databaseConnection.initialize();
    }
    
    private void onServerStopping(MinecraftServer server) {
        LOGGER.info("伺服器關閉中 - 關閉資料庫連接");
        if (databaseConnection != null) {
            databaseConnection.close();
        }
    }
    
    public static ChococarsInventoryBridgeFabric getInstance() {
        return instance;
    }
    
    public ConfigurationManager getConfigManager() {
        return configManager;
    }
    
    public DatabaseConnection getDatabaseConnection() {
        return databaseConnection;
    }
    
    public FabricInventorySyncManager getSyncManager() {
        return syncManager;
    }
    
    public static FabricLogger getLogger() {
        return LOGGER;
    }
    
    public boolean reconnectDatabase() {
        LOGGER.info("管理員請求重新連接資料庫");
        return databaseConnection.reconnect();
    }
}
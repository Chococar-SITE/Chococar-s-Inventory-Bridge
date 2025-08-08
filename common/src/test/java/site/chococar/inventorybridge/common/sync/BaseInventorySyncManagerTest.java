package site.chococar.inventorybridge.common.sync;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import site.chococar.inventorybridge.common.adapter.InventoryAdapter;
import site.chococar.inventorybridge.common.adapter.PlayerAdapter;
import site.chococar.inventorybridge.common.config.ConfigurationManager;
import site.chococar.inventorybridge.common.database.CommonDatabaseManager;
import site.chococar.inventorybridge.common.database.InventoryDataRecord;
import site.chococar.inventorybridge.common.util.Logger;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * BaseInventorySyncManager 的測試單元
 */
class BaseInventorySyncManagerTest {
    
    @Mock
    private CommonDatabaseManager mockDatabaseManager;
    
    @Mock
    private ConfigurationManager mockConfig;
    
    @Mock
    private PlayerAdapter mockPlayer;
    
    @Mock
    private InventoryAdapter mockInventory;
    
    private TestSyncManager syncManager;
    private UUID testPlayerId;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testPlayerId = UUID.randomUUID();
        
        // 設置模擬配置
        when(mockConfig.getString("sync.serverId", "server1")).thenReturn("test_server");
        when(mockConfig.getBoolean("sync.syncOnJoin", true)).thenReturn(true);
        when(mockConfig.getBoolean("sync.syncOnLeave", true)).thenReturn(true);
        when(mockConfig.getInt("sync.cooldown", 60)).thenReturn(5); // 5 秒冷卻
        
        // 設置模擬玩家
        when(mockPlayer.getUniqueId()).thenReturn(testPlayerId);
        when(mockPlayer.getName()).thenReturn("TestPlayer");
        when(mockPlayer.getInventory()).thenReturn(mockInventory);
        when(mockPlayer.getEnderChest()).thenReturn(mockInventory);
        
        // 設置模擬背包
        when(mockInventory.serialize()).thenReturn("{\"size\":41,\"items\":{}}");
        
        syncManager = new TestSyncManager(mockDatabaseManager, mockConfig);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        // BaseInventorySyncManager 沒有 close() 方法
        // 只是清理參考
        syncManager = null;
    }
    
    @Test
    @DisplayName("測試玩家加入時的自動同步")
    void testPlayerJoinSync() throws Exception {
        // 模擬數據庫有玩家數據
        InventoryDataRecord testData = new InventoryDataRecord(
            "{\"size\":41,\"items\":{}}", 
            null, 100, 5, 20.0, 20, 
            "1.21.4", 4071, 
            new Timestamp(System.currentTimeMillis())
        );
        when(mockDatabaseManager.loadInventory(testPlayerId, "test_server")).thenReturn(testData);
        
        // 執行玩家加入
        syncManager.onPlayerJoin(mockPlayer);
        
        // 等待異步操作完成
        Thread.sleep(100);
        
        // 驗證數據庫查詢被調用
        verify(mockDatabaseManager, timeout(1000)).loadInventory(testPlayerId, "test_server");
        
        // 驗證同步狀態
        assertTrue(syncManager.getLastSyncTime(testPlayerId) > 0);
    }
    
    @Test
    @DisplayName("測試玩家離開時的自動同步")
    void testPlayerLeaveSync() throws Exception {
        // 執行玩家離開
        syncManager.onPlayerLeave(mockPlayer);
        
        // 等待異步操作完成
        Thread.sleep(100);
        
        // 驗證數據庫保存被調用
        verify(mockDatabaseManager, timeout(1000)).saveInventory(
            eq(testPlayerId), eq("test_server"), anyString(), isNull(), 
            eq(0), eq(0), eq(20.0), eq(20), eq("1.21.4"), eq(4071)
        );
    }
    
    @Test
    @DisplayName("測試手動同步功能")
    void testManualSync() throws Exception {
        // 測試保存
        syncManager.manualSync(mockPlayer, true);
        Thread.sleep(100);
        
        verify(mockDatabaseManager, timeout(1000)).saveInventory(
            eq(testPlayerId), eq("test_server"), anyString(), isNull(),
            eq(0), eq(0), eq(20.0), eq(20), eq("1.21.4"), eq(4071)
        );
        
        // 測試載入
        InventoryDataRecord testData = new InventoryDataRecord(
            "{\"size\":41,\"items\":{}}", null, 150, 8, 18.0, 18,
            "1.21.4", 4071, new Timestamp(System.currentTimeMillis())
        );
        when(mockDatabaseManager.loadInventory(testPlayerId, "test_server")).thenReturn(testData);
        
        syncManager.manualSync(mockPlayer, false);
        Thread.sleep(100);
        
        verify(mockDatabaseManager, timeout(1000)).loadInventory(testPlayerId, "test_server");
    }
    
    @Test
    @DisplayName("測試同步冷卻機制")
    void testSyncCooldown() {
        // 第一次同步
        syncManager.onPlayerLeave(mockPlayer);
        
        // 立即再次嘗試同步 (應該被冷卻阻止)
        syncManager.onPlayerLeave(mockPlayer);
        
        // 只應該調用一次數據庫保存
        verify(mockDatabaseManager, timeout(1000).times(1)).saveInventory(
            any(UUID.class), anyString(), anyString(), any(),
            anyInt(), anyInt(), anyDouble(), anyInt(), anyString(), anyInt()
        );
    }
    
    @Test
    @DisplayName("測試同步進行中狀態")
    void testSyncInProgress() {
        assertFalse(syncManager.isSyncInProgress(testPlayerId));
        
        // 開始同步
        syncManager.onPlayerLeave(mockPlayer);
        
        // 短暫時間內應該顯示同步中
        assertTrue(syncManager.isSyncInProgress(testPlayerId) || 
                  syncManager.getLastSyncTime(testPlayerId) > 0);
    }
    
    @Test
    @DisplayName("測試錯誤處理")
    void testErrorHandling() {
        // 模擬數據庫錯誤
        doThrow(new RuntimeException("Database error"))
            .when(mockDatabaseManager).saveInventory(
                any(UUID.class), anyString(), anyString(), any(),
                anyInt(), anyInt(), anyDouble(), anyInt(), anyString(), anyInt()
            );
        
        // 同步不應該拋出異常
        assertDoesNotThrow(() -> {
            syncManager.onPlayerLeave(mockPlayer);
            Thread.sleep(100);
        });
        
        // 錯誤應該被記錄
        verify(mockDatabaseManager, timeout(1000)).logSync(
            eq(testPlayerId), eq("test_server"), eq("LEAVE"), eq("FAILED"), anyString()
        );
    }
    
    // 測試用的同步管理器實現
    private static class TestSyncManager extends BaseInventorySyncManager<PlayerAdapter> {
        
        public TestSyncManager(CommonDatabaseManager databaseManager, ConfigurationManager config) {
            super(databaseManager, config);
        }
        
        @Override
        protected String getServerId() {
            return "test_server";
        }
        
        @Override
        protected String getCurrentVersion() {
            return "1.21.4";
        }
        
        @Override
        protected int getCurrentDataVersion() {
            return 4071;
        }
        
        @Override
        protected Logger getLogger() {
            return new Logger() {
                @Override
                public void info(String message) {
                    System.out.println("[INFO] " + message);
                }
                
                @Override
                public void warning(String message) {
                    System.out.println("[WARN] " + message);
                }
                
                @Override
                public void severe(String message) {
                    System.out.println("[ERROR] " + message);
                }
            };
        }
        
        @Override
        protected void logError(String message, Exception e) {
            System.out.println("[ERROR] " + message + ": " + e.getMessage());
        }
    }
}
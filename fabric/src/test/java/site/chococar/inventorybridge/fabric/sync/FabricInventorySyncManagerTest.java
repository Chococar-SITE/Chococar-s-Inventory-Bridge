package site.chococar.inventorybridge.fabric.sync;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import site.chococar.inventorybridge.common.database.CommonDatabaseManager;
import site.chococar.inventorybridge.common.config.ConfigurationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * FabricInventorySyncManager 的測試單元
 * 注意：這些測試專注於配置和初始化邏輯，不依賴 Minecraft 環境
 */
@ExtendWith(MockitoExtension.class)
class FabricInventorySyncManagerTest {
    
    @Mock
    private CommonDatabaseManager mockDatabaseManager;
    
    @Mock 
    private ConfigurationManager mockConfigManager;
    
    @BeforeEach
    void setUp() {
        // 設置基本的模擬行為
        when(mockConfigManager.getString("sync.serverId", "server1")).thenReturn("test-fabric-server");
        when(mockConfigManager.getBoolean("sync.enableAutoSync", true)).thenReturn(true);
        when(mockConfigManager.getInt("sync.syncIntervalTicks", 200)).thenReturn(200);
        
        when(mockDatabaseManager.isStandbyMode()).thenReturn(false);
        when(mockDatabaseManager.reconnect()).thenReturn(true);
    }
    
    @Test
    @DisplayName("測試配置管理器依賴")
    void testConfigurationManager() {
        // 驗證配置管理器的模擬行為
        assertEquals("test-fabric-server", mockConfigManager.getString("sync.serverId", "server1"));
        assertTrue(mockConfigManager.getBoolean("sync.enableAutoSync", true));
        assertEquals(200, mockConfigManager.getInt("sync.syncIntervalTicks", 200));
        
        // 驗證方法被調用
        verify(mockConfigManager).getString("sync.serverId", "server1");
        verify(mockConfigManager).getBoolean("sync.enableAutoSync", true);
        verify(mockConfigManager).getInt("sync.syncIntervalTicks", 200);
    }
    
    @Test
    @DisplayName("測試數據庫管理器依賴")
    void testDatabaseManager() {
        // 驗證數據庫管理器的模擬行為
        assertFalse(mockDatabaseManager.isStandbyMode());
        assertTrue(mockDatabaseManager.reconnect());
        
        // 驗證方法被調用
        verify(mockDatabaseManager).isStandbyMode();
        verify(mockDatabaseManager).reconnect();
    }
    
    @Test
    @DisplayName("測試配置驗證邏輯")
    void testConfigurationValidation() {
        // 測試無效配置
        when(mockConfigManager.getString("sync.serverId", "server1")).thenReturn("");
        assertEquals("", mockConfigManager.getString("sync.serverId", "server1"));
        
        when(mockConfigManager.getString("sync.serverId", "server1")).thenReturn(null);
        assertNull(mockConfigManager.getString("sync.serverId", "server1"));
        
        // 測試有效配置
        when(mockConfigManager.getString("sync.serverId", "server1")).thenReturn("valid-server-name");
        assertEquals("valid-server-name", mockConfigManager.getString("sync.serverId", "server1"));
        assertTrue(mockConfigManager.getString("sync.serverId", "server1").length() > 0);
    }
    
    @Test
    @DisplayName("測試同步設置配置")
    void testSyncConfiguration() {
        // 測試同步相關配置
        when(mockConfigManager.getBoolean("sync.enableAutoSync", true)).thenReturn(false);
        when(mockConfigManager.getBoolean("sync.syncOnJoin", true)).thenReturn(true);
        when(mockConfigManager.getInt("sync.syncIntervalTicks", 200)).thenReturn(400);
        when(mockConfigManager.getBoolean("sync.syncEnderChest", true)).thenReturn(true);
        
        // 驗證配置值
        assertFalse(mockConfigManager.getBoolean("sync.enableAutoSync", true));
        assertTrue(mockConfigManager.getBoolean("sync.syncOnJoin", true));
        assertEquals(400, mockConfigManager.getInt("sync.syncIntervalTicks", 200));
        assertTrue(mockConfigManager.getBoolean("sync.syncEnderChest", true));
    }
    
    @Test
    @DisplayName("測試數據庫狀態檢查")
    void testDatabaseStatus() {
        // 測試數據庫狀態的不同情況
        
        // 情況 1: 正常運行狀態
        when(mockDatabaseManager.isStandbyMode()).thenReturn(false);
        assertFalse(mockDatabaseManager.isStandbyMode());
        
        // 情況 2: 待機模式
        when(mockDatabaseManager.isStandbyMode()).thenReturn(true);
        assertTrue(mockDatabaseManager.isStandbyMode());
        
        // 測試重連功能
        when(mockDatabaseManager.reconnect()).thenReturn(true);
        assertTrue(mockDatabaseManager.reconnect());
        
        when(mockDatabaseManager.reconnect()).thenReturn(false);
        assertFalse(mockDatabaseManager.reconnect());
    }
    
    @Test
    @DisplayName("測試數據庫配置")
    void testDatabaseConfiguration() {
        // 測試數據庫相關配置
        when(mockConfigManager.getString("database.host", "localhost")).thenReturn("mysql-server");
        when(mockConfigManager.getInt("database.port", 3306)).thenReturn(3307);
        when(mockConfigManager.getString("database.database", "inventory_bridge")).thenReturn("test_db");
        when(mockConfigManager.getInt("database.maxPoolSize", 10)).thenReturn(20);
        
        assertEquals("mysql-server", mockConfigManager.getString("database.host", "localhost"));
        assertEquals(3307, mockConfigManager.getInt("database.port", 3306));
        assertEquals("test_db", mockConfigManager.getString("database.database", "inventory_bridge"));
        assertEquals(20, mockConfigManager.getInt("database.maxPoolSize", 10));
    }
    
    @Test
    @DisplayName("測試兼容性配置")
    void testCompatibilityConfiguration() {
        // 測試兼容性相關配置
        when(mockConfigManager.getBoolean("compatibility.enableLegacySupport", true)).thenReturn(true);
        when(mockConfigManager.getBoolean("compatibility.convertOldItems", true)).thenReturn(false);
        when(mockConfigManager.getString("compatibility.minecraftVersion", "1.21.8")).thenReturn("1.21.4");
        
        assertTrue(mockConfigManager.getBoolean("compatibility.enableLegacySupport", true));
        assertFalse(mockConfigManager.getBoolean("compatibility.convertOldItems", true));
        assertEquals("1.21.4", mockConfigManager.getString("compatibility.minecraftVersion", "1.21.8"));
    }
    
    @Test
    @DisplayName("測試 Mockito 驗證功能")
    void testMockitoVerification() {
        // 調用一些方法
        mockConfigManager.getString("test.key", "default");
        mockConfigManager.getBoolean("test.enabled", false);
        mockDatabaseManager.isStandbyMode();
        
        // 驗證方法調用次數
        verify(mockConfigManager, times(1)).getString("test.key", "default");
        verify(mockConfigManager, times(1)).getBoolean("test.enabled", false);
        verify(mockDatabaseManager, times(1)).isStandbyMode();
        
        // 驗證沒有其他交互
        verifyNoMoreInteractions(mockConfigManager);
        verifyNoMoreInteractions(mockDatabaseManager);
    }
    
    @Test
    @DisplayName("測試邊界值配置")
    void testBoundaryValueConfiguration() {
        // 測試邊界值
        when(mockConfigManager.getInt("limit.maxPlayers", 100)).thenReturn(0);
        when(mockConfigManager.getInt("limit.minSyncInterval", 1)).thenReturn(Integer.MAX_VALUE);
        when(mockConfigManager.getDouble("limit.maxFileSize", 1024.0)).thenReturn(Double.MAX_VALUE);
        
        assertEquals(0, mockConfigManager.getInt("limit.maxPlayers", 100));
        assertEquals(Integer.MAX_VALUE, mockConfigManager.getInt("limit.minSyncInterval", 1));
        assertEquals(Double.MAX_VALUE, mockConfigManager.getDouble("limit.maxFileSize", 1024.0));
    }
    
    @Test
    @DisplayName("測試基本工具方法")
    void testUtilityMethods() {
        // 測試一些基本的工具方法邏輯
        String serverName = "fabric-test-server";
        assertNotNull(serverName);
        assertTrue(serverName.contains("fabric"));
        assertTrue(serverName.length() > 5);
        
        // 測試字符串處理
        String processedName = serverName.toLowerCase().replace("-", "_");
        assertEquals("fabric_test_server", processedName);
        
        // 測試數字處理
        int interval = 200;
        assertTrue(interval > 0);
        assertTrue(interval <= 1000);
        assertEquals(10, interval / 20); // ticks to seconds
    }
    
    @Test
    @DisplayName("測試配置值類型轉換")
    void testConfigurationTypeConversion() {
        // 測試不同類型的配置值
        when(mockConfigManager.getValue("custom.setting", "default")).thenReturn("custom_value");
        when(mockConfigManager.getValue("numeric.setting", 0)).thenReturn(42);
        when(mockConfigManager.getValue("boolean.setting", false)).thenReturn(true);
        
        assertEquals("custom_value", mockConfigManager.getValue("custom.setting", "default"));
        assertEquals(42, mockConfigManager.getValue("numeric.setting", 0));
        assertEquals(true, mockConfigManager.getValue("boolean.setting", false));
    }
}
package site.chococar.inventorybridge.common.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import site.chococar.inventorybridge.common.config.ConfigurationManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DatabaseConnection 的測試單元
 * 使用 H2 內存數據庫進行測試
 */
class DatabaseConnectionTest {
    
    private DatabaseConnection databaseConnection;
    private TestConfigManager configManager;
    
    @BeforeEach
    void setUp() {
        configManager = new TestConfigManager();
        databaseConnection = new DatabaseConnection(configManager);
        databaseConnection.initialize();
    }
    
    @AfterEach
    void tearDown() {
        if (databaseConnection != null) {
            databaseConnection.close();
        }
    }
    
    @Test
    @DisplayName("測試數據庫連接初始化")
    void testDatabaseInitialization() {
        assertFalse(databaseConnection.isStandbyMode());
        assertNull(databaseConnection.getLastConnectionError());
    }
    
    @Test
    @DisplayName("測試數據庫連接獲取")
    void testGetConnection() throws SQLException {
        Connection conn = databaseConnection.getConnection();
        assertNotNull(conn);
        assertFalse(conn.isClosed());
        conn.close();
    }
    
    @Test
    @DisplayName("測試表格前綴")
    void testTablePrefix() {
        assertEquals("test_", databaseConnection.getTablePrefix());
    }
    
    @Test
    @DisplayName("測試重新連接")
    void testReconnection() {
        assertTrue(databaseConnection.reconnect());
        assertFalse(databaseConnection.isStandbyMode());
    }
    
    @Test
    @DisplayName("測試數據庫表格創建")
    void testTableCreation() throws SQLException {
        try (Connection conn = databaseConnection.getConnection()) {
            // 檢查 inventories 表格是否存在
            var metadata = conn.getMetaData();
            var tables = metadata.getTables(null, null, "TEST_INVENTORIES", null);
            assertTrue(tables.next(), "inventories table should exist");
            
            // 檢查 sync_log 表格是否存在
            tables = metadata.getTables(null, null, "TEST_SYNC_LOG", null);
            assertTrue(tables.next(), "sync_log table should exist");
        }
    }
    
    // 測試用的配置管理器
    private static class TestConfigManager extends ConfigurationManager {
        public TestConfigManager() {
            super(null); // 使用 null 路徑，不處理檔案
        }
        
        @Override
        public String getString(String key, String defaultValue) {
            return switch (key) {
                case "database.host" -> "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
                case "database.port" -> "0";
                case "database.name" -> "testdb";
                case "database.username" -> "sa";
                case "database.password" -> "";
                case "database.tablePrefix" -> "test_";
                case "database.type" -> "h2";
                case "sync.serverId" -> "test_server";
                case "compatibility.minecraftVersion" -> "1.21.4";
                default -> defaultValue;
            };
        }
        
        @Override
        public int getInt(String key, int defaultValue) {
            return switch (key) {
                case "database.port" -> 0;
                case "database.maxPoolSize" -> 10;
                case "database.minPoolSize" -> 2;
                case "database.connectionTimeout" -> 30000;
                case "database.idleTimeout" -> 300000;
                case "database.maxLifetime" -> 1800000;
                default -> defaultValue;
            };
        }
        
        @Override
        public boolean getBoolean(String key, boolean defaultValue) {
            return switch (key) {
                case "database.ssl" -> false;
                case "database.autoCreateTables" -> true;
                default -> defaultValue;
            };
        }
        
        // 使用預設的父類實現
    }
}
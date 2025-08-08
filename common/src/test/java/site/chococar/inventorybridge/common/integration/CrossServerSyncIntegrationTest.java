package site.chococar.inventorybridge.common.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import site.chococar.inventorybridge.common.config.ConfigurationManager;
import site.chococar.inventorybridge.common.database.CommonDatabaseManager;
import site.chococar.inventorybridge.common.database.DatabaseConnection;
import site.chococar.inventorybridge.common.database.InventoryDataRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 跨伺服器同步功能的集成測試
 */
class CrossServerSyncIntegrationTest {
    
    private DatabaseConnection databaseConnection;
    private TestDatabaseManager serverA;
    private TestDatabaseManager serverB;
    private UUID testPlayerId;
    
    @BeforeEach
    void setUp() {
        // 使用 H2 內存數據庫
        TestConfigManager configManager = new TestConfigManager();
        databaseConnection = new DatabaseConnection(configManager);
        databaseConnection.initialize();
        
        serverA = new TestDatabaseManager(databaseConnection, "serverA");
        serverB = new TestDatabaseManager(databaseConnection, "serverB");
        testPlayerId = UUID.randomUUID();
    }
    
    @AfterEach
    void tearDown() {
        if (databaseConnection != null) {
            databaseConnection.close();
        }
    }
    
    @Test
    @DisplayName("測試跨伺服器數據載入")
    void testCrossServerDataLoading() {
        // 在 ServerA 保存玩家數據
        String inventoryData = """
        {
            "size": 41,
            "minecraft_version": "1.21.4", 
            "data_version": 4071,
            "items": {
                "0": {
                    "id": "minecraft:diamond_sword",
                    "count": 1
                },
                "1": {
                    "id": "minecraft:diamond",
                    "count": 10
                }
            }
        }
        """;
        
        serverA.saveInventory(testPlayerId, "serverA", inventoryData, 
            null, 100, 10, 20.0, 20, "1.21.4", 4071);
        
        // 驗證 ServerA 可以載入自己的數據
        InventoryDataRecord dataFromA = serverA.loadInventory(testPlayerId, "serverA");
        assertNotNull(dataFromA);
        assertEquals(inventoryData.replaceAll("\\s", ""), 
                    dataFromA.inventoryData().replaceAll("\\s", ""));
        assertEquals(100, dataFromA.experience());
        assertEquals(10, dataFromA.experienceLevel());
        
        // 驗證 ServerB 可以載入 ServerA 的數據（跨伺服器載入）
        InventoryDataRecord dataFromB = serverB.loadInventory(testPlayerId, "serverB");
        assertNotNull(dataFromB, "ServerB should be able to load data from ServerA");
        assertEquals(inventoryData.replaceAll("\\s", ""), 
                    dataFromB.inventoryData().replaceAll("\\s", ""));
        assertEquals(100, dataFromB.experience());
        assertEquals(10, dataFromB.experienceLevel());
    }
    
    @Test
    @DisplayName("測試數據更新和同步")
    void testDataUpdateAndSync() {
        // ServerA 保存初始數據
        serverA.saveInventory(testPlayerId, "serverA", 
            "{\"items\":{\"0\":{\"id\":\"minecraft:stone\",\"count\":1}}}", 
            null, 50, 5, 18.0, 18, "1.21.4", 4071);
        
        // 等待一毫秒確保時間戳不同
        try { Thread.sleep(1); } catch (InterruptedException e) {}
        
        // ServerB 保存更新的數據
        String updatedData = "{\"items\":{\"0\":{\"id\":\"minecraft:diamond\",\"count\":5}}}";
        serverB.saveInventory(testPlayerId, "serverB", updatedData,
            null, 150, 15, 20.0, 20, "1.21.4", 4071);
        
        // ServerA 應該能載入 ServerB 的更新數據（因為時間戳更新）
        InventoryDataRecord latestData = serverA.loadInventory(testPlayerId, "serverA");
        assertNotNull(latestData);
        assertEquals(updatedData, latestData.inventoryData());
        assertEquals(150, latestData.experience());
        assertEquals(15, latestData.experienceLevel());
    }
    
    @Test
    @DisplayName("測試同步日誌記錄")
    void testSyncLogging() {
        // 記錄同步日誌
        serverA.logSync(testPlayerId, "serverA", "JOIN", "SUCCESS", null);
        serverA.logSync(testPlayerId, "serverA", "MANUAL", "FAILED", "Connection timeout");
        
        // 驗證日誌被正確記錄
        try (Connection conn = databaseConnection.getConnection()) {
            String sql = "SELECT * FROM test_sync_log WHERE player_uuid = ? ORDER BY sync_time DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, testPlayerId.toString());
            
            ResultSet rs = stmt.executeQuery();
            
            // 檢查第一條記錄（最新的）
            assertTrue(rs.next());
            assertEquals("MANUAL", rs.getString("sync_type"));
            assertEquals("FAILED", rs.getString("status"));
            assertEquals("Connection timeout", rs.getString("error_message"));
            
            // 檢查第二條記錄
            assertTrue(rs.next());
            assertEquals("JOIN", rs.getString("sync_type"));
            assertEquals("SUCCESS", rs.getString("status"));
            assertNull(rs.getString("error_message"));
            
        } catch (SQLException e) {
            fail("Failed to verify sync logs: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("測試數據存在性檢查")
    void testDataExistenceCheck() {
        // 初始狀態：玩家數據不存在
        assertFalse(serverA.hasInventory(testPlayerId, "serverA"));
        assertFalse(serverB.hasInventory(testPlayerId, "serverB"));
        
        // 在 ServerA 保存數據
        serverA.saveInventory(testPlayerId, "serverA", 
            "{\"items\":{}}", null, 0, 0, 20.0, 20, "1.21.4", 4071);
        
        // 現在 ServerA 應該有數據，但 ServerB 特定數據仍然不存在
        assertTrue(serverA.hasInventory(testPlayerId, "serverA"));
        assertFalse(serverB.hasInventory(testPlayerId, "serverB"));
        
        // 在 ServerB 也保存數據
        serverB.saveInventory(testPlayerId, "serverB",
            "{\"items\":{}}", null, 0, 0, 20.0, 20, "1.21.4", 4071);
        
        // 現在兩個伺服器都應該有各自的數據
        assertTrue(serverA.hasInventory(testPlayerId, "serverA"));
        assertTrue(serverB.hasInventory(testPlayerId, "serverB"));
    }
    
    @Test
    @DisplayName("測試數據庫重新連接")
    void testDatabaseReconnection() {
        // 保存一些數據
        serverA.saveInventory(testPlayerId, "serverA",
            "{\"items\":{}}", null, 0, 0, 20.0, 20, "1.21.4", 4071);
        
        // 重新連接數據庫
        assertTrue(serverA.reconnect());
        assertFalse(serverA.isStandbyMode());
        
        // 重新連接後應該仍能訪問數據
        assertTrue(serverA.hasInventory(testPlayerId, "serverA"));
        assertNotNull(serverA.loadInventory(testPlayerId, "serverA"));
    }
    
    // 測試用的數據庫管理器
    private static class TestDatabaseManager implements CommonDatabaseManager {
        private final DatabaseConnection databaseConnection;
        private final String serverId;
        
        public TestDatabaseManager(DatabaseConnection databaseConnection, String serverId) {
            this.databaseConnection = databaseConnection;
            this.serverId = serverId;
        }
        
        @Override
        public void initialize() {
            databaseConnection.initialize();
        }
        
        @Override
        public void close() {
            databaseConnection.close();
        }
        
        @Override
        public boolean isStandbyMode() {
            return databaseConnection.isStandbyMode();
        }
        
        @Override
        public boolean reconnect() {
            return databaseConnection.reconnect();
        }
        
        @Override
        public String getLastConnectionError() {
            return databaseConnection.getLastConnectionError();
        }
        
        @Override
        public void saveInventory(UUID playerUuid, String serverId, String inventoryData,
                                String enderChestData, int experience, int experienceLevel,
                                double health, int hunger, String minecraftVersion, int dataVersion) {
            String sql = String.format("""
                INSERT INTO `%sinventories` 
                (`player_uuid`, `server_id`, `inventory_data`, `ender_chest_data`, 
                 `experience`, `experience_level`, `health`, `hunger`, 
                 `minecraft_version`, `data_version`)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                `inventory_data` = VALUES(`inventory_data`),
                `ender_chest_data` = VALUES(`ender_chest_data`),
                `experience` = VALUES(`experience`),
                `experience_level` = VALUES(`experience_level`),
                `health` = VALUES(`health`),
                `hunger` = VALUES(`hunger`),
                `minecraft_version` = VALUES(`minecraft_version`),
                `data_version` = VALUES(`data_version`),
                `last_updated` = CURRENT_TIMESTAMP
                """, databaseConnection.getTablePrefix());
            
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, serverId);
                stmt.setString(3, inventoryData);
                stmt.setString(4, enderChestData);
                stmt.setInt(5, experience);
                stmt.setInt(6, experienceLevel);
                stmt.setDouble(7, health);
                stmt.setInt(8, hunger);
                stmt.setString(9, minecraftVersion);
                stmt.setInt(10, dataVersion);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save inventory", e);
            }
        }
        
        @Override
        public InventoryDataRecord loadInventory(UUID playerUuid, String serverId) {
            // 首先嘗試從當前伺服器載入
            String currentServerSql = String.format("""
                SELECT `inventory_data`, `ender_chest_data`, `experience`, `experience_level`,
                       `health`, `hunger`, `minecraft_version`, `data_version`, `last_updated`
                FROM `%sinventories`
                WHERE `player_uuid` = ? AND `server_id` = ?
                """, databaseConnection.getTablePrefix());
            
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(currentServerSql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, serverId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new InventoryDataRecord(
                            rs.getString("inventory_data"),
                            rs.getString("ender_chest_data"),
                            rs.getInt("experience"),
                            rs.getInt("experience_level"),
                            rs.getDouble("health"),
                            rs.getInt("hunger"),
                            rs.getString("minecraft_version"),
                            rs.getInt("data_version"),
                            rs.getTimestamp("last_updated")
                        );
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load from current server", e);
            }
            
            // 如果當前伺服器沒有資料，從其他伺服器載入最新資料
            String crossServerSql = String.format("""
                SELECT `inventory_data`, `ender_chest_data`, `experience`, `experience_level`,
                       `health`, `hunger`, `minecraft_version`, `data_version`, `last_updated`
                FROM `%sinventories`
                WHERE `player_uuid` = ?
                ORDER BY `last_updated` DESC
                LIMIT 1
                """, databaseConnection.getTablePrefix());
            
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(crossServerSql)) {
                stmt.setString(1, playerUuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new InventoryDataRecord(
                            rs.getString("inventory_data"),
                            rs.getString("ender_chest_data"),
                            rs.getInt("experience"),
                            rs.getInt("experience_level"),
                            rs.getDouble("health"),
                            rs.getInt("hunger"),
                            rs.getString("minecraft_version"),
                            rs.getInt("data_version"),
                            rs.getTimestamp("last_updated")
                        );
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to load from other servers", e);
            }
            
            return null;
        }
        
        @Override
        public boolean hasInventory(UUID playerUuid, String serverId) {
            String sql = String.format("""
                SELECT 1 FROM `%sinventories` 
                WHERE `player_uuid` = ? AND `server_id` = ? 
                LIMIT 1
                """, databaseConnection.getTablePrefix());
            
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, serverId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException e) {
                return false;
            }
        }
        
        @Override
        public void logSync(UUID playerUuid, String serverId, String syncType, String status, String errorMessage) {
            String sql = String.format("""
                INSERT INTO `%ssync_log` (`player_uuid`, `server_id`, `sync_type`, `status`, `error_message`)
                VALUES (?, ?, ?, ?, ?)
                """, databaseConnection.getTablePrefix());
            
            try (Connection conn = databaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUuid.toString());
                stmt.setString(2, serverId);
                stmt.setString(3, syncType);
                stmt.setString(4, status);
                stmt.setString(5, errorMessage);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to log sync", e);
            }
        }
    }
    
    // 測試用的配置管理器
    private static class TestConfigManager extends ConfigurationManager {
        public TestConfigManager() {
            super(null); // 使用 null 路徑
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
                default -> defaultValue;
            };
        }
        
        @Override
        public int getInt(String key, int defaultValue) {
            return switch (key) {
                case "database.maxPoolSize" -> 10;
                case "database.minPoolSize" -> 2;
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
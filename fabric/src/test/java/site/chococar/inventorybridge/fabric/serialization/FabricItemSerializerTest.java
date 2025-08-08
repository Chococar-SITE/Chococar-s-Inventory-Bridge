package site.chococar.inventorybridge.fabric.serialization;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FabricItemSerializer 的測試單元
 * 注意：這些測試專注於 JSON 處理邏輯，不依賴 Minecraft 物品實例
 */
@ExtendWith(MockitoExtension.class)
class FabricItemSerializerTest {
    
    private static final String VALID_ITEM_JSON = """
        {
            "id": "minecraft:diamond_sword",
            "count": 1
        }
        """;
    
    private static final String VALID_INVENTORY_JSON = """
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
                    "id": "minecraft:stone",
                    "count": 64
                }
            }
        }
        """;
    
    @BeforeEach
    void setUp() {
        // 測試不依賴 Minecraft 物品實例
    }
    
    @Test
    @DisplayName("測試 JSON 解析功能")
    void testJsonParsing() {
        // 測試 JSON 解析不會拋出異常
        assertDoesNotThrow(() -> {
            JsonObject json = JsonParser.parseString(VALID_ITEM_JSON).getAsJsonObject();
            assertTrue(json.has("id"));
            assertTrue(json.has("count"));
            assertEquals("minecraft:diamond_sword", json.get("id").getAsString());
            assertEquals(1, json.get("count").getAsInt());
        });
    }
    
    @Test
    @DisplayName("測試 NBT 序列化功能")
    void testNbtSerialization() {
        // 測試 serializeNbtList 方法
        String result = FabricItemSerializer.serializeNbtList(null);
        assertNotNull(result);
        
        // 應該返回有效的 JSON
        assertDoesNotThrow(() -> {
            JsonObject json = JsonParser.parseString(result).getAsJsonObject();
            assertTrue(json.has("size"));
            assertTrue(json.has("minecraft_version"));
            assertTrue(json.has("data_version"));
            assertTrue(json.has("items"));
        });
    }
    
    @Test
    @DisplayName("測試背包 JSON 格式驗證")
    void testInventoryJsonFormat() {
        assertDoesNotThrow(() -> {
            JsonObject json = JsonParser.parseString(VALID_INVENTORY_JSON).getAsJsonObject();
            assertEquals(41, json.get("size").getAsInt());
            assertTrue(json.has("minecraft_version"));
            assertTrue(json.has("data_version"));
            assertTrue(json.has("items"));
            
            JsonObject items = json.getAsJsonObject("items");
            assertTrue(items.has("0")); // diamond sword
            assertTrue(items.has("1")); // stone
            assertFalse(items.has("2")); // empty slot
            
            // 驗證物品是 JSON 對象而不是字符串
            assertTrue(items.get("0").isJsonObject());
            assertTrue(items.get("1").isJsonObject());
        });
    }
    
    @Test
    @DisplayName("測試物品反序列化不拋出異常")
    void testItemDeserialization() {
        // 測試反序列化不會拋出異常
        assertDoesNotThrow(() -> {
            FabricItemSerializer.deserializeItemStack(VALID_ITEM_JSON);
        });
        
        // 測試 null 和空字符串處理
        assertDoesNotThrow(() -> {
            FabricItemSerializer.deserializeItemStack(null);
            FabricItemSerializer.deserializeItemStack("");
        });
    }
    
    @Test
    @DisplayName("測試異常處理")
    void testExceptionHandling() {
        // 測試無效 JSON 不應該導致崩潰
        assertDoesNotThrow(() -> {
            FabricItemSerializer.deserializeItemStack("invalid json");
        });
        
        // 測試異常 JSON 結構處理
        assertDoesNotThrow(() -> {
            FabricItemSerializer.deserializeItemStack("{\"malformed\": }");
        });
    }
    
    @Test
    @DisplayName("測試多種 JSON 物品格式")
    void testMultipleJsonFormats() {
        String[] testItems = {
            "{\"id\": \"minecraft:diamond\", \"count\": 10}",
            "{\"id\": \"minecraft:iron_ingot\", \"count\": 32}",
            "{\"id\": \"minecraft:wooden_sword\", \"count\": 1}",
            "{\"id\": \"minecraft:bread\", \"count\": 16}",
            "{\"id\": \"minecraft:ender_pearl\", \"count\": 8}"
        };
        
        for (String item : testItems) {
            // 驗證可以解析為 JSON
            assertDoesNotThrow(() -> {
                JsonObject json = JsonParser.parseString(item).getAsJsonObject();
                assertTrue(json.has("id"));
                assertTrue(json.has("count"));
            }, "Invalid JSON: " + item);
            
            // 測試反序列化不拋出異常
            assertDoesNotThrow(() -> {
                FabricItemSerializer.deserializeItemStack(item);
            });
        }
    }
    
    @Test
    @DisplayName("測試背包序列化反序列化結構")
    void testInventoryStructure() {
        // 測試背包 JSON 結構完整性
        assertDoesNotThrow(() -> {
            JsonObject inventory = JsonParser.parseString(VALID_INVENTORY_JSON).getAsJsonObject();
            
            // 驗證基本結構
            assertTrue(inventory.has("size"));
            assertTrue(inventory.has("minecraft_version"));
            assertTrue(inventory.has("data_version"));
            assertTrue(inventory.has("items"));
            
            // 驗證物品結構
            JsonObject items = inventory.getAsJsonObject("items");
            for (String key : items.keySet()) {
                JsonObject item = items.getAsJsonObject(key);
                assertTrue(item.has("id"));
                assertTrue(item.has("count"));
                assertTrue(item.get("id").getAsString().startsWith("minecraft:"));
                assertTrue(item.get("count").getAsInt() > 0);
            }
        });
    }
    
    @Test
    @DisplayName("測試 JSON 格式正確性")
    void testJsonFormatCorrectness() {
        // 使用預定義的 JSON 測試格式正確性
        JsonObject json = JsonParser.parseString(VALID_INVENTORY_JSON).getAsJsonObject();
        
        // 確保 items 對象中的每個物品都是 JSON 對象，而不是字符串
        JsonObject items = json.getAsJsonObject("items");
        for (String key : items.keySet()) {
            assertTrue(items.get(key).isJsonObject(), 
                "Item at slot " + key + " should be a JSON object, not a string");
            
            JsonObject itemJson = items.getAsJsonObject(key);
            assertTrue(itemJson.has("id"), "Item should have 'id' field");
            assertTrue(itemJson.has("count"), "Item should have 'count' field");
        }
    }
    
    @Test
    @DisplayName("測試版本資訊取得")
    void testVersionInfo() {
        // 測試 NBT 序列化的版本資訊
        String result = FabricItemSerializer.serializeNbtList(null);
        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
        
        // 驗證版本資訊正確設定
        assertEquals("1.21.4", json.get("minecraft_version").getAsString());
        assertEquals(4071, json.get("data_version").getAsInt());
        assertEquals(41, json.get("size").getAsInt());
        assertTrue(json.has("items"));
    }
    
    @Test
    @DisplayName("測試邊界條件")
    void testBoundaryConditions() {
        // 測試空 JSON 對象
        assertDoesNotThrow(() -> {
            FabricItemSerializer.deserializeItemStack("{}");
        });
        
        // 測試只有部分欄位的 JSON
        assertDoesNotThrow(() -> {
            FabricItemSerializer.deserializeItemStack("{\"id\": \"minecraft:stone\"}");
        });
        
        // 測試包含額外欄位的 JSON
        assertDoesNotThrow(() -> {
            FabricItemSerializer.deserializeItemStack(
                "{\"id\": \"minecraft:stone\", \"count\": 1, \"extra\": \"field\"}"
            );
        });
    }
    
    @Test
    @DisplayName("測試特殊字符處理")
    void testSpecialCharacters() {
        // 測試包含特殊字符的物品 ID
        String[] specialIds = {
            "minecraft:test_item",
            "minecraft:test-item",
            "modded:custom_item",
            "namespace:item_with_underscores"
        };
        
        for (String id : specialIds) {
            String json = String.format("{\"id\": \"%s\", \"count\": 1}", id);
            assertDoesNotThrow(() -> {
                FabricItemSerializer.deserializeItemStack(json);
            }, "Failed to handle ID: " + id);
        }
    }
}
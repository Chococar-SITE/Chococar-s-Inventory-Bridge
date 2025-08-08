package site.chococar.inventorybridge.common.serialization;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommonItemSerializer 的測試單元
 */
class CommonItemSerializerTest {
    
    private TestItemStackProvider[] testItems;
    private TestInventoryProvider testInventory;
    
    @BeforeEach
    void setUp() {
        testItems = new TestItemStackProvider[41];
        testItems[0] = new TestItemStackProvider("minecraft:diamond_sword", 1, false);
        testItems[1] = new TestItemStackProvider("minecraft:stone", 64, false);
        testItems[2] = new TestItemStackProvider("minecraft:air", 0, true); // empty item
        testItems[5] = new TestItemStackProvider("minecraft:shulker_box", 1, false);
        
        testInventory = new TestInventoryProvider(41);
    }
    
    @Test
    @DisplayName("測試基本背包序列化")
    void testBasicInventorySerialization() {
        String result = CommonItemSerializer.serializeInventory(
            41, "1.21.4", 4071, testItems
        );
        
        assertNotNull(result);
        
        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
        assertEquals(41, json.get("size").getAsInt());
        assertEquals("1.21.4", json.get("minecraft_version").getAsString());
        assertEquals(4071, json.get("data_version").getAsInt());
        
        assertTrue(json.has("items"));
        JsonObject items = json.getAsJsonObject("items");
        
        // 檢查非空物品是否正確序列化
        assertTrue(items.has("0")); // diamond sword
        assertTrue(items.has("1")); // stone
        assertFalse(items.has("2")); // empty item should not be serialized
        assertTrue(items.has("5")); // shulker box
    }
    
    @Test
    @DisplayName("測試背包反序列化")
    void testInventoryDeserialization() {
        // 創建測試用的 JSON 數據
        String testData = """
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
        
        CommonItemSerializer.deserializeInventory(testData, testInventory);
        
        // 驗證物品是否正確設置
        assertNotNull(testInventory.getItem(0));
        assertNotNull(testInventory.getItem(1));
        assertNull(testInventory.getItem(2)); // should remain empty
        
        assertTrue(testInventory.getItem(0).contains("diamond_sword"));
        assertTrue(testInventory.getItem(1).contains("stone"));
    }
    
    @Test
    @DisplayName("測試舊格式兼容性")
    void testLegacyFormatCompatibility() {
        // 測試舊的字符串格式
        String legacyData = """
        {
            "size": 41,
            "minecraft_version": "1.21.4", 
            "data_version": 4071,
            "items": {
                "0": "{\\"id\\":\\"minecraft:diamond_sword\\",\\"count\\":1}",
                "1": "{\\"id\\":\\"minecraft:stone\\",\\"count\\":64}"
            }
        }
        """;
        
        assertDoesNotThrow(() -> {
            CommonItemSerializer.deserializeInventory(legacyData, testInventory);
        });
        
        // 驗證能正確處理舊格式
        assertNotNull(testInventory.getItem(0));
        assertNotNull(testInventory.getItem(1));
    }
    
    @Test
    @DisplayName("測試空背包處理")
    void testEmptyInventory() {
        TestItemStackProvider[] emptyItems = new TestItemStackProvider[41];
        
        String result = CommonItemSerializer.serializeInventory(
            41, "1.21.4", 4071, emptyItems
        );
        
        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
        JsonObject items = json.getAsJsonObject("items");
        
        // 空背包應該有空的 items 對象
        assertEquals(0, items.size());
    }
    
    @Test
    @DisplayName("測試容器內容處理")
    void testContainerContent() {
        JsonObject itemJson = CommonItemSerializer.createItemJson(
            "minecraft:shulker_box", 1, "1.21.4", 4071
        );
        
        // 創建容器物品
        TestItemStackProvider[] containerItems = new TestItemStackProvider[27];
        containerItems[0] = new TestItemStackProvider("minecraft:diamond", 10, false);
        containerItems[1] = new TestItemStackProvider("minecraft:emerald", 5, false);
        
        CommonItemSerializer.addContainerContent(
            itemJson, 27, containerItems, "1.21.4", 4071
        );
        
        // 驗證容器內容被正確添加
        assertTrue(itemJson.has("meta"));
        JsonObject meta = itemJson.getAsJsonObject("meta");
        assertTrue(meta.has("container"));
        
        JsonObject container = meta.getAsJsonObject("container");
        assertEquals(27, container.get("size").getAsInt());
        assertTrue(container.has("items"));
        
        JsonObject containerItemsJson = container.getAsJsonObject("items");
        assertTrue(containerItemsJson.has("0")); // diamond
        assertTrue(containerItemsJson.has("1")); // emerald
    }
    
    @Test
    @DisplayName("測試異常處理")
    void testExceptionHandling() {
        // 測試 null 數據
        assertDoesNotThrow(() -> {
            CommonItemSerializer.deserializeInventory(null, testInventory);
        });
        
        // 測試空字符串
        assertDoesNotThrow(() -> {
            CommonItemSerializer.deserializeInventory("", testInventory);
        });
        
        // 測試無效 JSON
        assertThrows(RuntimeException.class, () -> {
            CommonItemSerializer.deserializeInventory("invalid json", testInventory);
        });
    }
    
    // 測試用的 ItemStackProvider 實現
    private static class TestItemStackProvider implements CommonItemSerializer.ItemStackProvider {
        private final String id;
        private final int count;
        private final boolean empty;
        
        public TestItemStackProvider(String id, int count, boolean empty) {
            this.id = id;
            this.count = count;
            this.empty = empty;
        }
        
        @Override
        public boolean isEmpty() {
            return empty || count <= 0;
        }
        
        @Override
        public String serialize() {
            if (isEmpty()) return null;
            return String.format("{\"id\":\"%s\",\"count\":%d}", id, count);
        }
    }
    
    // 測試用的 InventoryProvider 實現
    private static class TestInventoryProvider implements CommonItemSerializer.InventoryProvider {
        private final String[] items;
        
        public TestInventoryProvider(int size) {
            this.items = new String[size];
        }
        
        @Override
        public int size() {
            return items.length;
        }
        
        @Override
        public void setItem(int slot, String itemData) {
            if (slot >= 0 && slot < items.length) {
                items[slot] = itemData;
            }
        }
        
        public String getItem(int slot) {
            return slot >= 0 && slot < items.length ? items[slot] : null;
        }
    }
}
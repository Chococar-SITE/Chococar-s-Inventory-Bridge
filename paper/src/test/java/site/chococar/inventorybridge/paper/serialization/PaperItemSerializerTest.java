package site.chococar.inventorybridge.paper.serialization;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PaperItemSerializer 的測試單元
 */
@ExtendWith(MockitoExtension.class)
class PaperItemSerializerTest {
    
    @Mock
    private Inventory mockInventory;
    
    private ItemStack[] testItems;
    
    @BeforeEach
    void setUp() {
        testItems = new ItemStack[41];
        testItems[0] = new ItemStack(Material.DIAMOND_SWORD, 1);
        testItems[1] = new ItemStack(Material.STONE, 64);
        testItems[5] = new ItemStack(Material.SHULKER_BOX, 1);
        // 其他位置保持為 null (空)
        
        // 設置模擬背包
        when(mockInventory.getSize()).thenReturn(41);
        when(mockInventory.getContents()).thenReturn(testItems);
    }
    
    @Test
    @DisplayName("測試物品堆疊序列化")
    void testItemStackSerialization() {
        ItemStack diamondSword = new ItemStack(Material.DIAMOND_SWORD, 1);
        
        String result = PaperItemSerializer.serializeItemStack(diamondSword);
        assertNotNull(result);
        
        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
        assertEquals("DIAMOND_SWORD", json.get("material").getAsString());
        assertEquals(1, json.get("amount").getAsInt());
        assertTrue(json.has("minecraft_version"));
        assertTrue(json.has("data_version"));
    }
    
    @Test
    @DisplayName("測試空物品堆疊處理")
    void testEmptyItemStack() {
        ItemStack emptyItem = new ItemStack(Material.AIR);
        
        String result = PaperItemSerializer.serializeItemStack(emptyItem);
        assertNull(result); // 空物品應該返回 null
        
        // 測試 null 物品
        result = PaperItemSerializer.serializeItemStack(null);
        assertNull(result);
    }
    
    @Test
    @DisplayName("測試背包序列化")
    void testInventorySerialization() {
        String result = PaperItemSerializer.serializeInventory(mockInventory);
        assertNotNull(result);
        
        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
        assertEquals(41, json.get("size").getAsInt());
        assertTrue(json.has("minecraft_version"));
        assertTrue(json.has("data_version"));
        assertTrue(json.has("items"));
        
        JsonObject items = json.getAsJsonObject("items");
        assertTrue(items.has("0")); // diamond sword
        assertTrue(items.has("1")); // stone
        assertFalse(items.has("2")); // empty slot
        assertTrue(items.has("5")); // shulker box
        
        // 驗證物品是 JSON 對象而不是字符串
        assertTrue(items.get("0").isJsonObject());
        assertTrue(items.get("1").isJsonObject());
    }
    
    @Test
    @DisplayName("測試背包陣列序列化")
    void testInventoryArraySerialization() {
        String result = PaperItemSerializer.serializeInventoryArray(testItems);
        assertNotNull(result);
        
        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
        assertEquals(testItems.length, json.get("size").getAsInt());
        assertTrue(json.has("items"));
        
        JsonObject items = json.getAsJsonObject("items");
        assertTrue(items.has("0"));
        assertTrue(items.has("1"));
        assertTrue(items.has("5"));
        
        // 重要：驗證物品是 JSON 對象而不是嵌套字符串
        assertTrue(items.get("0").isJsonObject());
        JsonObject item0 = items.getAsJsonObject("0");
        assertEquals("DIAMOND_SWORD", item0.get("material").getAsString());
    }
    
    @Test
    @DisplayName("測試背包反序列化")
    void testInventoryDeserialization() {
        String testData = """
        {
            "size": 41,
            "minecraft_version": "1.21.4",
            "data_version": 4071,
            "items": {
                "0": {
                    "material": "DIAMOND_SWORD",
                    "amount": 1,
                    "minecraft_version": "1.21.4",
                    "data_version": 4071
                },
                "1": {
                    "material": "STONE",
                    "amount": 64,
                    "minecraft_version": "1.21.4",
                    "data_version": 4071
                }
            }
        }
        """;
        
        ItemStack[] result = PaperItemSerializer.deserializeInventory(testData);
        assertNotNull(result);
        assertEquals(41, result.length);
        
        // 檢查物品是否正確反序列化
        assertNotNull(result[0]);
        assertEquals(Material.DIAMOND_SWORD, result[0].getType());
        assertEquals(1, result[0].getAmount());
        
        assertNotNull(result[1]);
        assertEquals(Material.STONE, result[1].getType());
        assertEquals(64, result[1].getAmount());
        
        // 空位置應該是 null
        assertNull(result[2]);
    }
    
    @Test
    @DisplayName("測試舊格式兼容性")
    void testLegacyFormatCompatibility() {
        String legacyData = """
        {
            "size": 41,
            "minecraft_version": "1.21.4",
            "data_version": 4071,
            "items": {
                "0": "{\\"material\\":\\"DIAMOND_SWORD\\",\\"amount\\":1}"
            }
        }
        """;
        
        // 應該能處理舊的字符串格式而不拋出異常
        assertDoesNotThrow(() -> {
            ItemStack[] result = PaperItemSerializer.deserializeInventory(legacyData);
            assertNotNull(result);
            assertEquals(41, result.length);
        });
    }
    
    @Test
    @DisplayName("測試物品材質兼容性")
    void testMaterialCompatibility() {
        // 測試未知材質的處理
        String unknownMaterialData = """
        {
            "material": "UNKNOWN_MATERIAL_12345",
            "amount": 1,
            "minecraft_version": "1.21.4",
            "data_version": 4071
        }
        """;
        
        ItemStack result = PaperItemSerializer.deserializeItemStack(unknownMaterialData);
        // 未知材質應該返回 null 或被替換為兼容材質
        // 具體行為取決於 findCompatibleMaterial 的實現
        assertNull(result); // 或者檢查是否被替換為其他材質
    }
    
    @Test
    @DisplayName("測試異常處理")
    void testExceptionHandling() {
        // 測試 null 輸入
        assertNull(PaperItemSerializer.deserializeInventory(null));
        assertNull(PaperItemSerializer.deserializeInventory(""));
        
        // 測試無效 JSON
        assertNull(PaperItemSerializer.deserializeInventory("invalid json"));
        
        // 測試空陣列序列化
        String result = PaperItemSerializer.serializeInventoryArray(null);
        assertEquals("[]", result);
    }
    
    @Test
    @DisplayName("測試 JSON 格式正確性")
    void testJsonFormatCorrectness() {
        String result = PaperItemSerializer.serializeInventory(mockInventory);
        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
        
        // 確保 items 對象中的每個物品都是 JSON 對象，而不是字符串
        JsonObject items = json.getAsJsonObject("items");
        for (String key : items.keySet()) {
            assertTrue(items.get(key).isJsonObject(), 
                "Item at slot " + key + " should be a JSON object, not a string");
        }
    }
}
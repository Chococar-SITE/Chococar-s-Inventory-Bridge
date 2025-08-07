package site.chococar.inventorybridge.common.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 通用的物品和背包序列化器
 * 提供平台無關的序列化邏輯
 */
public class CommonItemSerializer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * 序列化背包數據為統一的JSON格式
     */
    public static String serializeInventory(int size, String version, int dataVersion, ItemStackProvider[] items) {
        JsonObject json = new JsonObject();
        json.addProperty("size", size);
        json.addProperty("minecraft_version", version);
        json.addProperty("data_version", dataVersion);
        
        JsonObject itemsJson = new JsonObject();
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null && !items[i].isEmpty()) {
                String serializedItem = items[i].serialize();
                if (serializedItem != null) {
                    JsonObject itemObj = JsonParser.parseString(serializedItem).getAsJsonObject();
                    itemsJson.add(String.valueOf(i), itemObj);
                }
            }
        }
        json.add("items", itemsJson);
        
        return GSON.toJson(json);
    }
    
    /**
     * 反序列化背包數據
     */
    public static void deserializeInventory(String data, InventoryProvider inventory) {
        if (data == null || data.isEmpty()) {
            return;
        }
        
        try {
            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
            
            if (json.has("items")) {
                JsonObject itemsJson = json.getAsJsonObject("items");
                itemsJson.entrySet().forEach(entry -> {
                    try {
                        int slot = Integer.parseInt(entry.getKey());
                        if (slot < inventory.size()) {
                            // 處理兩種格式：新的JSON對象格式和舊的字符串格式
                            String itemData;
                            if (entry.getValue().isJsonObject()) {
                                itemData = GSON.toJson(entry.getValue());
                            } else {
                                itemData = entry.getValue().getAsString();
                            }
                            inventory.setItem(slot, itemData);
                        }
                    } catch (NumberFormatException e) {
                        // Log warning but continue processing
                    }
                });
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize inventory", e);
        }
    }
    
    /**
     * 創建標準的物品JSON結構
     */
    public static JsonObject createItemJson(String id, int count, String version, int dataVersion) {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("count", count);
        json.addProperty("minecraft_version", version);
        json.addProperty("data_version", dataVersion);
        return json;
    }
    
    /**
     * 添加容器內容到物品JSON
     */
    public static void addContainerContent(JsonObject itemJson, int containerSize, ItemStackProvider[] containerItems, String version, int dataVersion) {
        if (containerItems == null || containerItems.length == 0) {
            return;
        }
        
        JsonObject containerJson = new JsonObject();
        containerJson.addProperty("size", containerSize);
        
        JsonObject containerItemsJson = new JsonObject();
        for (int i = 0; i < containerItems.length; i++) {
            if (containerItems[i] != null && !containerItems[i].isEmpty()) {
                String serializedItem = containerItems[i].serialize();
                if (serializedItem != null) {
                    JsonObject itemObj = JsonParser.parseString(serializedItem).getAsJsonObject();
                    containerItemsJson.add(String.valueOf(i), itemObj);
                }
            }
        }
        containerJson.add("items", containerItemsJson);
        
        if (!itemJson.has("meta")) {
            itemJson.add("meta", new JsonObject());
        }
        itemJson.getAsJsonObject("meta").add("container", containerJson);
    }
    
    /**
     * 從JSON中提取容器內容
     */
    public static void extractContainerContent(JsonObject itemJson, ContainerHandler containerHandler) {
        if (!itemJson.has("meta")) {
            return;
        }
        
        JsonObject metaJson = itemJson.getAsJsonObject("meta");
        if (!metaJson.has("container")) {
            return;
        }
        
        try {
            JsonObject containerJson = metaJson.getAsJsonObject("container");
            if (containerJson.has("items")) {
                JsonObject containerItems = containerJson.getAsJsonObject("items");
                int containerSize = containerJson.has("size") ? containerJson.get("size").getAsInt() : 27;
                
                containerHandler.handleContainer(containerSize, containerItems);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract container content", e);
        }
    }
    
    /**
     * 平台特定的物品堆疊提供者介面
     */
    public interface ItemStackProvider {
        boolean isEmpty();
        String serialize();
    }
    
    /**
     * 平台特定的背包提供者介面
     */
    public interface InventoryProvider {
        int size();
        void setItem(int slot, String itemData);
    }
    
    /**
     * 容器處理器介面
     */
    public interface ContainerHandler {
        void handleContainer(int size, JsonObject containerItems);
    }
}
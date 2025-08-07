package site.chococar.inventorybridge.fabric.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import site.chococar.inventorybridge.common.compatibility.ItemMappings;
import site.chococar.inventorybridge.common.serialization.CommonItemSerializer;
import site.chococar.inventorybridge.fabric.ChococarsInventoryBridgeFabric;

import java.util.ArrayList;
import java.util.List;

public class FabricItemSerializer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // 動態獲取版本信息
    private static String getCurrentVersion() {
        // 使用預設值避免API兼容性問題
        return "1.21.4";
    }
    
    private static int getCurrentDataVersion() {
        // 使用安全的預設值，避免API兼容性問題
        return 4071; // 1.21.4 的數據版本
    }
    
    public static String serializeItemStack(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return null;
        }
        
        JsonObject json = new JsonObject();
        json.addProperty("id", Registries.ITEM.getId(itemStack.getItem()).toString());
        json.addProperty("count", itemStack.getCount());
        
        // 序列化組件
        JsonObject componentsJson = new JsonObject();
        
        // 處理自定義模型數據
        if (itemStack.contains(DataComponentTypes.CUSTOM_MODEL_DATA)) {
            // 注釋掉因為 API 已改變
            // CustomModelDataComponent customModelData = itemStack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
            // componentsJson.addProperty("custom_model_data", customModelData.getValue());
        }
        
        // 處理束包內容 (1.21.2+)
        if (itemStack.contains(DataComponentTypes.BUNDLE_CONTENTS)) {
            BundleContentsComponent bundleContents = itemStack.get(DataComponentTypes.BUNDLE_CONTENTS);
            JsonObject bundleJson = new JsonObject();
            List<JsonObject> items = new ArrayList<>();
            bundleContents.iterate().forEach(stack -> {
                String serialized = serializeItemStack(stack);
                if (serialized != null) {
                    JsonObject itemObj = JsonParser.parseString(serialized).getAsJsonObject();
                    items.add(itemObj);
                }
            });
            bundleJson.add("items", GSON.toJsonTree(items));
            componentsJson.add("bundle_contents", bundleJson);
        }
        
        // 處理容器內容（界伏盒等）
        if (itemStack.contains(DataComponentTypes.CONTAINER)) {
            ContainerComponent container = itemStack.get(DataComponentTypes.CONTAINER);
            JsonObject containerJson = new JsonObject();
            JsonObject containerItems = new JsonObject();
            
            for (int i = 0; i < container.stream().toList().size(); i++) {
                ItemStack stack = container.stream().toList().get(i);
                if (!stack.isEmpty()) {
                    String serialized = serializeItemStack(stack);
                    if (serialized != null) {
                        JsonObject itemObj = JsonParser.parseString(serialized).getAsJsonObject();
                        containerItems.add(String.valueOf(i), itemObj);
                    }
                }
            }
            
            containerJson.add("items", containerItems);
            containerJson.addProperty("size", container.stream().toList().size());
            componentsJson.add("container", containerJson);
        }
        
        // 處理耐久度
        if (itemStack.contains(DataComponentTypes.DAMAGE)) {
            componentsJson.addProperty("damage", itemStack.get(DataComponentTypes.DAMAGE));
        }
        
        // 處理最大耐久度
        if (itemStack.contains(DataComponentTypes.MAX_DAMAGE)) {
            componentsJson.addProperty("max_damage", itemStack.get(DataComponentTypes.MAX_DAMAGE));
        }
        
        // 處理顯示名稱
        if (itemStack.contains(DataComponentTypes.CUSTOM_NAME)) {
            componentsJson.addProperty("custom_name", itemStack.get(DataComponentTypes.CUSTOM_NAME).getString());
        }
        
        // 處理描述
        if (itemStack.contains(DataComponentTypes.LORE)) {
            List<String> loreList = new ArrayList<>();
            itemStack.get(DataComponentTypes.LORE).lines().forEach(text -> loreList.add(text.getString()));
            componentsJson.add("lore", GSON.toJsonTree(loreList));
        }
        
        // 處理附魔 - 暫時注釋掉因為 API 改變
        /*
        if (itemStack.contains(DataComponentTypes.ENCHANTMENTS)) {
            JsonObject enchantmentsJson = new JsonObject();
            itemStack.get(DataComponentTypes.ENCHANTMENTS).getEnchantments().forEach((enchantment, level) -> {
                String enchantmentId = enchantment.toString();
                enchantmentsJson.addProperty(enchantmentId, level);
            });
            componentsJson.add("enchantments", enchantmentsJson);
        }
        */
        
        if (!componentsJson.entrySet().isEmpty()) {
            json.add("components", componentsJson);
        }
        
        return GSON.toJson(json);
    }
    
    public static ItemStack deserializeItemStack(String data) {
        if (data == null || data.isEmpty()) {
            return ItemStack.EMPTY;
        }
        
        try {
            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
            
            String itemId = json.get("id").getAsString();
            int count = json.get("count").getAsInt();
            
            // 版本兼容性檢查
            String version = json.has("minecraft_version") ? json.get("minecraft_version").getAsString() : "unknown";
            
            // 檢查物品兼容性
            if (!ItemMappings.isItemAvailableInVersion(itemId, getCurrentVersion())) {
                String compatibleId = ItemMappings.getCompatibleItem(itemId);
                if (!compatibleId.equals(itemId)) {
                    ChococarsInventoryBridgeFabric.getLogger().info(String.format("將物品 %s 轉換為 %s 以保持版本兼容", itemId, compatibleId));
                    itemId = compatibleId;
                }
            }
            
            Identifier identifier = Identifier.tryParse(itemId);
            if (identifier == null || !Registries.ITEM.containsId(identifier)) {
                ChococarsInventoryBridgeFabric.getLogger().warn(String.format("未知物品ID: %s 來自版本 %s", itemId, version));
                return ItemStack.EMPTY;
            }
            
            ItemStack itemStack = new ItemStack(Registries.ITEM.get(identifier), count);
            
            // 應用組件（如果存在）
            if (json.has("components")) {
                JsonObject componentsJson = json.getAsJsonObject("components");
                
                // 應用自定義模型數據
                if (componentsJson.has("custom_model_data")) {
                    // 注釋掉因為 API 已改變
                    // int customModelData = componentsJson.get("custom_model_data").getAsInt();
                    // itemStack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(customModelData));
                }
                
                // 應用耐久度
                if (componentsJson.has("damage")) {
                    int damage = componentsJson.get("damage").getAsInt();
                    itemStack.set(DataComponentTypes.DAMAGE, damage);
                }
                
                // 應用最大耐久度
                if (componentsJson.has("max_damage")) {
                    int maxDamage = componentsJson.get("max_damage").getAsInt();
                    itemStack.set(DataComponentTypes.MAX_DAMAGE, maxDamage);
                }
                
                // 應用自定義名稱
                if (componentsJson.has("custom_name")) {
                    String customName = componentsJson.get("custom_name").getAsString();
                    itemStack.set(DataComponentTypes.CUSTOM_NAME, net.minecraft.text.Text.literal(customName));
                }
                
                // 應用描述
                if (componentsJson.has("lore")) {
                    List<String> loreStrings = GSON.fromJson(componentsJson.get("lore"), new TypeToken<List<String>>(){}.getType());
                    List<net.minecraft.text.Text> loreTexts = loreStrings.stream()
                            .map(net.minecraft.text.Text::literal)
                            .map(text -> (net.minecraft.text.Text) text)
                            .toList();
                    itemStack.set(DataComponentTypes.LORE, 
                            new net.minecraft.component.type.LoreComponent(loreTexts));
                }
                
                // 處理束包內容的向下兼容性
                if (componentsJson.has("bundle_contents") && itemStack.getItem().toString().contains("bundle")) {
                    JsonObject bundleJson = componentsJson.getAsJsonObject("bundle_contents");
                    if (bundleJson.has("items")) {
                        // 注釋掉因為 API 已改變
                        // List<String> itemStrings = GSON.fromJson(bundleJson.get("items"), new TypeToken<List<String>>(){}.getType());
                        // List<ItemStack> bundleItems = itemStrings.stream()
                        //         .map(FabricItemSerializer::deserializeItemStack)
                        //         .filter(stack -> !stack.isEmpty())
                        //         .toList();
                        // BundleContentsComponent.Builder builder = new BundleContentsComponent.Builder();
                        // bundleItems.forEach(builder::add);
                        // itemStack.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
                    }
                }
                
                // 處理容器內容（界伏盒等）
                if (componentsJson.has("container")) {
                    JsonObject containerJson = componentsJson.getAsJsonObject("container");
                    if (containerJson.has("items")) {
                        try {
                            JsonObject containerItems = containerJson.getAsJsonObject("items");
                            int containerSize = containerJson.has("size") ? containerJson.get("size").getAsInt() : 27;
                            
                            // 初始化DefaultedList
                            DefaultedList<ItemStack> stacks = DefaultedList.ofSize(containerSize, ItemStack.EMPTY);
                            
                            // 設置物品
                            containerItems.entrySet().forEach(entry -> {
                                try {
                                    int slot = Integer.parseInt(entry.getKey());
                                    if (slot < containerSize) {
                                        String itemJsonString = GSON.toJson(entry.getValue());
                                        ItemStack stack = deserializeItemStack(itemJsonString);
                                        if (slot < stacks.size()) {
                                            stacks.set(slot, stack);
                                        }
                                    }
                                } catch (NumberFormatException e) {
                                    ChococarsInventoryBridgeFabric.getLogger().warn("無效的容器槽位: " + entry.getKey());
                                }
                            });
                            
                            // 創建容器組件
                            itemStack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(stacks));
                            
                        } catch (Exception e) {
                            ChococarsInventoryBridgeFabric.getLogger().warn("反序列化容器內容失敗: " + e.getMessage());
                        }
                    }
                }
            }
            
            return itemStack;
            
        } catch (Exception e) {
            ChococarsInventoryBridgeFabric.getLogger().error("反序列化物品堆疊失敗", e);
            return ItemStack.EMPTY;
        }
    }
    
    public static String serializeInventory(Inventory inventory) {
        // 創建適配器陣列
        CommonItemSerializer.ItemStackProvider[] items = new CommonItemSerializer.ItemStackProvider[inventory.size()];
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                items[i] = new FabricItemStackProvider(stack);
            }
        }
        
        return CommonItemSerializer.serializeInventory(
            inventory.size(), 
            getCurrentVersion(), 
            getCurrentDataVersion(), 
            items
        );
    }
    
    public static void deserializeInventory(String data, Inventory inventory) {
        CommonItemSerializer.deserializeInventory(data, new FabricInventoryProvider(inventory));
    }
    
    /**
     * 序列化NBT格式的物品清單 (用於離線玩家檔案讀取)
     * 註：因 Fabric NBT API 兼容性問題暫時簡化
     */
    public static String serializeNbtList(Object nbtList) {
        // 暫時返回空背包，避免 NBT API 兼容性問題
        ChococarsInventoryBridgeFabric.getLogger().warn("NBT 序列化功能因 API 兼容性問題暫時簡化");
        
        JsonObject json = new JsonObject();
        json.addProperty("size", 41); // 預設玩家背包大小
        json.addProperty("minecraft_version", getCurrentVersion());
        json.addProperty("data_version", getCurrentDataVersion());
        json.add("items", new JsonObject()); // 空的物品清單
        
        return GSON.toJson(json);
    }
    
    /**
     * Fabric ItemStack 提供者實現
     */
    private static class FabricItemStackProvider implements CommonItemSerializer.ItemStackProvider {
        private final ItemStack itemStack;
        
        public FabricItemStackProvider(ItemStack itemStack) {
            this.itemStack = itemStack;
        }
        
        @Override
        public boolean isEmpty() {
            return itemStack.isEmpty();
        }
        
        @Override
        public String serialize() {
            return serializeItemStack(itemStack);
        }
    }
    
    /**
     * Fabric 背包提供者實現
     */
    private static class FabricInventoryProvider implements CommonItemSerializer.InventoryProvider {
        private final Inventory inventory;
        
        public FabricInventoryProvider(Inventory inventory) {
            this.inventory = inventory;
        }
        
        @Override
        public int size() {
            return inventory.size();
        }
        
        @Override
        public void setItem(int slot, String itemData) {
            try {
                ItemStack stack = deserializeItemStack(itemData);
                inventory.setStack(slot, stack);
            } catch (Exception e) {
                ChococarsInventoryBridgeFabric.getLogger().warn(String.format("無效的槽位號碼: %d", slot));
            }
        }
    }
}
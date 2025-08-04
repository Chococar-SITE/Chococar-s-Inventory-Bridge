package site.chococar.inventorybridge.fabric.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import site.chococar.inventorybridge.common.compatibility.ItemMappings;
import site.chococar.inventorybridge.fabric.ChococarsInventoryBridgeFabric;

import java.util.ArrayList;
import java.util.List;

public class FabricItemSerializer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CURRENT_VERSION = "1.21.8";
    private static final int CURRENT_DATA_VERSION = 4082;
    
    public static String serializeItemStack(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return null;
        }
        
        JsonObject json = new JsonObject();
        json.addProperty("id", Registries.ITEM.getId(itemStack.getItem()).toString());
        json.addProperty("count", itemStack.getCount());
        json.addProperty("minecraft_version", CURRENT_VERSION);
        json.addProperty("data_version", CURRENT_DATA_VERSION);
        
        // 序列化組件
        JsonObject componentsJson = new JsonObject();
        
        // 處理自定義模型數據
        if (itemStack.contains(DataComponentTypes.CUSTOM_MODEL_DATA)) {
            CustomModelDataComponent customModelData = itemStack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
            // 注釋掉因為 API 已改變
            // componentsJson.addProperty("custom_model_data", customModelData.getValue());
        }
        
        // 處理束包內容 (1.21.2+)
        if (itemStack.contains(DataComponentTypes.BUNDLE_CONTENTS)) {
            BundleContentsComponent bundleContents = itemStack.get(DataComponentTypes.BUNDLE_CONTENTS);
            JsonObject bundleJson = new JsonObject();
            List<String> items = new ArrayList<>();
            bundleContents.iterate().forEach(stack -> {
                String serialized = serializeItemStack(stack);
                if (serialized != null) {
                    items.add(serialized);
                }
            });
            bundleJson.add("items", GSON.toJsonTree(items));
            componentsJson.add("bundle_contents", bundleJson);
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
            if (!ItemMappings.isItemAvailableInVersion(itemId, CURRENT_VERSION)) {
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
                    int customModelData = componentsJson.get("custom_model_data").getAsInt();
                    // 注釋掉因為 API 已改變
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
                        List<String> itemStrings = GSON.fromJson(bundleJson.get("items"), new TypeToken<List<String>>(){}.getType());
                        List<ItemStack> bundleItems = itemStrings.stream()
                                .map(FabricItemSerializer::deserializeItemStack)
                                .filter(stack -> !stack.isEmpty())
                                .toList();
                        
                        // 注釋掉因為 API 已改變
                        // BundleContentsComponent.Builder builder = new BundleContentsComponent.Builder();
                        // bundleItems.forEach(builder::add);
                        // itemStack.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
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
        JsonObject json = new JsonObject();
        json.addProperty("size", inventory.size());
        json.addProperty("minecraft_version", CURRENT_VERSION);
        json.addProperty("data_version", CURRENT_DATA_VERSION);
        
        JsonObject itemsJson = new JsonObject();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                String serializedItem = serializeItemStack(stack);
                if (serializedItem != null) {
                    itemsJson.addProperty(String.valueOf(i), serializedItem);
                }
            }
        }
        json.add("items", itemsJson);
        
        return GSON.toJson(json);
    }
    
    public static void deserializeInventory(String data, Inventory inventory) {
        if (data == null || data.isEmpty()) {
            return;
        }
        
        try {
            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
            String version = json.has("minecraft_version") ? json.get("minecraft_version").getAsString() : "unknown";
            
            if (json.has("items")) {
                JsonObject itemsJson = json.getAsJsonObject("items");
                itemsJson.entrySet().forEach(entry -> {
                    try {
                        int slot = Integer.parseInt(entry.getKey());
                        if (slot < inventory.size()) {
                            ItemStack stack = deserializeItemStack(entry.getValue().getAsString());
                            inventory.setStack(slot, stack);
                        }
                    } catch (NumberFormatException e) {
                        ChococarsInventoryBridgeFabric.getLogger().warn(String.format("無效的槽位號碼: %s", entry.getKey()));
                    }
                });
            }
            
        } catch (Exception e) {
            ChococarsInventoryBridgeFabric.getLogger().error("反序列化背包失敗", e);
        }
    }
    
    /**
     * 序列化NBT格式的物品清單 (用於離線玩家檔案讀取)
     */
    public static String serializeNbtList(NbtList nbtList) {
        if (nbtList == null || nbtList.isEmpty()) {
            return "[]";
        }
        
        JsonObject json = new JsonObject();
        json.addProperty("size", 41); // 預設玩家背包大小 (9*4 + 5 裝備槽)
        json.addProperty("minecraft_version", CURRENT_VERSION);
        json.addProperty("data_version", CURRENT_DATA_VERSION);
        
        JsonObject itemsJson = new JsonObject();
        
        for (int i = 0; i < nbtList.size(); i++) {
            try {
                NbtCompound itemNbt = nbtList.getCompound(i);
                if (itemNbt != null && !itemNbt.isEmpty()) {
                    // 讀取槽位
                    int slot = itemNbt.getByte("Slot") & 255; // 無符號字節
                    
                    // 創建ItemStack並序列化
                    var optionalItemStack = ItemStack.fromNbt(ChococarsInventoryBridgeFabric.getCurrentRegistryManager(), itemNbt);
                    if (optionalItemStack.isPresent()) {
                        ItemStack itemStack = optionalItemStack.get();
                        if (!itemStack.isEmpty()) {
                            String serializedItem = serializeItemStack(itemStack);
                            if (serializedItem != null) {
                                itemsJson.addProperty(String.valueOf(slot), serializedItem);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                ChococarsInventoryBridgeFabric.getLogger().warn("序列化NBT物品失敗 (索引 " + i + "): " + e.getMessage());
            }
        }
        
        json.add("items", itemsJson);
        return GSON.toJson(json);
    }
}
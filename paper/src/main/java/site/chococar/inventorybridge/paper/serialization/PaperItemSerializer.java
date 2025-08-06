package site.chococar.inventorybridge.paper.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.BundleMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class PaperItemSerializer {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // 動態獲取版本信息
    private static String getCurrentVersion() {
        try {
            return org.bukkit.Bukkit.getMinecraftVersion();
        } catch (Exception e) {
            logger.warning("無法獲取 Minecraft 版本，使用預設值: " + e.getMessage());
            return "1.21.4";
        }
    }
    
    private static int getCurrentDataVersion() {
        try {
            String version = getCurrentVersion();
            
            // 根據版本號推斷數據版本
            if (version.startsWith("1.21.4")) {
                return 4071;
            } else if (version.startsWith("1.21.5")) {
                return 4073;
            } else if (version.startsWith("1.21.6")) {
                return 4076;
            } else if (version.startsWith("1.21.7")) {
                return 4079;
            } else if (version.startsWith("1.21.8")) {
                return 4082;
            } else if (version.startsWith("1.21")) {
                return 4071; // 預設為 1.21.4 的數據版本
            } else {
                logger.warning("未知的 Minecraft 版本: " + version + "，使用預設數據版本");
                return 4071;
            }
        } catch (Exception e) {
            logger.warning("無法獲取數據版本，使用預設值: " + e.getMessage());
            return 4071;
        }
    }
    private static final Logger logger = Logger.getLogger("ChococarsInventoryBridge");
    
    public static String serializeItemStack(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }
        
        JsonObject json = new JsonObject();
        json.addProperty("material", itemStack.getType().name());
        json.addProperty("amount", itemStack.getAmount());
        json.addProperty("minecraft_version", getCurrentVersion());
        json.addProperty("data_version", getCurrentDataVersion());
        
        // Handle item meta
        if (itemStack.hasItemMeta()) {
            JsonObject metaJson = new JsonObject();
            ItemMeta meta = itemStack.getItemMeta();
            
            // Custom model data
            if (meta.hasCustomModelData()) {
                metaJson.addProperty("custom_model_data", meta.getCustomModelData());
            }
            
            // Display name
            if (meta.hasDisplayName()) {
                metaJson.addProperty("display_name", meta.displayName().toString());
            }
            
            // Lore
            if (meta.hasLore()) {
                List<String> loreStrings = new ArrayList<>();
                for (net.kyori.adventure.text.Component component : meta.lore()) {
                    loreStrings.add(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component));
                }
                metaJson.add("lore", GSON.toJsonTree(loreStrings));
            }
            
            // Enchantments
            if (meta.hasEnchants()) {
                JsonObject enchantmentsJson = new JsonObject();
                for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                    enchantmentsJson.addProperty(entry.getKey().getKey().toString(), entry.getValue());
                }
                metaJson.add("enchantments", enchantmentsJson);
            }
            
            // Handle bundle contents (1.21.2+)
            if (meta instanceof BundleMeta bundleMeta) {
                if (bundleMeta.hasItems()) {
                    List<String> bundleItems = new ArrayList<>();
                    for (ItemStack bundleItem : bundleMeta.getItems()) {
                        String serializedItem = serializeItemStack(bundleItem);
                        if (serializedItem != null) {
                            bundleItems.add(serializedItem);
                        }
                    }
                    metaJson.add("bundle_items", GSON.toJsonTree(bundleItems));
                }
            }
            
            // Damage for damageable items
            if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
                if (damageable.hasDamage()) {
                    metaJson.addProperty("damage", damageable.getDamage());
                }
            }
            
            json.add("meta", metaJson);
        }
        
        return GSON.toJson(json);
    }
    
    public static ItemStack deserializeItemStack(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        
        try {
            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
            
            String materialName = json.get("material").getAsString();
            int amount = json.get("amount").getAsInt();
            
            // Version compatibility check
            String version = json.has("minecraft_version") ? json.get("minecraft_version").getAsString() : "unknown";
            
            // Handle material compatibility
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                // Try to find compatible material for older versions
                material = findCompatibleMaterial(materialName, version);
                if (material == null) {
                    logger.warning("Unknown material: " + materialName + " from version " + version);
                    return null;
                }
            }
            
            ItemStack itemStack = new ItemStack(material, amount);
            
            // Apply meta if present
            if (json.has("meta")) {
                JsonObject metaJson = json.getAsJsonObject("meta");
                ItemMeta meta = itemStack.getItemMeta();
                
                if (meta != null) {
                    // Custom model data
                    if (metaJson.has("custom_model_data")) {
                        meta.setCustomModelData(metaJson.get("custom_model_data").getAsInt());
                    }
                    
                    // Display name
                    if (metaJson.has("display_name")) {
                        meta.displayName(net.kyori.adventure.text.Component.text(metaJson.get("display_name").getAsString()));
                    }
                    
                    // Lore
                    if (metaJson.has("lore")) {
                        List<String> loreStrings = GSON.fromJson(metaJson.get("lore"), 
                            new com.google.gson.reflect.TypeToken<List<String>>(){}.getType());
                        List<net.kyori.adventure.text.Component> loreComponents = new ArrayList<>();
                        for (String loreString : loreStrings) {
                            loreComponents.add(net.kyori.adventure.text.Component.text(loreString));
                        }
                        meta.lore(loreComponents);
                    }
                    
                    // Enchantments
                    if (metaJson.has("enchantments")) {
                        JsonObject enchantmentsJson = metaJson.getAsJsonObject("enchantments");
                        for (String enchantKey : enchantmentsJson.keySet()) {
                            try {
                                org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.fromString(enchantKey);
                                io.papermc.paper.registry.RegistryAccess registryAccess = io.papermc.paper.registry.RegistryAccess.registryAccess();
                                org.bukkit.Registry<Enchantment> enchantmentRegistry = registryAccess.getRegistry(io.papermc.paper.registry.RegistryKey.ENCHANTMENT);
                                Enchantment enchantment = enchantmentRegistry.get(key);
                                if (enchantment != null) {
                                    int level = enchantmentsJson.get(enchantKey).getAsInt();
                                    meta.addEnchant(enchantment, level, true);
                                }
                            } catch (Exception e) {
                                logger.warning("無法載入附魔: " + enchantKey + " - " + e.getMessage());
                            }
                        }
                    }
                    
                    // Damage
                    if (metaJson.has("damage") && meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
                        damageable.setDamage(metaJson.get("damage").getAsInt());
                    }
                    
                    // Bundle items
                    if (metaJson.has("bundle_items") && meta instanceof BundleMeta bundleMeta) {
                        List<String> bundleItemStrings = GSON.fromJson(metaJson.get("bundle_items"), 
                            new com.google.gson.reflect.TypeToken<List<String>>(){}.getType());
                        List<ItemStack> bundleItems = new ArrayList<>();
                        for (String itemString : bundleItemStrings) {
                            ItemStack bundleItem = deserializeItemStack(itemString);
                            if (bundleItem != null) {
                                bundleItems.add(bundleItem);
                            }
                        }
                        bundleMeta.setItems(bundleItems);
                    }
                    
                    itemStack.setItemMeta(meta);
                }
            }
            
            return itemStack;
            
        } catch (Exception e) {
            logger.severe("Failed to deserialize item stack: " + e.getMessage());
            return null;
        }
    }
    
    private static Material findCompatibleMaterial(String materialName, String version) {
        // Handle common material changes between versions
        return switch (materialName) {
            // Handle copper variants
            case "COPPER_BULB" -> Material.matchMaterial("COPPER_BLOCK");
            case "EXPOSED_COPPER_BULB" -> Material.matchMaterial("EXPOSED_COPPER");
            case "WEATHERED_COPPER_BULB" -> Material.matchMaterial("WEATHERED_COPPER");
            case "OXIDIZED_COPPER_BULB" -> Material.matchMaterial("OXIDIZED_COPPER");
            
            // Handle trial chamber items
            case "TRIAL_KEY" -> Material.matchMaterial("GOLD_INGOT");
            case "OMINOUS_BOTTLE" -> Material.matchMaterial("GLASS_BOTTLE");
            case "WIND_CHARGE" -> Material.matchMaterial("SNOWBALL");
            
            // Handle bundle (if not available in older versions)
            case "BUNDLE" -> Material.matchMaterial("LEATHER");
            
            // Handle resin items (1.21.4+)
            case "RESIN_CLUMP" -> Material.matchMaterial("SLIME_BALL");
            case "RESIN_BRICK" -> Material.matchMaterial("BRICK");
            
            default -> null;
        };
    }
    
    public static String serializeInventory(Inventory inventory) {
        JsonObject json = new JsonObject();
        json.addProperty("size", inventory.getSize());
        json.addProperty("minecraft_version", getCurrentVersion());
        json.addProperty("data_version", getCurrentDataVersion());
        
        JsonObject itemsJson = new JsonObject();
        ItemStack[] contents = inventory.getContents();
        
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                String serializedItem = serializeItemStack(contents[i]);
                if (serializedItem != null) {
                    itemsJson.addProperty(String.valueOf(i), serializedItem);
                }
            }
        }
        
        json.add("items", itemsJson);
        return GSON.toJson(json);
    }
    
    public static ItemStack[] deserializeInventory(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        
        try {
            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
            int size = json.get("size").getAsInt();
            ItemStack[] items = new ItemStack[size];
            
            if (json.has("items")) {
                JsonObject itemsJson = json.getAsJsonObject("items");
                for (String slotStr : itemsJson.keySet()) {
                    try {
                        int slot = Integer.parseInt(slotStr);
                        if (slot < size) {
                            String itemData = itemsJson.get(slotStr).getAsString();
                            ItemStack item = deserializeItemStack(itemData);
                            if (item != null) {
                                items[slot] = item;
                            }
                        }
                    } catch (NumberFormatException e) {
                        logger.warning("Invalid slot number: " + slotStr);
                    }
                }
            }
            
            return items;
            
        } catch (Exception e) {
            logger.severe("Failed to deserialize inventory: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 序列化ItemStack陣列 (用於NBT讀取)
     */
    public static String serializeInventoryArray(ItemStack[] items) {
        if (items == null) {
            return "[]";
        }
        
        JsonObject json = new JsonObject();
        json.addProperty("size", items.length);
        json.addProperty("minecraft_version", getCurrentVersion());
        json.addProperty("data_version", getCurrentDataVersion());
        
        JsonObject itemsJson = new JsonObject();
        
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null && items[i].getType() != Material.AIR) {
                String serializedItem = serializeItemStack(items[i]);
                if (serializedItem != null) {
                    itemsJson.addProperty(String.valueOf(i), serializedItem);
                }
            }
        }
        
        json.add("items", itemsJson);
        return GSON.toJson(json);
    }
}
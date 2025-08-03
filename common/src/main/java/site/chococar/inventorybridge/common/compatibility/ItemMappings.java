package site.chococar.inventorybridge.common.compatibility;

import java.util.Map;
import java.util.HashMap;

/**
 * 物品版本兼容性映射
 */
public class ItemMappings {
    
    // 物品ID映射表，用於版本兼容性
    private static final Map<String, String> ITEM_MAPPINGS = new HashMap<>();
    
    static {
        // 1.21.2+ Bundle 物品向下兼容
        ITEM_MAPPINGS.put("minecraft:bundle", "minecraft:leather");
        
        // 1.21.4+ 樹脂物品向下兼容
        ITEM_MAPPINGS.put("minecraft:resin_clump", "minecraft:slime_ball");
        ITEM_MAPPINGS.put("minecraft:resin_brick", "minecraft:brick");
        
        // 銅製品向下兼容
        ITEM_MAPPINGS.put("minecraft:copper_bulb", "minecraft:copper_block");
        ITEM_MAPPINGS.put("minecraft:exposed_copper_bulb", "minecraft:exposed_copper");
        ITEM_MAPPINGS.put("minecraft:weathered_copper_bulb", "minecraft:weathered_copper");
        ITEM_MAPPINGS.put("minecraft:oxidized_copper_bulb", "minecraft:oxidized_copper");
        ITEM_MAPPINGS.put("minecraft:waxed_copper_bulb", "minecraft:waxed_copper_block");
        ITEM_MAPPINGS.put("minecraft:waxed_exposed_copper_bulb", "minecraft:waxed_exposed_copper");
        ITEM_MAPPINGS.put("minecraft:waxed_weathered_copper_bulb", "minecraft:waxed_weathered_copper");
        ITEM_MAPPINGS.put("minecraft:waxed_oxidized_copper_bulb", "minecraft:waxed_oxidized_copper");
        
        // 銅門和陷阱門
        ITEM_MAPPINGS.put("minecraft:copper_door", "minecraft:iron_door");
        ITEM_MAPPINGS.put("minecraft:exposed_copper_door", "minecraft:iron_door");
        ITEM_MAPPINGS.put("minecraft:weathered_copper_door", "minecraft:iron_door");
        ITEM_MAPPINGS.put("minecraft:oxidized_copper_door", "minecraft:iron_door");
        
        ITEM_MAPPINGS.put("minecraft:copper_trapdoor", "minecraft:iron_trapdoor");
        ITEM_MAPPINGS.put("minecraft:exposed_copper_trapdoor", "minecraft:iron_trapdoor");
        ITEM_MAPPINGS.put("minecraft:weathered_copper_trapdoor", "minecraft:iron_trapdoor");
        ITEM_MAPPINGS.put("minecraft:oxidized_copper_trapdoor", "minecraft:iron_trapdoor");
        
        // 銅格柵
        ITEM_MAPPINGS.put("minecraft:copper_grate", "minecraft:copper_block");
        ITEM_MAPPINGS.put("minecraft:exposed_copper_grate", "minecraft:exposed_copper");
        ITEM_MAPPINGS.put("minecraft:weathered_copper_grate", "minecraft:weathered_copper");
        ITEM_MAPPINGS.put("minecraft:oxidized_copper_grate", "minecraft:oxidized_copper");
        
        // 試煉密室物品
        ITEM_MAPPINGS.put("minecraft:trial_key", "minecraft:gold_ingot");
        ITEM_MAPPINGS.put("minecraft:ominous_trial_key", "minecraft:gold_ingot");
        ITEM_MAPPINGS.put("minecraft:ominous_bottle", "minecraft:glass_bottle");
        ITEM_MAPPINGS.put("minecraft:wind_charge", "minecraft:snowball");
        
        // 合成器
        ITEM_MAPPINGS.put("minecraft:crafter", "minecraft:crafting_table");
        
        // 淺橡木物品 (1.21.4+)
        ITEM_MAPPINGS.put("minecraft:pale_oak_log", "minecraft:oak_log");
        ITEM_MAPPINGS.put("minecraft:pale_oak_wood", "minecraft:oak_wood");
        ITEM_MAPPINGS.put("minecraft:pale_oak_planks", "minecraft:oak_planks");
        ITEM_MAPPINGS.put("minecraft:pale_oak_stairs", "minecraft:oak_stairs");
        ITEM_MAPPINGS.put("minecraft:pale_oak_slab", "minecraft:oak_slab");
        ITEM_MAPPINGS.put("minecraft:pale_oak_fence", "minecraft:oak_fence");
        ITEM_MAPPINGS.put("minecraft:pale_oak_fence_gate", "minecraft:oak_fence_gate");
        ITEM_MAPPINGS.put("minecraft:pale_oak_door", "minecraft:oak_door");
        ITEM_MAPPINGS.put("minecraft:pale_oak_trapdoor", "minecraft:oak_trapdoor");
        ITEM_MAPPINGS.put("minecraft:pale_oak_button", "minecraft:oak_button");
        ITEM_MAPPINGS.put("minecraft:pale_oak_pressure_plate", "minecraft:oak_pressure_plate");
        ITEM_MAPPINGS.put("minecraft:pale_oak_sign", "minecraft:oak_sign");
        ITEM_MAPPINGS.put("minecraft:pale_oak_hanging_sign", "minecraft:oak_hanging_sign");
        ITEM_MAPPINGS.put("minecraft:pale_oak_boat", "minecraft:oak_boat");
        ITEM_MAPPINGS.put("minecraft:pale_oak_chest_boat", "minecraft:oak_chest_boat");
        ITEM_MAPPINGS.put("minecraft:pale_oak_sapling", "minecraft:oak_sapling");
        ITEM_MAPPINGS.put("minecraft:pale_oak_leaves", "minecraft:oak_leaves");
        
        // 嘎嘎怪相關
        ITEM_MAPPINGS.put("minecraft:creaking_heart", "minecraft:oak_log");
        ITEM_MAPPINGS.put("minecraft:creaking_spawn_egg", "minecraft:zombie_spawn_egg");
        
        // 1.21.6+ 新物品
        ITEM_MAPPINGS.put("minecraft:dried_ghast", "minecraft:soul_sand");
        ITEM_MAPPINGS.put("minecraft:white_harness", "minecraft:leather");
        ITEM_MAPPINGS.put("minecraft:orange_harness", "minecraft:leather");
        ITEM_MAPPINGS.put("minecraft:magenta_harness", "minecraft:leather");
        ITEM_MAPPINGS.put("minecraft:light_blue_harness", "minecraft:leather");
        ITEM_MAPPINGS.put("minecraft:yellow_harness", "minecraft:leather");
        ITEM_MAPPINGS.put("minecraft:lime_harness", "minecraft:leather");
        ITEM_MAPPINGS.put("minecraft:pink_harness", "minecraft:leather");
        ITEM_MAPPINGS.put("minecraft:gray_harness", "minecraft:leather");
        ITEM_MAPPINGS.put("minecraft:light_gray_harness", "minecraft:leather");
        ITEM_MAPPINGS.put("minecraft:cyan_harness", "minecraft:leather");
        ITEM_MAPPINGS.put("minecraft:purple_harness", "minecraft:leather");
        ITEM_MAPPINGS.put("minecraft:blue_harness", "minecraft:leather");
        ITEM_MAPPINGS.put("minecraft:brown_harness", "minecraft:leather");
        ITEM_MAPPINGS.put("minecraft:green_harness", "minecraft:leather");
        ITEM_MAPPINGS.put("minecraft:red_harness", "minecraft:leather");
        ITEM_MAPPINGS.put("minecraft:black_harness", "minecraft:leather");
        ITEM_MAPPINGS.put("minecraft:music_disc_tears", "minecraft:music_disc_13");
        
        // 1.21.7+ 新物品
        ITEM_MAPPINGS.put("minecraft:music_disc_lava_chicken", "minecraft:music_disc_13");
    }
    
    /**
     * 獲取物品映射
     * @param itemId 原始物品ID
     * @return 映射後的物品ID，如果沒有映射則返回原始ID
     */
    public static String getCompatibleItem(String itemId) {
        return ITEM_MAPPINGS.getOrDefault(itemId, itemId);
    }
    
    /**
     * 檢查物品是否需要版本轉換
     * @param itemId 物品ID
     * @param targetVersion 目標版本
     * @return 如果需要轉換則返回true
     */
    public static boolean needsVersionConversion(String itemId, String targetVersion) {
        if (isVersionOlderThan(targetVersion, "1.21.2")) {
            if (itemId.contains("bundle")) {
                return true;
            }
        }
        
        if (isVersionOlderThan(targetVersion, "1.21.4")) {
            if (itemId.contains("resin") || itemId.contains("pale_oak") || itemId.contains("creaking")) {
                return true;
            }
        }
        
        if (isVersionOlderThan(targetVersion, "1.21.6")) {
            if (itemId.contains("dried_ghast") || itemId.contains("harness") || itemId.equals("minecraft:music_disc_tears")) {
                return true;
            }
        }
        
        if (isVersionOlderThan(targetVersion, "1.21.7")) {
            if (itemId.equals("minecraft:music_disc_lava_chicken")) {
                return true;
            }
        }
        
        return ITEM_MAPPINGS.containsKey(itemId);
    }
    
    /**
     * 檢查物品在指定版本中是否可用
     * @param itemId 物品ID
     * @param version 版本號
     * @return 如果可用則返回true
     */
    public static boolean isItemAvailableInVersion(String itemId, String version) {
        if (isVersionOlderThan(version, "1.21.2") && itemId.contains("bundle")) {
            return false;
        }
        
        if (isVersionOlderThan(version, "1.21.4")) {
            if (itemId.contains("resin") || itemId.contains("pale_oak") || itemId.contains("creaking")) {
                return false;
            }
        }
        
        if (isVersionOlderThan(version, "1.21.6")) {
            if (itemId.contains("dried_ghast") || itemId.contains("harness") || itemId.equals("minecraft:music_disc_tears")) {
                return false;
            }
        }
        
        if (isVersionOlderThan(version, "1.21.7")) {
            if (itemId.equals("minecraft:music_disc_lava_chicken")) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 比較版本號
     * @param version1 版本1
     * @param version2 版本2
     * @return 如果版本1小於版本2則返回true
     */
    public static boolean isVersionOlderThan(String version1, String version2) {
        try {
            String[] v1Parts = version1.replace("1.21.", "").split("\\.");
            String[] v2Parts = version2.replace("1.21.", "").split("\\.");
            
            int v1Minor = Integer.parseInt(v1Parts[0]);
            int v2Minor = Integer.parseInt(v2Parts[0]);
            
            return v1Minor < v2Minor;
        } catch (Exception e) {
            return false;
        }
    }
}
package site.chococar.inventorybridge.paper.sync;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import site.chococar.inventorybridge.paper.config.PaperConfigManager;
import site.chococar.inventorybridge.paper.database.PaperDatabaseManager;
import site.chococar.inventorybridge.paper.database.PaperInventoryData;
import site.chococar.inventorybridge.paper.serialization.PaperItemSerializer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class PaperInventorySyncManager {
    private final PaperDatabaseManager databaseManager;
    private final Logger logger;
    private final Map<UUID, Long> lastSyncTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> syncInProgress = new ConcurrentHashMap<>();
    
    public PaperInventorySyncManager(PaperDatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.logger = Logger.getLogger("ChococarsInventoryBridge");
    }
    
    public void onPlayerJoin(Player player) {
        // Auto-load disabled for safety to prevent timing issues and item loss
        logger.info("Player " + player.getName() + " joined - auto-load disabled for safety. Use '/ib sync load' to manually load inventory.");
    }
    
    public void onPlayerLeave(Player player) {
        // Auto-save disabled for safety to prevent timing issues and item loss
        logger.info("Player " + player.getName() + " left - auto-save disabled for safety. Use '/ib sync save' to manually save inventory.");
    }
    
    public void manualSync(Player player, boolean save) {
        UUID playerUuid = player.getUniqueId();
        PaperConfigManager config = getConfigManager();
        
        if (syncInProgress.getOrDefault(playerUuid, false)) {
            logger.warning("Sync already in progress for player " + player.getName());
            return;
        }
        
        syncInProgress.put(playerUuid, true);
        
        CompletableFuture.runAsync(() -> {
            try {
                if (save) {
                    savePlayerInventory(player);
                } else {
                    loadPlayerInventory(player);
                }
                databaseManager.logSync(playerUuid, config.getServerId(), "MANUAL", "SUCCESS", null);
                logger.info("Manual " + (save ? "save" : "load") + " completed for player " + player.getName());
            } catch (Exception e) {
                databaseManager.logSync(playerUuid, config.getServerId(), "MANUAL", "FAILED", e.getMessage());
                logger.severe("Manual " + (save ? "save" : "load") + " failed for player " + player.getName() + ": " + e.getMessage());
            } finally {
                syncInProgress.put(playerUuid, false);
                lastSyncTimes.put(playerUuid, System.currentTimeMillis());
            }
        });
    }
    
    private void savePlayerInventory(Player player) {
        PaperConfigManager config = getConfigManager();
        
        // Serialize main inventory
        String inventoryData = PaperItemSerializer.serializeInventory(player.getInventory());
        
        // Serialize ender chest if enabled
        String enderChestData = null;
        if (config.isSyncEnderChest()) {
            enderChestData = PaperItemSerializer.serializeInventory(player.getEnderChest());
        }
        
        // Get experience data
        int experience = config.isSyncExperience() ? player.getTotalExperience() : 0;
        int experienceLevel = config.isSyncExperience() ? player.getLevel() : 0;
        
        // Get health and hunger data
        double health = config.isSyncHealth() ? player.getHealth() : 20.0;
        int hunger = config.isSyncHunger() ? player.getFoodLevel() : 20;
        
        databaseManager.saveInventory(
                player.getUniqueId(),
                config.getServerId(),
                inventoryData,
                enderChestData,
                experience,
                experienceLevel,
                health,
                hunger,
                config.getMinecraftVersion(),
                4082 // Current data version for 1.21.8
        );
    }
    
    private void loadPlayerInventory(Player player) {
        PaperConfigManager config = getConfigManager();
        
        PaperInventoryData data = databaseManager.loadInventory(player.getUniqueId(), config.getServerId());
        
        if (data == null) {
            logger.info("No saved inventory found for player " + player.getName());
            return;
        }
        
        // Load main inventory
        player.getInventory().clear();
        ItemStack[] items = PaperItemSerializer.deserializeInventory(data.inventoryData());
        if (items != null) {
            player.getInventory().setContents(items);
        }
        
        // Load ender chest if enabled and data exists
        if (config.isSyncEnderChest() && data.enderChestData() != null) {
            ItemStack[] enderItems = PaperItemSerializer.deserializeInventory(data.enderChestData());
            if (enderItems != null) {
                player.getEnderChest().setContents(enderItems);
            }
        }
        
        // Load experience if enabled
        if (config.isSyncExperience()) {
            player.setLevel(data.experienceLevel());
            player.setTotalExperience(data.experience());
        }
        
        // Load health if enabled
        if (config.isSyncHealth()) {
            player.setHealth(Math.min(data.health(), player.getMaxHealth()));
        }
        
        // Load hunger if enabled
        if (config.isSyncHunger()) {
            player.setFoodLevel(data.hunger());
        }
        
        // Update player
        player.updateInventory();
    }
    
    private PaperConfigManager getConfigManager() {
        return site.chococar.inventorybridge.paper.ChococarsInventoryBridgePlugin.getInstance().getConfigManager();
    }
    
    public boolean isSyncInProgress(UUID playerUuid) {
        return syncInProgress.getOrDefault(playerUuid, false);
    }
    
    public long getLastSyncTime(UUID playerUuid) {
        return lastSyncTimes.getOrDefault(playerUuid, 0L);
    }
}
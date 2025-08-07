package site.chococar.inventorybridge.paper.adapter;

import org.bukkit.entity.Player;
import site.chococar.inventorybridge.common.adapter.InventoryAdapter;
import site.chococar.inventorybridge.common.adapter.PlayerAdapter;
import site.chococar.inventorybridge.paper.serialization.PaperItemSerializer;

import java.util.UUID;

/**
 * Paper版本的玩家適配器實現
 */
public class PaperPlayerAdapter implements PlayerAdapter {
    private final Player player;
    
    public PaperPlayerAdapter(Player player) {
        this.player = player;
    }
    
    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }
    
    @Override
    public String getName() {
        return player.getName();
    }
    
    @Override
    public InventoryAdapter getInventory() {
        return new PaperInventoryAdapter(player.getInventory());
    }
    
    @Override
    public InventoryAdapter getEnderChest() {
        return new PaperInventoryAdapter(player.getEnderChest());
    }
    
    @Override
    public int getTotalExperience() {
        return player.getTotalExperience();
    }
    
    @Override
    public int getLevel() {
        return player.getLevel();
    }
    
    @Override
    public double getHealth() {
        return player.getHealth();
    }
    
    @Override
    public int getFoodLevel() {
        return player.getFoodLevel();
    }
    
    @Override
    public void setHealth(double health) {
        player.setHealth(health);
    }
    
    @Override
    public void setFoodLevel(int foodLevel) {
        player.setFoodLevel(foodLevel);
    }
    
    @Override
    public void setExperience(int totalExperience, int level) {
        player.setLevel(level);
        player.setTotalExperience(totalExperience);
    }
    
    @Override
    public void updateInventory() {
        player.updateInventory();
    }
    
    /**
     * Paper版本的背包適配器實現
     */
    private static class PaperInventoryAdapter implements InventoryAdapter {
        private final org.bukkit.inventory.Inventory inventory;
        
        public PaperInventoryAdapter(org.bukkit.inventory.Inventory inventory) {
            this.inventory = inventory;
        }
        
        @Override
        public int size() {
            return inventory.getSize();
        }
        
        @Override
        public void clear() {
            inventory.clear();
        }
        
        @Override
        public String serialize() {
            return PaperItemSerializer.serializeInventory(inventory);
        }
        
        @Override
        public void deserialize(String data) {
            org.bukkit.inventory.ItemStack[] items = PaperItemSerializer.deserializeInventory(data);
            if (items != null) {
                inventory.setContents(items);
            }
        }
    }
}
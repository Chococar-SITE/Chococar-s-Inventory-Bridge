package site.chococar.inventorybridge.fabric.adapter;

import net.minecraft.server.network.ServerPlayerEntity;
import site.chococar.inventorybridge.common.adapter.InventoryAdapter;
import site.chococar.inventorybridge.common.adapter.PlayerAdapter;
import site.chococar.inventorybridge.fabric.serialization.FabricItemSerializer;

import java.util.UUID;

/**
 * Fabric版本的玩家適配器實現
 */
public class FabricPlayerAdapter implements PlayerAdapter {
    private final ServerPlayerEntity player;
    
    public FabricPlayerAdapter(ServerPlayerEntity player) {
        this.player = player;
    }
    
    @Override
    public UUID getUniqueId() {
        return player.getUuid();
    }
    
    @Override
    public String getName() {
        return player.getName().getString();
    }
    
    @Override
    public InventoryAdapter getInventory() {
        return new FabricInventoryAdapter(player.getInventory());
    }
    
    @Override
    public InventoryAdapter getEnderChest() {
        return new FabricInventoryAdapter(player.getEnderChestInventory());
    }
    
    @Override
    public int getTotalExperience() {
        return player.totalExperience;
    }
    
    @Override
    public int getLevel() {
        return player.experienceLevel;
    }
    
    @Override
    public double getHealth() {
        return player.getHealth();
    }
    
    @Override
    public int getFoodLevel() {
        return player.getHungerManager().getFoodLevel();
    }
    
    @Override
    public void setHealth(double health) {
        player.setHealth((float) health);
    }
    
    @Override
    public void setFoodLevel(int foodLevel) {
        player.getHungerManager().setFoodLevel(foodLevel);
    }
    
    @Override
    public void setExperience(int totalExperience, int level) {
        player.setExperienceLevel(level);
        player.setExperiencePoints(totalExperience);
    }
    
    @Override
    public void updateInventory() {
        player.playerScreenHandler.syncState();
    }
    
    /**
     * Fabric版本的背包適配器實現
     */
    private static class FabricInventoryAdapter implements InventoryAdapter {
        private final net.minecraft.inventory.Inventory inventory;
        
        public FabricInventoryAdapter(net.minecraft.inventory.Inventory inventory) {
            this.inventory = inventory;
        }
        
        @Override
        public int size() {
            return inventory.size();
        }
        
        @Override
        public void clear() {
            inventory.clear();
        }
        
        @Override
        public String serialize() {
            return FabricItemSerializer.serializeInventory(inventory);
        }
        
        @Override
        public void deserialize(String data) {
            FabricItemSerializer.deserializeInventory(data, inventory);
        }
    }
}
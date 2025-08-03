package site.chococar.inventorybridge.paper.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import site.chococar.inventorybridge.paper.ChococarsInventoryBridgePlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InventoryBridgeCommand implements CommandExecutor, TabCompleter {
    private final ChococarsInventoryBridgePlugin plugin;
    
    public InventoryBridgeCommand(ChococarsInventoryBridgePlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("chococars.inventorybridge.reload")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                    return true;
                }
                
                plugin.getConfigManager().reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully!");
                return true;
            }
            
            case "sync" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                    return true;
                }
                
                if (!sender.hasPermission("chococars.inventorybridge.sync")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /inventorybridge sync <load|save>");
                    return true;
                }
                
                boolean save = args[1].equalsIgnoreCase("save");
                boolean load = args[1].equalsIgnoreCase("load");
                
                if (!save && !load) {
                    sender.sendMessage(ChatColor.RED + "Usage: /inventorybridge sync <load|save>");
                    return true;
                }
                
                if (plugin.getSyncManager().isSyncInProgress(player.getUniqueId())) {
                    sender.sendMessage(ChatColor.YELLOW + "Sync already in progress, please wait...");
                    return true;
                }
                
                plugin.getSyncManager().manualSync(player, save);
                sender.sendMessage(ChatColor.GREEN + "Manual " + (save ? "save" : "load") + " initiated!");
                return true;
            }
            
            case "status" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                    return true;
                }
                
                if (!sender.hasPermission("chococars.inventorybridge.sync")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                    return true;
                }
                
                boolean inProgress = plugin.getSyncManager().isSyncInProgress(player.getUniqueId());
                long lastSync = plugin.getSyncManager().getLastSyncTime(player.getUniqueId());
                
                sender.sendMessage(ChatColor.GOLD + "=== Inventory Bridge Status ===");
                sender.sendMessage(ChatColor.YELLOW + "Sync in progress: " + (inProgress ? ChatColor.RED + "Yes" : ChatColor.GREEN + "No"));
                
                if (lastSync > 0) {
                    long timeSince = (System.currentTimeMillis() - lastSync) / 1000;
                    sender.sendMessage(ChatColor.YELLOW + "Last sync: " + ChatColor.WHITE + timeSince + " seconds ago");
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Last sync: " + ChatColor.WHITE + "Never");
                }
                
                return true;
            }
            
            case "info" -> {
                sender.sendMessage(ChatColor.GOLD + "=== Chococar's Inventory Bridge ===");
                sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
                sender.sendMessage(ChatColor.YELLOW + "Author: " + ChatColor.WHITE + "chococar.site");
                sender.sendMessage(ChatColor.YELLOW + "Minecraft Version: " + ChatColor.WHITE + plugin.getConfigManager().getMinecraftVersion());
                sender.sendMessage(ChatColor.YELLOW + "Server ID: " + ChatColor.WHITE + plugin.getConfigManager().getServerId());
                return true;
            }
            
            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Chococar's Inventory Bridge Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/ib reload" + ChatColor.WHITE + " - Reload the configuration");
        sender.sendMessage(ChatColor.YELLOW + "/ib sync <load|save>" + ChatColor.WHITE + " - Manually sync inventory");
        sender.sendMessage(ChatColor.YELLOW + "/ib status" + ChatColor.WHITE + " - Check sync status");
        sender.sendMessage(ChatColor.YELLOW + "/ib info" + ChatColor.WHITE + " - Show plugin information");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("reload", "sync", "status", "info");
            for (String subcommand : subcommands) {
                if (subcommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subcommand);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("sync")) {
            List<String> syncOptions = Arrays.asList("load", "save");
            for (String option : syncOptions) {
                if (option.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(option);
                }
            }
        }
        
        return completions;
    }
}
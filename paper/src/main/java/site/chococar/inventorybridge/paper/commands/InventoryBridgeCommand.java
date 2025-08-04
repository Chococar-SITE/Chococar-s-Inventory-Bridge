package site.chococar.inventorybridge.paper.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import site.chococar.inventorybridge.common.Constants;
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
                    sender.sendMessage(Component.text("You don't have permission to use this command!").color(NamedTextColor.RED));
                    return true;
                }
                
                sender.sendMessage(Component.text("Reloading configuration...").color(NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("This operation is running asynchronously to prevent server lag.").color(NamedTextColor.GRAY));
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        boolean success = plugin.reloadPluginConfig();
                        
                        // Switch back to main thread for sending messages
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (success) {
                                    sender.sendMessage(Component.text("✅ Configuration reloaded successfully!").color(NamedTextColor.GREEN));
                                    sender.sendMessage(Component.text("Database connection updated, all features restored.").color(NamedTextColor.GREEN));
                                } else {
                                    sender.sendMessage(Component.text("❌ Configuration reload failed!").color(NamedTextColor.RED));
                                    sender.sendMessage(Component.text("Check console for detailed error information.").color(NamedTextColor.RED));
                                }
                            }
                        }.runTask(plugin);
                    }
                }.runTaskAsynchronously(plugin);
                return true;
            }
            
            case "reconnect" -> {
                if (!sender.hasPermission("chococars.inventorybridge.reload")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command!").color(NamedTextColor.RED));
                    return true;
                }
                
                sender.sendMessage(Component.text("Reconnecting to database...").color(NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("This operation is running asynchronously to prevent server lag.").color(NamedTextColor.GRAY));
                
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        boolean success = plugin.reconnectDatabase();
                        
                        // Switch back to main thread for sending messages
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (success) {
                                    sender.sendMessage(Component.text("✅ Database reconnected successfully!").color(NamedTextColor.GREEN));
                                    sender.sendMessage(Component.text("All inventory sync features are now active.").color(NamedTextColor.GREEN));
                                } else {
                                    sender.sendMessage(Component.text("❌ Database reconnection failed!").color(NamedTextColor.RED));
                                    sender.sendMessage(Component.text("Please check database settings and connection.").color(NamedTextColor.RED));
                                }
                            }
                        }.runTask(plugin);
                    }
                }.runTaskAsynchronously(plugin);
                return true;
            }
            
            case "sync" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("This command can only be used by players!").color(NamedTextColor.RED));
                    return true;
                }
                
                if (!sender.hasPermission("chococars.inventorybridge.sync")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command!").color(NamedTextColor.RED));
                    return true;
                }
                
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /inventorybridge sync <load|save>").color(NamedTextColor.RED));
                    return true;
                }
                
                boolean save = args[1].equalsIgnoreCase("save");
                boolean load = args[1].equalsIgnoreCase("load");
                
                if (!save && !load) {
                    sender.sendMessage(Component.text("Usage: /inventorybridge sync <load|save>").color(NamedTextColor.RED));
                    return true;
                }
                
                if (plugin.getSyncManager().isSyncInProgress(player.getUniqueId())) {
                    sender.sendMessage(Component.text("Sync already in progress, please wait...").color(NamedTextColor.YELLOW));
                    return true;
                }
                
                sender.sendMessage(Component.text("Manual " + (save ? "save" : "load") + " initiated!").color(NamedTextColor.GREEN));
                sender.sendMessage(Component.text("This operation is running asynchronously to prevent server lag.").color(NamedTextColor.GRAY));
                
                plugin.getSyncManager().manualSync(player, save);
                return true;
            }
            
            case "status" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("This command can only be used by players!").color(NamedTextColor.RED));
                    return true;
                }
                
                if (!sender.hasPermission("chococars.inventorybridge.sync")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command!").color(NamedTextColor.RED));
                    return true;
                }
                
                boolean inProgress = plugin.getSyncManager().isSyncInProgress(player.getUniqueId());
                long lastSync = plugin.getSyncManager().getLastSyncTime(player.getUniqueId());
                
                sender.sendMessage(Component.text("=== Inventory Bridge Status ===").color(NamedTextColor.GOLD));
                sender.sendMessage(Component.text("Sync in progress: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(inProgress ? "Yes" : "No").color(inProgress ? NamedTextColor.RED : NamedTextColor.GREEN)));
                
                if (lastSync > 0) {
                    long timeSince = (System.currentTimeMillis() - lastSync) / 1000;
                    sender.sendMessage(Component.text("Last sync: ").color(NamedTextColor.YELLOW)
                        .append(Component.text(timeSince + " seconds ago").color(NamedTextColor.WHITE)));
                } else {
                    sender.sendMessage(Component.text("Last sync: ").color(NamedTextColor.YELLOW)
                        .append(Component.text("Never").color(NamedTextColor.WHITE)));
                }
                
                return true;
            }
            
            case "info" -> {
                sender.sendMessage(Component.text("=== " + Constants.PLUGIN_NAME + " ===").color(NamedTextColor.GOLD));
                sender.sendMessage(Component.text("Version: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(Constants.PLUGIN_VERSION).color(NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("Author: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(Constants.PLUGIN_AUTHOR).color(NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("Minecraft Version: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(plugin.getConfigManager().getMinecraftVersion()).color(NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("Server ID: ").color(NamedTextColor.YELLOW)
                    .append(Component.text(plugin.getConfigManager().getServerId()).color(NamedTextColor.WHITE)));
                return true;
            }
            
            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Chococar's Inventory Bridge Commands ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/ib reload").color(NamedTextColor.YELLOW)
            .append(Component.text(" - Reload configuration and reconnect database").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/ib reconnect").color(NamedTextColor.YELLOW)
            .append(Component.text(" - Reconnect to database only").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/ib sync <load|save>").color(NamedTextColor.YELLOW)
            .append(Component.text(" - Manually sync inventory").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/ib status").color(NamedTextColor.YELLOW)
            .append(Component.text(" - Check sync status").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/ib info").color(NamedTextColor.YELLOW)
            .append(Component.text(" - Show plugin information").color(NamedTextColor.WHITE)));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("reload", "reconnect", "sync", "status", "info");
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
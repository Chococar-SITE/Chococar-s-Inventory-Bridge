package site.chococar.inventorybridge.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import site.chococar.inventorybridge.fabric.ChococarsInventoryBridgeFabric;

public class InventoryBridgeCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("inventorybridge")
            .requires(source -> source.hasPermissionLevel(4)) // OP level required
            .then(CommandManager.literal("reload")
                .executes(InventoryBridgeCommand::executeReload))
            .then(CommandManager.literal("reconnect")
                .executes(InventoryBridgeCommand::executeReconnect))
            .then(CommandManager.literal("info")
                .executes(InventoryBridgeCommand::executeInfo))
            .executes(InventoryBridgeCommand::executeHelp)
        );
        
        // Short alias
        dispatcher.register(CommandManager.literal("ib")
            .requires(source -> source.hasPermissionLevel(4))
            .then(CommandManager.literal("reload")
                .executes(InventoryBridgeCommand::executeReload))
            .then(CommandManager.literal("reconnect")
                .executes(InventoryBridgeCommand::executeReconnect))
            .then(CommandManager.literal("info")
                .executes(InventoryBridgeCommand::executeInfo))
            .executes(InventoryBridgeCommand::executeHelp)
        );
    }
    
    private static int executeReload(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("Reloading configuration...").formatted(Formatting.YELLOW), false);
        
        boolean success = ChococarsInventoryBridgeFabric.getInstance().reloadPluginConfig();
        
        if (success) {
            source.sendFeedback(() -> Text.literal("✅ Configuration reloaded successfully!").formatted(Formatting.GREEN), true);
            source.sendFeedback(() -> Text.literal("Database connection updated, all features restored.").formatted(Formatting.GREEN), false);
        } else {
            source.sendFeedback(() -> Text.literal("❌ Configuration reload failed!").formatted(Formatting.RED), true);
            source.sendFeedback(() -> Text.literal("Check console for detailed error information.").formatted(Formatting.RED), false);
        }
        
        return 1;
    }
    
    private static int executeReconnect(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("Reconnecting to database...").formatted(Formatting.YELLOW), false);
        
        boolean success = ChococarsInventoryBridgeFabric.getInstance().reconnectDatabase();
        
        if (success) {
            source.sendFeedback(() -> Text.literal("✅ Database reconnected successfully!").formatted(Formatting.GREEN), true);
            source.sendFeedback(() -> Text.literal("All inventory sync features are now active.").formatted(Formatting.GREEN), false);
        } else {
            source.sendFeedback(() -> Text.literal("❌ Database reconnection failed!").formatted(Formatting.RED), true);
            source.sendFeedback(() -> Text.literal("Please check database settings and connection.").formatted(Formatting.RED), false);
        }
        
        return 1;
    }
    
    private static int executeInfo(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        boolean isStandby = ChococarsInventoryBridgeFabric.getInstance().getDatabaseConnection().isStandbyMode();
        String lastError = ChococarsInventoryBridgeFabric.getInstance().getDatabaseConnection().getLastConnectionError();
        
        source.sendFeedback(() -> Text.literal("=== Chococar's Inventory Bridge ===").formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("Version: ").formatted(Formatting.YELLOW)
            .append(Text.literal("1.0.0-SNAPSHOT").formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> Text.literal("Author: ").formatted(Formatting.YELLOW)
            .append(Text.literal("chococar.site").formatted(Formatting.WHITE)), false);
        
        if (isStandby) {
            source.sendFeedback(() -> Text.literal("Status: ").formatted(Formatting.YELLOW)
                .append(Text.literal("🚧 STANDBY MODE").formatted(Formatting.RED)), false);
            if (lastError != null) {
                source.sendFeedback(() -> Text.literal("Error: ").formatted(Formatting.YELLOW)
                    .append(Text.literal(lastError).formatted(Formatting.RED)), false);
            }
        } else {
            source.sendFeedback(() -> Text.literal("Status: ").formatted(Formatting.YELLOW)
                .append(Text.literal("✅ ACTIVE").formatted(Formatting.GREEN)), false);
        }
        
        return 1;
    }
    
    private static int executeHelp(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("=== Chococar's Inventory Bridge Commands ===").formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("/ib reload").formatted(Formatting.YELLOW)
            .append(Text.literal(" - Reload configuration and reconnect database").formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> Text.literal("/ib reconnect").formatted(Formatting.YELLOW)
            .append(Text.literal(" - Reconnect to database only").formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> Text.literal("/ib info").formatted(Formatting.YELLOW)
            .append(Text.literal(" - Show plugin status and information").formatted(Formatting.WHITE)), false);
        
        return 1;
    }
}
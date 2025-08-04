package site.chococar.inventorybridge.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.concurrent.CompletableFuture;
import site.chococar.inventorybridge.common.Constants;
import site.chococar.inventorybridge.fabric.ChococarsInventoryBridgeFabric;

public class InventoryBridgeCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("inventorybridge")
            .requires(source -> source.hasPermissionLevel(4)) // OP level required
            .then(CommandManager.literal("reload")
                .executes(InventoryBridgeCommand::executeReload))
            .then(CommandManager.literal("reconnect")
                .executes(InventoryBridgeCommand::executeReconnect))
            .then(CommandManager.literal("sync")
                .then(CommandManager.argument("action", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        builder.suggest("load");
                        builder.suggest("save");
                        return builder.buildFuture();
                    })
                    .executes(InventoryBridgeCommand::executeSync)))
            .then(CommandManager.literal("status")
                .executes(InventoryBridgeCommand::executeStatus))
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
            .then(CommandManager.literal("sync")
                .then(CommandManager.argument("action", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        builder.suggest("load");
                        builder.suggest("save");
                        return builder.buildFuture();
                    })
                    .executes(InventoryBridgeCommand::executeSync)))
            .then(CommandManager.literal("status")
                .executes(InventoryBridgeCommand::executeStatus))
            .then(CommandManager.literal("info")
                .executes(InventoryBridgeCommand::executeInfo))
            .executes(InventoryBridgeCommand::executeHelp)
        );
    }
    
    private static int executeReload(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("Reloading configuration...").formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("This operation is running asynchronously to prevent server lag.").formatted(Formatting.GRAY), false);
        
        CompletableFuture.runAsync(() -> {
            boolean success = ChococarsInventoryBridgeFabric.getInstance().reloadPluginConfig();
            
            // Send feedback back on the main thread
            source.getServer().execute(() -> {
                if (success) {
                    source.sendFeedback(() -> Text.literal("✅ Configuration reloaded successfully!").formatted(Formatting.GREEN), true);
                    source.sendFeedback(() -> Text.literal("Database connection updated, all features restored.").formatted(Formatting.GREEN), false);
                } else {
                    source.sendFeedback(() -> Text.literal("❌ Configuration reload failed!").formatted(Formatting.RED), true);
                    source.sendFeedback(() -> Text.literal("Check console for detailed error information.").formatted(Formatting.RED), false);
                }
            });
        });
        
        return 1;
    }
    
    private static int executeReconnect(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(() -> Text.literal("Reconnecting to database...").formatted(Formatting.YELLOW), false);
        source.sendFeedback(() -> Text.literal("This operation is running asynchronously to prevent server lag.").formatted(Formatting.GRAY), false);
        
        CompletableFuture.runAsync(() -> {
            boolean success = ChococarsInventoryBridgeFabric.getInstance().reconnectDatabase();
            
            // Send feedback back on the main thread
            source.getServer().execute(() -> {
                if (success) {
                    source.sendFeedback(() -> Text.literal("✅ Database reconnected successfully!").formatted(Formatting.GREEN), true);
                    source.sendFeedback(() -> Text.literal("All inventory sync features are now active.").formatted(Formatting.GREEN), false);
                } else {
                    source.sendFeedback(() -> Text.literal("❌ Database reconnection failed!").formatted(Formatting.RED), true);
                    source.sendFeedback(() -> Text.literal("Please check database settings and connection.").formatted(Formatting.RED), false);
                }
            });
        });
        
        return 1;
    }
    
    private static int executeSync(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        // Check if command is executed by a player
        if (source.getEntity() == null || !(source.getEntity() instanceof ServerPlayerEntity)) {
            source.sendFeedback(() -> Text.literal("This command can only be used by players!").formatted(Formatting.RED), false);
            return 0;
        }
        
        ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();
        String action = StringArgumentType.getString(context, "action");
        
        if (!action.equals("load") && !action.equals("save")) {
            source.sendFeedback(() -> Text.literal("Usage: /ib sync <load|save>").formatted(Formatting.RED), false);
            return 0;
        }
        
        if (ChococarsInventoryBridgeFabric.getInstance().getSyncManager().isSyncInProgress(player.getUuid())) {
            source.sendFeedback(() -> Text.literal("Sync already in progress, please wait...").formatted(Formatting.YELLOW), false);
            return 0;
        }
        
        boolean save = action.equals("save");
        source.sendFeedback(() -> Text.literal("Manual " + action + " initiated!").formatted(Formatting.GREEN), false);
        source.sendFeedback(() -> Text.literal("This operation is running asynchronously to prevent server lag.").formatted(Formatting.GRAY), false);
        
        ChococarsInventoryBridgeFabric.getInstance().getSyncManager().manualSync(player, save);
        
        return 1;
    }
    
    private static int executeStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        // Check if command is executed by a player
        if (source.getEntity() == null || !(source.getEntity() instanceof ServerPlayerEntity)) {
            source.sendFeedback(() -> Text.literal("This command can only be used by players!").formatted(Formatting.RED), false);
            return 0;
        }
        
        ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();
        boolean inProgress = ChococarsInventoryBridgeFabric.getInstance().getSyncManager().isSyncInProgress(player.getUuid());
        long lastSync = ChococarsInventoryBridgeFabric.getInstance().getSyncManager().getLastSyncTime(player.getUuid());
        
        source.sendFeedback(() -> Text.literal("=== Inventory Bridge Status ===").formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("Sync in progress: ").formatted(Formatting.YELLOW)
            .append(Text.literal(inProgress ? "Yes" : "No").formatted(inProgress ? Formatting.RED : Formatting.GREEN)), false);
        
        if (lastSync > 0) {
            long timeSince = (System.currentTimeMillis() - lastSync) / 1000;
            source.sendFeedback(() -> Text.literal("Last sync: ").formatted(Formatting.YELLOW)
                .append(Text.literal(timeSince + " seconds ago").formatted(Formatting.WHITE)), false);
        } else {
            source.sendFeedback(() -> Text.literal("Last sync: ").formatted(Formatting.YELLOW)
                .append(Text.literal("Never").formatted(Formatting.WHITE)), false);
        }
        
        return 1;
    }
    
    private static int executeInfo(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        boolean isStandby = ChococarsInventoryBridgeFabric.getInstance().getDatabaseConnection().isStandbyMode();
        String lastError = ChococarsInventoryBridgeFabric.getInstance().getDatabaseConnection().getLastConnectionError();
        
        source.sendFeedback(() -> Text.literal("=== " + Constants.PLUGIN_NAME + " ===").formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("Version: ").formatted(Formatting.YELLOW)
            .append(Text.literal(Constants.PLUGIN_VERSION).formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> Text.literal("Author: ").formatted(Formatting.YELLOW)
            .append(Text.literal(Constants.PLUGIN_AUTHOR).formatted(Formatting.WHITE)), false);
        
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
        source.sendFeedback(() -> Text.literal("/ib sync <load|save>").formatted(Formatting.YELLOW)
            .append(Text.literal(" - Manually sync inventory").formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> Text.literal("/ib status").formatted(Formatting.YELLOW)
            .append(Text.literal(" - Check sync status").formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> Text.literal("/ib info").formatted(Formatting.YELLOW)
            .append(Text.literal(" - Show plugin status and information").formatted(Formatting.WHITE)), false);
        
        return 1;
    }
}
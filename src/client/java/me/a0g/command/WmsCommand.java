package me.a0g.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.a0g.WmsClient;
import me.a0g.api.CustomItemManager;
import me.a0g.api.WynnApi;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Клиентская команда /wms для управления пользовательскими предметами
 */
public class WmsCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(
                literal("wms")
                    .executes(WmsCommand::showHelp)
                    .then(literal("add")
                        .then(argument("item", StringArgumentType.greedyString())
                            .suggests((context, builder) -> suggestAdd(builder))
                            .executes(WmsCommand::addItem)
                        )
                    )
                    .then(literal("del")
                        .then(argument("item", StringArgumentType.greedyString())
                            .suggests((context, builder) -> suggestDel(builder))
                            .executes(WmsCommand::deleteItem)
                        )
                    )
                    .then(literal("list")
                        .executes(WmsCommand::listItems)
                    )
                    .then(literal("reload")
                        .executes(WmsCommand::reloadApi)
                    )
                    .then(literal("open")
                        .executes(WmsCommand::openGui)
                    )
            )
        );
    }

    private static int showHelp(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Text.literal("§6§lWynnMarketSearch §f— Commands:"));
        context.getSource().sendFeedback(Text.literal("  §e/wms add <item>§7 — Add custom item"));
        context.getSource().sendFeedback(Text.literal("  §e/wms del <item>§7 — Remove custom item"));
        context.getSource().sendFeedback(Text.literal("  §e/wms list§7 — List custom items"));
        context.getSource().sendFeedback(Text.literal("  §e/wms reload§7 — Reload API items"));
        context.getSource().sendFeedback(Text.literal("  §e/wms open§7 — Open search GUI"));
        return 1;
    }

    private static int addItem(CommandContext<FabricClientCommandSource> context) {
        String itemName = StringArgumentType.getString(context, "item").replace("\"", "");
        WynnApi api = WmsClient.getWynnApi();

        if (api == null) {
            context.getSource().sendError(Text.literal("§cWynnMarketSearch API not available"));
            return 0;
        }

        CustomItemManager customManager = api.getCustomItemManager();

        if (customManager.hasItem(itemName)) {
            context.getSource().sendError(Text.literal("§cItem '" + itemName + "' already exists"));
            return 0;
        }

        if (customManager.addItem(itemName)) {
            context.getSource().sendFeedback(Text.literal("§aAdded custom item: §f" + itemName));
        } else {
            context.getSource().sendError(Text.literal("§cFailed to add item"));
        }

        return 1;
    }

    private static int deleteItem(CommandContext<FabricClientCommandSource> context) {
        String itemName = StringArgumentType.getString(context, "item").replace("\"", "");
        WynnApi api = WmsClient.getWynnApi();

        if (api == null) {
            context.getSource().sendError(Text.literal("§cWynnMarketSearch API not available"));
            return 0;
        }

        CustomItemManager customManager = api.getCustomItemManager();

        if (customManager.removeItem(itemName)) {
            context.getSource().sendFeedback(Text.literal("§aRemoved custom item: §f" + itemName));
        } else {
            context.getSource().sendError(Text.literal("§cItem '" + itemName + "' not found"));
        }

        return 1;
    }

    private static int listItems(CommandContext<FabricClientCommandSource> context) {
        WynnApi api = WmsClient.getWynnApi();

        if (api == null) {
            context.getSource().sendError(Text.literal("§cWynnMarketSearch API not available"));
            return 0;
        }

        CustomItemManager customManager = api.getCustomItemManager();
        Set<String> items = customManager.getCustomItems();

        if (items.isEmpty()) {
            context.getSource().sendFeedback(Text.literal("§eNo custom items added"));
            return 0;
        }

        context.getSource().sendFeedback(Text.literal("§aCustom items (" + items.size() + "):"));
        for (String item : items) {
            context.getSource().sendFeedback(Text.literal("  §f- " + item));
        }

        return 1;
    }

    private static int reloadApi(CommandContext<FabricClientCommandSource> context) {
        WynnApi api = WmsClient.getWynnApi();

        if (api == null) {
            context.getSource().sendError(Text.literal("§cWynnMarketSearch API not available"));
            return 0;
        }

        context.getSource().sendFeedback(Text.literal("§aReloading items from API..."));

        api.forceReloadFromApi().thenRun(() -> {
            MinecraftClient.getInstance().execute(() -> {
                if (api.isLoaded()) {
                    context.getSource().sendFeedback(Text.literal("§aReloaded " + api.getAllItems().size() + " items"));
                } else {
                    context.getSource().sendError(Text.literal("§cFailed to reload items"));
                }
            });
        });

        return 1;
    }

    private static int openGui(CommandContext<FabricClientCommandSource> context) {
        WynnApi api = WmsClient.getWynnApi();

        if (api == null) {
            context.getSource().sendError(Text.literal("§cWynnMarketSearch API not available"));
            return 0;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            me.a0g.gui.SearchGuiScreen screen = new me.a0g.gui.SearchGuiScreen(api, false);
            client.setScreen(screen);
        });

        return 1;
    }

    private static CompletableFuture<Suggestions> suggestAdd(SuggestionsBuilder builder) {
        builder.suggest("Your Custom Item");
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestDel(SuggestionsBuilder builder) {
        WynnApi api = WmsClient.getWynnApi();
        if (api != null) {
            CustomItemManager customManager = api.getCustomItemManager();
            for (String item : customManager.getCustomItems()) {
                builder.suggest(item);
            }
        }
        return builder.buildFuture();
    }
}

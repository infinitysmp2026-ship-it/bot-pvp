package com.pixy.bot;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class PixyBotMod implements ModInitializer {

    private static final SuggestionProvider<CommandSourceStack> BOT_NAMES = (ctx, b) ->
        SharedSuggestionProvider.suggest(
            BotManager.all().stream().map(x -> x.getGameProfile().name()), b);

    @Override
    public void onInitialize() {
        // Bots are not persisted in stage 1: make sure none linger on shutdown.
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> BotManager.removeAll());

        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) ->
            dispatcher.register(
                literal("pixybot")
                    // NOTE: if this line fails to compile on 1.21.11, see README "Troubleshooting".
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))

                    // /pixybot spawn [name] [skin]
                    .then(literal("spawn")
                        .executes(c -> spawn(c.getSource(), null, null))
                        .then(argument("name", StringArgumentType.word())
                            .executes(c -> spawn(c.getSource(), StringArgumentType.getString(c, "name"), null))
                            .then(argument("skin", StringArgumentType.word())
                                .executes(c -> spawn(c.getSource(),
                                    StringArgumentType.getString(c, "name"),
                                    StringArgumentType.getString(c, "skin"))))))

                    // /pixybot random [skin]  -> random name (+ optional skin)
                    .then(literal("random")
                        .executes(c -> spawn(c.getSource(), BotManager.randomName(), null))
                        .then(argument("skin", StringArgumentType.word())
                            .executes(c -> spawn(c.getSource(), BotManager.randomName(),
                                StringArgumentType.getString(c, "skin")))))

                    // /pixybot remove <name>
                    .then(literal("remove")
                        .then(argument("name", StringArgumentType.word()).suggests(BOT_NAMES)
                            .executes(c -> {
                                String n = StringArgumentType.getString(c, "name");
                                if (BotManager.remove(n)) {
                                    c.getSource().sendSuccess(() -> Component.literal("Removed " + n), true);
                                    return 1;
                                }
                                c.getSource().sendFailure(Component.literal("No bot named " + n));
                                return 0;
                            })))

                    // /pixybot removeall
                    .then(literal("removeall").executes(c -> {
                        int n = BotManager.removeAll();
                        c.getSource().sendSuccess(() -> Component.literal("Removed " + n + " bot(s)."), true);
                        return n;
                    }))

                    // /pixybot list
                    .then(literal("list").executes(c -> {
                        var bots = BotManager.all();
                        if (bots.isEmpty()) {
                            c.getSource().sendSuccess(() -> Component.literal("No bots."), false);
                            return 0;
                        }
                        String names = String.join(", ",
                            bots.stream().map(b -> b.getGameProfile().name()).toList());
                        c.getSource().sendSuccess(() ->
                            Component.literal(bots.size() + " bot(s): " + names), false);
                        return bots.size();
                    }))
            ));
    }

    private static int spawn(CommandSourceStack src, String name, String skin) {
        ServerPlayer p = src.getPlayer();
        if (p == null) { src.sendFailure(Component.literal("Run this as a player.")); return 0; }
        if (name == null) name = BotManager.randomName();
        if (name.length() > 16) { src.sendFailure(Component.literal("Name max 16 characters.")); return 0; }

        BotManager.spawn(src.getServer(), p.level(), p.position(), p.getYRot(), p.getXRot(),
            name, skin, msg -> src.sendSuccess(() -> msg, false));
        return 1;
    }
}

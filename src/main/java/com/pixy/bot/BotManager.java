package com.pixy.bot;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BotManager {
    private static final Map<UUID, PixyBotPlayer> BOTS = new ConcurrentHashMap<>();
    private static final Random RNG = new Random();

    public static final String[] RANDOM_NAMES = {
        "Shadow", "Nova", "Blaze", "Ghost", "Viper", "Rogue", "Zephyr", "Raven",
        "Storm", "Ember", "Frost", "Wraith", "Titan", "Specter", "Hunter", "Drift"
    };

    private BotManager() { }

    public static boolean isBot(Player p) { return p instanceof PixyBotPlayer; }
    public static Collection<PixyBotPlayer> all() { return BOTS.values(); }
    public static void unregister(PixyBotPlayer b) { BOTS.remove(b.getUUID()); }

    public static PixyBotPlayer byName(String name) {
        for (PixyBotPlayer b : BOTS.values())
            if (b.getGameProfile().name().equalsIgnoreCase(name)) return b;
        return null;
    }

    public static String randomName() {
        String base = RANDOM_NAMES[RNG.nextInt(RANDOM_NAMES.length)];
        String n;
        do { n = base + RNG.nextInt(100); } while (byName(n) != null);
        return n;
    }

    /**
     * Spawn at exactly (pos, yaw, pitch). If skinName is non-null the skin of that Minecraft
     * account is fetched asynchronously, then the bot is spawned on the server thread.
     */
    public static void spawn(MinecraftServer server, ServerLevel level, Vec3 pos,
                             float yaw, float pitch, String name, String skinName,
                             java.util.function.Consumer<Component> feedback) {

        if (byName(name) != null) { feedback.accept(Component.literal("A bot named " + name + " already exists.")); return; }

        UUID uuid = UUID.nameUUIDFromBytes(("PixyBot:" + name).getBytes());
        GameProfile offline = new GameProfile(uuid, name);

        if (skinName == null || skinName.isBlank()) {
            server.execute(() -> doSpawn(server, level, offline, pos, yaw, pitch, feedback));
            return;
        }

        // Fetch the skin's texture property from Mojang, keep our own name + uuid.
                java.util.concurrent.CompletableFuture
            .supplyAsync(() -> server.services().profileResolver().fetchByName(skinName))
            .whenComplete((opt, err) ->
            server.execute(() -> {
                GameProfile profile = offline;
                if (opt != null && opt.isPresent()) {
                    profile = new GameProfile(uuid, name, opt.get().properties());
                } else {
                    feedback.accept(Component.literal("Skin '" + skinName + "' not found, using default skin."));
                }
                doSpawn(server, level, profile, pos, yaw, pitch, feedback);
            }));
    }

    private static void doSpawn(MinecraftServer server, ServerLevel level, GameProfile profile,
                                Vec3 pos, float yaw, float pitch,
                                java.util.function.Consumer<Component> feedback) {
        PixyBotPlayer bot = new PixyBotPlayer(server, level, profile, yaw, pitch);
        bot.snapTo(pos.x, pos.y, pos.z, yaw, pitch);
        bot.setYHeadRot(yaw);
        bot.setYBodyRot(yaw);

        server.getPlayerList().placeNewPlayer(new FakeConnection(), bot,
                CommonListenerCookie.createInitial(profile, false));

        // placeNewPlayer may load saved data / reposition; force the exact spawn transform again.
        bot.teleportTo(level, pos.x, pos.y, pos.z, Set.of(), yaw, pitch, false);
        bot.setYHeadRot(yaw);
        bot.setYBodyRot(yaw);

        BOTS.put(bot.getUUID(), bot);
        feedback.accept(Component.literal("Spawned bot " + profile.name() + "."));
    }

    public static boolean remove(String name) {
        PixyBotPlayer b = byName(name);
        if (b == null) return false;
        b.scheduleRemoval();
        return true;
    }

    public static int removeAll() {
        int n = BOTS.size();
        for (PixyBotPlayer b : new ArrayList<>(BOTS.values())) b.scheduleRemoval();
        return n;
    }
}

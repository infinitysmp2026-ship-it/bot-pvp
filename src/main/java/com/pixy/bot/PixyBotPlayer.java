package com.pixy.bot;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/** The bot. A real ServerPlayer subclass, so it renders, fights and dies exactly like a player. */
public class PixyBotPlayer extends ServerPlayer {

    public final float spawnYaw;
    public final float spawnPitch;
    private final MinecraftServer botServer;
    private boolean removing = false;

    public PixyBotPlayer(MinecraftServer server, ServerLevel level, GameProfile profile,
                         float yaw, float pitch) {
        super(server, level, profile, ClientInformation.createDefault());
        this.botServer = server;
        this.spawnYaw = yaw;
        this.spawnPitch = pitch;
    }

    /** Fake players have no connection ticking them, so we drive the tick ourselves. */
    @Override
    public void tick() {
        if (botServer.getTickCount() % 10 == 0) {
            this.connection.resetPosition();
            this.level().getChunkSource().move(this);
        }
        try {
            super.tick();
            this.doTick();
        } catch (NullPointerException ignored) { }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source); // vanilla death message is broadcast here
        scheduleRemoval();
    }

    public void scheduleRemoval() {
        if (removing) return;
        removing = true;
        botServer.execute(() -> {
            BotManager.unregister(this);
            botServer.getPlayerList().remove(this);
        });
    }
}

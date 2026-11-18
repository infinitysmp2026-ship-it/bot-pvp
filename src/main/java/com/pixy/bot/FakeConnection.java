package com.pixy.bot;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.PacketFlow;

/** A network connection that goes nowhere. Lets a ServerPlayer exist without a real client. */
public class FakeConnection extends Connection {
    public FakeConnection() {
        super(PacketFlow.SERVERBOUND);
        this.channel = new EmbeddedChannel(); // keeps isConnected() true; packets are discarded
    }

    @Override public void setReadOnly() { }
    @Override public void handleDisconnection() { }
    @Override public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> info, T listener) { }
}

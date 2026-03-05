package net.meatwo310.m2toolbox.network;

import net.meatwo310.m2toolbox.M2Toolbox;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class M2ToolboxNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            M2Toolbox.loc( "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int nextId = 0;

    public static void register() {
        CHANNEL.registerMessage(nextId++, OpenTrayPacket.class,
                OpenTrayPacket::encode, OpenTrayPacket::decode, OpenTrayPacket::handle);
    }
}

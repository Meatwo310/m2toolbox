package net.meatwo310.m2toolbox.network;

import net.meatwo310.m2toolbox.menu.ToolboxMenu;
import net.meatwo310.m2toolbox.util.ToolboxFinder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public class ReopenToolboxPacket {
    private final boolean fromCurios;

    public ReopenToolboxPacket(boolean fromCurios) {
        this.fromCurios = fromCurios;
    }

    public ReopenToolboxPacket(FriendlyByteBuf buf) {
        this.fromCurios = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(fromCurios);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack toolboxStack = ToolboxFinder.find(player, fromCurios);

            if (toolboxStack.isEmpty()) return;

            final ItemStack finalStack = toolboxStack;
            NetworkHooks.openScreen(player,
                    new SimpleMenuProvider(
                            (id, inv, p) -> new ToolboxMenu(id, inv, finalStack, fromCurios),
                            finalStack.getHoverName()
                    ),
                    buf -> {
                        buf.writeItem(finalStack);
                        buf.writeBoolean(fromCurios);
                    }
            );
        });
        ctx.get().setPacketHandled(true);
    }
}

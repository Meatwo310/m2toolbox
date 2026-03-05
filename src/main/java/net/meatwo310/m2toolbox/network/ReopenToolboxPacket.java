package net.meatwo310.m2toolbox.network;

import net.meatwo310.m2toolbox.item.M2ToolboxItems;
import net.meatwo310.m2toolbox.menu.ToolboxMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public class ReopenToolboxPacket {
    public static void encode(ReopenToolboxPacket packet, FriendlyByteBuf buf) {}

    public static ReopenToolboxPacket decode(FriendlyByteBuf buf) {
        return new ReopenToolboxPacket();
    }

    public static void handle(ReopenToolboxPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack toolboxStack = ItemStack.EMPTY;
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack held = player.getItemInHand(hand);
                if (held.is(M2ToolboxItems.TOOLBOX.get())) {
                    toolboxStack = held;
                    break;
                }
            }
            if (toolboxStack.isEmpty()) return;

            final ItemStack finalStack = toolboxStack;
            NetworkHooks.openScreen(player,
                    new SimpleMenuProvider(
                            (id, inv, p) -> new ToolboxMenu(id, inv, finalStack),
                            finalStack.getHoverName()
                    ),
                    buf -> buf.writeItem(finalStack)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}

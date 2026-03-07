package net.meatwo310.m2toolbox.network;

import net.meatwo310.m2toolbox.handler.ToolboxHandler;
import net.meatwo310.m2toolbox.item.M2ToolboxItems;
import net.meatwo310.m2toolbox.menu.TrayMenu;
import net.meatwo310.m2toolbox.util.ToolboxFinder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public class OpenTrayPacket {
    private final int slotIndex;
    private final boolean fromCurios;

    public OpenTrayPacket(int slotIndex, boolean fromCurios) {
        this.slotIndex = slotIndex;
        this.fromCurios = fromCurios;
    }

    public OpenTrayPacket(FriendlyByteBuf buf) {
        this(buf.readByte(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(slotIndex);
        buf.writeBoolean(fromCurios);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack toolboxStack = ToolboxFinder.find(player, fromCurios);
            if (toolboxStack.isEmpty()) return;

            if (slotIndex < 0 || slotIndex >= ToolboxHandler.SLOTS) return;

            ToolboxHandler handler = new ToolboxHandler(toolboxStack);
            ItemStack trayStack = handler.getStackInSlot(slotIndex);
            if (trayStack.isEmpty() || !trayStack.is(M2ToolboxItems.TRAY.get())) return;

            NetworkHooks.openScreen(player,
                    new SimpleMenuProvider(
                            (id, inv, p) -> new TrayMenu(id, inv, trayStack, fromCurios, toolboxStack, slotIndex),
                            trayStack.getHoverName()
                    ),
                    buf -> {
                        buf.writeItem(trayStack);
                        buf.writeBoolean(fromCurios);
                        buf.writeByte(slotIndex);
                    }
            );
        });
        ctx.get().setPacketHandled(true);
    }

}

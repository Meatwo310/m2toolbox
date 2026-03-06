package net.meatwo310.m2toolbox.network;

import net.meatwo310.m2toolbox.compat.curios.CuriosCompat;
import net.meatwo310.m2toolbox.handler.ToolboxHandler;
import net.meatwo310.m2toolbox.item.M2ToolboxItems;
import net.meatwo310.m2toolbox.menu.TrayMenu;
import net.minecraft.nbt.CompoundTag;
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

            ItemStack toolboxStack = findToolbox(player);
            if (toolboxStack.isEmpty()) return;

            if (slotIndex < 0 || slotIndex >= ToolboxHandler.SLOTS) return;

            ToolboxHandler handler = new ToolboxHandler();
            CompoundTag tag = toolboxStack.getTag();
            if (tag != null && tag.contains("Items")) {
                handler.deserializeNBT(tag.getCompound("Items"));
            }

            ItemStack trayStack = handler.getStackInSlot(slotIndex);
            if (trayStack.isEmpty() || !trayStack.is(M2ToolboxItems.TRAY.get())) return;

            NetworkHooks.openScreen(player,
                    new SimpleMenuProvider(
                            (id, inv, p) -> new TrayMenu(id, inv, trayStack, toolboxStack, slotIndex, fromCurios),
                            trayStack.getHoverName()
                    ),
                    buf -> {
                        buf.writeItem(trayStack);
                        buf.writeBoolean(true); // fromToolbox
                        buf.writeByte(slotIndex);
                        buf.writeBoolean(fromCurios);
                    }
            );
        });
        ctx.get().setPacketHandled(true);
    }

    private ItemStack findToolbox(ServerPlayer player) {
        if (fromCurios) {
            return CuriosCompat.getToolboxStack(player).orElse(ItemStack.EMPTY);
        }

        ItemStack main = player.getMainHandItem();
        if (main.is(M2ToolboxItems.TOOLBOX.get())) return main;

        ItemStack off = player.getOffhandItem();
        if (off.is(M2ToolboxItems.TOOLBOX.get())) return off;

        return ItemStack.EMPTY;
    }
}

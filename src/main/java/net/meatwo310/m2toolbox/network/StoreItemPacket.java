package net.meatwo310.m2toolbox.network;

import net.meatwo310.m2toolbox.M2ToolboxKeys;
import net.meatwo310.m2toolbox.compat.curios.CuriosCompat;
import net.meatwo310.m2toolbox.handler.ToolboxHandler;
import net.meatwo310.m2toolbox.handler.TrayHandler;
import net.meatwo310.m2toolbox.item.M2ToolboxItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StoreItemPacket {
    private final int traySlot;     // ツールボックス内のトレイスロット 0-8
    private final int trayItemSlot; // トレイ内のtoolスロット 0-8

    public StoreItemPacket(int traySlot, int trayItemSlot) {
        this.traySlot = traySlot;
        this.trayItemSlot = trayItemSlot;
    }

    public StoreItemPacket(FriendlyByteBuf buf) {
        this.traySlot = buf.readByte();
        this.trayItemSlot = buf.readByte();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(traySlot);
        buf.writeByte(trayItemSlot);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 範囲チェック
            if (traySlot < 0 || traySlot >= ToolboxHandler.SLOTS) return;
            if (trayItemSlot < 0 || trayItemSlot >= TrayHandler.TOOL_SLOTS) return;

            // メインハンドのアイテムを取得
            ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (mainHandItem.isEmpty()) {
                player.sendSystemMessage(
                        Component.translatable(M2ToolboxKeys.RADIAL_MAIN_HAND_EMPTY)
                                .withStyle(ChatFormatting.RED)
                );
                return;
            }

            // CuriosスロットからツールボックスItemStackを取得・検証
            var toolboxStack = CuriosCompat.getToolboxStack(player).orElse(null);
            if (toolboxStack == null) return;

            // ツールボックスハンドラにNBT読み込み
            ToolboxHandler toolboxHandler = new ToolboxHandler();
            CompoundTag toolboxTag = toolboxStack.getTag();
            if (toolboxTag != null && toolboxTag.contains("Items")) {
                toolboxHandler.deserializeNBT(toolboxTag.getCompound("Items"));
            }

            // トレイ取得・検証
            ItemStack trayStack = toolboxHandler.getStackInSlot(traySlot);
            if (trayStack.isEmpty() || !trayStack.is(M2ToolboxItems.TRAY.get())) return;

            // トレイハンドラにNBT読み込み
            TrayHandler trayHandler = new TrayHandler();
            CompoundTag trayTag = trayStack.getTag();
            if (trayTag != null && trayTag.contains("Items")) {
                trayHandler.deserializeNBT(trayTag.getCompound("Items"));
            }

            // 対象スロットが空であることを確認
            ItemStack existingItem = trayHandler.getStackInSlot(trayItemSlot);
            if (!existingItem.isEmpty()) return;

            // アイテムを移動: メインハンドから削除 → トレイに配置
            trayHandler.setStackInSlot(trayItemSlot, mainHandItem.copy());
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            // トレイのNBTをtrayStackに書き戻す
            trayStack.getOrCreateTag().put("Items", trayHandler.serializeNBT());

            // ツールボックスのNBTをtoolboxStackに書き戻す
            toolboxStack.getOrCreateTag().put("Items", toolboxHandler.serializeNBT());
        });
        ctx.get().setPacketHandled(true);
    }
}

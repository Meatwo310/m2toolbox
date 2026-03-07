package net.meatwo310.m2toolbox.network;

import net.meatwo310.m2toolbox.M2ToolboxKeys;
import net.meatwo310.m2toolbox.handler.ToolboxHandler;
import net.meatwo310.m2toolbox.handler.TrayHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class StoreItemPacket extends AbstractTraySlotPacket {

    public StoreItemPacket(int traySlot, int trayItemSlot) {
        super(traySlot, trayItemSlot);
    }

    public StoreItemPacket(FriendlyByteBuf buf) {
        super(buf);
    }

    @Override
    protected boolean processWithHandlers(
            ServerPlayer player,
            ToolboxHandler toolboxHandler,
            ItemStack toolboxStack,
            TrayHandler trayHandler,
            ItemStack trayStack
    ) {
        // メインハンドのアイテムを取得
        ItemStack mainHandItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (mainHandItem.isEmpty()) {
            player.sendSystemMessage(
                    Component.translatable(M2ToolboxKeys.RADIAL_MAIN_HAND_EMPTY)
                            .withStyle(ChatFormatting.RED)
            );
            return false;
        }

        // 対象スロットが空であることを確認
        ItemStack existingItem = trayHandler.getStackInSlot(trayItemSlot);
        if (!existingItem.isEmpty()) return false;

        // アイテムを移動: メインハンドから削除 → トレイに配置
        trayHandler.setStackInSlot(trayItemSlot, mainHandItem.copy());
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        return true;
    }
}

package net.meatwo310.m2toolbox.network;

import net.meatwo310.m2toolbox.M2ToolboxKeys;
import net.meatwo310.m2toolbox.handler.ToolboxHandler;
import net.meatwo310.m2toolbox.handler.TrayHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class ExtractItemPacket extends AbstractTraySlotPacket {

    public ExtractItemPacket(int traySlot, int trayItemSlot) {
        super(traySlot, trayItemSlot);
    }

    public ExtractItemPacket(FriendlyByteBuf buf) {
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
        // 取り出すアイテムを取得
        ItemStack itemToMove = trayHandler.getStackInSlot(trayItemSlot);
        if (itemToMove.isEmpty()) return false;

        // 現在選択中のホットバースロットを起点に右方向へ走査して空きを探す
        // スロット順: selected, selected+1, ..., 8, 0, ..., selected-1
        Inventory playerInv = player.getInventory();
        int start = playerInv.selected; // 0-8
        int destSlot = -1;
        for (int offset = 0; offset < 9; offset++) {
            int slot = (start + offset) % 9;
            if (playerInv.getItem(slot).isEmpty()) {
                destSlot = slot;
                break;
            }
        }

        // すべて埋まっていた場合はキャンセル
        if (destSlot == -1) {
            player.sendSystemMessage(
                    Component.translatable(M2ToolboxKeys.RADIAL_HOTBAR_FULL)
                            .withStyle(ChatFormatting.RED)
            );
            return false;
        }

        // アイテムを移動: トレイから削除 → ホットバーに配置
        playerInv.setItem(destSlot, itemToMove.copy());
        trayHandler.setStackInSlot(trayItemSlot, ItemStack.EMPTY);

        // クライアントにホットバースロットの変更を通知
        playerInv.selected = destSlot;
        player.connection.send(new ClientboundSetCarriedItemPacket(destSlot));

        return true;
    }
}

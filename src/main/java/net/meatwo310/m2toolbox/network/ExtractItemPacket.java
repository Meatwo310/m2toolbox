package net.meatwo310.m2toolbox.network;

import net.meatwo310.m2toolbox.M2ToolboxKeys;
import net.meatwo310.m2toolbox.handler.ToolboxHandler;
import net.meatwo310.m2toolbox.handler.TrayHandler;
import net.meatwo310.m2toolbox.item.M2ToolboxItems;
import net.meatwo310.m2toolbox.util.ToolboxFinder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ExtractItemPacket {
    private final int traySlot;     // ツールボックス内のトレイスロット 0-8
    private final int trayItemSlot; // トレイ内のtoolスロット 0-8

    public ExtractItemPacket(int traySlot, int trayItemSlot) {
        this.traySlot = traySlot;
        this.trayItemSlot = trayItemSlot;
    }

    public ExtractItemPacket(FriendlyByteBuf buf) {
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

            // CuriosスロットからツールボックスItemStackを取得・検証
            ItemStack toolboxStack = ToolboxFinder.findFromCurios(player);
            if (toolboxStack.isEmpty()) return;

            // ツールボックスハンドラにNBT読み込み
            ToolboxHandler toolboxHandler = new ToolboxHandler(toolboxStack);

            // トレイ取得・検証
            ItemStack trayStack = toolboxHandler.getStackInSlot(traySlot);
            if (trayStack.isEmpty() || !trayStack.is(M2ToolboxItems.TRAY.get())) return;

            // トレイハンドラにNBT読み込み
            TrayHandler trayHandler = new TrayHandler(trayStack);

            // 取り出すアイテムを取得
            ItemStack itemToMove = trayHandler.getStackInSlot(trayItemSlot);
            if (itemToMove.isEmpty()) return;

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
                return;
            }

            // アイテムを移動: トレイから削除 → ホットバーに配置
            playerInv.setItem(destSlot, itemToMove.copy());
            trayHandler.setStackInSlot(trayItemSlot, ItemStack.EMPTY);

            // クライアントにホットバースロットの変更を通知
            playerInv.selected = destSlot;
            player.connection.send(new ClientboundSetCarriedItemPacket(destSlot));

            // トレイ・ツールボックスの NBT を書き戻す
            trayHandler.saveToStack(trayStack);
            toolboxHandler.saveToStack(toolboxStack);
        });
        ctx.get().setPacketHandled(true);
    }
}

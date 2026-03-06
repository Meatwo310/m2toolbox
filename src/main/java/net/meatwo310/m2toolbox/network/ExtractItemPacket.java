package net.meatwo310.m2toolbox.network;

import net.meatwo310.m2toolbox.M2ToolboxKeys;
import net.meatwo310.m2toolbox.handler.ToolboxHandler;
import net.meatwo310.m2toolbox.handler.TrayHandler;
import net.meatwo310.m2toolbox.item.M2ToolboxItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ExtractItemPacket {
    private final int toolboxInventorySlot; // プレイヤーインベントリ内スロット 0-35
    private final int traySlot;             // ツールボックス内のトレイスロット 0-8
    private final int trayItemSlot;         // トレイ内のtoolスロット 0-8

    public ExtractItemPacket(int toolboxInventorySlot, int traySlot, int trayItemSlot) {
        this.toolboxInventorySlot = toolboxInventorySlot;
        this.traySlot = traySlot;
        this.trayItemSlot = trayItemSlot;
    }

    public ExtractItemPacket(FriendlyByteBuf buf) {
        this.toolboxInventorySlot = buf.readByte();
        this.traySlot = buf.readByte();
        this.trayItemSlot = buf.readByte();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(toolboxInventorySlot);
        buf.writeByte(traySlot);
        buf.writeByte(trayItemSlot);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 範囲チェック
            if (toolboxInventorySlot < 0 || toolboxInventorySlot >= 36) return;
            if (traySlot < 0 || traySlot >= ToolboxHandler.SLOTS) return;
            if (trayItemSlot < 0 || trayItemSlot >= TrayHandler.TOOL_SLOTS) return;

            // ツールボックス取得・検証
            ItemStack toolboxStack = player.getInventory().getItem(toolboxInventorySlot);
            if (toolboxStack.isEmpty() || !toolboxStack.is(M2ToolboxItems.TOOLBOX.get())) return;

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

            // トレイのNBTをtrayStackに書き戻す
            // trayStackはtoolboxHandlerの内部参照なので、ここで更新すれば
            // toolboxHandler.serializeNBT()にも反映される
            boolean trayEmpty = true;
            for (int i = 0; i < TrayHandler.TOTAL_SLOTS; i++) {
                if (!trayHandler.getStackInSlot(i).isEmpty()) {
                    trayEmpty = false;
                    break;
                }
            }
            if (trayEmpty) {
                trayStack.setTag(null);
            } else {
                trayStack.getOrCreateTag().put("Items", trayHandler.serializeNBT());
            }

            // ツールボックスのNBTをtoolboxStackに書き戻す
            boolean toolboxEmpty = true;
            for (int i = 0; i < ToolboxHandler.SLOTS; i++) {
                if (!toolboxHandler.getStackInSlot(i).isEmpty()) {
                    toolboxEmpty = false;
                    break;
                }
            }
            if (toolboxEmpty) {
                toolboxStack.setTag(null);
            } else {
                toolboxStack.getOrCreateTag().put("Items", toolboxHandler.serializeNBT());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

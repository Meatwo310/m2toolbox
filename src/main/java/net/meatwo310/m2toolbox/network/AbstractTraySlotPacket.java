package net.meatwo310.m2toolbox.network;

import net.meatwo310.m2toolbox.handler.ToolboxHandler;
import net.meatwo310.m2toolbox.handler.TrayHandler;
import net.meatwo310.m2toolbox.item.M2ToolboxItems;
import net.meatwo310.m2toolbox.util.ToolboxFinder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * ツールボックス内トレイのスロットを操作するパケットの共通基底クラス。
 *
 * <p>以下の処理を共通化する：
 * <ul>
 *   <li>{@code traySlot} / {@code trayItemSlot} のエンコード・デコード</li>
 *   <li>範囲チェック</li>
 *   <li>ToolboxFinder → ToolboxHandler → TrayHandler の取得・検証</li>
 *   <li>NBT の書き戻し（saveToStack）</li>
 *   <li>{@link #handle(Supplier)} のボイラープレート</li>
 * </ul>
 *
 * <p>サブクラスは {@link #processWithHandlers} にアイテム移動ロジックのみ実装すればよい。
 * NBT の保存は本クラスが {@code processWithHandlers} 呼び出し後に自動で行う。
 */
public abstract class AbstractTraySlotPacket {
    protected final int traySlot;     // ツールボックス内のトレイスロット 0-8
    protected final int trayItemSlot; // トレイ内のtoolスロット 0-8

    protected AbstractTraySlotPacket(int traySlot, int trayItemSlot) {
        this.traySlot = traySlot;
        this.trayItemSlot = trayItemSlot;
    }

    protected AbstractTraySlotPacket(FriendlyByteBuf buf) {
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

            // サブクラスのアイテム移動ロジックを実行
            boolean modified = processWithHandlers(player, toolboxHandler, toolboxStack, trayHandler, trayStack);

            // 変更があった場合のみNBTを書き戻す
            if (modified) {
                trayHandler.saveToStack(trayStack);
                toolboxHandler.saveToStack(toolboxStack);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * アイテム移動ロジックをサブクラスで実装する。
     * NBT の保存（saveToStack）は本クラスが担当するため、サブクラスで呼ぶ必要はない。
     *
     * @return アイテムが実際に移動された場合 {@code true}。
     *         {@code false} を返すと NBT の書き戻しがスキップされる。
     */
    protected abstract boolean processWithHandlers(
            ServerPlayer player,
            ToolboxHandler toolboxHandler,
            ItemStack toolboxStack,
            TrayHandler trayHandler,
            ItemStack trayStack
    );
}

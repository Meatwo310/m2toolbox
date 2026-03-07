package net.meatwo310.m2toolbox.handler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.stream.IntStream;

public abstract class AbstractItemHandler extends ItemStackHandler {

    protected AbstractItemHandler(int slots) {
        super(slots);
    }

    /**
     * ItemStack の NBT を読み込んだ状態でハンドラを初期化する。
     * "Items" タグが存在しない場合は空の状態で初期化される。
     */
    protected AbstractItemHandler(int slots, ItemStack stack) {
        super(slots);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Items")) {
            deserializeNBT(tag.getCompound("Items"));
        }
    }

    /**
     * ハンドラの内容を ItemStack の NBT へ書き戻す。
     * 全スロットが空の場合は NBT ごと削除する。
     */
    public void saveToStack(ItemStack stack) {
        boolean isEmpty = IntStream.range(0, getSlots())
                .allMatch(i -> getStackInSlot(i).isEmpty());
        if (isEmpty) {
            stack.setTag(null);
        } else {
            stack.getOrCreateTag().put("Items", serializeNBT());
        }
    }
}

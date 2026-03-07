package net.meatwo310.m2toolbox.handler;

import net.meatwo310.m2toolbox.item.M2ToolboxItems;
import net.minecraft.world.item.ItemStack;

public class ToolboxHandler extends AbstractItemHandler {
    public static final int SLOTS = 9;

    public ToolboxHandler() {
        super(SLOTS);
    }

    public ToolboxHandler(ItemStack stack) {
        super(SLOTS, stack);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.is(M2ToolboxItems.TRAY.get());
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }
}

package net.meatwo310.m2toolbox.handler;

import net.meatwo310.m2toolbox.item.M2ToolboxItems;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public class ToolboxHandler extends ItemStackHandler {
    public static final int SLOTS = 9;

    public ToolboxHandler() {
        super(SLOTS);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.is(M2ToolboxItems.TRAY.get());
    }
}

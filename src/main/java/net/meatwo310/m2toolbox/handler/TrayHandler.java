package net.meatwo310.m2toolbox.handler;

import net.minecraftforge.items.ItemStackHandler;

import java.util.Optional;

public class TrayHandler extends ItemStackHandler {
    public static final int TOOL_SLOTS = 9;
    public static final int SUB_SLOTS = 9;
    public static final int TOTAL_SLOTS = TOOL_SLOTS + SUB_SLOTS;
    public static final int SUB_SLOT_OFFSET = TOOL_SLOTS;

    public TrayHandler() {
        super(TOTAL_SLOTS);
    }

    public static boolean isToolSlot(int slot) {
        return slot < TOOL_SLOTS;
    }

    public static boolean isSubSlot(int slot) {
        return slot >= SUB_SLOT_OFFSET;
    }

    public static Optional<Integer> getSubslotForTool(int toolSlot) {
        if (isToolSlot(toolSlot)) {
            return Optional.of(toolSlot + SUB_SLOT_OFFSET);
        }
        return Optional.empty();
    }

    public static Optional<Integer> getToolSlotForSubslot(int subSlot) {
        if (isSubSlot(subSlot)) {
            return Optional.of(subSlot - SUB_SLOT_OFFSET);
        }
        return Optional.empty();
    }
}

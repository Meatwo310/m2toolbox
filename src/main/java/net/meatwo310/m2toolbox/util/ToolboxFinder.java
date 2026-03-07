package net.meatwo310.m2toolbox.util;

import net.meatwo310.m2toolbox.compat.curios.CuriosCompat;
import net.meatwo310.m2toolbox.item.M2ToolboxItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class ToolboxFinder {
    private ToolboxFinder() {}

    public static ItemStack find(ServerPlayer player, boolean fromCurios) {
        if (fromCurios) {
            return CuriosCompat.getToolboxStack(player).orElse(ItemStack.EMPTY);
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.is(M2ToolboxItems.TOOLBOX.get())) return held;
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack findFromCurios(ServerPlayer player) {
        return find(player, true);
    }
}

package net.meatwo310.m2toolbox.compat.curios;

import net.meatwo310.m2toolbox.item.M2ToolboxItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.Optional;

public class CuriosCompat {
    public static final String IDENTIFIER = "m2toolbox";

    public static Optional<ItemStack> getToolboxStack(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .resolve()
                .flatMap(inv -> inv.getStacksHandler(IDENTIFIER))
                .flatMap(CuriosCompat::getFirstStack);
    }

    private static Optional<ItemStack> getFirstStack(ICurioStacksHandler slot) {
        var stacks = slot.getStacks();
        if (stacks.getSlots() <= 0) {
            return Optional.empty();
        }

        return Optional.of(stacks.getStackInSlot(0))
                .filter(stack -> stack.is(M2ToolboxItems.TOOLBOX.get()));
    }
}

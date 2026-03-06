package net.meatwo310.m2toolbox.item;

import net.meatwo310.m2toolbox.menu.TrayMenu;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TrayItem extends AbstractContainerItem {
    public TrayItem(Properties properties) {
        super(properties);
    }

    protected AbstractContainerMenu createMenu(int id, Inventory inv, ItemStack stack) {
        return new TrayMenu(id, inv, stack, false);
    }

    @Override
    protected void writeBuf(FriendlyByteBuf buf, ItemStack stack) {
        super.writeBuf(buf, stack);
        buf.writeByte(-1);
    }
}

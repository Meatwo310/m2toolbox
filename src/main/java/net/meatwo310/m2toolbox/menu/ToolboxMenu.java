package net.meatwo310.m2toolbox.menu;

import net.meatwo310.m2toolbox.compat.curios.CuriosCompat;
import net.meatwo310.m2toolbox.handler.ToolboxHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ToolboxMenu extends AbstractItemContainerMenu {

    // Client
    public ToolboxMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, extraData.readItem(), extraData.readBoolean());
    }

    // Server
    public ToolboxMenu(int containerId, Inventory playerInv, ItemStack toolboxStack, boolean fromCurios) {
        super(M2ToolboxMenus.TOOLBOX_MENU.get(), containerId, playerInv, toolboxStack, fromCurios);
    }

    @Override
    public boolean stillValid(Player player) {
        if (fromCurios) {
            return CuriosCompat.getToolboxStack(player)
                    .map(s -> s == containerStack)
                    .orElse(false);
        }
        return super.stillValid(player);
    }

    @Override
    protected ItemStackHandler createHandler() {
        return new ToolboxHandler();
    }

    @Override
    protected void layoutContainerSlots() {
        int startX = 8;
        int startY = 18;
        for (int i = 0; i < 9; i++) {
            this.addSlot(new SlotItemHandler(inventory, i, i * 18 + startX, startY) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    saveOrClearNBT();
                }
            });
        }
    }
}

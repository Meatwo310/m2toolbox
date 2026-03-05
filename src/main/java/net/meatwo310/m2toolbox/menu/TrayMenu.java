package net.meatwo310.m2toolbox.menu;

import net.meatwo310.m2toolbox.handler.ToolboxHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TrayMenu extends AbstractItemContainerMenu {
    // Client
    public TrayMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, extraData.readItem());
    }

    // Server
    public TrayMenu(int containerId, Inventory playerInv, ItemStack toolboxStack) {
        super(M2ToolboxMenus.TRAY_MENU.get(), containerId, playerInv, toolboxStack);
    }

    @Override
    protected ItemStackHandler createHandler() {
        return new ToolboxHandler();
    }

    @Override
    protected void layoutContainerSlots() {
        int cols = 9;
        int startX = 8;
        int toolSlotY = 18;
        int subSlotY = 36;
        for (int i = 0; i < cols; i++) {
            this.addSlot(new SlotItemHandler(inventory, i, i * 18 + startX, toolSlotY) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    containerStack.getOrCreateTag().put("Items", inventory.serializeNBT());
                }
            });
            this.addSlot(new SlotItemHandler(inventory, i + cols, i * 18 + startX, subSlotY) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    containerStack.getOrCreateTag().put("Items", inventory.serializeNBT());
                }
            });
        }
    }
}

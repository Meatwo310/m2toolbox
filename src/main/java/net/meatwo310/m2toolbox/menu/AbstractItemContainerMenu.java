package net.meatwo310.m2toolbox.menu;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AbstractItemContainerMenu extends AbstractContainerMenu {
    protected final ItemStack containerStack;
    protected final ItemStackHandler inventory;

    protected AbstractItemContainerMenu(MenuType<?> type, int id, Inventory playerInv, ItemStack containerStack) {
        super(type, id);
        this.containerStack = containerStack;
        this.inventory = createHandler();
        loadNBT();
        layoutContainerSlots();
        layoutPlayerInventory(playerInv, 8, 140);
    }

    protected abstract ItemStackHandler createHandler();
    protected abstract void layoutContainerSlots();

    protected void loadNBT() {
        if (containerStack.hasTag()) {
            var tag = containerStack.getTag();
            if (tag != null && tag.contains("Items")) {
                this.inventory.deserializeNBT(tag.getCompound("Items"));
            }
        }
    }

    private void layoutPlayerInventory(Inventory playerInv, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, x + col * 18, y + 58));
        }
    }

    protected void saveOrClearNBT() {
        boolean empty = isEmpty();
        if (empty) {
            containerStack.setTag(null);
        } else {
            containerStack.getOrCreateTag().put("Items", inventory.serializeNBT());
        }
    }

    public boolean isEmpty() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem() == containerStack || player.getOffhandItem() == containerStack;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < slots.size()) {
            ItemStack stack = slots.get(slotId).getItem();
            if (stack == this.containerStack) {
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        return ItemStack.EMPTY;
    }
}

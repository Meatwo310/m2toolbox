package net.meatwo310.m2toolbox.menu;

import net.meatwo310.m2toolbox.handler.ToolboxHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ToolboxMenu extends AbstractContainerMenu {
    private final ItemStack toolboxStack;
    private final ItemStackHandler inventory;

    // Client
    public ToolboxMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, extraData.readItem());
    }

    // Server
    public ToolboxMenu(int containerId, Inventory playerInv, ItemStack toolboxStack) {
        super(M2ToolboxMenus.TOOLBOX_MENU.get(), containerId);
        this.toolboxStack = toolboxStack;

        this.inventory = new ToolboxHandler();
        if (toolboxStack.hasTag()) {
            var tag = toolboxStack.getTag();
            if (tag != null && tag.contains("Items")) {
                this.inventory.deserializeNBT(tag.getCompound("Items"));
            }
        }

        int startX = 8;
        int startY = 18;
        for (int i = 0; i < 9; i++) {
            this.addSlot(new SlotItemHandler(inventory, i, i * 18 + startX, startY) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    // スロットの中身が変わったらToolboxのNBTを更新
                    toolboxStack.getOrCreateTag().put("Items", inventory.serializeNBT());
                }
            });
        }

        layoutPlayerInventory(playerInv, 8, 140);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem() == toolboxStack || player.getOffhandItem() == toolboxStack;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < slots.size()) {
            ItemStack stack = slots.get(slotId).getItem();
            if (stack == this.toolboxStack) {
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
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

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        return ItemStack.EMPTY;
    }
}

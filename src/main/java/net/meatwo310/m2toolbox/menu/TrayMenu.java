package net.meatwo310.m2toolbox.menu;

import net.meatwo310.m2toolbox.compat.curios.CuriosCompat;
import net.meatwo310.m2toolbox.handler.ToolboxHandler;
import net.meatwo310.m2toolbox.handler.TrayHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TrayMenu extends AbstractItemContainerMenu {
    @Nullable
    private ItemStack toolboxStack = null;
    private int toolboxSlotIndex = -1;
    private boolean fromToolbox = false;

    // Client
    public TrayMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, extraData.readItem(), extraData.readBoolean());
        this.fromToolbox = extraData.readBoolean();
        if (this.fromToolbox) {
            this.toolboxSlotIndex = extraData.readByte();
        }
    }

    // Server - 直接開く場合
    public TrayMenu(int containerId, Inventory playerInv, ItemStack trayStack, boolean fromCurios) {
        super(M2ToolboxMenus.TRAY_MENU.get(), containerId, playerInv, trayStack, fromCurios);
    }

    // Server - Toolbox経由で開く場合
    public TrayMenu(int containerId, Inventory playerInv, ItemStack trayStack, boolean fromCurios, ItemStack toolboxStack, int toolboxSlotIndex) {
        super(M2ToolboxMenus.TRAY_MENU.get(), containerId, playerInv, trayStack, fromCurios);
        this.toolboxStack = toolboxStack;
        this.toolboxSlotIndex = toolboxSlotIndex;
        this.fromToolbox = true;
    }

    public boolean isFromToolbox() {
        return fromToolbox;
    }

    public int getToolboxSlotIndex() {
        return toolboxSlotIndex;
    }

    @Override
    public boolean stillValid(Player player) {
        if (toolboxStack != null) {
            if (fromCurios) {
                return CuriosCompat.getToolboxStack(player)
                        .map(s -> s == toolboxStack)
                        .orElse(false);
            }
            return player.getMainHandItem() == toolboxStack || player.getOffhandItem() == toolboxStack;
        }
        return super.stillValid(player);
    }

    @Override
    protected ItemStackHandler createHandler() {
        return new TrayHandler();
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
                    onSlotChanged();
                }
            });
            this.addSlot(new SlotItemHandler(inventory, i + cols, i * 18 + startX, subSlotY) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    onSlotChanged();
                }
            });
        }
    }

    private void onSlotChanged() {
        saveOrClearNBT();
        if (toolboxStack != null) {
            syncToToolbox();
        }
    }

    /**
     * Trayの変更内容をToolbox側のNBTに書き戻す。
     * saveOrClearNBT()の後に呼ぶこと。
     */
    private void syncToToolbox() {
        if (toolboxStack == null) return;
        ToolboxHandler toolboxHandler = new ToolboxHandler();
        CompoundTag tag = toolboxStack.getTag();
        if (tag != null && tag.contains("Items")) {
            toolboxHandler.deserializeNBT(tag.getCompound("Items"));
        }
        toolboxHandler.setStackInSlot(toolboxSlotIndex, containerStack);

        boolean toolboxEmpty = true;
        for (int i = 0; i < toolboxHandler.getSlots(); i++) {
            if (!toolboxHandler.getStackInSlot(i).isEmpty()) {
                toolboxEmpty = false;
                break;
            }
        }

        if (toolboxEmpty) {
            toolboxStack.setTag(null);
        } else {
            toolboxStack.getOrCreateTag().put("Items", toolboxHandler.serializeNBT());
        }
    }
}

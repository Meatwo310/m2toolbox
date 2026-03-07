package net.meatwo310.m2toolbox.client.gui;

import net.meatwo310.m2toolbox.M2Toolbox;
import net.meatwo310.m2toolbox.M2ToolboxKeys;
import net.meatwo310.m2toolbox.item.AbstractContainerItem;
import net.meatwo310.m2toolbox.menu.TrayMenu;
import net.meatwo310.m2toolbox.network.M2ToolboxNetworks;
import net.meatwo310.m2toolbox.network.ReopenToolboxPacket;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TrayScreen extends AbstractItemContainerScreen<TrayMenu> {
    private static final ResourceLocation TEXTURE = M2Toolbox.loc("textures/gui/tray.png");

    private static final int BACK_BTN_W = 40;
    private static final int BACK_BTN_H = 12;
    private static final int BACK_BTN_X_OFFSET = 7; // from right edge
    private static final int BACK_BTN_Y_OFFSET = 4;

    public TrayScreen(TrayMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, menu.isFromToolbox()
                ? AbstractContainerItem.getCustomOrIndexedName(menu.containerStack, menu.toolboxSlotIndex)
                : title);
    }

    @Override
    protected ResourceLocation getTexture() {
        return TEXTURE;
    }

    @Override
    protected void init() {
        super.init();

        if (menu.isFromToolbox()) {
            int btnX = this.leftPos + this.imageWidth - BACK_BTN_W - BACK_BTN_X_OFFSET;
            int btnY = this.topPos + BACK_BTN_Y_OFFSET;

            this.addRenderableWidget(
                    Button.builder(Component.translatable(M2ToolboxKeys.GUI_BACK), b ->
                                    M2ToolboxNetworks.CHANNEL.sendToServer(new ReopenToolboxPacket(menu.fromCurios)))
                            .pos(btnX, btnY)
                            .size(BACK_BTN_W, BACK_BTN_H)
                            .build()
            );
        }
    }
}

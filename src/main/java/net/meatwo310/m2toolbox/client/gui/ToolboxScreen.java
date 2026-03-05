package net.meatwo310.m2toolbox.client.gui;

import net.meatwo310.m2toolbox.M2Toolbox;
import net.meatwo310.m2toolbox.item.M2ToolboxItems;
import net.meatwo310.m2toolbox.menu.ToolboxMenu;
import net.meatwo310.m2toolbox.network.M2ToolboxNetwork;
import net.meatwo310.m2toolbox.network.OpenTrayPacket;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ToolboxScreen extends AbstractItemContainerScreen<ToolboxMenu> {
    private static final ResourceLocation TEXTURE = M2Toolbox.loc("textures/gui/toolbox.png");

    private static final int SLOT_COUNT = 9;
    private static final int SLOT_START_X = 8;
    private static final int SLOT_Y = 18;

    private static final int BTN_W = 14;
    private static final int BTN_H = 10;
    private static final int BTN_Y_OFFSET = SLOT_Y + 16 + 2; // = 36

    public ToolboxScreen(ToolboxMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation getTexture() {
        return TEXTURE;
    }

    private int btnX(int slot) {
        return this.leftPos + slot * 18 + SLOT_START_X + 8 - BTN_W / 2;
    }

    private int btnY() {
        return this.topPos + BTN_Y_OFFSET;
    }

    private boolean hasTrayAt(int slot) {
        return menu.getSlot(slot).getItem().is(M2ToolboxItems.TRAY.get());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(guiGraphics, partialTick, mouseX, mouseY);
        renderTrayButtons(guiGraphics, mouseX, mouseY);
    }

    private void renderTrayButtons(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int btnY = btnY();
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!hasTrayAt(i)) continue;
            int btnX = btnX(i);
            boolean hovered = mouseX >= btnX && mouseX < btnX + BTN_W
                    && mouseY >= btnY && mouseY < btnY + BTN_H;

            guiGraphics.fill(
                    btnX,
                    btnY,
                    btnX + BTN_W,
                    btnY + BTN_H,
                    hovered ? 0xFFAAAAAA : 0xFF555555
            );

            guiGraphics.drawCenteredString(
                    this.font,
                    "▼",
                    btnX + BTN_W / 2,
                    btnY + 1,
                    hovered ? 0xFFFFFF : 0xCCCCCC
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int btnY = btnY();
            for (int i = 0; i < SLOT_COUNT; i++) {
                if (!hasTrayAt(i)) continue;
                int btnX = btnX(i);
                if (mouseX >= btnX && mouseX < btnX + BTN_W
                        && mouseY >= btnY && mouseY < btnY + BTN_H) {
                    M2ToolboxNetwork.CHANNEL.sendToServer(new OpenTrayPacket(i));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

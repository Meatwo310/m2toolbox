package net.meatwo310.m2toolbox.client.gui;

import net.meatwo310.m2toolbox.M2Toolbox;
import net.meatwo310.m2toolbox.item.M2ToolboxItems;
import net.meatwo310.m2toolbox.menu.ToolboxMenu;
import net.meatwo310.m2toolbox.network.M2ToolboxNetwork;
import net.meatwo310.m2toolbox.network.OpenTrayPacket;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

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

    private final List<Button> trayButtons = new ArrayList<>();

    public ToolboxScreen(ToolboxMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation getTexture() {
        return TEXTURE;
    }

    @Override
    protected void init() {
        super.init();
        trayButtons.clear();

        int btnY = this.topPos + BTN_Y_OFFSET;
        for (int i = 0; i < SLOT_COUNT; i++) {
            final int slotIndex = i;
            int btnX = this.leftPos + slotIndex * 18 + SLOT_START_X + 8 - BTN_W / 2;

            Button btn = Button.builder(Component.literal("▼"), b ->
                            M2ToolboxNetwork.CHANNEL.sendToServer(new OpenTrayPacket(slotIndex, menu.isFromCurios())))
                    .pos(btnX, btnY)
                    .size(BTN_W, BTN_H)
                    .build();
            btn.visible = isTrayButtonVisible(i);

            trayButtons.add(btn);
            this.addRenderableWidget(btn);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int i = 0; i < SLOT_COUNT; i++) {
                trayButtons.get(i).visible = isTrayButtonVisible(i);
        }
    }

    private boolean isTrayButtonVisible(int slotIndex) {
        return menu.getSlot(slotIndex).getItem().is(M2ToolboxItems.TRAY.get());
    }
}

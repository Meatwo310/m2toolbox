package net.meatwo310.m2toolbox.client.gui;

import net.meatwo310.m2toolbox.M2Toolbox;
import net.meatwo310.m2toolbox.menu.TrayMenu;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TrayScreen extends AbstractItemContainerScreen<TrayMenu> {
    private static final ResourceLocation TEXTURE = M2Toolbox.loc("textures/gui/tray.png");

    public TrayScreen(TrayMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation getTexture() {
        return TEXTURE;
    }
}

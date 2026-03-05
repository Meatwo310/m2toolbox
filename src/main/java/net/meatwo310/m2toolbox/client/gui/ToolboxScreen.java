package net.meatwo310.m2toolbox.client.gui;

import net.meatwo310.m2toolbox.M2Toolbox;
import net.meatwo310.m2toolbox.menu.ToolboxMenu;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ToolboxScreen extends AbstractItemContainerScreen<ToolboxMenu> {
    private static final ResourceLocation TEXTURE = M2Toolbox.loc("textures/gui/toolbox.png");

    public ToolboxScreen(ToolboxMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation getTexture() {
        return TEXTURE;
    }
}

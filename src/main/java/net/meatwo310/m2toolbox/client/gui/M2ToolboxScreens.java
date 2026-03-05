package net.meatwo310.m2toolbox.client.gui;

import net.meatwo310.m2toolbox.menu.M2ToolboxMenus;
import net.minecraft.client.gui.screens.MenuScreens;

public class M2ToolboxScreens {
    public static void register() {
        MenuScreens.register(M2ToolboxMenus.TOOLBOX_MENU.get(), ToolboxScreen::new);
        MenuScreens.register(M2ToolboxMenus.TRAY_MENU.get(), TrayScreen::new);
    }
}

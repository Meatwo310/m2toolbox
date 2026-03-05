package net.meatwo310.m2toolbox.menu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class M2ToolboxMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "m2toolbox");

    public static final RegistryObject<MenuType<ToolboxMenu>> TOOLBOX_MENU = MENUS.register("toolbox_menu",
            () -> IForgeMenuType.create(ToolboxMenu::new));

        public static final RegistryObject<MenuType<TrayMenu>> TRAY_MENU = MENUS.register("tray_menu",
                () -> IForgeMenuType.create(TrayMenu::new));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}

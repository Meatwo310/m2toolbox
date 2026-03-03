package net.meatwo310.m2toolbox.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.meatwo310.m2toolbox.client.gui.ToolboxScreen;
import net.meatwo310.m2toolbox.menu.M2ToolboxMenus;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class M2ToolboxClient {
    public static final Lazy<KeyMapping> OPEN_RADIAL_MENU = Lazy.of(() -> new KeyMapping(
            "key.m2toolbox.open_radial",
            KeyConflictContext.UNIVERSAL,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories.m2toolbox"
    ));

    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        event.register(OPEN_RADIAL_MENU.get());
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MenuScreens.register(M2ToolboxMenus.TOOLBOX_MENU.get(), ToolboxScreen::new);
    }
}

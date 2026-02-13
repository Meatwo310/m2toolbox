package net.meatwo310.m2toolbox.client.event;

import com.mojang.logging.LogUtils;
import net.meatwo310.m2toolbox.client.M2ToolboxClient;
import net.meatwo310.m2toolbox.client.gui.RadialMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class KeyInputHandler {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        while (M2ToolboxClient.OPEN_RADIAL_MENU.get().consumeClick()) {
            Minecraft.getInstance().setScreen(new RadialMenuScreen());
        }
    }
}

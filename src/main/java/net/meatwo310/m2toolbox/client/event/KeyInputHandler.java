package net.meatwo310.m2toolbox.client.event;

import net.meatwo310.m2toolbox.client.M2ToolboxClient;
import net.meatwo310.m2toolbox.client.gui.RadialMenuScreen;
import net.meatwo310.m2toolbox.compat.curios.CuriosCompat;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class KeyInputHandler {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        while (M2ToolboxClient.OPEN_RADIAL_MENU.get().consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) continue;

            CuriosCompat.getToolboxStack(mc.player).ifPresent(stack ->
                    mc.setScreen(new RadialMenuScreen(stack))
            );
        }
    }
}

package net.meatwo310.m2toolbox.client.event;

import net.meatwo310.m2toolbox.client.M2ToolboxClient;
import net.meatwo310.m2toolbox.client.gui.RadialMenuScreen;
import net.meatwo310.m2toolbox.item.M2ToolboxItems;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
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

            // インベントリスロット 0-35 を順に走査してツールボックスを探す
            // スロット 0-8 がホットバー、9-35 がメインインベントリ
            int toolboxSlot = -1;
            ItemStack toolboxStack = ItemStack.EMPTY;
            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.is(M2ToolboxItems.TOOLBOX.get())) {
                    toolboxSlot = i;
                    toolboxStack = stack;
                    break;
                }
            }

            if (toolboxSlot == -1) continue; // ツールボックスが見つからない

            mc.setScreen(new RadialMenuScreen(toolboxSlot, toolboxStack));
        }
    }
}

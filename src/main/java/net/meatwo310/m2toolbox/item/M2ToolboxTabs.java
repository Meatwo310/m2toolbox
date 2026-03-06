package net.meatwo310.m2toolbox.item;

import net.meatwo310.m2toolbox.M2Toolbox;
import net.meatwo310.m2toolbox.M2ToolboxKeys;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class M2ToolboxTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, M2Toolbox.MODID);

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable(M2ToolboxKeys.TAB_KEY))
            .icon(() -> new ItemStack(M2ToolboxItems.TOOLBOX.get()))
            .displayItems((params, output) ->
                    M2ToolboxItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()))
            )
            .build()
    );

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}

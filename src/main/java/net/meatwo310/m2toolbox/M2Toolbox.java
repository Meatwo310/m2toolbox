package net.meatwo310.m2toolbox;

import net.meatwo310.m2toolbox.config.ClientConfig;
import net.meatwo310.m2toolbox.item.M2ToolboxItems;
import net.meatwo310.m2toolbox.item.M2ToolboxTabs;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(M2Toolbox.MODID)
public class M2Toolbox {
    public static final String MODID = "m2toolbox";

    public M2Toolbox(FMLJavaModLoadingContext ctx) {
        IEventBus bus = ctx.getModEventBus();

        M2ToolboxItems.register(bus);
        M2ToolboxTabs.register(bus);

        ctx.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }
}

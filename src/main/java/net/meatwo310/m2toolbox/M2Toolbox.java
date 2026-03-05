package net.meatwo310.m2toolbox;

import net.meatwo310.m2toolbox.config.ClientConfig;
import net.meatwo310.m2toolbox.item.M2ToolboxItems;
import net.meatwo310.m2toolbox.item.M2ToolboxTabs;
import net.meatwo310.m2toolbox.menu.M2ToolboxMenus;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(M2Toolbox.MODID)
public class M2Toolbox {
    public static final String MODID = "m2toolbox";

    public static final String TAB_KEY = "itemGroup." + MODID;

    public static final String KEY_OPEN_RADIAL = "key." + MODID + ".open_radial";
    public static final String KEY_CATEGORY = "key.categories." + MODID;

    public static final String ITEM_OVERSTACKED = "item." + MODID + ".overstacked";

    public M2Toolbox(FMLJavaModLoadingContext ctx) {
        IEventBus bus = ctx.getModEventBus();

        M2ToolboxItems.register(bus);
        M2ToolboxTabs.register(bus);

        M2ToolboxMenus.register(bus);

        ctx.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}

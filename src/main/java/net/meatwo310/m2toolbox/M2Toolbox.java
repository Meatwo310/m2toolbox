package net.meatwo310.m2toolbox;

import net.meatwo310.m2toolbox.config.ClientConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(M2Toolbox.MODID)
public class M2Toolbox {
    public static final String MODID = "m2toolbox";

    public M2Toolbox(FMLJavaModLoadingContext ctx) {
        ctx.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }
}

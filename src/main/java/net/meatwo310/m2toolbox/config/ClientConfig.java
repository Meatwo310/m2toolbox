package net.meatwo310.m2toolbox.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue MENU_RADIUS = BUILDER
            .push("menu")
            .comment("Radius of the radial menu in pixels")
            .defineInRange("menuRadius", 100, 20, 200);

    public static final ForgeConfigSpec.IntValue MENU_INNER_RADIUS = BUILDER
            .comment("Inner radius of the radial menu in pixels")
            .defineInRange("menuInnerRadius", 40, 10, 100);

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}

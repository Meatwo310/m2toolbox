package net.meatwo310.m2toolbox.item;

import net.meatwo310.m2toolbox.M2Toolbox;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class M2ToolboxItems {
    static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, M2Toolbox.MODID);

    public static final RegistryObject<Item> TOOLBOX = ITEMS.register("toolbox", () ->
            new Item(new Item.Properties().stacksTo(1))
    );
    public static final RegistryObject<Item> TRAY = ITEMS.register("tray", () ->
            new Item(new Item.Properties())
    );

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}

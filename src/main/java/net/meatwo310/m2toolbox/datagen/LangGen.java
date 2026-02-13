package net.meatwo310.m2toolbox.datagen;

import net.meatwo310.m2toolbox.M2Toolbox;
import net.meatwo310.m2toolbox.item.M2ToolboxItems;
import net.meatwo310.m2toolbox.item.M2ToolboxTabs;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.data.event.GatherDataEvent;

public class LangGen {
    protected static void register(GatherDataEvent event) {
        var run = event.includeClient();
        var gen = event.getGenerator();
        gen.addProvider(run, (DataProvider.Factory<EnUs>) EnUs::new);
        gen.addProvider(run, (DataProvider.Factory<JaJp>) JaJp::new);
    }

    public static class EnUs extends LanguageProvider {
        public EnUs(PackOutput output) {
            super(output, M2Toolbox.MODID, "en_us");
        }

        @Override
        protected void addTranslations() {
            add(M2ToolboxTabs.TAB_KEY, "m2toolbox");
            addItem(M2ToolboxItems.TOOLBOX, "Toolbox");
            addItem(M2ToolboxItems.TRAY, "Tray");
        }
    }

    public static class JaJp extends LanguageProvider {
        public JaJp(PackOutput output) {
            super(output, M2Toolbox.MODID, "ja_jp");
        }

        @Override
        protected void addTranslations() {
            addItem(M2ToolboxItems.TOOLBOX, "ツールボックス");
            addItem(M2ToolboxItems.TRAY, "トレイ");
        }
    }
}

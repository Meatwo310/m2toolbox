package net.meatwo310.m2toolbox.datagen;

import net.meatwo310.m2toolbox.M2Toolbox;
import net.meatwo310.m2toolbox.M2ToolboxKeys;
import net.meatwo310.m2toolbox.item.M2ToolboxItems;
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
            add(M2ToolboxKeys.TAB_KEY, "m2toolbox");

            addItem(M2ToolboxItems.TOOLBOX, "Toolbox");
            addItem(M2ToolboxItems.TRAY, "Tray");

            add(M2ToolboxKeys.KEY_CATEGORY, "m2toolbox");
            add(M2ToolboxKeys.KEY_OPEN_RADIAL, "Open Radial Menu");
            add(M2ToolboxKeys.KEY_TOGGLE_TOOLTIPS, "Toggle Menu Tooltips");

            add(M2ToolboxKeys.ITEM_OVERSTACKED, "Cannot use while overstacked");
            add(M2ToolboxKeys.RADIAL_HOTBAR_FULL, "Hotbar is full, cannot extract item");
            add(M2ToolboxKeys.RADIAL_MAIN_HAND_EMPTY, "No item to store");

            add(M2ToolboxKeys.GUI_BACK, "Back");
            add(M2ToolboxKeys.GUI_MENU, "Menu");

            add(M2ToolboxKeys.CURIOS_IDENTIFIER, "Toolbox");
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

            add(M2ToolboxKeys.KEY_OPEN_RADIAL, "ラジアルメニューを開く");
            add(M2ToolboxKeys.KEY_TOGGLE_TOOLTIPS, "ツールチップの切り替え");

            add(M2ToolboxKeys.ITEM_OVERSTACKED, "スタックされた状態では使用できません");
            add(M2ToolboxKeys.RADIAL_HOTBAR_FULL, "ホットバーが満杯のため、アイテムを取り出せません");
            add(M2ToolboxKeys.RADIAL_MAIN_HAND_EMPTY, "しまうアイテムがありません");

            add(M2ToolboxKeys.GUI_BACK, "戻る");
            add(M2ToolboxKeys.GUI_MENU, "メニュー");

            add(M2ToolboxKeys.CURIOS_IDENTIFIER, "ツールボックス");
        }
    }
}

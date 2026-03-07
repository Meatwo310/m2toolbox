package net.meatwo310.m2toolbox.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.meatwo310.m2toolbox.client.M2ToolboxClient;
import net.meatwo310.m2toolbox.config.ClientConfig;
import net.meatwo310.m2toolbox.handler.ToolboxHandler;
import net.meatwo310.m2toolbox.handler.TrayHandler;
import net.meatwo310.m2toolbox.item.AbstractContainerItem;
import net.meatwo310.m2toolbox.network.ExtractItemPacket;
import net.meatwo310.m2toolbox.network.M2ToolboxNetworks;
import net.meatwo310.m2toolbox.network.ReopenToolboxPacket;
import net.meatwo310.m2toolbox.network.StoreItemPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class RadialMenuScreen extends Screen {

    // ---- 定数 ----------------------------------------------------------------

    private static final int    ITEM_COUNT     = 10;
    private static final double ANGLE_PER_ITEM = 360.0 / ITEM_COUNT; // 36°
    private static final double HALF_ANGLE     = ANGLE_PER_ITEM / 2.0; // 18°（12時合わせ用オフセット）

    // コンテンツ位置のパラメータ
    /** アイコン中心がセクター中央から外側へずれる比率 (0=内縁, 1=外縁) */
    private static final float CONTENT_RADIUS_RATIO = 0.60f;
    /** 選択時のアイコンスケール */
    private static final float ICON_SCALE_SELECTED  = 1.25f;
    /** 通常時のアイコンスケール */
    private static final float ICON_SCALE_NORMAL    = 1.00f;

    // ---- フェーズ管理 ---------------------------------------------------------

    public enum Phase { TRAY_SELECT, ITEM_SELECT }

    private Phase phase = Phase.TRAY_SELECT;

    // ツールボックス情報（フェーズ1用）
    private final ItemStack toolboxStack;
    private final ItemStack[] trayStacks = new ItemStack[9];

    // トレイ情報（フェーズ2用）
    private int selectedTraySlot = -1;
    private final ItemStack[] toolStacks = new ItemStack[9];

    // ---- コンストラクタ -------------------------------------------------------

    public RadialMenuScreen(ItemStack toolboxStack) {
        super(Component.literal("Radial Menu"));
        this.toolboxStack = toolboxStack;
        loadTrayStacks();
    }

    // ---- データロード --------------------------------------------------------

    private void loadTrayStacks() {
        ToolboxHandler handler = new ToolboxHandler();
        CompoundTag tag = toolboxStack.getTag();
        if (tag != null && tag.contains("Items")) {
            handler.deserializeNBT(tag.getCompound("Items"));
        }
        for (int i = 0; i < 9; i++) trayStacks[i] = handler.getStackInSlot(i);
    }

    private void loadToolStacks(ItemStack trayStack) {
        TrayHandler handler = new TrayHandler();
        CompoundTag tag = trayStack.getTag();
        if (tag != null && tag.contains("Items")) {
            handler.deserializeNBT(tag.getCompound("Items"));
        }
        for (int i = 0; i < 9; i++) toolStacks[i] = handler.getStackInSlot(i);
    }

    // ---- ヘルパー ------------------------------------------------------------

    @Override
    public boolean isPauseScreen() { return false; }

    /**
     * マウス座標からホバー中のセクターインデックスを返す。
     * リング外・中心穴の内側なら -1。
     */
    private int getHoveredIndex(int mouseX, int mouseY) {
        int radius      = ClientConfig.MENU_RADIUS.get();
        int innerRadius = ClientConfig.MENU_INNER_RADIUS.get();
        int cx          = this.width  / 2;
        int cy          = this.height / 2;

        double dx   = mouseX - cx;
        double dy   = mouseY - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < innerRadius || dist > radius) return -1;

        double angleDeg = Math.toDegrees(Math.atan2(dy, dx));
        if (angleDeg < 0) angleDeg += 360;
        double adjusted = (angleDeg + 90 + HALF_ANGLE) % 360;
        return (int) (adjusted / ANGLE_PER_ITEM);
    }

    /** そのインデックスが選択可能（アイテムあり or スタブ）かどうか */
    private boolean isSlotActive(int index) {
        if (index == 0) return true; // 設定 / ユーティリティ（スタブ）
        int slot = index - 1;
        return phase == Phase.TRAY_SELECT
                ? !trayStacks[slot].isEmpty()
                : !toolStacks[slot].isEmpty();
    }

    // ---- レンダリング --------------------------------------------------------

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int radius      = ClientConfig.MENU_RADIUS.get();
        int innerRadius = ClientConfig.MENU_INNER_RADIUS.get();
        int cx          = this.width  / 2;
        int cy          = this.height / 2;
        int hovered     = getHoveredIndex(mouseX, mouseY);

        // アイコン・ラベル
        renderContents(guiGraphics, cx, cy, radius, innerRadius, hovered);

        // 中央ラベル
        renderCenterLabel(guiGraphics, cx, cy);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    /** 全セクターのアイコン・ラベルを描画する */
    private void renderContents(GuiGraphics g, int cx, int cy, int radius, int innerRadius, int hovered) {
        for (int i = 0; i < ITEM_COUNT; i++) {
            boolean active   = isSlotActive(i);
            boolean isHovered = i == hovered;

            double baseStart = Math.toRadians(i * ANGLE_PER_ITEM - 90 - HALF_ANGLE);
            double baseEnd   = Math.toRadians((i + 1) * ANGLE_PER_ITEM - 90 - HALF_ANGLE);
            double midAngle  = (baseStart + baseEnd) / 2.0;

            float contentRadius = innerRadius + (radius - innerRadius) * CONTENT_RADIUS_RATIO;

            int iconCx = (int)(cx + Math.cos(midAngle) * contentRadius);
            int iconCy = (int)(cy + Math.sin(midAngle) * contentRadius);

            renderSlotContent(g, i, iconCx, iconCy, isHovered, active);
        }
    }

    /** 1スロット分のアイコン・ラベルを描画する */
    private void renderSlotContent(GuiGraphics g, int index, int cx, int cy,
                                   boolean hovered, boolean active) {
        if (index == 0) {
            renderSpecialSlot(g, cx, cy, hovered, active);
        } else {
            renderItemSlot(g, index - 1, cx, cy, hovered, active);
        }
    }

    /** インデックス0の特殊スロット（設定 / 戻る）を描画する */
    private void renderSpecialSlot(GuiGraphics g, int cx, int cy,
                                   boolean hovered, boolean active) {
        boolean selected = hovered && active;
        float scale = hovered ? ICON_SCALE_SELECTED : ICON_SCALE_NORMAL;

        String symbol = (phase == Phase.TRAY_SELECT) ? "≡" : "«";
        int color = selected ? 0xFFFFFFFF : (active ? 0xFFDDDDDD : 0xFF444444);
        renderScaledCenteredText(g, symbol, cx, cy, scale, color);

        if (selected) {
            Component label = (phase == Phase.TRAY_SELECT)
                    ? Component.translatable("gui.m2toolbox.menu")
                    : Component.translatable("gui.m2toolbox.back");
            g.drawCenteredString(this.font, label, cx, cy + 12, 0xFFFFFFFF);
        }
    }

    /** インデックス1〜9の通常スロットを描画する */
    private void renderItemSlot(GuiGraphics g, int slot, int cx, int cy,
                                boolean hovered, boolean active) {
        boolean selected = hovered && active;
        float scale = hovered ? ICON_SCALE_SELECTED : ICON_SCALE_NORMAL;

        ItemStack stack = (phase == Phase.TRAY_SELECT) ? trayStacks[slot] : toolStacks[slot];

        if (!stack.isEmpty()) {
            renderScaledItem(g, stack, cx, cy, scale);
            if (selected) {
                Component name = switch (phase) {
                    case TRAY_SELECT -> AbstractContainerItem.getCustomOrIndexedName(stack, slot);
                    case ITEM_SELECT -> stack.getHoverName();
                };
                g.drawCenteredString(this.font, name, cx, cy + 12, 0xFFFFFFFF);
            }
        } else {
            renderEmptySlot(g, slot, cx, cy, hovered, scale);
        }
    }

    /**
     * 空スロットを描画する。
     * ITEM_SELECT フェーズかつホバー中の場合、メインハンドのアイテムをプレビュー表示する。
     */
    private void renderEmptySlot(GuiGraphics g, int slot, int cx, int cy,
                                 boolean hovered, float scale) {
        if (phase != Phase.ITEM_SELECT || !hovered) {
            g.drawCenteredString(this.font, "-", cx, cy - 4, 0xFF333333);
            return;
        }

        // ITEM_SELECT フェーズ・ホバー中: メインハンドのアイテムをプレビュー
        var player = Minecraft.getInstance().player;
        ItemStack mainHandItem = (player != null) ? player.getMainHandItem() : ItemStack.EMPTY;

        if (mainHandItem.isEmpty()) {
            g.drawCenteredString(this.font, "-", cx, cy - 4, 0xFF333333);
            return;
        }

        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(scale, scale, 1.0f);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.4f);
        g.renderItem(mainHandItem, -8, -8);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        g.pose().popPose();
        g.drawCenteredString(this.font, mainHandItem.getHoverName(), cx, cy + 12, 0xFFAAAAAA);
    }

    /** 中央のフェーズ名ラベル */
    private void renderCenterLabel(GuiGraphics g, int cx, int cy) {
        Component text;
        if (phase == Phase.TRAY_SELECT) {
            text = toolboxStack.getHoverName();
        } else if (selectedTraySlot >= 0 && selectedTraySlot < trayStacks.length) {
            text = AbstractContainerItem.getCustomOrIndexedName(trayStacks[selectedTraySlot], selectedTraySlot);
        } else {
            text = Component.literal("?");
        }
        g.drawCenteredString(this.font, text, cx, cy + this.font.lineHeight, 0xFFFFFFFF);
    }

    // ---- スケール描画ユーティリティ ------------------------------------------

    private void renderScaledItem(GuiGraphics g, ItemStack stack, int cx, int cy, float scale) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.renderItem(stack, -8, -8);
        g.pose().popPose();
    }

    private void renderScaledCenteredText(GuiGraphics g, String text, int cx, int cy,
                                          float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(scale, scale, 1.0f);
        int tw = this.font.width(text);
        g.drawString(this.font, text, -tw / 2, -this.font.lineHeight / 2, color, true);
        g.pose().popPose();
    }

    // ---- 入力ハンドリング ----------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int index = getHoveredIndex((int) mouseX, (int) mouseY);

        if (index == -1) {
            this.onClose();
            return true;
        }

        if (index == 0) {
            handleSpecialSlotClick();
            return true;
        }

        handleItemSlotClick(index - 1);
        return true;
    }

    /** インデックス0クリック: 設定を開く / フェーズ1へ戻る */
    private void handleSpecialSlotClick() {
        playSound(SoundEvents.UI_BUTTON_CLICK.get());
        if (phase == Phase.TRAY_SELECT) {
            // フェーズ1: ToolboxGUIを開く（Curiosスロット経由）
            M2ToolboxNetworks.CHANNEL.sendToServer(new ReopenToolboxPacket(true));
            this.onClose();
        } else {
            // フェーズ2: フェーズ1へ戻る
            phase = Phase.TRAY_SELECT;
            selectedTraySlot = -1;
        }
    }

    /** インデックス1〜9クリック: トレイ選択 or アイテム取り出し/しまう */
    private void handleItemSlotClick(int slot) {
        if (phase == Phase.TRAY_SELECT) {
            if (trayStacks[slot].isEmpty()) return;
            playSound(SoundEvents.UI_BUTTON_CLICK.get());
            selectedTraySlot = slot;
            loadToolStacks(trayStacks[slot]);
            phase = Phase.ITEM_SELECT;
            return;
        }

        // ITEM_SELECT フェーズ
        if (!toolStacks[slot].isEmpty()) {
            // アイテムを取り出す
            playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.75F, 1.5F);
            M2ToolboxNetworks.CHANNEL.sendToServer(new ExtractItemPacket(selectedTraySlot, slot));
            this.onClose();
            return;
        }

        // 空スロットへメインハンドのアイテムをしまう
        var player = Minecraft.getInstance().player;
        ItemStack mainHandItem = (player != null) ? player.getMainHandItem() : ItemStack.EMPTY;
        if (mainHandItem.isEmpty()) return;

        playSound(SoundEvents.BUNDLE_INSERT, 0.75F, 1.0F);
        M2ToolboxNetworks.CHANNEL.sendToServer(new StoreItemPacket(selectedTraySlot, slot));
        this.onClose();
    }

    private void playSound(SoundEvent sound) {
        playSound(sound, 1.0F, 1.0F);
    }

    private void playSound(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(sound, pitch, volume)
        );
    }

    @Override
    public boolean keyPressed(int key, int scancode, int mods) {
        if (M2ToolboxClient.OPEN_RADIAL_MENU.get().isActiveAndMatches(
                InputConstants.getKey(key, scancode))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(key, scancode, mods);
    }
}

package net.meatwo310.m2toolbox.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class RadialMenuScreen extends Screen {

    // ---- 定数 ----------------------------------------------------------------

    private static final int    ITEM_COUNT     = 10;
    private static final double ANGLE_PER_ITEM = 360.0 / ITEM_COUNT;   // 36°
    private static final double HALF_ANGLE     = ANGLE_PER_ITEM / 2.0; // 18°（12時合わせ用オフセット）

    // コンテンツ位置のパラメータ
    /** アイコン中心がセクター中央から外側へずれる比率 (0=内縁, 1=外縁) */
    private static final float CONTENT_RADIUS_RATIO = 0.60f;

    /** セクター弧の分割数（値が大きいほど滑らか） */
    private static final int   ARC_SEGMENTS         = 10;

    /** 円周全体の総ステップ数 = ITEM_COUNT × ARC_SEGMENTS */
    private static final int   TOTAL_STEPS          = ITEM_COUNT * ARC_SEGMENTS; // 200

    // ---- 三角関数キャッシュ ---------------------------------------------------

    /**
     * ドーナツ描画用 cos/sin キャッシュ。インデックス = item * ARC_SEGMENTS + i (0〜TOTAL_STEPS)。
     * 対応角度 = toRadians(index * ANGLE_PER_ITEM / ARC_SEGMENTS - 90 - HALF_ANGLE)
     */
    private static final double[] DONUT_COS  = new double[TOTAL_STEPS + 1];
    private static final double[] DONUT_SIN  = new double[TOTAL_STEPS + 1];

    /**
     * 内円描画用 cos/sin キャッシュ。インデックス = i (0〜TOTAL_STEPS)。
     * 対応角度 = 2π * i / TOTAL_STEPS（均等分割）
     */
    private static final double[] CIRCLE_COS = new double[TOTAL_STEPS + 1];
    private static final double[] CIRCLE_SIN = new double[TOTAL_STEPS + 1];

    /**
     * セクター中央方向の cos/sin キャッシュ（renderContents 用）。
     * インデックス = item (0〜ITEM_COUNT-1)。
     * 対応角度 = toRadians(item * ANGLE_PER_ITEM - 90)
     */
    private static final double[] SECTOR_MID_COS = new double[ITEM_COUNT];
    private static final double[] SECTOR_MID_SIN = new double[ITEM_COUNT];

    static {
        for (int step = 0; step <= TOTAL_STEPS; step++) {
            double donutAngle  = Math.toRadians(step * ANGLE_PER_ITEM / ARC_SEGMENTS - 90 - HALF_ANGLE);
            DONUT_COS[step]  = Math.cos(donutAngle);
            DONUT_SIN[step]  = Math.sin(donutAngle);

            double circleAngle = 2.0 * Math.PI * step / TOTAL_STEPS;
            CIRCLE_COS[step] = Math.cos(circleAngle);
            CIRCLE_SIN[step] = Math.sin(circleAngle);
        }
        for (int item = 0; item < ITEM_COUNT; item++) {
            double midAngle        = Math.toRadians(item * ANGLE_PER_ITEM - 90);
            SECTOR_MID_COS[item] = Math.cos(midAngle);
            SECTOR_MID_SIN[item] = Math.sin(midAngle);
        }
    }

    // ---- フェーズ管理 ---------------------------------------------------------

    public enum Phase { TRAY_SELECT, ITEM_SELECT }

    private Phase phase = Phase.TRAY_SELECT;

    // ツールボックス情報（フェーズ1用）
    private final ItemStack toolboxStack;
    private final ItemStack[] trayStacks = new ItemStack[ITEM_COUNT - 1];

    // トレイ情報（フェーズ2用）
    private int selectedTraySlot = -1;
    private final ItemStack[] toolStacks = new ItemStack[ITEM_COUNT - 1];

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

    /** そのインデックスが選択可能（アイテムあり or スタブ）かどうか。インデックス0は特殊ボタン、1-10はアイテム。境界外は false。 */
    private boolean isSectorActive(int index) {
        if (index == 0) {
            return true; // メニュー / 戻る
        }

        int slot = index - 1;
        if (slot < 0 || slot >= trayStacks.length) {
            return false; // 範囲外
        }

        var stack = switch (phase) {
            case TRAY_SELECT -> trayStacks[slot];
            case ITEM_SELECT -> toolStacks[slot];
        };
        return !stack.isEmpty();
    }

    // ---- レンダリング --------------------------------------------------------

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int radius      = ClientConfig.MENU_RADIUS.get();
        int innerRadius = ClientConfig.MENU_INNER_RADIUS.get();
        int cx          = this.width  / 2;
        int cy          = this.height / 2;
        int hovered     = getHoveredIndex(mouseX, mouseY);

        // ラジアル背景（ドーナツ型セクター）
        renderRadialBackground(guiGraphics, cx, cy, radius, innerRadius, hovered);

        // アイコン・ラベル
        renderContents(guiGraphics, cx, cy, radius, innerRadius, hovered);

        // 中央ラベル
        renderCenterLabel(guiGraphics, cx, cy);

        // ツールチップ
        renderTooltip(guiGraphics, mouseX, mouseY, hovered);
    }

    /**
     * ドーナツ型のラジアルメニュー背景を描画する。
     * 各セクターを TRIANGLE_STRIP で塗りつぶし、ホバー中のセクターのみ色を変える。
     */
    private void renderRadialBackground(GuiGraphics guiGraphics, int cx, int cy,
                                        int radius, int innerRadius, int hovered) {
        MultiBufferSource.BufferSource src = guiGraphics.bufferSource();
        drawInnerCircle(src.getBuffer(RadialRenderType.GUI_CIRCLE), cx, cy, innerRadius);
        drawOuterDonut(src.getBuffer(RadialRenderType.GUI_DONUT), cx, cy, radius, innerRadius, hovered);
        src.endBatch();
    }

    private void drawOuterDonut(VertexConsumer buf, int cx, int cy, int radius, int innerRadius, int hovered) {
        for (int item = 0; item < ITEM_COUNT; item++) {
            int color = (item == hovered) ? 0x40000000 : 0x50000000;
            int r = FastColor.ARGB32.red(color);
            int g = FastColor.ARGB32.green(color);
            int b = FastColor.ARGB32.blue(color);
            int a = FastColor.ARGB32.alpha(color);

            for (int i = 0; i <= ARC_SEGMENTS; i++) {
                int    step = item * ARC_SEGMENTS + i;
                double cos  = DONUT_COS[step];
                double sin  = DONUT_SIN[step];

                buf.vertex(cx + cos * radius,      cy + sin * radius,      0).color(r, g, b, a).endVertex();
                buf.vertex(cx + cos * innerRadius, cy + sin * innerRadius, 0).color(r, g, b, a).endVertex();
            }
        }
    }

    private void drawInnerCircle(VertexConsumer buf, int cx, int cy, int innerRadius) {
        int argb = 0x20000000;
        int r = FastColor.ARGB32.red(argb);
        int g = FastColor.ARGB32.green(argb);
        int b = FastColor.ARGB32.blue(argb);
        int a = FastColor.ARGB32.alpha(argb);

        buf.vertex(cx, cy, 0).color(r, g, b, a).endVertex();

        for (int i = TOTAL_STEPS; i >= 0; i--) {
            buf.vertex(cx + CIRCLE_COS[i] * innerRadius, cy + CIRCLE_SIN[i] * innerRadius, 0)
               .color(r, g, b, a).endVertex();
        }
    }

    /** 全セクターのアイコン・ラベルを描画する */
    private void renderContents(GuiGraphics g, int cx, int cy, int radius, int innerRadius, int hovered) {
        float contentRadius = innerRadius + (radius - innerRadius) * CONTENT_RADIUS_RATIO;
        for (int i = 0; i < ITEM_COUNT; i++) {
            boolean active = isSectorActive(i);
            boolean isHovered = i == hovered;

            int iconCx = (int)(cx + SECTOR_MID_COS[i] * contentRadius);
            int iconCy = (int)(cy + SECTOR_MID_SIN[i] * contentRadius);

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
        float scale = getIconScale(hovered);

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
        float scale = getIconScale(hovered);

        ItemStack stack = (phase == Phase.TRAY_SELECT) ? trayStacks[slot] : toolStacks[slot];

        if (!stack.isEmpty()) {
            renderScaledItem(g, stack, cx, cy, scale);
            if (selected) {
                Component name = switch (phase) {
                    case TRAY_SELECT -> AbstractContainerItem.getCustomOrIndexedName(stack, slot);
                    case ITEM_SELECT -> stack.getHoverName();
                };
                g.drawCenteredString(this.font, name, cx, cy + 13, 0xFFFFFFFF);
            }
        } else {
            renderEmptySlot(g, slot, cx, cy, hovered, scale);
        }
    }

    private float getIconScale(boolean hovered) {
        return (hovered ? ClientConfig.MENU_SELECTED_ITEM_SCALE.get() : ClientConfig.MENU_ITEM_SCALE.get()).floatValue();
    }

    /**
     * 空スロットを描画する。
     * ITEM_SELECT フェーズかつホバー中の場合、メインハンドのアイテムをプレビュー表示する。
     */
    private void renderEmptySlot(GuiGraphics g, int slot, int cx, int cy, boolean hovered, float scale) {
        boolean showPreview = phase == Phase.ITEM_SELECT && hovered;

        var player = showPreview ? Minecraft.getInstance().player : null;
        ItemStack mainHandItem = (player != null) ? player.getMainHandItem() : ItemStack.EMPTY;
        boolean hasItem = showPreview && !mainHandItem.isEmpty();

        if (!hasItem) {
            g.drawCenteredString(this.font, Component.literal("-"), cx, cy - 4, 0xFFAAAAAA);
            return;
        }

        renderScaledItem(g, mainHandItem, cx, cy, scale);
        g.drawCenteredString(this.font, mainHandItem.getHoverName(), cx, cy + 13, 0xFFAAAAAA);
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

    private void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, int hovered) {
        if (!shouldRenderItemTooltip() || hovered == 0 || !isSectorActive(hovered)) {
            return;
        }

        int slot = hovered - 1;
        var stack = (switch (phase) {
            case TRAY_SELECT -> trayStacks;
            case ITEM_SELECT -> toolStacks;
        })[slot];

        guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY);
    }

    private boolean shouldRenderItemTooltip() {
        return ClientConfig.MENU_TOOLTIP.get() || hasShiftDown();
    }

    // ---- スケール描画ユーティリティ ------------------------------------------

    private void renderScaledItem(GuiGraphics g, ItemStack stack, int cx, int cy, float scale) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 0);
        pose.scale(scale, scale, 1.0f);
        g.renderItem(stack, -8, -8);
        g.renderItemDecorations(this.font, stack, -8, -8);
        pose.popPose();
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
        var pressed = InputConstants.getKey(key, scancode);
        if (M2ToolboxClient.OPEN_RADIAL_MENU.get().isActiveAndMatches(pressed)) {
            this.onClose();
        } else if (M2ToolboxClient.TOGGLE_MENU_TOOLTIPS.get().isActiveAndMatches(pressed)) {
            var cfg = ClientConfig.MENU_TOOLTIP;
            cfg.set(!cfg.get());
        } else {
            return super.keyPressed(key, scancode, mods);
        }

        return true;
    }
}

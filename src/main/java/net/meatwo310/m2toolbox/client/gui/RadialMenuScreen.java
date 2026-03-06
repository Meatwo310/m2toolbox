package net.meatwo310.m2toolbox.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.meatwo310.m2toolbox.client.M2ToolboxClient;
import net.meatwo310.m2toolbox.config.ClientConfig;
import net.meatwo310.m2toolbox.handler.ToolboxHandler;
import net.meatwo310.m2toolbox.handler.TrayHandler;
import net.meatwo310.m2toolbox.network.ExtractItemPacket;
import net.meatwo310.m2toolbox.network.M2ToolboxNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class RadialMenuScreen extends Screen {

    // ---- 定数 ----------------------------------------------------------------

    private static final int   ITEM_COUNT     = 10;
    private static final double ANGLE_PER_ITEM = 360.0 / ITEM_COUNT; // 36°
    private static final double HALF_ANGLE     = ANGLE_PER_ITEM / 2.0; // 18°（12時合わせ用オフセット）
    private static final int   ARC_SEGMENTS   = 24; // 弧の分割数（滑らかさ）
    private static final int   GAP_PX         = 2;  // セクター間の隙間（ピクセル）

    // 色定数（ARGB ではなく RGBA float）
    private static final float[] COLOR_ACTIVE   = { 0.18f, 0.18f, 0.18f, 0.82f };
    private static final float[] COLOR_SELECTED = { 0.88f, 0.88f, 0.88f, 0.90f };
    private static final float[] COLOR_INACTIVE = { 0.06f, 0.06f, 0.06f, 0.65f };
    private static final float[] COLOR_BG_RING  = { 0.05f, 0.05f, 0.05f, 0.72f };

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
    private final int       toolboxInventorySlot;
    private final ItemStack toolboxStack;
    private final ItemStack[] trayStacks = new ItemStack[9];

    // トレイ情報（フェーズ2用）
    private int selectedTraySlot = -1;
    private final ItemStack[] toolStacks = new ItemStack[9];

    // ---- コンストラクタ -------------------------------------------------------

    public RadialMenuScreen(int toolboxInventorySlot, ItemStack toolboxStack) {
        super(Component.literal("Radial Menu"));
        this.toolboxInventorySlot = toolboxInventorySlot;
        this.toolboxStack         = toolboxStack;
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

    /**
     * セクター i の開始・終了角度 [rad] を計算する。
     * GAP_PX 分だけ両端を内側に詰めてセクター間に隙間を作る。
     *
     * @param i         セクターインデックス
     * @param radius    外縁半径（隙間の角度換算に使用）
     * @param forFill   true=塗り用（隙間あり）, false=クリック判定用（隙間なし）
     * @return [startAngle, endAngle] (rad)
     */
    private double[] sectorAngles(int i, int radius, boolean forFill) {
        double baseStart = Math.toRadians(i * ANGLE_PER_ITEM - 90 - HALF_ANGLE);
        double baseEnd   = Math.toRadians((i + 1) * ANGLE_PER_ITEM - 90 - HALF_ANGLE);
        if (!forFill) return new double[]{ baseStart, baseEnd };
        double gapAngle  = Math.atan2(GAP_PX, radius); // 隙間ピクセル → 角度
        return new double[]{ baseStart + gapAngle, baseEnd - gapAngle };
    }

    // ---- レンダリング --------------------------------------------------------

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int radius      = ClientConfig.MENU_RADIUS.get();
        int innerRadius = ClientConfig.MENU_INNER_RADIUS.get();
        int cx          = this.width  / 2;
        int cy          = this.height / 2;
        int hovered     = getHoveredIndex(mouseX, mouseY);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        // 1. 背景リング（セクター全体を覆う単一の暗い円）
        renderBackgroundRing(guiGraphics, cx, cy, radius, innerRadius);

        // 2. 各セクターの塗り
        renderSegments(guiGraphics, cx, cy, radius, innerRadius, hovered);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        // 3. アイコン・ラベル（Pose スタックで拡縮するため blend 外で可）
        renderContents(guiGraphics, cx, cy, radius, innerRadius, hovered);

        // 4. 中央ラベル
        renderCenterLabel(guiGraphics, cx, cy);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    /** 背景リング: セクター全体をひとつの暗い円で下塗りする */
    private void renderBackgroundRing(GuiGraphics g, int cx, int cy, int radius, int innerRadius) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tess   = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        Matrix4f mat      = g.pose().last().pose();
        float[] c = COLOR_BG_RING;

        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(mat, cx, cy, 0).color(c[0], c[1], c[2], c[3]).endVertex();
        for (int j = 0; j <= ARC_SEGMENTS * ITEM_COUNT; j++) {
            double theta = Math.toRadians((double) j / (ARC_SEGMENTS * ITEM_COUNT) * 360 - 90);
            buf.vertex(mat,
                    (float)(cx + Math.cos(theta) * radius),
                    (float)(cy + Math.sin(theta) * radius), 0)
                    .color(c[0], c[1], c[2], c[3]).endVertex();
        }
        tess.end();
    }

    /** 全セクターの塗りを描画する */
    private void renderSegments(GuiGraphics g, int cx, int cy, int radius, int innerRadius, int hovered) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tess   = Tesselator.getInstance();
        BufferBuilder buf = tess.getBuilder();
        Matrix4f mat      = g.pose().last().pose();

        for (int i = 0; i < ITEM_COUNT; i++) {
            boolean active   = isSlotActive(i);
            boolean selected = active && (i == hovered);
            float[] c = selected ? COLOR_SELECTED : (active ? COLOR_ACTIVE : COLOR_INACTIVE);

            double[] angles = sectorAngles(i, radius, true);
            double startAngle = angles[0];
            double endAngle   = angles[1];
            double midAngle   = (startAngle + endAngle) / 2.0;

            buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            // 扇の中心点は内縁の中点に置く（穴をきれいに抜くため）
            buf.vertex(mat,
                    (float)(cx + Math.cos(midAngle) * innerRadius),
                    (float)(cy + Math.sin(midAngle) * innerRadius), 0)
                    .color(c[0], c[1], c[2], c[3]).endVertex();
            // 外弧
            for (int j = 0; j <= ARC_SEGMENTS; j++) {
                double theta = startAngle + (endAngle - startAngle) * j / ARC_SEGMENTS;
                buf.vertex(mat,
                        (float)(cx + Math.cos(theta) * radius),
                        (float)(cy + Math.sin(theta) * radius), 0)
                        .color(c[0], c[1], c[2], c[3]).endVertex();
            }
            // 内弧（逆順で戻る）
            for (int j = ARC_SEGMENTS; j >= 0; j--) {
                double theta = startAngle + (endAngle - startAngle) * j / ARC_SEGMENTS;
                buf.vertex(mat,
                        (float)(cx + Math.cos(theta) * innerRadius),
                        (float)(cy + Math.sin(theta) * innerRadius), 0)
                        .color(c[0], c[1], c[2], c[3]).endVertex();
            }
            tess.end();
        }
    }

    /** 全セクターのアイコン・ラベルを描画する */
    private void renderContents(GuiGraphics g, int cx, int cy, int radius, int innerRadius, int hovered) {
        for (int i = 0; i < ITEM_COUNT; i++) {
            boolean active   = isSlotActive(i);
            boolean selected = active && (i == hovered);

            double[] angles = sectorAngles(i, radius, false);
            double midAngle = (angles[0] + angles[1]) / 2.0;

            // コンテンツ中心: innerRadius〜radius の CONTENT_RADIUS_RATIO 地点（選択状態に関わらず固定）
            float contentRadius = innerRadius + (radius - innerRadius) * CONTENT_RADIUS_RATIO;

            int iconCx = (int)(cx + Math.cos(midAngle) * contentRadius);
            int iconCy = (int)(cy + Math.sin(midAngle) * contentRadius);

            renderSlotContent(g, i, iconCx, iconCy, selected, active);
        }
    }

    /** 1スロット分のアイコン・ラベルを描画する */
    private void renderSlotContent(GuiGraphics g, int index, int cx, int cy,
                                   boolean selected, boolean active) {
        float scale = selected ? ICON_SCALE_SELECTED : ICON_SCALE_NORMAL;

        if (index == 0) {
            // 設定 / ユーティリティ（スタブ: ⚙ or ★ をテキスト描画）
            String symbol = (phase == Phase.TRAY_SELECT) ? "\u2699" : "\u2605";
            int color = selected ? 0xFFFFFFFF : (active ? 0xFFAAAAAA : 0xFF444444);
            renderScaledCenteredText(g, symbol, cx, cy, scale, color);
        } else {
            int slot = index - 1;
            ItemStack stack = (phase == Phase.TRAY_SELECT) ? trayStacks[slot] : toolStacks[slot];

            if (!stack.isEmpty()) {
                renderScaledItem(g, stack, cx, cy, scale);
                // アイテム名（選択時のみ、アイコン下に表示）
                if (selected) {
                    String name = stack.getHoverName().getString();
                    // 長すぎる場合は省略
                    if (this.font.width(name) > 60) {
                        name = this.font.plainSubstrByWidth(name, 57) + "...";
                    }
                    g.drawCenteredString(this.font, name, cx, cy + 12, 0xFFFFFFFF);
                }
            } else {
                // 空スロット: 薄い "-"
                g.drawCenteredString(this.font, "-", cx, cy - 4, 0xFF333333);
            }
        }
    }

    /** 中央のフェーズ名ラベル */
    private void renderCenterLabel(GuiGraphics g, int cx, int cy) {
        String text = (phase == Phase.TRAY_SELECT) ? "Tray" : "Item";
        g.drawCenteredString(this.font, text, cx, cy - this.font.lineHeight / 2, 0xFFAAAAAA);
    }

    // ---- スケール描画ユーティリティ ------------------------------------------

    /**
     * アイテムアイコンをスケール付きで中心 (cx, cy) に描画する。
     * GuiGraphics.renderItem は 16x16 固定なので Pose スタックで拡縮する。
     */
    private void renderScaledItem(GuiGraphics g, ItemStack stack, int cx, int cy, float scale) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.renderItem(stack, -8, -8);
        g.pose().popPose();
    }

    /** テキストをスケール付きで中心 (cx, cy) に描画する */
    private void renderScaledCenteredText(GuiGraphics g, String text, int cx, int cy,
                                          float scale, int color) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(scale, scale, 1.0f);
        int tw = this.font.width(text);
        g.drawString(this.font, text, -tw / 2, -this.font.lineHeight / 2, color, false);
        g.pose().popPose();
    }

    // ---- 入力ハンドリング ----------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = getHoveredIndex((int) mouseX, (int) mouseY);

        if (button == 1) { // 右クリック: フェーズを戻す or 閉じる
            if (phase == Phase.ITEM_SELECT) {
                phase = Phase.TRAY_SELECT;
                selectedTraySlot = -1;
            } else {
                this.onClose();
            }
            return true;
        }

        if (button == 0) { // 左クリック
            if (index == -1) { // 中心 or 外側
                this.onClose();
                return true;
            }
            if (index == 0) { // スタブ
                return true;
            }

            int slot = index - 1;

            if (phase == Phase.TRAY_SELECT) {
                if (!trayStacks[slot].isEmpty()) {
                    selectedTraySlot = slot;
                    loadToolStacks(trayStacks[slot]);
                    phase = Phase.ITEM_SELECT;
                }
            } else {
                if (!toolStacks[slot].isEmpty()) {
                    M2ToolboxNetwork.CHANNEL.sendToServer(
                            new ExtractItemPacket(toolboxInventorySlot, selectedTraySlot, slot)
                    );
                    this.onClose();
                }
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
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

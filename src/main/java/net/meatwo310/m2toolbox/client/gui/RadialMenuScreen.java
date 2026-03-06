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
    private static final int ITEM_COUNT = 10;
    private static final double ANGLE_PER_ITEM = 360.0 / ITEM_COUNT; // 36度
    private static final double OFFSET = ANGLE_PER_ITEM / 2.0;       // 18度
    private static final int SEGMENTS = 12; // セクター弧の分割数

    public enum Phase { TRAY_SELECT, ITEM_SELECT }

    private Phase phase = Phase.TRAY_SELECT;

    // ツールボックス情報（フェーズ1用）
    private final int toolboxInventorySlot;
    private final ItemStack toolboxStack;
    private final ItemStack[] trayStacks = new ItemStack[9];

    // トレイ情報（フェーズ2用）
    private int selectedTraySlot = -1;
    private final ItemStack[] toolStacks = new ItemStack[9];

    public RadialMenuScreen(int toolboxInventorySlot, ItemStack toolboxStack) {
        super(Component.literal("Radial Menu"));
        this.toolboxInventorySlot = toolboxInventorySlot;
        this.toolboxStack = toolboxStack;
        loadTrayStacks();
    }

    /** ツールボックスのNBTからトレイ一覧を読み込む */
    private void loadTrayStacks() {
        ToolboxHandler handler = new ToolboxHandler();
        CompoundTag tag = toolboxStack.getTag();
        if (tag != null && tag.contains("Items")) {
            handler.deserializeNBT(tag.getCompound("Items"));
        }
        for (int i = 0; i < 9; i++) {
            trayStacks[i] = handler.getStackInSlot(i);
        }
    }

    /** 選択したトレイのNBTからツールスロット一覧を読み込む */
    private void loadToolStacks(ItemStack trayStack) {
        TrayHandler handler = new TrayHandler();
        CompoundTag tag = trayStack.getTag();
        if (tag != null && tag.contains("Items")) {
            handler.deserializeNBT(tag.getCompound("Items"));
        }
        for (int i = 0; i < 9; i++) {
            toolStacks[i] = handler.getStackInSlot(i); // toolスロット 0-8
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * マウス座標からセクターインデックスを返す。
     * 中心に近すぎる・外側すぎる場合は -1。
     */
    private int getSelectedIndex(int mouseX, int mouseY) {
        int radius = ClientConfig.MENU_RADIUS.get();
        int innerRadius = ClientConfig.MENU_INNER_RADIUS.get();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < innerRadius || dist > radius) return -1;

        double angleDeg = Math.toDegrees(Math.atan2(dy, dx));
        if (angleDeg < 0) angleDeg += 360;

        // 0度 = 右方向 → 12時位置(index 0)が上になるよう調整
        double adjustedAngle = (angleDeg + 90 + OFFSET) % 360;
        return (int) (adjustedAngle / ANGLE_PER_ITEM);
    }

    /** インデックスのスロットが選択可能かどうか */
    private boolean isSlotActive(int index) {
        if (index == 0) return true; // 設定/ユーティリティ（スタブ）は常に表示
        int slotIndex = index - 1;
        if (phase == Phase.TRAY_SELECT) {
            return !trayStacks[slotIndex].isEmpty();
        } else {
            return !toolStacks[slotIndex].isEmpty();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // ゲーム画面を透過させ、メニュー自体だけ描画する
        int radius = ClientConfig.MENU_RADIUS.get();
        int innerRadius = ClientConfig.MENU_INNER_RADIUS.get();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int selectedIndex = getSelectedIndex(mouseX, mouseY);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = guiGraphics.pose().last().pose();

        for (int i = 0; i < ITEM_COUNT; i++) {
            boolean isSelected = (i == selectedIndex) && isSlotActive(i);
            boolean isActive = isSlotActive(i);

            double startAngle = Math.toRadians((i * ANGLE_PER_ITEM) - 90 - OFFSET);
            double endAngle   = Math.toRadians(((i + 1) * ANGLE_PER_ITEM) - 90 - OFFSET);
            double midAngle   = (startAngle + endAngle) / 2.0;

            // セクターの色: 無効=暗い, 選択中=明るい, 通常=灰色
            float r, g, b, a;
            if (!isActive) {
                r = 0.08f; g = 0.08f; b = 0.08f; a = 0.40f;
            } else if (isSelected) {
                r = 0.95f; g = 0.95f; b = 0.95f; a = 0.65f;
            } else {
                r = 0.20f; g = 0.20f; b = 0.20f; a = 0.50f;
            }

            // セクター描画（TRIANGLE_FAN）
            buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            buffer.vertex(matrix,
                    (float)(centerX + Math.cos(midAngle) * innerRadius),
                    (float)(centerY + Math.sin(midAngle) * innerRadius), 0)
                    .color(r, g, b, a).endVertex();
            for (int j = 0; j <= SEGMENTS; j++) {
                double theta = startAngle + (endAngle - startAngle) * j / SEGMENTS;
                buffer.vertex(matrix,
                        (float)(centerX + Math.cos(theta) * radius),
                        (float)(centerY + Math.sin(theta) * radius), 0)
                        .color(r, g, b, a).endVertex();
            }
            for (int j = SEGMENTS; j >= 0; j--) {
                double theta = startAngle + (endAngle - startAngle) * j / SEGMENTS;
                buffer.vertex(matrix,
                        (float)(centerX + Math.cos(theta) * innerRadius),
                        (float)(centerY + Math.sin(theta) * innerRadius), 0)
                        .color(r, g, b, a).endVertex();
            }
            tesselator.end();

            // コンテンツ描画（セクター中央）
            float contentRadius = (radius + innerRadius) / 2.0f;
            int cx = (int)(centerX + Math.cos(midAngle) * contentRadius);
            int cy = (int)(centerY + Math.sin(midAngle) * contentRadius);

            if (i == 0) {
                // 12時位置: 設定 or ユーティリティ（スタブ）
                String label = (phase == Phase.TRAY_SELECT) ? "\u2699" : "\u2605"; // ⚙ or ★
                int labelColor = isSelected ? 0xFFFF88 : (isActive ? 0xAAAA66 : 0x555544);
                guiGraphics.drawCenteredString(this.font, label, cx, cy - 4, labelColor);
            } else {
                int slotIndex = i - 1;
                ItemStack stack = (phase == Phase.TRAY_SELECT) ? trayStacks[slotIndex] : toolStacks[slotIndex];

                if (!stack.isEmpty()) {
                    // アイテムアイコン（16x16）
                    guiGraphics.renderItem(stack, cx - 8, cy - 8);
                    // スロット番号（アイコン下）
                    int numColor = isSelected ? 0xFFFFFF : 0xAAAAAA;
                    guiGraphics.drawCenteredString(this.font,
                            String.valueOf(slotIndex + 1), cx, cy + 10, numColor);
                } else {
                    // 空スロット
                    guiGraphics.drawCenteredString(this.font, "-", cx, cy - 4, 0x444444);
                }
            }
        }

        RenderSystem.disableBlend();

        // 中央テキスト
        String centerText = (phase == Phase.TRAY_SELECT) ? "Select Tray" : "Select Item";
        guiGraphics.drawCenteredString(this.font, centerText, centerX, centerY - 4, 0xDDDDDD);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = getSelectedIndex((int) mouseX, (int) mouseY);

        if (button == 1) { // 右クリック
            if (phase == Phase.ITEM_SELECT) {
                // フェーズ2 → フェーズ1 に戻る
                phase = Phase.TRAY_SELECT;
                selectedTraySlot = -1;
            } else {
                this.onClose();
            }
            return true;
        }

        if (button == 0) { // 左クリック
            if (index == -1) {
                // 中心クリック → 閉じる
                this.onClose();
                return true;
            }

            if (index == 0) {
                // 設定/ユーティリティボタン（スタブ）
                return true;
            }

            int slotIndex = index - 1; // 0-8

            if (phase == Phase.TRAY_SELECT) {
                if (!trayStacks[slotIndex].isEmpty()) {
                    selectedTraySlot = slotIndex;
                    loadToolStacks(trayStacks[slotIndex]);
                    phase = Phase.ITEM_SELECT;
                }
            } else {
                // フェーズ2: アイテム選択 → 取り出しパケット送信
                if (!toolStacks[slotIndex].isEmpty()) {
                    M2ToolboxNetwork.CHANNEL.sendToServer(
                            new ExtractItemPacket(toolboxInventorySlot, selectedTraySlot, slotIndex)
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
        if (M2ToolboxClient.OPEN_RADIAL_MENU.get().isActiveAndMatches(InputConstants.getKey(key, scancode))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(key, scancode, mods);
    }
}

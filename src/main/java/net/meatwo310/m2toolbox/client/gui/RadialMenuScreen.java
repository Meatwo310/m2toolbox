package net.meatwo310.m2toolbox.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.meatwo310.m2toolbox.client.M2ToolboxClient;
import net.meatwo310.m2toolbox.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class RadialMenuScreen extends Screen {
    private static final int ITEM_COUNT = 8;

    public RadialMenuScreen() {
        super(Component.literal("Radial Menu"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int radius = ClientConfig.MENU_RADIUS.get();
        int innerRadius = ClientConfig.MENU_INNER_RADIUS.get();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;

        double angleRad = Math.atan2(dy, dx);
        double angleDeg = Math.toDegrees(angleRad);
        if (angleDeg < 0) {
            angleDeg += 360;
        }

        double anglePerItem = 360.0 / ITEM_COUNT;
        double offset = anglePerItem / 2.0;

        double adjustedAngle = (angleDeg + 90 + offset) % 360;

        int selectedIndex = (int) (adjustedAngle / anglePerItem);
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < innerRadius) {
            selectedIndex = -1;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = guiGraphics.pose().last().pose();

        for (int i = 0; i < ITEM_COUNT; i++) {
            boolean isSelected = (i == selectedIndex);

            double startAngle = Math.toRadians((i * anglePerItem) - 90 - offset);
            double endAngle = Math.toRadians(((i + 1) * anglePerItem) - 90 - offset);

            float r = 0.0f, g = 0.0f, b = 0.0f, a = 0.5f;
            if (isSelected) {
                r = 1.0f; g = 1.0f; b = 1.0f; a = 0.6f;
            } else {
                r = 0.2f; g = 0.2f; b = 0.2f; a = 0.4f;
            }

            buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

            buffer.vertex(matrix,
                            (float)(centerX + Math.cos((startAngle + endAngle)/2) * innerRadius),
                            (float)(centerY + Math.sin((startAngle + endAngle)/2) * innerRadius), 0)
                    .color(r, g, b, a).endVertex();

            int segments = 10;
            for (int j = 0; j <= segments; j++) {
                double theta = startAngle + (endAngle - startAngle) * j / segments;
                float px = (float) (centerX + Math.cos(theta) * radius);
                float py = (float) (centerY + Math.sin(theta) * radius);
                buffer.vertex(matrix, px, py, 0).color(r, g, b, a).endVertex();
            }
            for (int j = segments; j >= 0; j--) {
                double theta = startAngle + (endAngle - startAngle) * j / segments;
                float px = (float) (centerX + Math.cos(theta) * innerRadius);
                float py = (float) (centerY + Math.sin(theta) * innerRadius);
                buffer.vertex(matrix, px, py, 0).color(r, g, b, a).endVertex();
            }

            tesselator.end();

            double midAngle = (startAngle + endAngle) / 2.0;
            float textRadius = (float)((radius + innerRadius) / 2.0);
            float tx = (float)(centerX + Math.cos(midAngle) * textRadius);
            float ty = (float)(centerY + Math.sin(midAngle) * textRadius);

            String text = "Item " + (i + 1);
            int color = isSelected ? 0xFFFFFF : 0xAAAAAA;

            guiGraphics.drawCenteredString(this.font, text, (int)tx, (int)ty - 4, color);
        }

        RenderSystem.disableBlend();

        if (selectedIndex != -1) {
            guiGraphics.drawCenteredString(this.font, "Selected: Item " + (selectedIndex + 1), centerX, centerY, 0xFFFFFF);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.onClose();
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

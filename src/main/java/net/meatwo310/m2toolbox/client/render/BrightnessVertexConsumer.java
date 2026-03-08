package net.meatwo310.m2toolbox.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;

@MethodsReturnNonnullByDefault
public class BrightnessVertexConsumer implements VertexConsumer {

    private final VertexConsumer delegate;
    private final int brightnessMul; // 0-255

    public BrightnessVertexConsumer(VertexConsumer delegate, float brightness) {
        this.delegate = delegate;
        this.brightnessMul = Mth.clamp((int)(brightness * 255f), 0, 255);
    }

    // --- アルファを乗算して委譲 ---

    @Override
    public VertexConsumer color(int r, int g, int b, int a) {
        delegate.color((r * brightnessMul) / 255, (g * brightnessMul) / 255, (b * brightnessMul) / 255, a);
        return this;
    }

    @Override
    public void defaultColor(int r, int g, int b, int a) {
        delegate.defaultColor((r * brightnessMul) / 255, (g * brightnessMul) / 255, (b * brightnessMul) / 255, a);
    }

    // --- 残りはそのまま委譲 ---

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        delegate.vertex(x, y, z); return this;
    }
    @Override
    public VertexConsumer uv(float u, float v) {
        delegate.uv(u, v); return this;
    }
    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        delegate.overlayCoords(u, v); return this;
    }
    @Override
    public VertexConsumer uv2(int u, int v) {
        delegate.uv2(u, v); return this;
    }
    @Override
    public VertexConsumer normal(float x, float y, float z) {
        delegate.normal(x, y, z); return this;
    }
    @Override
    public void endVertex()       { delegate.endVertex(); }
    @Override
    public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
}

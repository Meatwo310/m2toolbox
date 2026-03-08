package net.meatwo310.m2toolbox.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BrightnessMultiBufferSource implements MultiBufferSource {
    private final MultiBufferSource wrapped;
    private final float brightness;

    public BrightnessMultiBufferSource(MultiBufferSource wrapped, float brightness) {
        this.wrapped = wrapped;
        this.brightness = brightness;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return new BrightnessVertexConsumer(wrapped.getBuffer(renderType), brightness);
    }

    public static void renderItem(GuiGraphics guiGraphics, ItemStack stack, int cx, int cy, float scale, float brightness) {
        if (stack.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        ItemRenderer renderer = mc.getItemRenderer();
        BakedModel model = renderer.getModel(stack, mc.level, mc.player, 0);
        boolean noBlockLight = !model.usesBlockLight();

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(cx, cy, 150);
        pose.mulPoseMatrix((new Matrix4f()).scaling(1.0F, -1.0F, 1.0F));
        pose.scale(16.0F, 16.0F, 16.0F);
        pose.scale(scale, scale, scale);

        if (noBlockLight) {
            Lighting.setupForFlatItems();
        }

        MultiBufferSource.BufferSource originalSrc = mc.renderBuffers().bufferSource();
        BrightnessMultiBufferSource brightnessSrc = new BrightnessMultiBufferSource(originalSrc, brightness);

        renderer.render(
                stack,
                ItemDisplayContext.GUI,
                false,
                pose,
                brightnessSrc,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                model
        );

        originalSrc.endBatch();

        if (noBlockLight) {
            Lighting.setupFor3DItems();
        }

        pose.popPose();
    }
}

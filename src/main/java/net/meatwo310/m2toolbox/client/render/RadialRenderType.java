package net.meatwo310.m2toolbox.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

public class RadialRenderType extends RenderType {
    public static final RenderType GUI_CIRCLE = create(
            "gui_circle",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLE_FAN,
            256,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_GUI_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .createCompositeState(false)
    );

    public static final RenderType GUI_DONUT = create(
            "gui_donut",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLE_STRIP,
            256,
            false,
            false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_GUI_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .createCompositeState(false)
    );

    private RadialRenderType(String pName, VertexFormat pFormat, VertexFormat.Mode pMode, int pBufferSize, boolean pAffectsCrumbling, boolean pSortOnUpload, Runnable pSetupState, Runnable pClearState) {
        super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
    }
}

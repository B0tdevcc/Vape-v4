package gg.vape.utils.render;

import gg.vape.Vape;
import gg.vape.utils.render.GlImageTexture;
import gg.vape.utils.render.GuiRenderPrimitives;
import gg.vape.utils.render.OpenGlBackendHolder;
import gg.vape.utils.render.RenderBatchBuilder;
import gg.vape.utils.render.RenderBatchManager;
import gg.vape.wrapper.impl.GlStateManager;
import gg.vape.wrapper.impl.Minecraft;
import java.nio.ByteBuffer;
import java.util.function.Supplier;
import org.lwjgl.opengl.GL11;

public class BlurRegionRenderer {
    private int textureId = -1;
    private int regionWidth;
    private boolean textureInitialized = false;
    private static boolean legacyGlobalFlag;
    private int regionHeight;

    public BlurRegionRenderer(int width, int height) {
        this.regionWidth = width;
        this.regionHeight = height;
    }

    private Void copyHorizontalPassSource(int x, float captureScale, int sourceY) {
        GL11.glBindTexture((int)3553, (int)this.textureId);
        GL11.glCopyTexSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)((int)((float)x * captureScale)), (int)sourceY, (int)this.regionWidth, (int)this.regionHeight);
        return null;
    }

    public void setDimensions(int width, int height) {
        double uiScale = Vape.INSTANCE.getClientSettings().getGuiScaleFactor();
        width = (int)((double)width * uiScale);
        height = (int)((double)height * uiScale);
        if (this.regionWidth == width && this.regionHeight == height) {
            return;
        }
        this.regionWidth = width;
        this.regionHeight = height;
        this.textureInitialized = false;
    }

    public static boolean getLegacyGlobalFlag() {
        return legacyGlobalFlag;
    }


    public static void setLegacyGlobalFlag(boolean enabled) {
        legacyGlobalFlag = enabled;
    }

    public void renderBlur(int x, int y, float blurRadius, float inset) {
        RenderBatchBuilder blurBatch;
        Supplier<Void> drawSetupCallback;
        if (!Minecraft.gameSettings().Y$src$Z$1rxemad()) {
            return;
        }
        float captureScale = 2.0f;
        double uiScale = Vape.INSTANCE.getClientSettings().getGuiScaleFactor();
        x = (int)((double)x * uiScale);
        y = (int)((double)y * uiScale);
        int framebufferHeight = Minecraft.h();
        int framebufferWidth = Minecraft.J();
        int sourceY = (int)((float)framebufferHeight - (float)y * captureScale - (float)this.regionHeight);
        int sourceX = (int)((float)x * captureScale);
        // 捕获区域必须完全落在 framebuffer 内：glCopyTexSubImage2D 越界时拷贝会被丢弃，
        // 纹理保持 glTexImage2D(..., null) 的未初始化内容（全黑），而 blur pass 输出 alpha
        // 恒为 1（不透明）⇒ 面板/背景被画成不透明黑色矩形。越界时整帧跳过模糊。
        if (this.regionWidth <= 0 || this.regionHeight <= 0
                || sourceX < 0 || sourceY < 0
                || sourceX + this.regionWidth > framebufferWidth
                || sourceY + this.regionHeight > framebufferHeight) {
            return;
        }
        if (!this.textureInitialized
                || this.allocatedWidth != this.regionWidth
                || this.allocatedHeight != this.regionHeight) {
            this.initializeTexture();
        }
        float renderScale = 0.5f;
        renderScale = (float)((double)renderScale / uiScale);
        OpenGlBackendHolder.backend.pushMatrix();
        if (!GuiRenderPrimitives.d()) {
            OpenGlBackendHolder.backend.scale(renderScale, renderScale, renderScale);
        }
        if (!GuiRenderPrimitives.d()) {
            GL11.glBindTexture((int)3553, (int)this.textureId);
        }
        if (GuiRenderPrimitives.d()) {
            final int capturedX = x;
            drawSetupCallback = () -> this.copyHorizontalPassSource(capturedX, captureScale, sourceY);
            if (this.regionWidth == 0 || this.regionHeight == 0) {
                return;
            }
            blurBatch = new RenderBatchBuilder().setDrawSetupCallback(drawSetupCallback).setTexture(new GlImageTexture(this.textureId)).addBlurPass(x, y, (float)this.regionWidth / 2.0f, (float)this.regionHeight / 2.0f, blurRadius, inset, 1.0f, 0.0f);
            RenderBatchManager.getInstance().queueGuiBatch(blurBatch);
        } else {
            GL11.glCopyTexSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)((int)((float)x * captureScale)), (int)sourceY, (int)this.regionWidth, (int)this.regionHeight);
            GuiRenderPrimitives.v((float)x * captureScale, (float)y * captureScale, this.regionWidth, this.regionHeight, blurRadius, inset, 1.0f, 0);
        }
        if (GuiRenderPrimitives.d()) {
            final int capturedX = x;
            drawSetupCallback = () -> this.copyVerticalPassSource(capturedX, captureScale, sourceY);
            if (this.regionWidth == 0 || this.regionHeight == 0) {
                return;
            }
            blurBatch = new RenderBatchBuilder().setDrawSetupCallback(drawSetupCallback).setTexture(new GlImageTexture(this.textureId)).addBlurPass(x, y, (float)this.regionWidth / 2.0f, (float)this.regionHeight / 2.0f, blurRadius, inset, 1.0f, 1.0f);
            RenderBatchManager.getInstance().queueGuiBatch(blurBatch);
        } else {
            GL11.glCopyTexSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)((int)((float)x * captureScale)), (int)sourceY, (int)this.regionWidth, (int)this.regionHeight);
            GuiRenderPrimitives.v((float)x * captureScale, (float)y * captureScale, this.regionWidth, this.regionHeight, blurRadius, inset, 1.0f, 1);
        }
        OpenGlBackendHolder.backend.popMatrix();
        GlStateManager.bindTexture(0);
    }

    public static boolean isBlurAvailable() {
        boolean ignoredLegacyFlag = BlurRegionRenderer.getLegacyGlobalFlag();
        return true;
    }

    private Void copyVerticalPassSource(int x, float captureScale, int sourceY) {
        GL11.glBindTexture((int)3553, (int)this.textureId);
        GL11.glCopyTexSubImage2D((int)3553, (int)0, (int)0, (int)0, (int)((int)((float)x * captureScale)), (int)sourceY, (int)this.regionWidth, (int)this.regionHeight);
        return null;
    }

    private void initializeTexture() {
        if (this.textureId == -1) {
            this.textureId = GL11.glGenTextures();
        }
        GL11.glBindTexture((int)3553, (int)this.textureId);
        GL11.glTexParameteri((int)3553, (int)10241, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10240, (int)9729);
        GL11.glTexParameteri((int)3553, (int)10242, (int)33071);
        GL11.glTexParameteri((int)3553, (int)10243, (int)33071);
        GL11.glTexImage2D((int)3553, (int)0, (int)6407, (int)this.regionWidth, (int)this.regionHeight, (int)0, (int)6407, (int)5121, (ByteBuffer)null);
        // 记录已分配尺寸：区域尺寸变化后必须重新分配，否则后续 glCopyTexSubImage2D
        // 的拷贝区域会超出纹理尺寸（GL_INVALID_VALUE），纹理内容保持未初始化的全黑。
        this.allocatedWidth = this.regionWidth;
        this.allocatedHeight = this.regionHeight;
        this.textureInitialized = true;
    }

    // 已分配的捕获纹理尺寸（见 initializeTexture 注释）。
    private int allocatedWidth;
    private int allocatedHeight;

    static {
        BlurRegionRenderer.setLegacyGlobalFlag(false);
    }
}

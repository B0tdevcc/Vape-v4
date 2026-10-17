package gg.vape.rotation;

import gg.vape.utils.render.BufferedGuiRenderPrimitives;
import gg.vape.utils.render.RenderMatrix4f;
import gg.vape.wrapper.impl.EntityRenderer;
import gg.vape.wrapper.impl.ForgeVersion;
import gg.vape.wrapper.impl.GameSettings;
import gg.vape.wrapper.impl.Matrix4f;
import gg.vape.wrapper.impl.MatrixStack;
import gg.vape.wrapper.impl.Minecraft;

public class LocalPlayerRotationUtil {

    private static RenderMatrix4f modelViewMatrix;

    public static void setModelViewMatrix(Object object) {
        try {
            if (object == null) {
                modelViewMatrix = null;
                return;
            }
            RenderMatrix4f renderMatrix4f = new Matrix4f(object).m$src$Lgg_vape_utils_render_RenderMatrix4f_$1hodrum();
            if (renderMatrix4f != null && renderMatrix4f.elements != null) {
                renderMatrix4f.elements[12] = 0.0f;
                renderMatrix4f.elements[13] = 0.0f;
                renderMatrix4f.elements[14] = 0.0f;
                modelViewMatrix = renderMatrix4f;
            }
        }
        catch (Throwable throwable) {
            modelViewMatrix = null;
        }
    }

    public static void updateProjectionMatrix(float partialTicks) {
        if (Minecraft.D().getActiveRenderInfo().isNull()) {
            return;
        }
        EntityRenderer renderer = Minecraft.m$src$Lgg_vape_wrapper_impl_EntityRenderer_$13begmf();
        GameSettings settings = Minecraft.gameSettings();
        Matrix4f gameProjectionMatrix = renderer.l(renderer.l(), partialTicks, true);
        // 1.20.1：游戏投影矩阵（joml Matrix4f）有时读出来不可用（空/全零），
        // 此时用同一次调用里 getFov 的结果自建标准透视矩阵兜底，
        // 否则 projectionMatrix 会退化成 resetProjectionMatrix() 的 GUI 正交矩阵，
        // 所有世界叠加物（ESP/Tracers/Arrows/StorageESP/ESP2D 投影）都会失效。
        double cameraFov = renderer.getCameraFov();
        RenderMatrix4f projectionMatrix = null;
        if (!gameProjectionMatrix.isNull()) {
            RenderMatrix4f convertedProjection = gameProjectionMatrix.m$src$Lgg_vape_utils_render_RenderMatrix4f_$1hodrum();
            if (LocalPlayerRotationUtil.isUsableMatrix(convertedProjection)) {
                projectionMatrix = convertedProjection;
            }
        }
        if (projectionMatrix == null && Double.isFinite(cameraFov) && cameraFov > 0.0) {
            double fovDegrees = cameraFov <= 3.2 ? Math.toDegrees(cameraFov) : cameraFov;
            projectionMatrix = LocalPlayerRotationUtil.buildPerspectiveMatrix(fovDegrees);
        }
        if (projectionMatrix == null) {
            return;
        }
        RenderMatrix4f viewRotation;
        if (ForgeVersion.MC_1_21_10.d() && !ForgeVersion.MC_26_1.d() && LocalPlayerRotationUtil.modelViewMatrix != null) {
            viewRotation = LocalPlayerRotationUtil.modelViewMatrix;
        } else {
            MatrixStack matrixStack = MatrixStack.A();
            renderer.B(matrixStack, partialTicks);
            if (settings.k()) {
                renderer.Z(matrixStack, partialTicks);
            }
            Matrix4f viewMatrix = matrixStack.F().getMatrix();
            viewRotation = viewMatrix.isNull() ? null : viewMatrix.m$src$Lgg_vape_utils_render_RenderMatrix4f_$1hodrum();
            // 1.20.1 上新建 MatrixStack 的栈顶是零矩阵（bobHurt/bobView 用 mulPose 乘上去仍是 0），
            // 零 viewRotation 会把投影矩阵乘成零矩阵。相机旋转本来在模型矩阵里（RenderUtil.f()），
            // 所以这里回退单位矩阵（等价于只丢 bob 附加量）。
            if (!LocalPlayerRotationUtil.isUsableMatrix(viewRotation)) {
                viewRotation = new RenderMatrix4f().setIdentity();
            }
        }
        RenderMatrix4f combinedMatrix = new RenderMatrix4f().setIdentity();
        combinedMatrix.multiply(projectionMatrix);
        combinedMatrix.multiply(viewRotation);
        BufferedGuiRenderPrimitives.projectionMatrix = combinedMatrix;
        BufferedGuiRenderPrimitives.viewMatrix = new RenderMatrix4f().setIdentity();
    }

    private static boolean isUsableMatrix(RenderMatrix4f matrix) {
        if (matrix == null || matrix.elements == null) {
            return false;
        }
        for (int elementIndex = 0; elementIndex < 16; ++elementIndex) {
            if (matrix.elements[elementIndex] != 0.0f) {
                return true;
            }
        }
        return false;
    }

    private static RenderMatrix4f buildPerspectiveMatrix(double fovDegrees) {
        int width = Minecraft.J();
        int height = Minecraft.h();
        float aspectRatio = height <= 0 ? 1.0f : (float)width / (float)height;
        if (aspectRatio <= 0.0f) {
            aspectRatio = 1.0f;
        }
        float nearPlane = 0.05f;
        float farPlane = 21000.0f;
        float tangent = (float)Math.tan(Math.toRadians(fovDegrees) / 2.0);
        if (tangent <= 0.0f) {
            tangent = 1.0f;
        }
        RenderMatrix4f matrix = new RenderMatrix4f().setIdentity();
        matrix.elements[0] = 1.0f / (aspectRatio * tangent);
        matrix.elements[5] = 1.0f / tangent;
        matrix.elements[10] = (farPlane + nearPlane) / (nearPlane - farPlane);
        matrix.elements[11] = -1.0f;
        matrix.elements[14] = 2.0f * farPlane * nearPlane / (nearPlane - farPlane);
        matrix.elements[15] = 0.0f;
        return matrix;
    }

    public static void resetProjectionMatrix() {
        LocalPlayerRotationUtil.modelViewMatrix = null;
        float left = 0.0f;
        float right = (float)Minecraft.p().I() / 2.0f;
        float bottom = (float)Minecraft.p().R() / 2.0f;
        float top = 0.0f;
        float nearPlane = -21000.0f;
        float farPlane = 21000.0f;
        BufferedGuiRenderPrimitives.projectionMatrix = new RenderMatrix4f().setIdentity().setOrthographic(left, right, bottom, top, farPlane, nearPlane);
        BufferedGuiRenderPrimitives.viewMatrix = new RenderMatrix4f().setIdentity();
    }
}

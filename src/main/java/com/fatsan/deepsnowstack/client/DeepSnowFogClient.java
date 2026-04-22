package com.fatsan.deepsnowstack.client;

import com.fatsan.deepsnowstack.DeepSnowConfig;
import com.fatsan.deepsnowstack.PrecipTfcCompat;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ViewportEvent;

public final class DeepSnowFogClient {

    private DeepSnowFogClient() {}

    public static void onRenderFog(ViewportEvent.RenderFog event) {
        final Minecraft mc = Minecraft.getInstance();
        final FogSample sample = sampleFog(mc, event.getCamera().getPosition(), (float) event.getPartialTick());
        if (!sample.active()) return;

        // Do NOT shrink far plane => avoids ugly chunk cut-off
        final float view = event.getFarPlaneDistance();

        float fogEnd = Mth.lerp(
            sample.strength(),
            Math.max(32f, view * 0.75f),
            (float) DeepSnowConfig.CLIENT.fogTargetFar.get().doubleValue()
        );
        fogEnd = Math.max(6.0f, fogEnd);

        float fogStart = Mth.lerp(
            sample.strength(),
            fogEnd * 0.45f,
            (float) DeepSnowConfig.CLIENT.fogTargetNear.get().doubleValue()
        );
        fogStart = Mth.clamp(fogStart, 0.05f, fogEnd - 0.05f);

        RenderSystem.setShaderFogStart(fogStart);
        RenderSystem.setShaderFogEnd(fogEnd);

        trySetFogShapeSphere();
    }

    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        final Minecraft mc = Minecraft.getInstance();
        final FogSample sample = sampleFog(mc, event.getCamera().getPosition(), (float) event.getPartialTick());
        if (!sample.active()) return;

        float tr = (float) DeepSnowConfig.CLIENT.fogTargetR.get().doubleValue();
        float tg = (float) DeepSnowConfig.CLIENT.fogTargetG.get().doubleValue();
        float tb = (float) DeepSnowConfig.CLIENT.fogTargetB.get().doubleValue();

        event.setRed(Mth.lerp(sample.strength(), event.getRed(), tr));
        event.setGreen(Mth.lerp(sample.strength(), event.getGreen(), tg));
        event.setBlue(Mth.lerp(sample.strength(), event.getBlue(), tb));
    }

    private static FogSample sampleFog(Minecraft mc, net.minecraft.world.phys.Vec3 cameraPos, float partialTick) {
        if (mc.level == null) return FogSample.INACTIVE;
        if (!DeepSnowConfig.CLIENT.enableSnowFog.get()) return FogSample.INACTIVE;

        final BlockPos pos = BlockPos.containing(cameraPos);

        // Snow only (never rain)
        if (!PrecipTfcCompat.isSnowingHere(mc.level, pos, partialTick)) return FogSample.INACTIVE;

        final float real = PrecipTfcCompat.realIntensity(mc.level, pos);

        final float start = (float) DeepSnowConfig.CLIENT.fogStartIntensity.get().doubleValue();
        final float max = (float) DeepSnowConfig.CLIENT.fogMaxIntensity.get().doubleValue();
        if (real < start) return FogSample.INACTIVE;

        float s = (real - start) / Math.max(1e-6f, (max - start));
        s = Mth.clamp(s, 0f, 1f);

        float curve = (float) DeepSnowConfig.CLIENT.fogCurve.get().doubleValue();
        float strength = 1f - (float) Math.pow(1f - s, Math.max(0.2f, curve));

        return new FogSample(true, strength);
    }

    private record FogSample(boolean active, float strength) {
        private static final FogSample INACTIVE = new FogSample(false, 0f);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void trySetFogShapeSphere() {
        try {
            Class<?> fogShape = Class.forName("net.minecraft.client.renderer.FogShape");
            Object sphere = Enum.valueOf((Class<Enum>) fogShape, "SPHERE");
            RenderSystem.class.getMethod("setShaderFogShape", fogShape).invoke(null, sphere);
        } catch (Throwable ignored) {
        }
    }
}

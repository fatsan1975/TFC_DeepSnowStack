package com.fatsan.deepsnowstack;

import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.ClimateModel;
import net.dries007.tfc.util.tracker.WorldTracker;
import net.dries007.tfc.util.tracker.WeatherHelpers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class PrecipTfcCompat {
    private PrecipTfcCompat() {}

    public static int stackSteps(ServerLevel level, BlockPos pos) {
        float real = realIntensity(level, pos);

        double heavy = DeepSnowConfig.COMMON.heavyThreshold.get();
        double extreme = DeepSnowConfig.COMMON.extremeThreshold.get();

        int steps = computeBaseSteps(level, pos, real, heavy, extreme);

        // Temperature scaling: applied LAST so cold air boosts both light and heavy precip alike,
        // and warm-but-still-snowing weather (0..-2 C) can throttle accumulation below baseline.
        if (DeepSnowConfig.COMMON.enableTemperatureScaling.get()) {
            float tmult = temperatureMultiplier(level, pos);
            if (tmult != 1.0f) {
                float scaled = steps * tmult;
                int whole = Mth.floor(scaled);
                float frac = scaled - whole;
                if (level.random.nextFloat() < frac) whole++;
                steps = whole;
            }
        }

        int cap = DeepSnowConfig.COMMON.maxStepsExtreme.get();
        return Math.max(0, Math.min(steps, cap));
    }

    private static int computeBaseSteps(ServerLevel level, BlockPos pos, float real, double heavy, double extreme) {
        if (real < (float) heavy) return 1;

        int steps = 1;

        if (DeepSnowConfig.COMMON.enableIntensityWeightedSteps.get()) {
            int normalCap = DeepSnowConfig.COMMON.maxStepsNormal.get();
            if (normalCap > 1) {
                float normalized = Mth.clamp((real - (float) heavy) / Math.max(1e-6f, 1f - (float) heavy), 0f, 1f);
                float scaledExtra = normalized * (normalCap - 1);
                int guaranteedExtra = Mth.floor(scaledExtra);
                float fractional = scaledExtra - guaranteedExtra;

                steps += guaranteedExtra;
                if (level.random.nextFloat() < fractional) {
                    steps += 1;
                }
            }
        }

        double p2 = DeepSnowConfig.COMMON.chanceSecondStep.get();
        double p3 = DeepSnowConfig.COMMON.chanceThirdStep.get();

        if (level.random.nextDouble() < p2) steps++;
        if (level.random.nextDouble() < p3) steps++;

        steps = Math.min(steps, DeepSnowConfig.COMMON.maxStepsNormal.get());

        if (DeepSnowConfig.COMMON.extremeDoublesSteps.get() && real >= (float) extreme) {
            steps = Math.min(steps * 2, DeepSnowConfig.COMMON.maxStepsExtreme.get());
        }

        return Math.max(1, steps);
    }

    /**
     * Returns a non-negative scalar by which the per-tick step count should be multiplied,
     * based on TFC's instant temperature at {@code pos}.
     * Above 0 C the result is 0 (no accumulation expected anyway).
     */
    public static float temperatureMultiplier(Level level, BlockPos pos) {
        float t;
        try {
            t = Climate.getInstantTemperature(level, pos);
        } catch (Throwable ignored) {
            return 1.0f;
        }

        if (t > 0.0f) return 0.0f;
        if (t > -2.0f)  return DeepSnowConfig.COMMON.tempMultMild.get().floatValue();
        if (t > -5.0f)  return DeepSnowConfig.COMMON.tempMultNormal.get().floatValue();
        if (t > -8.0f)  return DeepSnowConfig.COMMON.tempMultCold.get().floatValue();
        if (t > -10.0f) return DeepSnowConfig.COMMON.tempMultColder.get().floatValue();
        if (t > -15.0f) return DeepSnowConfig.COMMON.tempMultFrigid.get().floatValue();
        if (t > -20.0f) return DeepSnowConfig.COMMON.tempMultArctic.get().floatValue();
        return DeepSnowConfig.COMMON.tempMultExtreme.get().floatValue();
    }

    public static float realIntensity(ServerLevel level, BlockPos pos) {
        return realIntensity((Level) level, pos);
    }

    public static float realIntensity(Level level, BlockPos pos) {
        try {
            var tracker = WorldTracker.get(level);
            ClimateModel model = tracker.getClimateModel();
            if (!model.supportsRain()) return 0f;

            long ticks = Calendars.get(level).getCalendarTicks();
            float rain = tracker.isWeatherEnabled() ? model.getRain(ticks) : -1f;
            float instant = model.getInstantRainfall(level, pos);

            return Mth.clamp(WeatherHelpers.calculateRealRainIntensity(rain, instant), 0f, 1f);
        } catch (Throwable t) {
            return 0f;
        }
    }

    public static boolean isSnowingHere(Level level, BlockPos pos, float partialTick) {
        if (!level.isRaining()) return false;
        if (level.getRainLevel(partialTick) <= 0.01f) return false;

        try {
            Biome.Precipitation p = WeatherHelpers.getPrecipitationAt(level, pos, Biome.Precipitation.RAIN);
            return p == Biome.Precipitation.SNOW;
        } catch (Throwable t) {
            return false;
        }
    }
}

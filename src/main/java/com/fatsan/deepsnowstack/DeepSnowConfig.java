package com.fatsan.deepsnowstack;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class DeepSnowConfig {

    public static final ModConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    public static final ModConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        var commonPair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = commonPair.getRight();
        COMMON = commonPair.getLeft();

        var clientPair = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = clientPair.getRight();
        CLIENT = clientPair.getLeft();
    }

    public static final class Common {
        public final ModConfigSpec.DoubleValue heavyThreshold;
        public final ModConfigSpec.DoubleValue extremeThreshold;

        public final ModConfigSpec.IntValue maxStepsNormal;
        public final ModConfigSpec.IntValue maxStepsExtreme;

        public final ModConfigSpec.DoubleValue chanceSecondStep;
        public final ModConfigSpec.DoubleValue chanceThirdStep;

        public final ModConfigSpec.BooleanValue enableIntensityWeightedSteps;
        public final ModConfigSpec.BooleanValue extremeDoublesSteps;
        public final ModConfigSpec.BooleanValue enableVerticalOverSpread;
        public final ModConfigSpec.BooleanValue enableSurviveOnFullSnow;

        public final ModConfigSpec.IntValue maxVerticalSearch;

        // Temperature-driven accumulation scaling
        public final ModConfigSpec.BooleanValue enableTemperatureScaling;
        public final ModConfigSpec.DoubleValue tempMultMild;     // (0..-2)  default 0.85
        public final ModConfigSpec.DoubleValue tempMultNormal;   // (-2..-5) default 1.0
        public final ModConfigSpec.DoubleValue tempMultCold;     // (-5..-8) default 1.75
        public final ModConfigSpec.DoubleValue tempMultColder;   // (-8..-10) default 2.25
        public final ModConfigSpec.DoubleValue tempMultFrigid;   // (-10..-15) default 3.0
        public final ModConfigSpec.DoubleValue tempMultArctic;   // (-15..-20) default 4.0
        public final ModConfigSpec.DoubleValue tempMultExtreme;  // (<=-20)   default 5.0

        // Snow leveling / settling (post-storm equalization)
        public final ModConfigSpec.BooleanValue enableSnowLeveling;
        public final ModConfigSpec.IntValue levelingTickInterval;
        public final ModConfigSpec.IntValue levelingChecksPerInterval;
        public final ModConfigSpec.IntValue levelingRadiusChunks;
        public final ModConfigSpec.IntValue levelingMinDifference;
        public final ModConfigSpec.IntValue levelingMinSourceLayers;
        public final ModConfigSpec.DoubleValue levelingDropChance;
        public final ModConfigSpec.BooleanValue levelingAllowNewNeighborPlacement;
        public final ModConfigSpec.BooleanValue levelingDebugLog;

        private Common(ModConfigSpec.Builder b) {
            b.push("accumulation");

            heavyThreshold = b
                .comment("Real precip intensity (0..1) at which snow accumulation becomes 'heavy'.")
                .defineInRange("heavyThreshold", 0.50, 0.0, 1.0);

            extremeThreshold = b
                .comment("Real precip intensity (0..1) at which extreme multiplier applies.")
                .defineInRange("extremeThreshold", 0.75, 0.0, 1.0);

            maxStepsNormal = b
                .comment("Max stacking steps per tick in heavy precip (before extreme multiplier).")
                .defineInRange("maxStepsNormal", 3, 1, 32);

            maxStepsExtreme = b
                .comment("Max stacking steps per tick when extreme multiplier applies.")
                .defineInRange("maxStepsExtreme", 6, 1, 64);

            chanceSecondStep = b
                .comment("During heavy precip, chance to add +1 step (so 2 total).")
                .defineInRange("chanceSecondStep", 0.50, 0.0, 1.0);

            chanceThirdStep = b
                .comment("During heavy precip, additional chance to add +1 step (so 3 total).")
                .defineInRange("chanceThirdStep", 0.25, 0.0, 1.0);

            enableIntensityWeightedSteps = b
                .comment("Blend intensity into accumulation so stronger storms produce more consistent extra steps.")
                .define("enableIntensityWeightedSteps", true);

            extremeDoublesSteps = b
                .comment("If true, when intensity >= extremeThreshold, steps are multiplied by 2 (capped by maxStepsExtreme).")
                .define("extremeDoublesSteps", true);

            enableVerticalOverSpread = b
                .comment("Prefer vertical stacking over TFC spreading when the current snow layer is full (8).")
                .define("enableVerticalOverSpread", true);

            enableSurviveOnFullSnow = b
                .comment("Allow snow layers to survive when placed on top of a full (8-layer) snow layer (enables >8 stacking).")
                .define("enableSurviveOnFullSnow", true);

            maxVerticalSearch = b
                .comment("Safety cap for searching upward in a snow column.")
                .defineInRange("maxVerticalSearch", 64, 8, 256);

            b.pop();

            b.push("temperature");

            enableTemperatureScaling = b
                .comment(
                    "Scale snow accumulation rate by TFC instant temperature (Celsius). When false the mod behaves as before.")
                .define("enableTemperatureScaling", true);

            tempMultMild = b
                .comment("Multiplier for temperatures in the (0, -2] C range. Slightly slows accumulation when <1.")
                .defineInRange("tempMultMild", 0.85, 0.0, 16.0);

            tempMultNormal = b
                .comment("Multiplier for temperatures in (-2, -5] C. 1.0 keeps mod's current baseline.")
                .defineInRange("tempMultNormal", 1.0, 0.0, 16.0);

            tempMultCold = b
                .comment("Multiplier for temperatures in (-5, -8] C.")
                .defineInRange("tempMultCold", 1.75, 0.0, 16.0);

            tempMultColder = b
                .comment("Multiplier for temperatures in (-8, -10] C.")
                .defineInRange("tempMultColder", 2.25, 0.0, 16.0);

            tempMultFrigid = b
                .comment("Multiplier for temperatures in (-10, -15] C.")
                .defineInRange("tempMultFrigid", 3.0, 0.0, 16.0);

            tempMultArctic = b
                .comment("Multiplier for temperatures in (-15, -20] C.")
                .defineInRange("tempMultArctic", 4.0, 0.0, 16.0);

            tempMultExtreme = b
                .comment("Multiplier for temperatures <= -20 C.")
                .defineInRange("tempMultExtreme", 5.0, 0.0, 16.0);

            b.pop();

            b.push("leveling");

            enableSnowLeveling = b
                .comment(
                    "When enabled, snow layers slowly equalize with neighbors of the same terrain height while it is NOT precipitating.",
                    "This avoids permanent uneven 'spikes' left over from storms without flattening mountain snow.")
                .define("enableSnowLeveling", true);

            levelingTickInterval = b
                .comment("Server ticks between leveling passes. Higher = slower & cheaper.")
                .defineInRange("levelingTickInterval", 40, 5, 6000);

            levelingChecksPerInterval = b
                .comment("How many random surface positions are inspected per pass per online player.")
                .defineInRange("levelingChecksPerInterval", 3, 0, 64);

            levelingRadiusChunks = b
                .comment("Radius (chunks) around each player from which leveling samples are drawn.")
                .defineInRange("levelingRadiusChunks", 6, 1, 16);

            levelingMinDifference = b
                .comment("Source must have at least this many more snow layers than the neighbor before a layer is moved.")
                .defineInRange("levelingMinDifference", 2, 1, 8);

            levelingMinSourceLayers = b
                .comment("Source snow layer count must be at least this much. Avoids destroying the last layer.")
                .defineInRange("levelingMinSourceLayers", 2, 2, 8);

            levelingDropChance = b
                .comment("Probability per eligible source position that a single layer actually transfers this pass. Low = subtle.")
                .defineInRange("levelingDropChance", 0.20, 0.0, 1.0);

            levelingAllowNewNeighborPlacement = b
                .comment("If true, leveling may place a new 1-layer snow on an adjacent same-height air position. False = only feed existing snow neighbors.")
                .define("levelingAllowNewNeighborPlacement", true);

            levelingDebugLog = b
                .comment("Verbose log every leveling sample (where it looked, why it skipped, what it moved). Use only while testing.")
                .define("levelingDebugLog", false);

            b.pop();
        }
    }

    public static final class Client {
        public final ModConfigSpec.BooleanValue enableSnowFog;

        public final ModConfigSpec.DoubleValue fogStartIntensity;
        public final ModConfigSpec.DoubleValue fogMaxIntensity;
        public final ModConfigSpec.DoubleValue fogCurve;

        public final ModConfigSpec.DoubleValue fogTargetFar;
        public final ModConfigSpec.DoubleValue fogTargetNear;

        public final ModConfigSpec.DoubleValue fogTargetR;
        public final ModConfigSpec.DoubleValue fogTargetG;
        public final ModConfigSpec.DoubleValue fogTargetB;

        private Client(ModConfigSpec.Builder b) {
            b.push("fog");

            enableSnowFog = b
                .comment("Enable blizzard fog on the client during snow (never during rain).")
                .define("enableSnowFog", true);

            fogStartIntensity = b
                .comment("Intensity where fog starts (0..1). Below this, fog is off.")
                .defineInRange("fogStartIntensity", 0.50, 0.0, 1.0);

            fogMaxIntensity = b
                .comment("Intensity treated as 'max fog' (0..1).")
                .defineInRange("fogMaxIntensity", 0.90, 0.0, 1.0);

            fogCurve = b
                .comment("Fog curve exponent. Higher => faster ramp after start.")
                .defineInRange("fogCurve", 3.0, 0.2, 8.0);

            fogTargetFar = b
                .comment("Fog end distance at max fog (blocks).")
                .defineInRange("fogTargetFar", 8.0, 2.0, 128.0);

            fogTargetNear = b
                .comment("Fog start distance tweak at max fog.")
                .defineInRange("fogTargetNear", 0.10, 0.0, 10.0);

            fogTargetR = b.defineInRange("fogTargetR", 0.92, 0.0, 1.0);
            fogTargetG = b.defineInRange("fogTargetG", 0.92, 0.0, 1.0);
            fogTargetB = b.defineInRange("fogTargetB", 0.97, 0.0, 1.0);

            b.pop();
        }
    }
}

package com.fatsan.deepsnowstack.mixin;

import com.fatsan.deepsnowstack.PrecipTfcCompat;
import net.dries007.tfc.util.tracker.WeatherHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WeatherHelpers.class)
public class WeatherHelpersSnowStackMixin {

    @Inject(
        method = "placeSnowOrSnowPileAt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private static void deepsnowstack$stackSnowLayersModern(ServerLevel level, BlockPos pos, BlockState stateAtPos, CallbackInfoReturnable<Boolean> cir) {
        deepsnowstack$applyStackSnowLayers(level, pos, stateAtPos, cir);
    }

    @Inject(
        method = "placeSnowOrSnowPileAt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private static void deepsnowstack$stackSnowLayersLegacy(ServerLevel level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        deepsnowstack$applyStackSnowLayers(level, pos, level.getBlockState(pos), cir);
    }

    @Unique
    private static void deepsnowstack$applyStackSnowLayers(ServerLevel level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!(state.getBlock() instanceof SnowLayerBlock) || !state.hasProperty(SnowLayerBlock.LAYERS)) return;

        int steps = PrecipTfcCompat.stackSteps(level, pos);

        if (steps <= 0) {
            // Temperature multiplier rolled the tick out — skip both our work AND TFC's default
            // single-layer placement so warm-snow conditions actually accumulate slower.
            cir.setReturnValue(false);
            return;
        }

        boolean didAnything = false;
        for (int i = 0; i < steps; i++) {
            if (!deepsnowstack$accumulateHere(level, pos)) break;
            didAnything = true;
        }

        if (didAnything) {
            cir.setReturnValue(true);
        }
    }

    private static boolean deepsnowstack$accumulateHere(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SnowLayerBlock) || !state.hasProperty(SnowLayerBlock.LAYERS)) return false;

        int layers = state.getValue(SnowLayerBlock.LAYERS);
        if (layers < 8) {
            level.setBlock(pos, state.setValue(SnowLayerBlock.LAYERS, layers + 1), 3);
            return true;
        }
        return false;
    }
}

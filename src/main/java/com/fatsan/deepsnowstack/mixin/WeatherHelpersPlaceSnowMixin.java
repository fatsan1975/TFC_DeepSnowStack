package com.fatsan.deepsnowstack.mixin;

import com.fatsan.deepsnowstack.DeepSnowConfig;
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
public class WeatherHelpersPlaceSnowMixin {

    @Inject(
        method = "placeSnowOrSnowPile(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void deepsnowstack$preferVerticalWhenFull(ServerLevel level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!DeepSnowConfig.COMMON.enableVerticalOverSpread.get()) return;

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SnowLayerBlock && state.hasProperty(SnowLayerBlock.LAYERS)) {
            int layers = state.getValue(SnowLayerBlock.LAYERS);
            if (layers >= 8) {
                int steps = PrecipTfcCompat.stackSteps(level, pos);

                if (steps <= 0) {
                    cir.setReturnValue(false);
                    return;
                }

                boolean didAnything = false;
                for (int i = 0; i < steps; i++) {
                    BlockState now = level.getBlockState(pos);
                    if (!deepsnowstack$growSnowColumnAbove(level, pos, now)) break;
                    didAnything = true;
                }
                if (didAnything) cir.setReturnValue(true);
            }
        }
    }

    @Unique
    private static boolean deepsnowstack$growSnowColumnAbove(ServerLevel level, BlockPos pos, BlockState baseState) {
        BlockState oneLayerSameType = baseState.getBlock().defaultBlockState()
            .setValue(SnowLayerBlock.LAYERS, 1);

        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);

        if (aboveState.getBlock() instanceof SnowLayerBlock && aboveState.hasProperty(SnowLayerBlock.LAYERS)) {
            int aboveLayers = aboveState.getValue(SnowLayerBlock.LAYERS);
            if (aboveLayers < 8) {
                level.setBlock(above, aboveState.setValue(SnowLayerBlock.LAYERS, aboveLayers + 1), 3);
                return true;
            }

            BlockPos cursor = above;
            BlockState cursorState = aboveState;

            int cap = DeepSnowConfig.COMMON.maxVerticalSearch.get();
            for (int i = 0; i < cap; i++) {
                if (!(cursorState.getBlock() instanceof SnowLayerBlock) || !cursorState.hasProperty(SnowLayerBlock.LAYERS)) return false;

                int cl = cursorState.getValue(SnowLayerBlock.LAYERS);
                if (cl < 8) {
                    level.setBlock(cursor, cursorState.setValue(SnowLayerBlock.LAYERS, cl + 1), 3);
                    return true;
                }

                BlockPos next = cursor.above();
                BlockState nextState = level.getBlockState(next);

                if (nextState.getBlock() instanceof SnowLayerBlock && nextState.hasProperty(SnowLayerBlock.LAYERS)) {
                    cursor = next;
                    cursorState = nextState;
                    continue;
                }

                if (nextState.isAir() && oneLayerSameType.canSurvive(level, next)) {
                    level.setBlock(next, oneLayerSameType, 3);
                    return true;
                }

                return false;
            }

            return false;
        }

        if (aboveState.isAir() && oneLayerSameType.canSurvive(level, above)) {
            level.setBlock(above, oneLayerSameType, 3);
            return true;
        }

        return false;
    }
}

package com.fatsan.deepsnowstack.mixin;

import com.fatsan.deepsnowstack.DeepSnowConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SnowLayerBlock.class)
public class SnowLayerSupportMixin {

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void deepsnowstack$surviveOnFullSnowLayer(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!DeepSnowConfig.COMMON.enableSurviveOnFullSnow.get()) return;

        BlockState below = level.getBlockState(pos.below());
        if (below.getBlock() instanceof SnowLayerBlock && below.hasProperty(SnowLayerBlock.LAYERS)) {
            int belowLayers = below.getValue(SnowLayerBlock.LAYERS);
            if (belowLayers >= 8) {
                cir.setReturnValue(true);
            }
        }
    }
}

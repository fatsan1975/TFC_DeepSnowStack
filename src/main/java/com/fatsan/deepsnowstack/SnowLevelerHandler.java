package com.fatsan.deepsnowstack;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;

import java.util.List;

public final class SnowLevelerHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static long tickAccumulator = 0L;

    private SnowLevelerHandler() {}

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        Level lvl = event.getLevel();
        if (!(lvl instanceof ServerLevel level)) return;
        if (!event.hasTime()) return;
        if (!DeepSnowConfig.COMMON.enableSnowLeveling.get()) return;

        int interval = DeepSnowConfig.COMMON.levelingTickInterval.get();
        tickAccumulator++;
        if (tickAccumulator % interval != 0L) return;

        int checksPerPlayer = DeepSnowConfig.COMMON.levelingChecksPerInterval.get();
        if (checksPerPlayer <= 0) return;

        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;

        int radius = DeepSnowConfig.COMMON.levelingRadiusChunks.get();
        boolean debug = DeepSnowConfig.COMMON.levelingDebugLog.get();
        RandomSource rand = level.random;

        for (ServerPlayer player : players) {
            BlockPos playerPos = player.blockPosition();
            int pcx = playerPos.getX() >> 4;
            int pcz = playerPos.getZ() >> 4;

            for (int i = 0; i < checksPerPlayer; i++) {
                int dx = rand.nextInt(radius * 2 + 1) - radius;
                int dz = rand.nextInt(radius * 2 + 1) - radius;
                int cx = pcx + dx;
                int cz = pcz + dz;

                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    if (debug) LOGGER.info("[DSS-leveling] chunk ({}, {}) not loaded, skip", cx, cz);
                    continue;
                }

                trySettleOneInChunk(level, chunk, new ChunkPos(cx, cz), rand, debug);
            }
        }
    }

    private static void trySettleOneInChunk(ServerLevel level, LevelChunk chunk, ChunkPos cp, RandomSource rand, boolean debug) {
        int relX = rand.nextInt(16);
        int relZ = rand.nextInt(16);
        int worldX = cp.getMinBlockX() + relX;
        int worldZ = cp.getMinBlockZ() + relZ;

        // WORLD_SURFACE counts every non-air block (including 1-layer snow) — MOTION_BLOCKING
        // would skip layers 1..4 because they have no collision shape.
        int topY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, relX, relZ);
        BlockPos pos = new BlockPos(worldX, topY - 1, worldZ);

        BlockState state = chunk.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof SnowLayerBlock) || !state.hasProperty(SnowLayerBlock.LAYERS)) {
            if (debug) LOGGER.info("[DSS-leveling] {} top is {} (not snow) — skip", pos, block);
            return;
        }

        if (PrecipTfcCompat.isSnowingHere(level, pos, 0f)) {
            if (debug) LOGGER.info("[DSS-leveling] {} is currently snowing — skip", pos);
            return;
        }

        int layers = state.getValue(SnowLayerBlock.LAYERS);
        int minSource = DeepSnowConfig.COMMON.levelingMinSourceLayers.get();
        if (layers < minSource) {
            if (debug) LOGGER.info("[DSS-leveling] {} layers={} < minSource={} — skip", pos, layers, minSource);
            return;
        }

        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        if (below.getBlock() instanceof SnowLayerBlock) {
            if (debug) LOGGER.info("[DSS-leveling] {} sits on snow ({}), source must be ground-rooted — skip", pos, below.getBlock());
            return;
        }
        if (!below.isFaceSturdy(level, belowPos, Direction.UP)) {
            if (debug) LOGGER.info("[DSS-leveling] {} below ({}) is not sturdy — skip", pos, below.getBlock());
            return;
        }

        int minDiff = DeepSnowConfig.COMMON.levelingMinDifference.get();
        double dropChance = DeepSnowConfig.COMMON.levelingDropChance.get();
        boolean allowNew = DeepSnowConfig.COMMON.levelingAllowNewNeighborPlacement.get();

        Direction[] dirs = SHUFFLED_HORIZONTALS[rand.nextInt(SHUFFLED_HORIZONTALS.length)];
        for (Direction dir : dirs) {
            BlockPos nPos = pos.relative(dir);
            BlockState nState = level.getBlockState(nPos);
            BlockPos nBelowPos = nPos.below();
            BlockState nBelow = level.getBlockState(nBelowPos);

            boolean neighborGroundOk = !(nBelow.getBlock() instanceof SnowLayerBlock)
                && nBelow.isFaceSturdy(level, nBelowPos, Direction.UP);

            if (nState.getBlock() == block && nState.hasProperty(SnowLayerBlock.LAYERS)) {
                int nLayers = nState.getValue(SnowLayerBlock.LAYERS);
                if (nLayers >= 8) continue;
                if ((layers - nLayers) < minDiff) {
                    if (debug) LOGGER.info("[DSS-leveling] {} -> {} diff {}<{} — skip", pos, nPos, layers - nLayers, minDiff);
                    continue;
                }
                if (rand.nextDouble() >= dropChance) {
                    if (debug) LOGGER.info("[DSS-leveling] {} -> {} eligible but drop chance failed", pos, nPos);
                    return;
                }
                transferLayer(level, pos, state, layers, nPos, nState, nLayers);
                if (debug) LOGGER.info("[DSS-leveling] TRANSFER {}({}->{}), {}({}->{})",
                    pos, layers, layers - 1, nPos, nLayers, nLayers + 1);
                return;
            }

            if (allowNew && nState.isAir() && neighborGroundOk) {
                BlockState newState = block.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1);
                if (!newState.canSurvive(level, nPos)) {
                    if (debug) LOGGER.info("[DSS-leveling] {} -> {} new placement: canSurvive=false", pos, nPos);
                    continue;
                }
                if ((layers - 1) < minDiff) {
                    if (debug) LOGGER.info("[DSS-leveling] {} -> {} new placement: layers-1<{} — skip", pos, nPos, minDiff);
                    continue;
                }
                if (rand.nextDouble() >= dropChance) {
                    if (debug) LOGGER.info("[DSS-leveling] {} -> {} new placement: drop chance failed", pos, nPos);
                    return;
                }
                placeNewLayer(level, pos, state, layers, nPos, newState);
                if (debug) LOGGER.info("[DSS-leveling] PLACE NEW {}({}->{}) -> {}(0->1)", pos, layers, layers - 1, nPos);
                return;
            }
        }

        if (debug) LOGGER.info("[DSS-leveling] {} layers={} no eligible neighbor", pos, layers);
    }

    private static void transferLayer(ServerLevel level, BlockPos src, BlockState srcState, int srcLayers,
                                      BlockPos dst, BlockState dstState, int dstLayers) {
        level.setBlock(dst, dstState.setValue(SnowLayerBlock.LAYERS, dstLayers + 1), 2);
        level.setBlock(src, srcState.setValue(SnowLayerBlock.LAYERS, srcLayers - 1), 2);
    }

    private static void placeNewLayer(ServerLevel level, BlockPos src, BlockState srcState, int srcLayers,
                                      BlockPos dst, BlockState newOneLayer) {
        level.setBlock(dst, newOneLayer, 2);
        level.setBlock(src, srcState.setValue(SnowLayerBlock.LAYERS, srcLayers - 1), 2);
    }

    private static final Direction[][] SHUFFLED_HORIZONTALS = new Direction[][] {
        { Direction.NORTH, Direction.EAST,  Direction.SOUTH, Direction.WEST  },
        { Direction.EAST,  Direction.SOUTH, Direction.WEST,  Direction.NORTH },
        { Direction.SOUTH, Direction.WEST,  Direction.NORTH, Direction.EAST  },
        { Direction.WEST,  Direction.NORTH, Direction.EAST,  Direction.SOUTH },
        { Direction.NORTH, Direction.WEST,  Direction.SOUTH, Direction.EAST  },
        { Direction.EAST,  Direction.NORTH, Direction.WEST,  Direction.SOUTH },
        { Direction.SOUTH, Direction.EAST,  Direction.NORTH, Direction.WEST  },
        { Direction.WEST,  Direction.SOUTH, Direction.EAST,  Direction.NORTH },
    };
}

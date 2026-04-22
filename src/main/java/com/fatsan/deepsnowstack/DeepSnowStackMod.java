package com.fatsan.deepsnowstack;

import com.fatsan.deepsnowstack.client.DeepSnowFogClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(DeepSnowStackMod.MODID)
public final class DeepSnowStackMod {

    public static final String MODID = "deepsnowstack";

    public DeepSnowStackMod(ModContainer container) {
        // Configs
        container.registerConfig(ModConfig.Type.COMMON, DeepSnowConfig.COMMON_SPEC, "deepsnowstack-common.toml");
        container.registerConfig(ModConfig.Type.CLIENT, DeepSnowConfig.CLIENT_SPEC, "deepsnowstack-client.toml");

        // Server-side: post-storm snow leveling.
        NeoForge.EVENT_BUS.addListener(SnowLevelerHandler::onLevelTickPost);

        // Client-only fog listeners (GAME bus)
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.addListener(DeepSnowFogClient::onRenderFog);
            NeoForge.EVENT_BUS.addListener(DeepSnowFogClient::onFogColor);
            System.out.println("[DeepSnowStack] Fog listeners attached to NeoForge.EVENT_BUS");
        }
    }
}

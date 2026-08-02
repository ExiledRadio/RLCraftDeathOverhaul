package com.exiledradio.rlcraftdeathpenalty;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = RLCraftDeathPenalty.MODID,
        name = RLCraftDeathPenalty.NAME,
        version = RLCraftDeathPenalty.VERSION,
        guiFactory = "com.exiledradio.rlcraftdeathpenalty.ModGuiFactory",
        // Hard dependency. Every health change this mod makes goes through Scaling
        // Health's player data, so there is nothing to do without it — and declaring
        // it here means Forge blocks loading rather than letting us hit a
        // NoClassDefFoundError on the first death.
        dependencies = "required-after:scalinghealth"
)
public class RLCraftDeathPenalty {

    public static final String MODID = "rlcraftdeathpenalty";
    public static final String NAME = "RLCraft Death Penalty";
    // Replaced at build time by ForgeGradle from mod_version in gradle.properties.
    // Shows literally as "@VERSION@" in IDE dev runs; that is expected.
    public static final String VERSION = "@VERSION@";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.init(event.getSuggestedConfigurationFile());
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandDeathPenalty());
        // Deferred to server start because it reads Scaling Health's config, which is
        // only guaranteed to be fully populated once every mod has finished loading.
        ScalingHealthBridge.logCompatibilityWarnings();
    }
}

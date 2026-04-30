package ar.hxi.jsonreg

import ar.hxi.jsonreg.config.ModConfig
import ar.hxi.jsonreg.registry.BlockRegistry
import ar.hxi.jsonreg.registry.FluidRegistry
import ar.hxi.jsonreg.registry.ItemRegistry
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.nio.file.Path

object JsonregMain : ModInitializer {
    val LOGGER = LoggerFactory.getLogger("jsonreg")
    const val MOD_ID = "jsonreg"

    override fun onInitialize() {
        LOGGER.info("[JRegister] JSON Registered is starting up!")

        val configPath: Path = FabricLoader.getInstance().configDir.resolve("jsonreg_entries.json")
        val config = ConfigLoader.load(configPath)

        ItemRegistry.registerAll(config.items)
        BlockRegistry.registerAll(config.blocks)
        FluidRegistry.registerAll(config.fluids)

        LOGGER.info("[JRegister] Registration complete. Check the console for any missing model warnings (that's normal).")
    }
}
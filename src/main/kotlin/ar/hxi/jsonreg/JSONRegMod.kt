package ar.hxi.jsonreg

import ar.hxi.jsonreg.config.ModConfig
import ar.hxi.jsonreg.registry.BlockRegistry
import ar.hxi.jsonreg.registry.FluidRegistry
import ar.hxi.jsonreg.registry.ItemRegistry
import com.google.gson.GsonBuilder
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

class JSONRegMod : ModInitializer {
    companion object {
        val LOGGER = LoggerFactory.getLogger("jsonreg")
        const val MOD_ID = "jsonreg"
    }

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

object ConfigLoader {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun load(path: Path): ModConfig {
        return if (Files.exists(path)) {
            try {
                Files.newBufferedReader(path).use { reader ->
                    gson.fromJson(reader, ModConfig::class.java)
                }.also {
                    JSONRegMod.LOGGER.info("Config loaded successfully.")
                }
            } catch (e: Exception) {
                JSONRegMod.LOGGER.error("Failed to parse config, using empty default.", e)
                ModConfig()
            }
        } else {
            // Write an example config
            val example = ModConfig()
            Files.createDirectories(path.parent)
            Files.writeString(path, gson.toJson(example))
            JSONRegMod.LOGGER.warn("No config found. Created an example at $path")
            example
        }
    }
}
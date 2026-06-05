package ar.hxi.jsonreg

import ar.hxi.jsonreg.config.ModConfig
import ar.hxi.jsonreg.registry.BlockRegistry
import ar.hxi.jsonreg.registry.FluidRegistry
import ar.hxi.jsonreg.registry.ItemRegistry
import ar.hxi.jsonreg.registry.ResourceGenerator
import com.google.gson.GsonBuilder
import de.rubixdev.yarrp.api.PackPosition
import de.rubixdev.yarrp.api.YarrpCallbacks
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resource.ResourceType
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

class JSONRegMod : ModInitializer {
    companion object {
        val LOGGER = LoggerFactory.getLogger("jsonreg")
        const val MOD_ID = "jsonreg"

        var config: ModConfig = ModConfig()
            private set
    }

    override fun onInitialize() {
        LOGGER.info("[JRegister] JSON Registered is starting up!")

        val configPath: Path = FabricLoader.getInstance().configDir.resolve("jsonreg_entries.json")
        config = ConfigLoader.load(configPath)

        ItemRegistry.registerAll(config.items)
        BlockRegistry.registerAll(config.blocks)
        FluidRegistry.registerAll(config.fluids)

        ResourceGenerator.registerResources(config.items, config.blocks, config.fluids)

        YarrpCallbacks.register(PackPosition.BEFORE_USER, ResourceType.CLIENT_RESOURCES) {
            add(ResourceGenerator.PACK)
        }

        LOGGER.info("[JRegister] Registration complete. Resources are now loaded at runtime!")
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
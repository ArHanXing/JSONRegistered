package ar.hxi.jsonreg

import ar.hxi.jsonreg.config.BlockEntry
import ar.hxi.jsonreg.config.ItemEntry
import ar.hxi.jsonreg.config.ModConfig
import ar.hxi.jsonreg.config.RawBlockEntry
import ar.hxi.jsonreg.config.RawFluidEntry
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

        // 注意：必须用 BEFORE_ALL（由 MultiPackResourceManager 构造时对所有类型触发），
        // BEFORE_USER 只在服务端数据包文件仓库（FolderRepositorySource）扫描时触发，客户端资源永远不会注入！
        YarrpCallbacks.register(PackPosition.BEFORE_ALL, ResourceType.CLIENT_RESOURCES) {
            add(ResourceGenerator.PACK)
        }

        // 战利品表属于服务端数据（data/...），必须单独注册一个 SERVER_DATA 的包，
        // 否则方块破坏时按 loot_table key 找不到表 → 什么都不掉。
        YarrpCallbacks.register(PackPosition.BEFORE_ALL, ResourceType.SERVER_DATA) {
            add(ResourceGenerator.DATA_PACK)
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
                    gson.fromJson(reader, ModConfigRaw::class.java).toConfig()
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

    /**
     * 原始配置 DTO：Gson 不会调用 Kotlin data class 构造函数，缺失字段只会得到
     * JVM 默认值（0/null/false）而非默认参数，因此流体条目先用全可空字段解析，
     * 再在 [toConfig] 中把未填写的字段回退到原版水（见 [RawFluidEntry.resolve]）。
     */
    private data class ModConfigRaw(
        val items: List<ItemEntry> = emptyList(),
        val blocks: List<RawBlockEntry> = emptyList(),
        val fluids: List<RawFluidEntry> = emptyList()
    ) {
        fun toConfig(): ModConfig {
            val resolvedBlocks = blocks.mapNotNull { raw ->
                val resolved = raw.resolve()
                if (resolved == null) {
                    JSONRegMod.LOGGER.warn("[JRegister] Skipped a block entry: missing required 'id' field.")
                }
                resolved
            }
            val resolvedFluids = fluids.mapNotNull { raw ->
                val resolved = raw.resolve()
                if (resolved == null) {
                    JSONRegMod.LOGGER.warn("[JRegister] Skipped a fluid entry: missing required 'id' field.")
                }
                resolved
            }
            return ModConfig(items = items, blocks = resolvedBlocks, fluids = resolvedFluids)
        }
    }
}
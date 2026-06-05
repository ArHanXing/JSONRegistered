package ar.hxi.jsonreg.registry

import ar.hxi.jsonreg.JSONRegMod
import ar.hxi.jsonreg.config.BlockEntry
import ar.hxi.jsonreg.config.FluidEntry
import ar.hxi.jsonreg.config.ItemEntry
import com.google.gson.Gson
import com.google.gson.JsonObject
import de.rubixdev.yarrp.api.RuntimeResourcePack
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resource.ResourceType
import net.minecraft.text.Text
import net.minecraft.util.Identifier

object ResourceGenerator {
    private val gson = Gson()

    lateinit var PACK: RuntimeResourcePack
        private set

    fun registerResources(items: List<ItemEntry>, blocks: List<BlockEntry>, fluids: List<FluidEntry>) {
        JSONRegMod.LOGGER.info("Registering runtime resources...")

        val modVersion = FabricLoader.getInstance()
            .getModContainer(JSONRegMod.MOD_ID)
            .map { it.metadata.version.friendlyString }
            .orElse("0.3")

        // 修正: 使用 YARRP 官方示例的构造函数方式
        PACK = RuntimeResourcePack(
            RuntimeResourcePack.createInfo(
                Identifier.of(JSONRegMod.MOD_ID, "runtime_resources"),
                Text.literal("JSON Registered Resources"),
                modVersion,
            ),
            RuntimeResourcePack.createMetadata(
                Text.literal("Runtime-generated resources for JSON Registered"),
                ResourceType.CLIENT_RESOURCES,
            ),
        )

        items.forEach { addItemModel(it.id) }
        blocks.forEach {
            addBlockModel(it.id)
            addBlockstate(it.id)
            addBlockItemModel(it.id)
        }
        fluids.filter { it.has_block }.forEach {
            addFluidBlockModel(it.id)
            addFluidBlockstate(it.id)
        }
        fluids.filter { it.has_bucket_item }.forEach {
            addBucketModel(it.id)
        }
        addLangFile(items, blocks, fluids)

        JSONRegMod.LOGGER.info("Runtime resources registered successfully!")
    }

    // ----- 通用的 JSON 资源添加方法 -----

    private fun addAsset(assetPath: String, json: JsonObject) {
        // 修正: 路径处理 - 去掉 "assets/" 前缀后分割
        val relativePath = assetPath.removePrefix("assets/").split("/")
        PACK.addResource(ResourceType.CLIENT_RESOURCES, relativePath, gson.toJson(json))
    }

    // ----- 物品模型 -----

    private fun addItemModel(id: String) {
        val model = JsonObject().apply {
            addProperty("parent", "minecraft:item/generated")
            add("textures", JsonObject().apply {
                addProperty("layer0", "${JSONRegMod.MOD_ID}:item/$id")
            })
        }
        addAsset("assets/${JSONRegMod.MOD_ID}/models/item/$id.json", model)
    }

    // ----- 方块模型/状态/物品模型 -----

    private fun addBlockModel(id: String) {
        val model = JsonObject().apply {
            addProperty("parent", "minecraft:block/cube_all")
            add("textures", JsonObject().apply {
                addProperty("all", "${JSONRegMod.MOD_ID}:block/$id")
            })
        }
        addAsset("assets/${JSONRegMod.MOD_ID}/models/block/$id.json", model)
    }

    private fun addBlockstate(id: String) {
        val blockstate = JsonObject().apply {
            add("variants", JsonObject().apply {
                add("", JsonObject().apply {
                    addProperty("model", "${JSONRegMod.MOD_ID}:block/$id")
                })
            })
        }
        addAsset("assets/${JSONRegMod.MOD_ID}/blockstates/$id.json", blockstate)
    }

    private fun addBlockItemModel(id: String) {
        val model = JsonObject().apply {
            addProperty("parent", "${JSONRegMod.MOD_ID}:block/$id")
        }
        addAsset("assets/${JSONRegMod.MOD_ID}/models/item/$id.json", model)
    }

    // ----- 流体桶模型 -----

    private fun addBucketModel(id: String) {
        val model = JsonObject().apply {
            addProperty("parent", "minecraft:item/generated")
            add("textures", JsonObject().apply {
                addProperty("layer0", "${JSONRegMod.MOD_ID}:item/${id}_bucket")
            })
        }
        addAsset("assets/${JSONRegMod.MOD_ID}/models/item/${id}_bucket.json", model)
    }

    // ----- 流体方块模型 -----

    private fun addFluidBlockModel(id: String) {
        val model = JsonObject().apply {
            addProperty("parent", "minecraft:block/cube_all")
            add("textures", JsonObject().apply {
                addProperty("all", "${JSONRegMod.MOD_ID}:block/${id}_still")
            })
        }
        addAsset("assets/${JSONRegMod.MOD_ID}/models/block/${id}_still.json", model)
    }

    // ----- 流体方块 blockstate（含 level=0..15）-----

    private fun addFluidBlockstate(id: String) {
        val variants = JsonObject()
        for (level in 0..15) {
            variants.add("level=$level", JsonObject().apply {
                addProperty("model", "${JSONRegMod.MOD_ID}:block/${id}_still")
            })
        }
        val blockstate = JsonObject().apply {
            add("variants", variants)
        }
        addAsset("assets/${JSONRegMod.MOD_ID}/blockstates/$id.json", blockstate)
    }

    // ----- 语言文件 -----

    private fun addLangFile(
        items: List<ItemEntry>,
        blocks: List<BlockEntry>,
        fluids: List<FluidEntry>,
    ) {
        val lang = JsonObject()
        items.forEach { item ->
            lang.addProperty("item.${JSONRegMod.MOD_ID}.${item.id}", item.name)
        }
        blocks.forEach { block ->
            lang.addProperty("block.${JSONRegMod.MOD_ID}.${block.id}", block.name)
        }
        fluids.forEach { fluid ->
            lang.addProperty("block.${JSONRegMod.MOD_ID}.${fluid.id}", fluid.name)
        }
        fluids.filter { it.has_bucket_item }.forEach { fluid ->
            lang.addProperty("item.${JSONRegMod.MOD_ID}.${fluid.id}_bucket", "${fluid.name} 桶")
        }

        addAsset("assets/${JSONRegMod.MOD_ID}/lang/zh_cn.json", lang)
    }
}

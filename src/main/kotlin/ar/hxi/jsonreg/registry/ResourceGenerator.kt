package ar.hxi.jsonreg.registry

import ar.hxi.jsonreg.JSONRegMod
import ar.hxi.jsonreg.config.BlockEntry
import ar.hxi.jsonreg.config.FluidEntry
import ar.hxi.jsonreg.config.ItemEntry
import ar.hxi.jsonreg.config.ToolFallback
import com.google.gson.Gson
import com.google.gson.JsonArray
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

    /** 服务端数据包（战利品表在 data/<ns>/loot_table/... 下）。 */
    lateinit var DATA_PACK: RuntimeResourcePack
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

        DATA_PACK = RuntimeResourcePack(
            RuntimeResourcePack.createInfo(
                Identifier.of(JSONRegMod.MOD_ID, "runtime_data"),
                Text.literal("JSON Registered Data"),
                modVersion,
            ),
            RuntimeResourcePack.createMetadata(
                Text.literal("Runtime-generated data for JSON Registered"),
                ResourceType.SERVER_DATA,
            ),
        )

        items.forEach { addItemModel(it.id) }
        blocks.forEach {
            addBlockModel(it.id)
            addBlockstate(it.id)
            addBlockItemModel(it.id)
            // 方块默认掉落自身（对应原版方块生成的 “drops itself” 战利品表）。
            // has_item=false 的方块没有对应物品，不生成表，破坏后自然什么都不掉。
            if (it.has_item) addBlockLootTable(it.id)
        }
        fluids.filter { it.has_block }.forEach {
            addFluidBlockModel(it.id)
            addFluidBlockstate(it.id)
        }
        // 桶物品在 has_block 时总是注册（否则收流体会吞桶，见 FluidRegistry），所以模型/语言也总是生成：
        // 模型由「原版铁桶底图 + 流体纹理截块」拼出来，不再依赖手画 <id>_bucket.png。
        fluids.filter { it.has_block }.forEach {
            addBucketModel(it)
        }
        addLangFile(items, blocks, fluids)
        // requires_tool 的方块必须进 mineable 标签，否则任何工具都挖不出掉落物
        addMineableTags(blocks)

        JSONRegMod.LOGGER.info("Runtime resources registered successfully!")
    }

    // ----- 通用的 JSON 资源添加方法 -----

    private fun addAsset(assetPath: String, json: JsonObject) {
        // 修正: 路径处理 - 去掉 "assets/" 前缀后分割
        val relativePath = assetPath.removePrefix("assets/").split("/")
        PACK.addResource(ResourceType.CLIENT_RESOURCES, relativePath, gson.toJson(json))
    }

    private fun addData(dataPath: String, json: JsonObject) {
        val relativePath = dataPath.removePrefix("data/").split("/")
        DATA_PACK.addResource(ResourceType.SERVER_DATA, relativePath, gson.toJson(json))
    }

    // ----- 方块战利品表（掉落自身）-----

    /**
     * 生成等价于原版 “drops itself” 的战利品表（参考 dirt.json / 数据生成器里的 addDrop）：
     * `minecraft:block` + `survives_explosion` 条件 + 一个掉落自身的 item entry。
     *
     * 路径必须是 `data/<ns>/loot_table/blocks/<id>.json`：方块默认的战利品表 key 就是
     * `Registries.BLOCK.getKey(block).withPrefixedPath("blocks/")`，即 `jsonreg:blocks/<id>`。
     *
     * requires_tool 不在表里加条件——原版是在破坏时用 `player.canHarvest(state)` 决定要不要
     * 走 afterBreak，所以这里的表保持和原版 dirt 一样的形状即可。
     */
    private fun addBlockLootTable(id: String) {
        val table = JsonObject().apply {
            addProperty("type", "minecraft:block")
            add("pools", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("rolls", 1.0)
                    addProperty("bonus_rolls", 0.0)
                    add("conditions", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("condition", "minecraft:survives_explosion")
                        })
                    })
                    add("entries", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("type", "minecraft:item")
                            addProperty("name", "${JSONRegMod.MOD_ID}:$id")
                        })
                    })
                })
            })
            // 和原版一致：给这张表一个固定的随机序列 id
            addProperty("random_sequence", "${JSONRegMod.MOD_ID}:blocks/$id")
        }
        addData("data/${JSONRegMod.MOD_ID}/loot_table/blocks/$id.json", table)
    }

    // ----- 挖掘工具标签（mineable/*）-----

    /**
     * `requires_tool` 只是把 `AbstractBlock.Settings.toolRequired` 设成 true，它本身**不会**
     * 让任何工具“可收获”：`PlayerEntity.canHarvest` 里判断的是
     * `!state.isToolRequired() || 主手物品 isSuitableFor(state)`，而 `isSuitableFor` 走的是
     * 工具组件的规则表——镐的规则是 “#minecraft:mineable/pickaxe → 可掉落”、
     * “#minecraft:incorrect_for_iron_tool → 不可掉落”。所以方块如果不在 mineable 标签里，
     * 拿任何工具挖都是“破坏了但什么都不掉”。
     *
     * 这里按 `tool` 字段把方块写进对应的 `minecraft:mineable/<tool>` 标签；
     * `requires_tool=true` 又没写 `tool` 时，[RawBlockEntry.resolve] 已经把它回退成 pickaxe。
     * 标签是跨数据包合并的（默认 replace=false），所以只写自己的条目不会覆盖原版内容。
     */
    private fun addMineableTags(blocks: List<BlockEntry>) {
        blocks.filter { it.tool != ToolFallback.NONE }
            .groupBy { it.tool }
            .forEach { (tool, entries) ->
                val values = JsonArray().apply {
                    entries.forEach { add("${JSONRegMod.MOD_ID}:${it.id}") }
                }
                addData(
                    "data/minecraft/tags/block/mineable/$tool.json",
                    JsonObject().apply { add("values", values) }
                )
                JSONRegMod.LOGGER.info(
                    "[JRegister] Added ${entries.size} block(s) to minecraft:mineable/$tool: " +
                        entries.joinToString { it.id }
                )
            }
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

    // ----- 流体桶模型（底桶 + 流体纹理截块，动态合成） -----

    /**
     * 生成桶模型：一张平的底桶贴图，加上从流体 still 纹理上截下来、贴在桶口的液面。
     * 全部是模型 JSON，不需要在运行时处理 PNG，也不需要为每个流体准备桶材质。
     *
     * 液面轮廓是照抄原版 `water_bucket.png` 的三行梯形（上窄 4..12 / 中宽 3..13 / 下窄 5..11，
     * 对应 sprite 第 3~5 行）：这三行正好是原版桶身画成深色的“桶内”，贴上去才是“桶里装着流体”；
     * 如果用一整块矩形，会盖住白色的桶沿，看起来就像液体浮在桶口外面。
     * UV 与几何用同一套坐标，等于把流体纹理对应位置的像素原样取过来，所以流体的动态图案会连着走。
     */
    private fun addBucketModel(fluid: FluidEntry) {
        val base = fluid.bucket_texture.ifBlank { "minecraft:item/bucket" }
        val fluidTexture = "${JSONRegMod.MOD_ID}:block/${fluid.id}_still"
        val z1 = 7.6
        val z2 = 7.8

        val model = JsonObject().apply {
            // 扁平的物品模型，用 front 光照，和 item/generated 的观感一致
            addProperty("gui_light", "front")
            add("textures", JsonObject().apply {
                addProperty("particle", base)
                addProperty("base", base)
                addProperty("fluid", fluidTexture)
            })
            add("display", flatItemDisplay())
            add("elements", JsonArray().apply {
                // 底桶：整张 16x16 贴图
                add(flatQuad(0.0, 0.0, 16.0, 16.0, 7.4, 7.6, "base", 0.0, 0.0, 16.0, 16.0))
                // 液面三行，y/v 都按 sprite 行号（第 3、4、5 行）
                add(flatQuad(4.0, 12.0, 12.0, 13.0, z1, z2, "fluid", 4.0, 3.0, 12.0, 5.0))
                add(flatQuad(3.0, 11.0, 13.0, 12.0, z1, z2, "fluid", 3.0, 5.0, 13.0, 7.0))
                add(flatQuad(5.0, 10.0, 11.0, 11.0, z1, z2, "fluid", 5.0, 7.0, 11.0, 9.0))
            })
        }
        addAsset(
            "assets/${JSONRegMod.MOD_ID}/models/item/${fluid.id}_bucket.json",
            model
        )
    }

    /** 与 `minecraft:item/generated` 一致的平面物品展示变换，保证拿在手里/掉在地上/展示框里都正常。 */
    private fun flatItemDisplay(): JsonObject {
        fun transform(
            rotation: List<Number>? = null,
            translation: List<Number>? = null,
            scale: List<Number>? = null
        ): JsonObject = JsonObject().apply {
            rotation?.let { add("rotation", it.toJsonArray()) }
            translation?.let { add("translation", it.toJsonArray()) }
            scale?.let { add("scale", it.toJsonArray()) }
        }

        return JsonObject().apply {
            add("thirdperson_righthand", transform(translation = listOf(0, 3, 1), scale = listOf(0.55, 0.55, 0.55)))
            add("thirdperson_lefthand", transform(translation = listOf(0, 3, 1), scale = listOf(0.55, 0.55, 0.55)))
            add("firstperson_righthand", transform(rotation = listOf(0, -90, 25), translation = listOf(1.13, 3.2, 1.13), scale = listOf(0.68, 0.68, 0.68)))
            add("firstperson_lefthand", transform(rotation = listOf(0, -90, 25), translation = listOf(1.13, 3.2, 1.13), scale = listOf(0.68, 0.68, 0.68)))
            add("ground", transform(translation = listOf(0, 2, 0), scale = listOf(0.5, 0.5, 0.5)))
            add("gui", transform())
            add("head", transform(rotation = listOf(0, 180, 0), translation = listOf(0, 13, 7)))
            add("fixed", transform(rotation = listOf(0, 180, 0)))
        }
    }

    private fun List<Number>.toJsonArray(): JsonArray {
        val array = JsonArray()
        // 注意：不能写 JsonArray().apply { forEach { add(it) } } —— 那里的 forEach 会解析成
        // JsonArray 自己的迭代器（空数组 = 什么都不加），最后序列化成 []，模型加载会报错。
        for (value in this) array.add(value)
        return array
    }

    /**
     * 生成一个只有正反两面的“薄片”元素，避免零厚度元素可能被模型加载器拒绝。
     */
    private fun flatQuad(
        x1: Double, y1: Double, x2: Double, y2: Double,
        z1: Double, z2: Double,
        texture: String,
        u1: Double, v1: Double, u2: Double, v2: Double
    ): JsonObject = JsonObject().apply {
        add("from", JsonArray().apply { add(x1); add(y1); add(z1) })
        add("to", JsonArray().apply { add(x2); add(y2); add(z2) })
        val face = JsonObject().apply {
            add("uv", JsonArray().apply { add(u1); add(v1); add(u2); add(v2) })
            addProperty("texture", "#$texture")
        }
        add("faces", JsonObject().apply {
            add("south", face)
            add("north", face)
        })
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
        fluids.filter { it.has_block }.forEach { fluid ->
            lang.addProperty("item.${JSONRegMod.MOD_ID}.${fluid.id}_bucket", "${fluid.name} 桶")
        }

        addAsset("assets/${JSONRegMod.MOD_ID}/lang/zh_cn.json", lang)
    }
}

package ar.hxi.jsonreg.registry

import ar.hxi.jsonreg.JSONRegMod
import ar.hxi.jsonreg.config.BlockEntry
import ar.hxi.jsonreg.config.FluidEntry
import ar.hxi.jsonreg.config.ItemEntry
import net.devtech.arrp.api.RuntimeResourcePack
import net.devtech.arrp.json.blockstate.JState
import net.devtech.arrp.json.blockstate.JVariant
import net.devtech.arrp.json.lang.JLang
import net.devtech.arrp.json.models.JModel
import net.minecraft.util.Identifier

object ResourceGenerator {
    val RESOURCE_PACK: RuntimeResourcePack = RuntimeResourcePack.create(JSONRegMod.MOD_ID)

    fun registerResources(items: List<ItemEntry>, blocks: List<BlockEntry>, fluids: List<FluidEntry>) {
        JSONRegMod.LOGGER.info("Registering runtime resources...")

        // 注册物品模型
        items.forEach { item ->
            addItemModel(item.id)
        }

        // 注册方块模型和状态
        blocks.forEach { block ->
            addBlockModel(block.id)
            addBlockstate(block.id)
            addBlockItemModel(block.id)
        }

        // 注册流体方块模型和状态（如果启用了 has_block）
        fluids.filter { it.has_block }.forEach { fluid ->
            addFluidBlockModel(fluid.id)
            addFluidBlockstate(fluid.id)
        }

        // 注册流体桶模型
        fluids.filter { it.has_bucket_item }.forEach { fluid ->
            addBucketModel(fluid.id)
        }

        // 注册语言文件
        addLangFile(items, blocks, fluids)

        JSONRegMod.LOGGER.info("Runtime resources registered successfully!")
    }

    private fun addItemModel(id: String) {
        val model = JModel.model("minecraft:item/generated")
            .textures(
                JModel.textures()
                    .layer0("${JSONRegMod.MOD_ID}:item/$id")
            )

        RESOURCE_PACK.addModel(model, Identifier.of(JSONRegMod.MOD_ID, "item/$id"))
    }

    private fun addBlockModel(id: String) {
        val model = JModel.model("minecraft:block/cube_all")
            .textures(
                JModel.textures()
                    .`var`("all", "${JSONRegMod.MOD_ID}:block/$id")
            )

        RESOURCE_PACK.addModel(model, Identifier.of(JSONRegMod.MOD_ID, "block/$id"))
    }

    private fun addBlockstate(id: String) {
        val variant = JVariant().put("", JState.model("${JSONRegMod.MOD_ID}:block/$id"))
        val blockstate = JState.state(variant)
        RESOURCE_PACK.addBlockState(blockstate, Identifier.of(JSONRegMod.MOD_ID, "blockstates/$id"))
    }

    private fun addBlockItemModel(id: String) {
        val model = JModel.model("${JSONRegMod.MOD_ID}:block/$id")

        RESOURCE_PACK.addModel(model, Identifier.of(JSONRegMod.MOD_ID, "item/$id"))
    }

    private fun addBucketModel(id: String) {
        val model = JModel.model("minecraft:item/generated")
            .textures(
                JModel.textures()
                    .layer0("${JSONRegMod.MOD_ID}:item/${id}_bucket")
            )

        RESOURCE_PACK.addModel(model, Identifier.of(JSONRegMod.MOD_ID, "item/${id}_bucket"))
    }

    private fun addFluidBlockModel(id: String) {
        val model = JModel.model()
            .parent("minecraft:block/cube_all")
            .textures(
                JModel.textures()
                    .`var`("all", "${JSONRegMod.MOD_ID}:block/${id}_still")
            )

        RESOURCE_PACK.addModel(model, Identifier.of(JSONRegMod.MOD_ID, "block/${id}_still"))
    }

    private fun addFluidBlockstate(id: String) {
        val variants = mutableListOf<JVariant>()
        
        // 为所有 level 值 (0-15) 添加模型定义
        for (level in 0..15) {
            val variant = JState.variant()
            variant.put("level=$level", JState.model("${JSONRegMod.MOD_ID}:block/${id}_still"))
            variants.add(variant)
        }
        
        val blockstate = JState.state()
        variants.forEach { blockstate.add(it) }

        RESOURCE_PACK.addBlockState(blockstate, Identifier.of(JSONRegMod.MOD_ID, "blockstates/$id"))
    }

    private fun addLangFile(
        items: List<ItemEntry>,
        blocks: List<BlockEntry>,
        fluids: List<FluidEntry>
    ) {
        val lang = JLang.lang()

        // 添加物品翻译
        items.forEach { item ->
            lang.entry("item.${JSONRegMod.MOD_ID}.${item.id}", item.name)
        }

        // 添加方块翻译
        blocks.forEach { block ->
            lang.entry("block.${JSONRegMod.MOD_ID}.${block.id}", block.name)
        }

        // 添加流体方块翻译
        fluids.forEach { fluid ->
            lang.entry("block.${JSONRegMod.MOD_ID}.${fluid.id}", fluid.name)
        }

        // 添加流体桶翻译
        fluids.filter { it.has_bucket_item }.forEach { fluid ->
            lang.entry("item.${JSONRegMod.MOD_ID}.${fluid.id}_bucket", "${fluid.name} 桶")
        }

        RESOURCE_PACK.addLang(Identifier.of("zh_cn"), lang)
    }
}

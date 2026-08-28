package ar.hxi.jsonreg.registry

import ar.hxi.jsonreg.JSONRegMod
import ar.hxi.jsonreg.config.BlockEntry
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.TransparentBlock
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object BlockRegistry {
    fun registerAll(entries: List<BlockEntry>) {
        entries.forEach { entry ->
            val settings = AbstractBlock.Settings.create()
                .hardness(entry.hardness)
                .resistance(entry.resistance)
                .apply {
                    if (entry.requires_tool) requiresTool()
                    // 非 solid 渲染层（cutoff/translucent）必须标记为透明，
                    // 否则相邻方块会因“以为它不透明”而剔除面向它的面，造成透视/镂空
                    if (entry.render_layer != BlockEntry.RenderLayerFallback.SOLID) nonOpaque()
                }
            // 非 solid 渲染层：改用透明方块实现（继承 TransparentBlock，行为同原版玻璃）：
            //  - isTransparent=true → getOpacity=0，光照完全穿透
            //  - isSideInvisible 同种方块相邻时隐藏共享面（剔除相邻的自己）
            //  - getAmbientOcclusionLightLevel=1.0，无 AO 暗角
            val block: Block = if (entry.render_layer != BlockEntry.RenderLayerFallback.SOLID) {
                JsonRegTransparentBlock(settings)
            } else {
                Block(settings)
            }
            val blockId = Identifier.of(JSONRegMod.MOD_ID, entry.id)
            Registry.register(Registries.BLOCK, blockId, block)

            if (entry.has_item) {
                val blockItem = BlockItem(block, Item.Settings())
                Registry.register(Registries.ITEM, blockId, blockItem)
            }
        }
    }

    /**
     * TransparentBlock 构造器为 protected，这里提供子类供注册使用。
     */
    private class JsonRegTransparentBlock(settings: AbstractBlock.Settings) : TransparentBlock(settings)
}
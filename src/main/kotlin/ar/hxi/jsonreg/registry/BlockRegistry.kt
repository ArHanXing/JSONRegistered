package ar.hxi.jsonreg.registry

import ar.hxi.jsonreg.JSONRegMod
import ar.hxi.jsonreg.config.BlockEntry
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.Blocks
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
                .apply { if (entry.requires_tool) requiresTool() }
            val block = Block(settings)
            val blockId = Identifier.of(JSONRegMod.MOD_ID, entry.id)
            Registry.register(Registries.BLOCK, blockId, block)

            if (entry.has_item) {
                val blockItem = BlockItem(block, Item.Settings())
                Registry.register(Registries.ITEM, blockId, blockItem)
            }
        }
    }
}
package ar.hxi.jsonreg.registry

import ar.hxi.jsonreg.JSONRegMod
import ar.hxi.jsonreg.config.ItemEntry
import net.minecraft.item.Item
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object ItemRegistry {
    fun registerAll(entries: List<ItemEntry>) {
        entries.forEach { entry ->
            val item = Item(Item.Settings().maxCount(entry.max_count))
            val id = Identifier.of(JSONRegMod.MOD_ID, entry.id)
            Registry.register(Registries.ITEM, id, item)
        }
    }
}
package ar.hxi.jsonreg.config

data class ModConfig(
    val items: List<ItemEntry> = emptyList(),
    val blocks: List<BlockEntry> = emptyList(),
    val fluids: List<FluidEntry> = emptyList()
)
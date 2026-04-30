package ar.hxi.jsonreg.config

data class ItemEntry(
    val id: String,
    val name: String = id,
    val max_count: Int = 64
)
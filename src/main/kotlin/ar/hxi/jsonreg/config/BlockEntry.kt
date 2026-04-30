package ar.hxi.jsonreg.config

data class BlockEntry(
    val id: String,
    val name: String = id,
    val hardness: Float = 1.5f,
    val resistance: Float = 6.0f,
    val requires_tool: Boolean = false,
    val has_item: Boolean = true
)
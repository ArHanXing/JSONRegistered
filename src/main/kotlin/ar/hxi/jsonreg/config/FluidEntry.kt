package ar.hxi.jsonreg.config

data class FluidEntry(
    val id: String,
    val name: String = id,
    val viscosity: Int = 1000,
    val density: Int = 1000,
    val luminosity: Int = 0,
    val temperature: Int = 300,
    val has_bucket_item: Boolean = true
)
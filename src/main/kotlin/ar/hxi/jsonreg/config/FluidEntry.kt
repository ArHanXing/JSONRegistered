package ar.hxi.jsonreg.config

data class FluidEntry(
    val id: String,
    val name: String = id,
    val viscosity: Int = WaterFallback.VISCOSITY,
    val density: Int = WaterFallback.DENSITY,
    val luminosity: Int = WaterFallback.LUMINOSITY,
    val temperature: Int = WaterFallback.TEMPERATURE,
    /**
     * 已废弃：桶物品现在只要 [has_block] 为 true 就总会注册（否则收流体会把流体方块吞掉却不返还桶）。
     * 字段保留仅为兼容旧配置，不再影响行为。
     */
    val has_bucket_item: Boolean = true,
    val has_block: Boolean = true,
    /**
     * 桶模型用的“底桶”纹理，缺省是原版铁桶（`minecraft:item/bucket`）。
     * 生成的桶模型 = 这张底桶纹理 + 从 `<id>_still` 流体纹理上截一块贴到桶口，
     * 所以不需要再为每个流体手画桶材质。
     */
    val bucket_texture: String = ""
) {
    companion object WaterFallback {
        // 原版水（WaterFluid / WaterFluidBlock）的各项物理属性，
        // 配置文件中字段未填写时回退到这些值，使自定义流体遵循原版水的逻辑。
        const val VISCOSITY = 1000 // 水的粘度，经 getTickRate 映射后流动间隔为 5 tick
        const val DENSITY = 1000 // 水的密度，≥1000 时向下流动（原版逻辑）
        const val LUMINOSITY = 0 // 水的发光等级
        const val TEMPERATURE = 300 // 水的温度（开尔文）
        const val TICK_RATE = 5 // 水的流动间隔（tick）
        const val MAX_FLOW_DISTANCE = 4 // 水的最大流距（格）
        const val LEVEL_DECREASE_PER_BLOCK = 1 // 水每流经一格衰减的 level
    }
}

/**
 * Gson 直接反序列化用的原始流体条目：所有字段可空。
 *
 * Gson 通过 Unsafe 实例化 Kotlin data class，不会调用构造函数，
 * 因此缺失字段只能得到 JVM 默认值（0/null/false）而非 data class 默认参数，
 * 无法区分“未填写”与“显式填写”。这里用可空字段保留“未填写”信息，
 * 再通过 [resolve] 将缺失字段回退到原版水的值。
 */
internal data class RawFluidEntry(
    val id: String? = null,
    val name: String? = null,
    val viscosity: Int? = null,
    val density: Int? = null,
    val luminosity: Int? = null,
    val temperature: Int? = null,
    val has_bucket_item: Boolean? = null,
    val has_block: Boolean? = null,
    val bucket_texture: String? = null
) {
    /**
     * 将原始条目解析为完整 [FluidEntry]，缺失字段回退到原版水逻辑；
     * 缺少必填 [id] 时返回 null（调用方跳过该条目）。
     */
    fun resolve(): FluidEntry? {
        val resolvedId = id ?: return null
        return FluidEntry(
            id = resolvedId,
            name = name ?: resolvedId,
            viscosity = viscosity ?: FluidEntry.WaterFallback.VISCOSITY,
            density = density ?: FluidEntry.WaterFallback.DENSITY,
            luminosity = luminosity ?: FluidEntry.WaterFallback.LUMINOSITY,
            temperature = temperature ?: FluidEntry.WaterFallback.TEMPERATURE,
            has_bucket_item = has_bucket_item ?: true,
            has_block = has_block ?: true,
            bucket_texture = bucket_texture ?: ""
        )
    }
}
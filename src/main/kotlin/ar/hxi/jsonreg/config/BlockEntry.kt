package ar.hxi.jsonreg.config

import ar.hxi.jsonreg.JSONRegMod

data class BlockEntry(
    val id: String,
    val name: String = id,
    val hardness: Float = 1.5f,
    val resistance: Float = 6.0f,
    val requires_tool: Boolean = false,
    val has_item: Boolean = true,
    val render_layer: String = RenderLayerFallback.SOLID
) {
    companion object RenderLayerFallback {
        // 支持的渲染层取值：不填时默认 solid（与原版多数方块一致）
        const val SOLID = "solid"
        const val CUTOFF = "cutoff"
        const val TRANSLUCENT = "translucent"
    }
}

/**
 * Gson 直接反序列化用的原始方块条目：所有字段可空。
 *
 * Gson 通过 Unsafe 实例化 Kotlin data class，不会调用构造函数，
 * 缺失字段只能得到 JVM 默认值（0/null/false）而非 data class 默认参数，
 * 因此用可空字段保留“未填写”信息，再通过 [resolve] 回填默认值。
 */
internal data class RawBlockEntry(
    val id: String? = null,
    val name: String? = null,
    val hardness: Float? = null,
    val resistance: Float? = null,
    val requires_tool: Boolean? = null,
    val has_item: Boolean? = null,
    val render_layer: String? = null
) {
    /**
     * 将原始条目解析为完整 [BlockEntry]；缺失字段回填默认值，
     * 渲染层取值不合法时回退到 solid；缺少必填 [id] 时返回 null。
     */
    fun resolve(): BlockEntry? {
        val resolvedId = id ?: return null
        val layer = render_layer?.lowercase() ?: BlockEntry.RenderLayerFallback.SOLID
        val resolvedLayer = when (layer) {
            BlockEntry.RenderLayerFallback.SOLID,
            BlockEntry.RenderLayerFallback.CUTOFF,
            BlockEntry.RenderLayerFallback.TRANSLUCENT -> layer
            else -> {
                JSONRegMod.LOGGER.warn(
                    "[JRegister] Invalid render_layer '$render_layer' for block '$resolvedId', " +
                        "falling back to 'solid'."
                )
                BlockEntry.RenderLayerFallback.SOLID
            }
        }
        return BlockEntry(
            id = resolvedId,
            name = name ?: resolvedId,
            hardness = hardness ?: 1.5f,
            resistance = resistance ?: 6.0f,
            requires_tool = requires_tool ?: false,
            has_item = has_item ?: true,
            render_layer = resolvedLayer
        )
    }
}
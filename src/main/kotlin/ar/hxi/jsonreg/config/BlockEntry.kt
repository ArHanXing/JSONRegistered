package ar.hxi.jsonreg.config

import ar.hxi.jsonreg.JSONRegMod

data class BlockEntry(
    val id: String,
    val name: String = id,
    val hardness: Float = 1.5f,
    val resistance: Float = 6.0f,
    val requires_tool: Boolean = false,
    val has_item: Boolean = true,
    val render_layer: String = RenderLayerFallback.SOLID,
    /**
     * 方块属于哪种工具的“可收获”集合（会写进 `minecraft:mineable/<tool>` 标签）：
     * 不填且 [requires_tool]=true 时按原版习惯回退到 pickaxe。
     * 这个字段决定“哪种工具能挖出掉落物”，和 [requires_tool]（是否必须有工具）是两件事。
     */
    val tool: String = ToolFallback.NONE
)

/** 支持的渲染层取值：不填时默认 solid（与原版多数方块一致）。 */
object RenderLayerFallback {
    const val SOLID = "solid"
    const val CUTOFF = "cutoff"
    const val TRANSLUCENT = "translucent"
}

/** 原版只有这四种 mineable 标签；none 表示不加入任何工具标签。 */
object ToolFallback {
    const val NONE = "none"
    const val PICKAXE = "pickaxe"
    const val AXE = "axe"
    const val SHOVEL = "shovel"
    const val HOE = "hoe"

    val ALL = listOf(NONE, PICKAXE, AXE, SHOVEL, HOE)
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
    val render_layer: String? = null,
    val tool: String? = null
) {
    /**
     * 将原始条目解析为完整 [BlockEntry]；缺失字段回填默认值，
     * 渲染层/工具取值不合法时回退到默认值；缺少必填 [id] 时返回 null。
     */
    fun resolve(): BlockEntry? {
        val resolvedId = id ?: return null

        val layer = render_layer?.lowercase() ?: RenderLayerFallback.SOLID
        val resolvedLayer = when (layer) {
            RenderLayerFallback.SOLID,
            RenderLayerFallback.CUTOFF,
            RenderLayerFallback.TRANSLUCENT -> layer
            else -> {
                JSONRegMod.LOGGER.warn(
                    "[JRegister] Invalid render_layer '$render_layer' for block '$resolvedId', " +
                        "falling back to 'solid'."
                )
                RenderLayerFallback.SOLID
            }
        }

        val requiresTool = requires_tool ?: false
        // 需要工具却没写 tool → 按原版最常见的情况当作镐，否则方块会变成“任何工具都挖不掉落”
        val defaultTool = if (requiresTool) ToolFallback.PICKAXE else ToolFallback.NONE
        val declaredTool = tool?.lowercase()
        val resolvedTool = when {
            declaredTool == null -> defaultTool
            declaredTool in ToolFallback.ALL -> declaredTool
            else -> {
                JSONRegMod.LOGGER.warn(
                    "[JRegister] Invalid tool '$declaredTool' for block '$resolvedId', " +
                        "expected one of ${ToolFallback.ALL}; falling back to '$defaultTool'."
                )
                defaultTool
            }
        }

        return BlockEntry(
            id = resolvedId,
            name = name ?: resolvedId,
            hardness = hardness ?: 1.5f,
            resistance = resistance ?: 6.0f,
            requires_tool = requiresTool,
            has_item = has_item ?: true,
            render_layer = resolvedLayer,
            tool = resolvedTool
        )
    }
}

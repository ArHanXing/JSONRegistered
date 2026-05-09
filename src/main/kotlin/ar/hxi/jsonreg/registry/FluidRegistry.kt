package ar.hxi.jsonreg.registry

import ar.hxi.jsonreg.JSONRegMod
import ar.hxi.jsonreg.config.FluidEntry
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Blocks
import net.minecraft.block.BlockState
import net.minecraft.block.FluidBlock
import net.minecraft.fluid.FlowableFluid
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.FluidState
import net.minecraft.item.BucketItem
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldAccess
import net.minecraft.world.WorldView

object FluidRegistry {
    fun registerAll(entries: List<FluidEntry>) {
        entries.forEach { entry ->
            val stillId = Identifier.of(JSONRegMod.MOD_ID, entry.id)
            val flowingId = Identifier.of(JSONRegMod.MOD_ID, "${entry.id}_flowing")

            var bucket: Item = Items.AIR

            val blockRef = arrayOfNulls<FluidBlock>(1)
            val fluidRefs = arrayOfNulls<FlowableFluid>(2) // [0] = still, [1] = flowing

            // ----- Still 流体 -----
            val still: FlowableFluid = object : FlowableFluid() {
                init {
                    // 设置默认状态，包含 level 属性
                    setDefaultState(stateManager.defaultState.with(Properties.LEVEL_1_8, 8))
                }

                override fun appendProperties(builder: StateManager.Builder<Fluid, FluidState>) {
                    super.appendProperties(builder)
                    builder.add(Properties.LEVEL_1_8)
                }

                override fun getStill(): Fluid = this
                override fun getFlowing(): Fluid = fluidRefs[1] ?: this
                override fun isStill(state: FluidState) = state.get(Properties.LEVEL_1_8) == 8
                override fun getLevel(state: FluidState) = state.get(Properties.LEVEL_1_8)
                override fun getBucketItem(): Item = bucket
                override fun matchesType(fluid: Fluid) = fluid === this || fluid === fluidRefs[1]

                override fun isInfinite(world: World) = false
                override fun beforeBreakingBlock(world: WorldAccess, pos: BlockPos, state: BlockState) {}
                override fun getMaxFlowDistance(world: WorldView) = 4
                override fun getLevelDecreasePerBlock(world: WorldView) = 1
                override fun canBeReplacedWith(
                    state: FluidState,
                    world: BlockView,
                    pos: BlockPos,
                    fluid: Fluid,
                    direction: Direction
                ) = false
                override fun getTickRate(world: WorldView) = 5
                override fun getBlastResistance() = 100.0f
                override fun toBlockState(state: FluidState): BlockState {
                    return if (entry.has_block) blockRef[0]?.defaultState ?: Blocks.AIR.defaultState
                    else Blocks.AIR.defaultState
                }
            }

            // ----- Flowing 流体 -----
            val flowing: FlowableFluid = object : FlowableFluid() {
                init {
                    setDefaultState(stateManager.defaultState.with(Properties.LEVEL_1_8, 7))
                }

                override fun appendProperties(builder: StateManager.Builder<Fluid, FluidState>) {
                    super.appendProperties(builder)
                    builder.add(Properties.LEVEL_1_8)
                }

                override fun getStill(): Fluid = fluidRefs[0] ?: this
                override fun getFlowing(): Fluid = this
                override fun isStill(state: FluidState) = false
                override fun getLevel(state: FluidState) = state.get(Properties.LEVEL_1_8)
                override fun getBucketItem(): Item = bucket
                override fun matchesType(fluid: Fluid) = fluid === this || fluid === fluidRefs[0]

                override fun isInfinite(world: World) = false
                override fun beforeBreakingBlock(world: WorldAccess, pos: BlockPos, state: BlockState) {}
                override fun getMaxFlowDistance(world: WorldView) = 4
                override fun getLevelDecreasePerBlock(world: WorldView) = 1
                override fun canBeReplacedWith(
                    state: FluidState,
                    world: BlockView,
                    pos: BlockPos,
                    fluid: Fluid,
                    direction: Direction
                ) = false
                override fun getTickRate(world: WorldView) = 5
                override fun getBlastResistance() = 100.0f
                override fun toBlockState(state: FluidState): BlockState {
                    return if (entry.has_block) blockRef[0]?.defaultState ?: Blocks.AIR.defaultState
                    else Blocks.AIR.defaultState
                }
            }

            // 填充引用
            fluidRefs[0] = still
            fluidRefs[1] = flowing

            // 注册流体
            Registry.register(Registries.FLUID, stillId, still)
            Registry.register(Registries.FLUID, flowingId, flowing)

            // 创建流体方块（如果需要）
            if (entry.has_block) {
                val blockSettings = AbstractBlock.Settings.create()
                    .hardness(100.0f)
                    .luminance { state -> entry.luminosity }
                    .noCollision()
                    .nonOpaque()
                    .dropsNothing()
                val block = FluidBlock(still, blockSettings)
                blockRef[0] = block
                Registry.register(Registries.BLOCK, stillId, block)
            }

            // 注册桶
            if (entry.has_bucket_item) {
                bucket = BucketItem(still, Item.Settings().maxCount(1).recipeRemainder(Items.BUCKET))
                Registry.register(Registries.ITEM, Identifier.of(JSONRegMod.MOD_ID, "${entry.id}_bucket"), bucket)
            }
        }
    }
}
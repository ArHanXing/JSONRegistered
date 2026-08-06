package ar.hxi.jsonreg.registry

import ar.hxi.jsonreg.JSONRegMod
import ar.hxi.jsonreg.config.FluidEntry
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.DoorBlock
import net.minecraft.block.FluidBlock
import net.minecraft.block.FluidFillable
import net.minecraft.fluid.FlowableFluid
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.FluidState
import net.minecraft.fluid.Fluids
import net.minecraft.item.BucketItem
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.tag.BlockTags
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

            val blockRef = arrayOfNulls<FluidBlock>(1)
            val fluidRefs = arrayOfNulls<JsonRegFluid>(2)
            val bucketRef = arrayOf<Item>(Items.AIR)

            val still = JsonRegFluid(
                still = true,
                entry = entry,
                blockRef = blockRef,
                bucketRef = bucketRef,
                flowingRef = { fluidRefs[1]!! },
                stillRef = { fluidRefs[0]!! }
            )

            val flowing = JsonRegFluid(
                still = false,
                entry = entry,
                blockRef = blockRef,
                bucketRef = bucketRef,
                flowingRef = { fluidRefs[1]!! },
                stillRef = { fluidRefs[0]!! }
            )

            fluidRefs[0] = still
            fluidRefs[1] = flowing

            Registry.register(Registries.FLUID, stillId, still)
            Registry.register(Registries.FLUID, flowingId, flowing)

            if (entry.has_block) {
                val blockSettings = AbstractBlock.Settings.create()
                    .mapColor(net.minecraft.block.MapColor.WATER_BLUE)
                    .replaceable()
                    .noCollision()
                    .strength(100.0f)
                    .luminance { state -> entry.luminosity }
                    .dropsNothing()
                    .liquid()
                val block = FluidBlock(still, blockSettings)
                blockRef[0] = block
                Registry.register(Registries.BLOCK, stillId, block)
            }

            if (entry.has_bucket_item) {
                bucketRef[0] = BucketItem(
                    still,
                    Item.Settings().maxCount(1).recipeRemainder(Items.BUCKET)
                )
                Registry.register(
                    Registries.ITEM,
                    Identifier.of(JSONRegMod.MOD_ID, "${entry.id}_bucket"),
                    bucketRef[0]
                )
            }
        }
    }

    class JsonRegFluid(
        private val still: Boolean,
        private val entry: FluidEntry,
        private val blockRef: Array<FluidBlock?>,
        private val bucketRef: Array<Item>,
        private val flowingRef: () -> JsonRegFluid,
        private val stillRef: () -> JsonRegFluid
    ) : FlowableFluid() {

        override fun getStill(): Fluid = if (still) this else stillRef()
        override fun getFlowing(): Fluid = if (still) flowingRef() else this

        override fun isStill(state: FluidState): Boolean = still

        override fun getLevel(state: FluidState): Int =
            if (still) 8 else state.get(Properties.LEVEL_1_8)

        override fun appendProperties(builder: StateManager.Builder<Fluid, FluidState>) {
            super.appendProperties(builder)
            if (!still) {
                builder.add(Properties.LEVEL_1_8)
            }
        }

        override fun getBucketItem(): Item = bucketRef[0]

        override fun toBlockState(state: FluidState): BlockState {
            val block = blockRef[0] ?: return Blocks.AIR.defaultState
            return block.defaultState.with(FluidBlock.LEVEL, getBlockStateLevel(state))
        }

        override fun matchesType(fluid: Fluid): Boolean =
            fluid === getStill() || fluid === getFlowing()

        override fun isInfinite(world: World): Boolean = false

        override fun beforeBreakingBlock(world: WorldAccess, pos: BlockPos, state: BlockState) {
            val blockEntity = if (state.hasBlockEntity()) world.getBlockEntity(pos) else null
            Block.dropStacks(state, world, pos, blockEntity)
        }

        override fun getMaxFlowDistance(world: WorldView): Int = 4

        override fun getLevelDecreasePerBlock(world: WorldView): Int = 1

        override fun canBeReplacedWith(
            state: FluidState,
            world: BlockView,
            pos: BlockPos,
            fluid: Fluid,
            direction: Direction
        ): Boolean = direction == Direction.DOWN && !this.matchesType(fluid)

        override fun getTickRate(world: WorldView): Int =
            // 粘度(viscosity) 映射为流动间隔：默认 1000 → 5（与原版水一致），粘度越大流动越慢
            (entry.viscosity / 200).coerceIn(1, 100)

        override fun tryFlow(world: World, fluidPos: BlockPos, state: FluidState) {
            if (state.isEmpty()) return
            // 密度 ≥ 1000：正常流体，走原版逻辑（向下流动）
            if (entry.density >= 1000) {
                super.tryFlow(world, fluidPos, state)
                return
            }
            // 密度 < 1000：轻流体，向上流动
            val blockState = world.getBlockState(fluidPos)
            val upPos = fluidPos.up()
            val upBlockState = world.getBlockState(upPos)
            val upFluidState = getUpdatedStateUp(world, upPos)
            if (canFlow(
                    world, fluidPos, blockState, Direction.UP,
                    upPos, upBlockState, world.getFluidState(upPos), upFluidState.fluid
                )
            ) {
                flow(world, upPos, upBlockState, Direction.UP, upFluidState)
                if (countNeighboringSourcesLight(world, fluidPos) >= 3) {
                    flowToSidesLight(world, fluidPos, state, blockState)
                }
            } else if (state.isStill() || !canFlowUpTo(world, upFluidState.fluid, upPos, upBlockState)) {
                flowToSidesLight(world, fluidPos, state, blockState)
            }
        }

        // 轻流体版“计算上方流体状态”：原版 getUpdatedState 只为向下流设计（从水平/上方邻居取值，
        // 从不看下方邻居），对上升流会永远算出 EMPTY，导致气体无法上升。这里改为从下方邻居继承 level。
        private fun getUpdatedStateUp(world: World, pos: BlockPos): FluidState {
            // 正下方有同族流体：继承它的 level 并衰减 1，形成上升柱
            val below = world.getFluidState(pos.down())
            if (below.fluid.matchesType(this)) {
                val newLevel = below.level - getLevelDecreasePerBlock(world)
                return if (newLevel <= 0) Fluids.EMPTY.defaultState else getFlowing(newLevel, false)
            }
            // 正下方没有：从水平邻居取最大 level（气体从源向侧面扩散后再上升）
            var maxLevel = 0
            for (direction in Direction.Type.HORIZONTAL) {
                val neighbor = world.getFluidState(pos.offset(direction))
                if (neighbor.fluid.matchesType(this)) {
                    maxLevel = maxOf(maxLevel, neighbor.level)
                }
            }
            val level = maxLevel - getLevelDecreasePerBlock(world)
            return if (level <= 0) Fluids.EMPTY.defaultState else getFlowing(level, false)
        }

        // 轻流体版“向四周流”（对应原版 private flowToSides）
        private fun flowToSidesLight(world: World, pos: BlockPos, fluidState: FluidState, blockState: BlockState) {
            var level = fluidState.level - getLevelDecreasePerBlock(world)
            if (fluidState.get(Properties.FALLING)) level = 7
            if (level <= 0) return
            for ((direction, spreadState) in getSpread(world, pos, blockState)) {
                val spreadPos = pos.offset(direction)
                val spreadBlockState = world.getBlockState(spreadPos)
                if (canFlow(
                        world, pos, blockState, direction,
                        spreadPos, spreadBlockState, world.getFluidState(spreadPos), spreadState.fluid
                    )
                ) {
                    flow(world, spreadPos, spreadBlockState, direction, spreadState)
                }
            }
        }

        // 轻流体版“统计相邻源方块”（对应原版 private countNeighboringSources）
        private fun countNeighboringSourcesLight(world: WorldView, pos: BlockPos): Int {
            var count = 0
            for (direction in Direction.Type.HORIZONTAL) {
                val neighborState = world.getFluidState(pos.offset(direction))
                if (neighborState.fluid.matchesType(this) && neighborState.isStill()) count++
            }
            return count
        }

        // 轻流体版“能否向上填充”（对应原版 private canFlowDownTo，方向改为 UP）
        private fun canFlowUpTo(world: BlockView, fluid: Fluid, fromPos: BlockPos, fromState: BlockState): Boolean {
            if (fromState.fluidState.fluid.matchesType(this)) return true
            val block = fromState.block
            if (block is FluidFillable) return block.canFillWithFluid(null, world, fromPos, fromState, fluid)
            if (block is DoorBlock || fromState.isIn(BlockTags.SIGNS) || fromState.isOf(Blocks.LADDER)
                || fromState.isOf(Blocks.SUGAR_CANE) || fromState.isOf(Blocks.BUBBLE_COLUMN)
            ) {
                return false
            }
            return !fromState.isOf(Blocks.NETHER_PORTAL) && !fromState.isOf(Blocks.END_PORTAL)
                && !fromState.isOf(Blocks.END_GATEWAY) && !fromState.isOf(Blocks.STRUCTURE_VOID)
                && !fromState.blocksMovement()
        }

        override fun getBlastResistance(): Float = 100f
    }
}

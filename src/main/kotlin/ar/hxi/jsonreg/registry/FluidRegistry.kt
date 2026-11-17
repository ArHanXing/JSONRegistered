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
import net.minecraft.sound.SoundEvent
import net.minecraft.sound.SoundEvents
import net.minecraft.state.StateManager
import net.minecraft.state.property.Properties
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldAccess
import net.minecraft.world.WorldView
import java.util.EnumMap
import java.util.Optional

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

            // 桶物品必须存在，否则“收流体”这条路是坏的：
            // FluidBlock#tryDrainFluid 会先把流体方块删掉，再返回 new ItemStack(fluid.getBucketItem())。
            // 如果 getBucketItem() 返回 Items.AIR，得到的空栈会被调用方 BucketItem 判定为失败
            // （!itemStack.isEmpty() 不成立），于是流体被凭空吞掉、桶也不返还。
            // 科技复兴的流体单元同理：它要求抽出来的物品是 RebornCore 的 ItemFluidInfo（RebornCore 通过
            // mixin 给原版 BucketItem 实现了该接口），并且 !isEmpty()；返回空气时整条链路直接失败。
            // 所以只要流体有方块就注册 jsonreg:<id>_bucket；没有方块时桶既装不到也倒不出，直接跳过。
            if (entry.has_block) {
                val bucket = BucketItem(
                    still,
                    Item.Settings().maxCount(1).recipeRemainder(Items.BUCKET)
                )
                bucketRef[0] = bucket
                Registry.register(
                    Registries.ITEM,
                    Identifier.of(JSONRegMod.MOD_ID, "${entry.id}_bucket"),
                    bucket
                )
            }

            if (entry.has_block) {
                val blockSettings = AbstractBlock.Settings.create()
                    .mapColor(net.minecraft.block.MapColor.WATER_BLUE)
                    .replaceable()
                    .noCollision()
                    .strength(100.0f)
                    .luminance { _ -> entry.luminosity }
                    .dropsNothing()
                    .liquid()
                val block = FluidBlock(still, blockSettings)
                blockRef[0] = block
                Registry.register(Registries.BLOCK, stillId, block)
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

        /** 密度 < 1000 → 轻流体（气体），重力方向相反：向上流动。 */
        private val isLightFluid: Boolean
            get() = entry.density < FluidEntry.WaterFallback.DENSITY

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

        override fun getMaxFlowDistance(world: WorldView): Int =
            FluidEntry.WaterFallback.MAX_FLOW_DISTANCE

        override fun getLevelDecreasePerBlock(world: WorldView): Int =
            FluidEntry.WaterFallback.LEVEL_DECREASE_PER_BLOCK

        override fun canBeReplacedWith(
            state: FluidState,
            world: BlockView,
            pos: BlockPos,
            fluid: Fluid,
            direction: Direction
        ): Boolean =
            // 水的“被替换”方向来自上方（下落的水把别的流体挤开），
            // 轻流体反过来：从下方顶上来时把目标格里的东西挤开。
            if (isLightFluid) direction == Direction.UP && !this.matchesType(fluid)
            else direction == Direction.DOWN && !this.matchesType(fluid)

        override fun getTickRate(world: WorldView): Int =
            // 粘度(viscosity) 映射为流动间隔：不填时回退到原版水粘度 1000 → 5 tick（与水的 TICK_RATE 一致），粘度越大流动越慢
            (entry.viscosity / 200).coerceIn(1, 100)

        override fun getBucketFillSound(): Optional<SoundEvent> = Optional.of(SoundEvents.ITEM_BUCKET_FILL)

        override fun getBlastResistance(): Float = 100f

        // ------------------------------------------------------------------
        // 流动调度
        // ------------------------------------------------------------------

        /**
         * 原版 [FlowableFluid.getUpdatedState] 只为“向下流”设计：它只看水平邻居和上方邻居，
         * 从下方取不到任何东西。轻流体必须反过来看下方，否则每次计划刻都会算出 EMPTY：
         * 流体方块被删掉 → 下面的方块再把它顶上来 → 无限循环，每个方块每 tick 都在重建自己，
         * 这就是“气体流动一直变化、tick 消耗猎奇”的根源。
         *
         * 这里把轻流体的状态计算整体换成向上版本，让 [FlowableFluid.onScheduledTick]
         * 重算出的状态与现有状态一致，从而只流动、不抖动。
         */
        override fun getUpdatedState(world: World, pos: BlockPos, state: BlockState): FluidState =
            if (isLightFluid) getUpdatedStateUp(world, pos, state)
            else super.getUpdatedState(world, pos, state)

        override fun tryFlow(world: World, fluidPos: BlockPos, state: FluidState) {
            if (state.isEmpty()) return
            if (!isLightFluid) {
                // 密度 ≥ 1000：正常流体，走原版逻辑（向下流动）
                super.tryFlow(world, fluidPos, state)
                return
            }

            // 密度 < 1000：轻流体，向上流动（原版 tryFlow 的镜像版本）
            val blockState = world.getBlockState(fluidPos)
            val upPos = fluidPos.up()
            val upBlockState = world.getBlockState(upPos)
            val upFluidState = getUpdatedStateUp(world, upPos, upBlockState)
            if (!upFluidState.isEmpty &&
                canFlow(
                    world, fluidPos, blockState, Direction.UP,
                    upPos, upBlockState, world.getFluidState(upPos), upFluidState.fluid
                )
            ) {
                flow(world, upPos, upBlockState, Direction.UP, upFluidState)
                if (countNeighboringSources(world, fluidPos) >= 3) {
                    flowToSidesUp(world, fluidPos, state, blockState)
                }
            } else if (state.isStill || !canFlowUpTo(world, upFluidState.fluid, fluidPos, blockState, upPos, upBlockState)) {
                // 顶不动了（上方被挡）才向四周铺开：和原版水“落地后铺开”对称
                flowToSidesUp(world, fluidPos, state, blockState)
            }
        }

        // ------------------------------------------------------------------
        // 轻流体（气体）的向上流动，逐条镜像原版 FlowableFluid 的向下实现
        // ------------------------------------------------------------------

        /** 对应原版 [FlowableFluid.getUpdatedState]：把“上方来源”换成“下方来源”。 */
        private fun getUpdatedStateUp(world: World, pos: BlockPos, state: BlockState): FluidState {
            // 正下方有同族流体 → 本格是“满液面”的上升柱，和原版“上方有水我就正在下落”完全对称。
            // 注意这里不能像旧实现那样把 level 一路减下去：减没之后每 tick 都会算出 EMPTY，
            // 流体方块被删掉又被下面顶回来，就是这个 bug 的 tick 灾难来源。
            val belowPos = pos.down()
            val belowState = world.getBlockState(belowPos)
            val below = belowState.fluidState
            if (below.fluid.matchesType(this) && receivesFlow(world, pos, state, belowPos, belowState, Direction.DOWN)) {
                return getFlowing(8, true)
            }

            // 水平邻居：与原版一致，气体会从源向侧面扩散，level 逐格衰减
            var maxLevel = 0
            for (direction in Direction.Type.HORIZONTAL) {
                val fromPos = pos.offset(direction)
                val fromState = world.getBlockState(fromPos)
                val from = fromState.fluidState
                if (from.fluid.matchesType(this) && receivesFlow(world, pos, state, fromPos, fromState, direction)) {
                    maxLevel = maxOf(maxLevel, from.level)
                }
            }

            val level = maxLevel - getLevelDecreasePerBlock(world)
            return if (level <= 0) Fluids.EMPTY.defaultState else getFlowing(level, false)
        }

        /** 对应原版 private flowToSides。 */
        private fun flowToSidesUp(world: World, pos: BlockPos, fluidState: FluidState, blockState: BlockState) {
            var level = fluidState.level - getLevelDecreasePerBlock(world)
            if (fluidState.get(Properties.FALLING)) level = 7
            if (level <= 0) return

            for ((direction, spreadState) in getSpreadUp(world, pos, blockState)) {
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

        /**
         * 对应原版 protected getSpread：优先选择“能更快继续上升”的方向。
         * 原版用 getMinFlowDownDistance 找最近的下落点，这里换成 getMinRiseDistance。
         */
        private fun getSpreadUp(world: World, pos: BlockPos, state: BlockState): Map<Direction, FluidState> {
            var best = 1000
            val spread = EnumMap<Direction, FluidState>(Direction::class.java)

            for (direction in Direction.Type.HORIZONTAL) {
                val sidePos = pos.offset(direction)
                val sideState = world.getBlockState(sidePos)
                val sideFluid = sideState.fluidState
                val candidate = getUpdatedStateUp(world, sidePos, sideState)
                if (!canFlowThrough(world, candidate.fluid, pos, state, direction, sidePos, sideState, sideFluid)) {
                    continue
                }

                val upPos = sidePos.up()
                val canRise = canFlowUpTo(world, getFlowing(), sidePos, sideState, upPos, world.getBlockState(upPos))
                val distance = if (canRise) 0
                else getMinRiseDistance(world, sidePos, 1, direction.opposite, sideState)

                if (distance < best) spread.clear()
                if (distance <= best) {
                    spread[direction] = candidate
                    best = distance
                }
            }
            return spread
        }

        /** 对应原版 private getMinFlowDownDistance，方向换成向上。 */
        private fun getMinRiseDistance(
            world: WorldView,
            pos: BlockPos,
            depth: Int,
            from: Direction,
            state: BlockState
        ): Int {
            var best = 1000
            for (direction in Direction.Type.HORIZONTAL) {
                if (direction == from) continue
                val sidePos = pos.offset(direction)
                val sideState = world.getBlockState(sidePos)
                if (!canFlowThrough(world, getFlowing(), pos, state, direction, sidePos, sideState, sideState.fluidState)) {
                    continue
                }
                val upPos = sidePos.up()
                if (canFlowUpTo(world, getFlowing(), sidePos, sideState, upPos, world.getBlockState(upPos))) {
                    return depth
                }
                if (depth < getMaxFlowDistance(world)) {
                    val distance = getMinRiseDistance(world, sidePos, depth + 1, direction.opposite, sideState)
                    if (distance < best) best = distance
                }
            }
            return best
        }

        /** 对应原版 private canFill。 */
        private fun canFill(world: BlockView, pos: BlockPos, state: BlockState, fluid: Fluid): Boolean {
            val block = state.block
            if (block is FluidFillable) {
                return block.canFillWithFluid(null, world, pos, state, fluid)
            }
            if (block is DoorBlock || state.isIn(BlockTags.SIGNS) || state.isOf(Blocks.LADDER)
                || state.isOf(Blocks.SUGAR_CANE) || state.isOf(Blocks.BUBBLE_COLUMN)
            ) {
                return false
            }
            return !state.isOf(Blocks.NETHER_PORTAL) && !state.isOf(Blocks.END_PORTAL)
                && !state.isOf(Blocks.END_GATEWAY) && !state.isOf(Blocks.STRUCTURE_VOID)
                && !state.blocksMovement()
        }

        /** 对应原版 private canFlowThrough。 */
        private fun canFlowThrough(
            world: BlockView,
            fluid: Fluid,
            pos: BlockPos,
            state: BlockState,
            face: Direction,
            toPos: BlockPos,
            toState: BlockState,
            toFluidState: FluidState
        ): Boolean =
            !isMatchingAndStill(toFluidState)
                && receivesFlow(world, pos, state, toPos, toState, face)
                && canFill(world, toPos, toState, fluid)

        /** 对应原版 private canFlowDownTo，方向换成向上。 */
        private fun canFlowUpTo(
            world: BlockView,
            fluid: Fluid,
            pos: BlockPos,
            state: BlockState,
            upPos: BlockPos,
            upState: BlockState
        ): Boolean {
            if (!receivesFlow(world, pos, state, upPos, upState, Direction.UP)) return false
            return if (upState.fluidState.fluid.matchesType(this)) true else canFill(world, upPos, upState, fluid)
        }

        /** 对应原版 private countNeighboringSources。 */
        private fun countNeighboringSources(world: WorldView, pos: BlockPos): Int {
            var count = 0
            for (direction in Direction.Type.HORIZONTAL) {
                val neighborState = world.getFluidState(pos.offset(direction))
                if (neighborState.fluid.matchesType(this) && neighborState.isStill) count++
            }
            return count
        }

        private fun isMatchingAndStill(state: FluidState): Boolean =
            state.fluid.matchesType(this) && state.isStill

        /**
         * 对应原版 private receivesFlow：判断两个相邻方块之间是否被碰撞箱“挡住”。
         * 原版为了性能加了 200 项缓存；这边只用于轻流体，深度有限，直接算即可。
         */
        private fun receivesFlow(
            world: BlockView,
            pos: BlockPos,
            state: BlockState,
            fromPos: BlockPos,
            fromState: BlockState,
            face: Direction
        ): Boolean {
            val shape = state.getCollisionShape(world, pos)
            val fromShape = fromState.getCollisionShape(world, fromPos)
            return !VoxelShapes.adjacentSidesCoverSquare(shape, fromShape, face)
        }
    }
}

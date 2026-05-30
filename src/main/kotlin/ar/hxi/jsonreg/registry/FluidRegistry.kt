package ar.hxi.jsonreg.registry

import ar.hxi.jsonreg.JSONRegMod
import ar.hxi.jsonreg.config.FluidEntry
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
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

        override fun getTickRate(world: WorldView): Int = 10

        override fun getBlastResistance(): Float = 100f
    }
}

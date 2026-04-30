package ar.hxi.jsonreg.registry

import ar.hxi.jsonreg.JSONRegMod
import ar.hxi.jsonreg.config.FluidEntry
import net.minecraft.block.Blocks
import net.minecraft.block.BlockState
import net.minecraft.fluid.FlowableFluid
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.FluidState
import net.minecraft.item.BucketItem
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
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

            // 共享的桶物品引用，后边创建桶后赋值
            var bucket: Item = Items.AIR

            val still: FlowableFluid = object : FlowableFluid() {
                override fun getStill(): Fluid = this
                override fun getFlowing(): Fluid = flowing
                override fun isStill(level: FluidState) = true
                override fun getLevel(state: FluidState) = 0
                override fun getBucketItem(): Item = bucket
                override fun matchesType(fluid: Fluid) = fluid == this

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
                override fun toBlockState(state: FluidState): BlockState = Blocks.AIR.defaultState
            }

            val flowing: FlowableFluid = object : FlowableFluid() {
                override fun getStill(): Fluid = still
                override fun getFlowing(): Fluid = this
                override fun isStill(level: FluidState) = false
                override fun getLevel(state: FluidState) = 8
                override fun getBucketItem(): Item = bucket
                override fun matchesType(fluid: Fluid) = fluid == this

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
                override fun toBlockState(state: FluidState): BlockState = Blocks.AIR.defaultState
            }

            Registry.register(Registries.FLUID, stillId, still)
            Registry.register(Registries.FLUID, flowingId, flowing)

            if (entry.has_bucket_item) {
                bucket = BucketItem(still, Item.Settings().maxCount(1).recipeRemainder(Items.BUCKET))
                Registry.register(Registries.ITEM, Identifier.of(JSONRegMod.MOD_ID, "${entry.id}_bucket"), bucket)
            }
        }
    }
}
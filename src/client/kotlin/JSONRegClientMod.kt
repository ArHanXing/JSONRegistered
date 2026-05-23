package ar.hxi.jsonreg.client

import ar.hxi.jsonreg.JSONRegMod
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.client.texture.Sprite
import net.minecraft.client.texture.SpriteAtlasTexture
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.minecraft.fluid.FluidState
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockRenderView

class JSONRegClientMod : ClientModInitializer {
    override fun onInitializeClient() {
        JSONRegMod.LOGGER.info("[JRegister] Client initialization started!")

        // 注册流体渲染处理器
        registerFluidRendering()

        JSONRegMod.LOGGER.info("[JRegister] Client initialization complete!")
    }

    private fun registerFluidRendering() {
        JSONRegMod.LOGGER.info("Registering fluid rendering handlers...")

        JSONRegMod.config.fluids.forEach { fluid ->
            val stillId = Identifier.of(JSONRegMod.MOD_ID, fluid.id)
            val flowingId = Identifier.of(JSONRegMod.MOD_ID, "${fluid.id}_flowing")

            val stillFluid = Registries.FLUID.get(stillId)
            val flowingFluid = Registries.FLUID.get(flowingId)

            val textureStill = Identifier.of(JSONRegMod.MOD_ID, "block/${fluid.id}_still")
            val textureFlow = Identifier.of(JSONRegMod.MOD_ID, "block/${fluid.id}_flow")

            val handler = object : FluidRenderHandler {
                private var sprites: Array<Sprite?>? = null

                override fun getFluidSprites(
                    view: BlockRenderView?,
                    pos: BlockPos?,
                    state: FluidState?
                ): Array<Sprite?> {
                    if (sprites == null) {
                        val atlas = SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE
                        val textureManager = net.minecraft.client.MinecraftClient.getInstance().textureManager
                        val atlasTexture = textureManager.getTexture(atlas) as? SpriteAtlasTexture

                        if (atlasTexture != null) {
                            sprites = arrayOf(
                                atlasTexture.getSprite(textureStill),
                                atlasTexture.getSprite(textureFlow)
                            )
                        } else {
                            sprites = arrayOf(null, null)
                        }
                    }
                    return sprites!!
                }
            }

            FluidRenderHandlerRegistry.INSTANCE.register(stillFluid, handler)
            FluidRenderHandlerRegistry.INSTANCE.register(flowingFluid, handler)

            JSONRegMod.LOGGER.info("Registered fluid renderer for: ${fluid.id}")
        }

        JSONRegMod.LOGGER.info("Fluid rendering handlers registered successfully!")
    }
}

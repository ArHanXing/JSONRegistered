package ar.hxi.jsonreg.client

import ar.hxi.jsonreg.JSONRegMod
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.Sprite
import net.minecraft.fluid.Fluid
import net.minecraft.registry.Registries
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceType
import net.minecraft.screen.PlayerScreenHandler
import net.minecraft.util.Identifier

class JSONRegClientMod : ClientModInitializer {

    private val spriteCache = mutableMapOf<Fluid, ResettableLazy<Array<Sprite?>>>()

    override fun onInitializeClient() {
        JSONRegMod.LOGGER.info("[JRegister] Client initialization started!")

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
            .registerReloadListener(FluidSpriteReloadListener())

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

            val lazySprites = ResettableLazy {
                val atlas = MinecraftClient.getInstance()
                    .bakedModelManager
                    .getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE)
                arrayOf(
                    atlas.getSprite(textureStill),
                    atlas.getSprite(textureFlow)
                )
            }

            val handler = FluidRenderHandler { _, _, _ -> lazySprites.get() }

            FluidRenderHandlerRegistry.INSTANCE.register(stillFluid, handler)
            FluidRenderHandlerRegistry.INSTANCE.register(flowingFluid, handler)

            spriteCache[stillFluid] = lazySprites
            spriteCache[flowingFluid] = lazySprites

            JSONRegMod.LOGGER.info("Registered fluid renderer for: ${fluid.id}")
        }

        JSONRegMod.LOGGER.info("Fluid rendering handlers registered successfully!")
    }

    private inner class FluidSpriteReloadListener : SimpleSynchronousResourceReloadListener {
        override fun getFabricId(): Identifier =
            Identifier.of(JSONRegMod.MOD_ID, "fluid_sprite_reloader")

        override fun reload(manager: ResourceManager) {
            spriteCache.values.forEach { it.reset() }
        }

        override fun getFabricDependencies(): Collection<Identifier> =
            listOf(ResourceReloadListenerKeys.TEXTURES)
    }

    private class ResettableLazy<T>(private val supplier: () -> T) {
        @Volatile
        private var cached: T? = null

        fun get(): T {
            var value = cached
            if (value == null) {
                synchronized(this) {
                    value = cached
                    if (value == null) {
                        value = supplier()
                        cached = value
                    }
                }
            }
            return value!!
        }

        fun reset() {
            synchronized(this) {
                cached = null
            }
        }
    }
}

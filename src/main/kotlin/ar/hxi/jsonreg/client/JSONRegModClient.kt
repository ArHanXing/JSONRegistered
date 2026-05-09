package ar.hxi.jsonreg.client

import ar.hxi.jsonreg.ConfigLoader
import ar.hxi.jsonreg.JSONRegMod
import ar.hxi.jsonreg.config.FluidEntry
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.render.RenderLayer
import net.minecraft.util.Identifier
import java.nio.file.Path

class JSONRegModClient : ClientModInitializer {
    override fun onInitializeClient() {
        // 假设你有办法拿到已注册的流体列表（可以从配置文件读取）
        val configPath: Path = FabricLoader.getInstance().configDir.resolve("jsonreg_entries.json")
        val config = ConfigLoader.load(configPath)
        val fluidEntries = config.fluids // 获取你配置中的流体列表
        fluidEntries.filter { it.has_block }.forEach { entry ->
            registerFluidRender(entry)
        }
    }

    private fun registerFluidRender(entry: FluidEntry) {
        val stillId = Identifier.of(JSONRegMod.MOD_ID, entry.id)
        val flowingId = Identifier.of(JSONRegMod.MOD_ID, "${entry.id}_flowing")
        val stillFluid = Registries.FLUID.get(stillId)
        val flowingFluid = Registries.FLUID.get(flowingId)

        // 纹理路径：assets/mod_id/textures/block/fluid_name_still.png 和 _flowing.png
        val stillTexture = Identifier.of(JSONRegMod.MOD_ID, "block/${entry.id}_still")
        val flowingTexture = Identifier.of(JSONRegMod.MOD_ID, "block/${entry.id}_flowing")

        // 注册渲染处理器
        val renderHandler = SimpleFluidRenderHandler(stillTexture, flowingTexture)
        FluidRenderHandlerRegistry.INSTANCE.register(stillFluid, flowingFluid, renderHandler)

        // 设置渲染层（流体通常是半透明的）
        BlockRenderLayerMap.INSTANCE.putFluids(RenderLayer.getTranslucent(), stillFluid, flowingFluid)
    }
}
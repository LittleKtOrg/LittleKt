package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readLDtkMapLoader
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.use
import com.littlekt.graphics.webgpu.*
import com.littlekt.util.viewport.ExtendViewport

/**
 * Load and render an LDtk map.
 *
 * @author Colton Daily
 * @date 4/16/2024
 */
class LDtkTileMapExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        this.addCloseOnEsc()
        val device = graphics.device

        val mapLoader = resourcesVfs["ldtk/sample-1.0.ldtk"].readLDtkMapLoader()
        // we are only loading the first level
        // we can load additional levels by doing:
        // mapLoader.loadLevel(levelIdx)
        val world = mapLoader.loadMap()
        val surfaceCapabilities = graphics.surfaceCapabilities
        val preferredFormat = graphics.preferredFormat

        graphics.configureSurface(
            TextureUsage.RENDER_ATTACHMENT,
            preferredFormat,
            PresentMode.FIFO,
            surfaceCapabilities.alphaModes[0],
        )

        val batch = SpriteBatch(device, graphics, preferredFormat, size = 400)
        val viewport = ExtendViewport(270, 135)
        val camera = viewport.camera
        val bgColor =
            if (preferredFormat.srgb) world.defaultLevelBackgroundColor.toLinear()
            else world.defaultLevelBackgroundColor

        onResize { width, height ->
            viewport.update(width, height)
            graphics.configureSurface(
                TextureUsage.RENDER_ATTACHMENT,
                preferredFormat,
                PresentMode.FIFO,
                surfaceCapabilities.alphaModes[0],
            )
        }

        addWASDMovement(camera, 0.05f)
        onUpdate {
            val surfaceTexture = graphics.surface.getCurrentTexture()
            when (val status = surfaceTexture.status) {
                TextureStatus.SUCCESS -> {
                    // all good, could check for `surfaceTexture.suboptimal` here.
                }
                TextureStatus.TIMEOUT,
                TextureStatus.OUTDATED,
                TextureStatus.LOST -> {
                    surfaceTexture.texture?.release()
                    logger.info { "getCurrentTexture status=$status" }
                    return@onUpdate
                }
                else -> {
                    // fatal
                    logger.fatal { "getCurrentTexture status=$status" }
                    close()
                    return@onUpdate
                }
            }
            val swapChainTexture = checkNotNull(surfaceTexture.texture)
            val frame = swapChainTexture.createView()

            val commandEncoder = device.createCommandEncoder()
            val renderPassEncoder =
                commandEncoder.beginRenderPass(
                    desc =
                        RenderPassDescriptor(
                            listOf(
                                RenderPassColorAttachmentDescriptor(
                                    view = frame,
                                    loadOp = LoadOp.CLEAR,
                                    storeOp = StoreOp.STORE,
                                    clearColor = bgColor,
                                )
                            )
                        )
                )
            camera.update()

            batch.use(renderPassEncoder, camera.viewProjection) {
                world.render(it, camera, scale = 1f)
            }
            renderPassEncoder.end()
            renderPassEncoder.release()

            val commandBuffer = commandEncoder.finish()

            device.queue.submit(commandBuffer)
            graphics.surface.present()

            commandBuffer.release()
            commandEncoder.release()
            frame.release()
            swapChainTexture.release()
        }

        onRelease {
            batch.release()
            mapLoader.release()
        }
    }
}

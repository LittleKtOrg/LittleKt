package com.littlekt.examples

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.readPixmap
import com.littlekt.graphics.Color
import com.littlekt.graphics.webgpu.*

/**
 * An example rendering a texture using a [CommandEncoder] in pure WebGPU.
 *
 * @author Colton Daily
 * @date 4/5/2024
 */
class TextureViaCommandEncoderExample(context: Context) : ContextListener(context) {
    // language=wgsl
    private val textureShader =
        """
            struct VertexOutput {
                @location(0) uv: vec2<f32>,
                @builtin(position) position: vec4<f32>,
            };
                       
            
            @vertex
            fn vs_main(
                @location(0) pos: vec2<f32>,
                @location(1) uvs: vec2<f32>) -> VertexOutput {
                
                var output: VertexOutput;
                output.position = vec4<f32>(pos.x, pos.y, 0, 1);
                output.uv = uvs;
                
                return output;
            }
            
            @group(0) @binding(0)
            var my_texture: texture_2d<f32>;
            @group(0) @binding(1)
            var my_sampler: sampler;
            
            @fragment
            fn fs_main(in: VertexOutput) -> @location(0) vec4<f32> {
                return textureSample(my_texture, my_sampler, in.uv);
            }
        """
            .trimIndent()

    override suspend fun Context.start() {
        addStatsHandler()
        // @formatter:off
        val vertices =
            floatArrayOf(
                -0.5f,
                -0.5f,
                0f,
                1f,
                -0.5f,
                0.5f,
                0f,
                0f,
                0.5f,
                0.5f,
                1f,
                0f,
                0.5f,
                -0.5f,
                1f,
                1f,
            )
        val indices = shortArrayOf(0, 1, 2, 0, 2, 3)
        // @formatter:on
        val image = resourcesVfs["pika.png"].readPixmap()
        val device = graphics.device
        val vbo = device.createGPUFloatBuffer("vbo", vertices, BufferUsage.VERTEX)
        val ibo = device.createGPUShortBuffer("ibo", indices, BufferUsage.INDEX)
        val shader = device.createShaderModule(textureShader)
        val surfaceCapabilities = graphics.surfaceCapabilities
        val preferredFormat = graphics.preferredFormat
        val texture =
            device.createTexture(
                TextureDescriptor(
                    Extent3D(image.width, image.height, 1),
                    1,
                    1,
                    TextureDimension.D2,
                    TextureFormat.RGBA8_UNORM_SRGB,
                    TextureUsage.COPY_DST or TextureUsage.TEXTURE,
                )
            )

        val queue = device.queue
        run {
            val textureBuffer =
                device.createGPUByteBuffer("tex temp", image.pixels.toArray(), BufferUsage.COPY_SRC)
            val commandEncoder = device.createCommandEncoder()
            commandEncoder.copyBufferToTexture(
                BufferCopyView(textureBuffer, TextureDataLayout(image.width * 4, image.height)),
                TextureCopyView(texture),
                Extent3D(image.width, image.height, 1),
            )
            queue.submit(commandEncoder.finish())
        }

        val sampler = device.createSampler(SamplerDescriptor())
        val textureView = texture.createView()
        val bindGroupLayout =
            device.createBindGroupLayout(
                BindGroupLayoutDescriptor(
                    listOf(
                        BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, TextureBindingLayout()),
                        BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                    )
                )
            )
        val bindGroup =
            device.createBindGroup(
                desc =
                    BindGroupDescriptor(
                        bindGroupLayout,
                        listOf(BindGroupEntry(0, textureView), BindGroupEntry(1, sampler)),
                    )
            )
        val pipelineLayout = device.createPipelineLayout(PipelineLayoutDescriptor(bindGroupLayout))
        val renderPipelineDesc =
            RenderPipelineDescriptor(
                layout = pipelineLayout,
                vertex =
                    VertexState(
                        module = shader,
                        entryPoint = "vs_main",
                        WebGPUVertexBufferLayout(
                            4L * Float.SIZE_BYTES,
                            VertexStepMode.VERTEX,
                            listOf(
                                WebGPUVertexAttribute(VertexFormat.FLOAT32x2, 0, 0),
                                WebGPUVertexAttribute(
                                    VertexFormat.FLOAT32x2,
                                    2L * Float.SIZE_BYTES,
                                    1,
                                ),
                            ),
                        ),
                    ),
                fragment =
                    FragmentState(
                        module = shader,
                        entryPoint = "fs_main",
                        target =
                            ColorTargetState(
                                format = preferredFormat,
                                blendState = BlendState.NonPreMultiplied,
                                writeMask = ColorWriteMask.ALL,
                            ),
                    ),
                primitive = PrimitiveState(topology = PrimitiveTopology.TRIANGLE_LIST),
                depthStencil = null,
                multisample =
                    MultisampleState(count = 1, mask = 0xFFFFFFF, alphaToCoverageEnabled = false),
            )
        val renderPipeline = device.createRenderPipeline(renderPipelineDesc)
        graphics.configureSurface(
            TextureUsage.RENDER_ATTACHMENT,
            preferredFormat,
            PresentMode.FIFO,
            surfaceCapabilities.alphaModes[0],
        )

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
                    graphics.configureSurface(
                        TextureUsage.RENDER_ATTACHMENT,
                        preferredFormat,
                        PresentMode.FIFO,
                        surfaceCapabilities.alphaModes[0],
                    )
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
                                    clearColor = Color.CLEAR,
                                )
                            )
                        )
                )
            renderPassEncoder.setPipeline(renderPipeline)
            renderPassEncoder.setBindGroup(0, bindGroup)
            renderPassEncoder.setVertexBuffer(0, vbo)
            renderPassEncoder.setIndexBuffer(ibo, IndexFormat.UINT16)
            renderPassEncoder.drawIndexed(indices.size, 1)
            renderPassEncoder.end()
            renderPassEncoder.release()

            val commandBuffer = commandEncoder.finish()

            queue.submit(commandBuffer)
            graphics.surface.present()

            commandBuffer.release()
            commandEncoder.release()
            frame.release()
            swapChainTexture.release()
        }

        onRelease {
            renderPipeline.release()
            pipelineLayout.release()
            bindGroup.release()
            bindGroupLayout.release()
            sampler.release()
            textureView.release()
            texture.release()
            ibo.release()
            vbo.release()
            texture.release()
            shader.release()
        }
    }
}

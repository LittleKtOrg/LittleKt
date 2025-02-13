package com.littlekt.graphics.g2d

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.*
import com.littlekt.log.Logger
import com.littlekt.util.align
import kotlin.math.min

/**
 * The default [Shader] that is used in [SpriteCache].
 *
 * @param device the current device
 * @param staticSize the initial size of the static sprite storage buffer in floats
 * @param dynamicSize the initial size of the dynamic sprite storage buffer in floats
 * @author Colton Daily
 * @date 4/15/2024
 */
class SpriteCacheShader(device: Device, staticSize: Int, dynamicSize: Int) :
    Shader(
        device,
        src =
            """
        struct CameraUniform {
            view_proj: mat4x4<f32>
        };
        
        struct Sprite {
            position: vec2<f32>,
            scale: vec2<f32>,
            size: vec2<f32>,
            rotation: f32,
            color: vec4<f32>,
        };
        
        struct SpritesBuffer {
            models: array<Sprite>
        };
        
        struct UVBuffer {
            uvs: vec4<f32>,
            uvsRotated: f32
        };
        
        struct UVInfo {
            models: array<UVBuffer>
        };
        
        struct VertexOutput {
            @location(0) color: vec4<f32>,
            @location(1) uv: vec2<f32>,
            @builtin(position) position: vec4<f32>,
        };
        
        @group(0) @binding(0)
        var<uniform> camera: CameraUniform;
        @group(1) @binding(0)
        var <storage, read> sprites: SpritesBuffer;
        @group(1) @binding(1)
        var <storage, read> uvs: UVInfo;
        
        @vertex
        fn vs_main(
            @builtin(instance_index) i_id: u32,
            @builtin(vertex_index) v_id: u32,
            @location(0) pos: vec3<f32>) -> VertexOutput {

            var output: VertexOutput;
            let sprite: Sprite = sprites.models[i_id];
            let sx: f32 = sprite.scale.x * sprite.size.x;
            let sy: f32 = sprite.scale.y * sprite.size.y;
            let sz: f32 = 1.0;
            
            let rot: f32 = sprite.rotation;
            
            let tx: f32 = sprite.position.x;
            let ty: f32 = sprite.position.y;
            let tz: f32 = 0.0;
            
            let s: f32 = sin(rot);
            let c: f32 = cos(rot);
            
            let uvInfo = uvs.models[i_id];
            let u0: f32 = uvInfo.uvs.x;
            let v0: f32 = uvInfo.uvs.y;
            let u1: f32 = uvInfo.uvs.z;
            let v1: f32 = uvInfo.uvs.w;
            
            let scaleM: mat4x4<f32> = mat4x4<f32>(sx, 0, 0, 0,
                                                  0, sy, 0, 0,
                                                  0, 0, sz, 0,
                                                  0, 0, 0, 1);

            // rotation and translation
            let modelM: mat4x4<f32> = mat4x4<f32>(c, s,  0,  0,
                                                 -s, c,  0,  0,
                                                  0, 0,  1,  0,
                                                 tx, ty, tz, 1) * scaleM;
            
            // should be 1 for rotated otherwise 0                                     
            let rotated: u32 = (u32(uvInfo.uvsRotated) << 16) & 0xFF;
            
            // expects vertices in the following order: top left -> top right -> bottom right -> bottom left
            // 0 -> top left
            // 1 -> top right
            // 2 -> bottom right
            // 3 -> bottom left
            // x: u
            // y: v
            // z: u1
            // w: v1
            
            let cornerIndex: u32 = v_id % 4;
            var u: f32 =  uvInfo.uvs.x; // u
            if ((rotated == 1 && cornerIndex == 0) || cornerIndex == 1 || (rotated == 0 && cornerIndex == 2)) {
                u = uvInfo.uvs.z; // u1
            }
            
            var v: f32 = uvInfo.uvs.y; // v
            if ((rotated == 1 && cornerIndex == 1) || cornerIndex == 2 || (rotated == 0 && cornerIndex == 3)) {
                v = uvInfo.uvs.w; // v1
            }
            
            
            output.position = camera.view_proj * modelM * vec4<f32>(pos, 1);
            output.color = sprite.color;
            output.uv = vec2<f32>(u,v);

            return output;
        }

        @group(2) @binding(0)
        var my_texture: texture_2d<f32>;
        @group(2) @binding(1)
        var my_sampler: sampler;

        @fragment
        fn fs_main(in: VertexOutput) -> @location(0) vec4<f32> {
            return textureSample(my_texture, my_sampler, in.uv) * in.color;
        }
        """
                .trimIndent(),
        bindGroupLayoutUsageLayout =
            listOf(BindingUsage.CAMERA, SPRITE_STORAGE, BindingUsage.TEXTURE),
        layout =
            mapOf(
                BindingUsage.CAMERA to
                    BindGroupLayoutDescriptor(
                        listOf(
                            BindGroupLayoutEntry(
                                0,
                                ShaderStage.VERTEX,
                                BufferBindingLayout(
                                    hasDynamicOffset = true,
                                    minBindingSize =
                                        (Float.SIZE_BYTES * 16)
                                            .align(device.limits.minUniformBufferOffsetAlignment)
                                            .toLong(),
                                ),
                            )
                        ),
                        label = "SpriteCache Camera BindGroupLayoutDescriptor",
                    ),
                SPRITE_STORAGE to
                    BindGroupLayoutDescriptor(
                        listOf(
                            BindGroupLayoutEntry(
                                0,
                                ShaderStage.VERTEX,
                                BufferBindingLayout(type = BufferBindingType.READ_ONLY_STORAGE),
                            ),
                            BindGroupLayoutEntry(
                                1,
                                ShaderStage.VERTEX,
                                BufferBindingLayout(type = BufferBindingType.READ_ONLY_STORAGE),
                            ),
                        )
                    ),
                BindingUsage.TEXTURE to
                    BindGroupLayoutDescriptor(
                        listOf(
                            BindGroupLayoutEntry(0, ShaderStage.FRAGMENT, TextureBindingLayout()),
                            BindGroupLayoutEntry(1, ShaderStage.FRAGMENT, SamplerBindingLayout()),
                        ),
                        label = "SpriteCache texture BindGroupLayoutDescriptor",
                    ),
            ),
    ) {

    /**
     * The [GPUBuffer] that holds the static sprite data.
     *
     * @see updateSpriteStaticStorage
     */
    private var spriteStaticStorage =
        device.createGPUFloatBuffer(
            "static sprite storage buffer",
            FloatArray(staticSize),
            BufferUsage.STORAGE or BufferUsage.COPY_DST,
        )

    private var staticSpriteStorageBufferBinding = BufferBinding(spriteStaticStorage)

    /**
     * The [GPUBuffer] that holds the dynamic sprite data.
     *
     * @see updateSpriteStaticStorage
     */
    private var spriteDynamicStorage =
        device.createGPUFloatBuffer(
            "dynamic sprite storage buffer",
            FloatArray(dynamicSize),
            BufferUsage.STORAGE or BufferUsage.COPY_DST,
        )

    private var dynamicSpriteStorageBufferBinding = BufferBinding(spriteDynamicStorage)

    private var storageBindGroup =
        device.createBindGroup(
            BindGroupDescriptor(
                getBindGroupLayoutByUsage(SPRITE_STORAGE),
                listOf(
                    BindGroupEntry(0, staticSpriteStorageBufferBinding),
                    BindGroupEntry(1, dynamicSpriteStorageBufferBinding),
                ),
            )
        )

    override fun setBindGroup(
        renderPassEncoder: RenderPassEncoder,
        bindGroup: BindGroup,
        bindingUsage: BindingUsage,
        dynamicOffsets: List<Long>,
    ) {
        when (bindingUsage) {
            BindingUsage.CAMERA -> renderPassEncoder.setBindGroup(0, bindGroup, dynamicOffsets)
            BindingUsage.TEXTURE -> renderPassEncoder.setBindGroup(2, bindGroup)
        }
    }

    override fun setBindGroups(renderPassEncoder: RenderPassEncoder) {
        renderPassEncoder.setBindGroup(1, storageBindGroup)
    }

    /**
     * Update this [spriteStaticStorage] with the given data.
     *
     * @param data the sprite data to upload to the buffer
     * @return true if the storage buffer was recreated; false otherwise.
     */
    fun updateSpriteStaticStorage(data: FloatBuffer): Boolean {
        if (spriteStaticStorage.size < data.capacity * Float.SIZE_BYTES) {
            logger.debug {
                "Attempting to write data to static sprite storage buffer that exceeds its current size. Destroying and recreating the buffer..."
            }
            spriteStaticStorage.release()
            spriteStaticStorage =
                device.createGPUFloatBuffer(
                    "static sprite storage buffer",
                    data,
                    BufferUsage.STORAGE or BufferUsage.COPY_DST,
                )
            staticSpriteStorageBufferBinding = BufferBinding(spriteStaticStorage)
            storageBindGroup.release()
            storageBindGroup =
                device.createBindGroup(
                    BindGroupDescriptor(
                        getBindGroupLayoutByUsage(SPRITE_STORAGE),
                        listOf(
                            BindGroupEntry(0, staticSpriteStorageBufferBinding),
                            BindGroupEntry(1, dynamicSpriteStorageBufferBinding),
                        ),
                    )
                )
            return true
        } else {
            device.queue.writeBuffer(
                spriteStaticStorage,
                data,
                size = min(spriteStaticStorage.size / Float.SIZE_BYTES, data.limit.toLong()),
            )
        }
        return false
    }

    /**
     * Update this [spriteDynamicStorage] with the given data.
     *
     * @param data the sprite data to upload to the buffer
     * @return true if the storage buffer was recreated; false otherwise.
     */
    fun updateSpriteDynamicStorage(data: FloatBuffer): Boolean {
        if (spriteDynamicStorage.size < data.capacity * Float.SIZE_BYTES) {
            logger.debug {
                "Attempting to write data to dynamic sprite storage buffer that exceeds its current size. Destroying and recreating the buffer..."
            }
            spriteDynamicStorage.release()
            spriteDynamicStorage =
                device.createGPUFloatBuffer(
                    "dynamic sprite storage buffer",
                    data,
                    BufferUsage.STORAGE or BufferUsage.COPY_DST,
                )
            dynamicSpriteStorageBufferBinding = BufferBinding(spriteDynamicStorage)
            storageBindGroup.release()
            storageBindGroup =
                device.createBindGroup(
                    BindGroupDescriptor(
                        getBindGroupLayoutByUsage(SPRITE_STORAGE),
                        listOf(
                            BindGroupEntry(0, staticSpriteStorageBufferBinding),
                            BindGroupEntry(1, dynamicSpriteStorageBufferBinding),
                        ),
                    )
                )
            return true
        } else {
            device.queue.writeBuffer(
                spriteDynamicStorage,
                data,
                size = min(spriteDynamicStorage.size / Float.SIZE_BYTES, data.limit.toLong()),
            )
        }
        return false
    }

    companion object {
        val SPRITE_STORAGE = BindingUsage("SpriteCache Sprite Storage")
        private val logger = Logger<SpriteCacheShader>()
    }
}

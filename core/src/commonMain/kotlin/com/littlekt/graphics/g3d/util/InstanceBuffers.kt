package com.littlekt.graphics.g3d.util

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.webgpu.*
import com.littlekt.log.Logger
import kotlin.math.min

/**
 * A buffers helper class for handling instancing data.
 *
 * @param instanceDataSize the size of the instance data as number of components (NOT BYTES)
 * @author Colton Daily
 * @date 1/15/2025
 */
class InstanceBuffers(val device: Device, instanceDataSize: Int) : Releasable {

    /**
     * The [GPUBuffer] that holds the static instance data.
     *
     * @see updateStaticStorage
     */
    private var staticStorage =
        device.createGPUFloatBuffer(
            "instance buffers static storage buffer",
            FloatArray(instanceDataSize),
            BufferUsage.STORAGE or BufferUsage.COPY_DST,
        )

    private var staticStorageBufferBinding = BufferBinding(staticStorage)

    private var bindGroup: BindGroup? = null
    private var bindGroupLayout: BindGroupLayout? = null

    /**
     * @return an existing bind group, created previously, if not then creates the bind group for
     *   the instance.
     */
    fun getOrCreateBindGroup(layout: BindGroupLayout): BindGroup {
        return bindGroup
            ?: run {
                bindGroupLayout = layout
                return device
                    .createBindGroup(
                        BindGroupDescriptor(
                            layout,
                            listOf(BindGroupEntry(0, staticStorageBufferBinding)),
                        )
                    )
                    .also { bindGroup = it }
            }
    }

    /**
     * Update this [staticStorage] with the given data.
     *
     * @param data the data to upload to the buffer
     * @return true if the storage buffer was recreated; false otherwise.
     */
    fun updateStaticStorage(data: FloatBuffer): Boolean {
        if (staticStorage.size < data.capacity * Float.SIZE_BYTES) {
            logger.debug {
                "Attempting to write data to static instance storage buffer that exceeds its current size. Destroying and recreating the buffer..."
            }
            staticStorage.release()
            staticStorage =
                device.createGPUFloatBuffer(
                    "static instance storage buffer",
                    data,
                    BufferUsage.STORAGE or BufferUsage.COPY_DST,
                )
            staticStorageBufferBinding = BufferBinding(staticStorage)
            val layout = bindGroupLayout
            bindGroup?.release()
            if (layout != null) {
                bindGroup =
                    device.createBindGroup(
                        BindGroupDescriptor(
                            layout,
                            listOf(BindGroupEntry(0, staticStorageBufferBinding)),
                        )
                    )
            }
            return true
        } else {
            device.queue.writeBuffer(
                staticStorage,
                data,
                size = min(staticStorage.size / Float.SIZE_BYTES, data.limit.toLong()),
            )
        }
        return false
    }

    companion object {
        private val logger = Logger<InstanceBuffers>()
    }

    override fun release() {
        staticStorage.release()
        bindGroup?.release()
    }
}

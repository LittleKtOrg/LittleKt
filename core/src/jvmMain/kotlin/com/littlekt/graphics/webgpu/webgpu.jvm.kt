@file:OptIn(ExperimentalUnsignedTypes::class)

package com.littlekt.graphics.webgpu

import com.littlekt.Releasable
import com.littlekt.file.*
import com.littlekt.log.Logger
import com.littlekt.resources.BufferResourceInfo
import com.littlekt.resources.TextureResourceInfo
import io.ygdrasil.wgpu.wgpuDeviceCreateTexture
import ffi.ArrayHolder
import ffi.MemoryAllocator
import ffi.MemoryBuffer
import ffi.NativeAddress
import ffi.memoryScope
import io.ygdrasil.wgpu.WGPUAdapter
import io.ygdrasil.wgpu.WGPUBuffer
import io.ygdrasil.wgpu.WGPUBufferDescriptor
import io.ygdrasil.wgpu.WGPUCommandEncoderDescriptor
import io.ygdrasil.wgpu.WGPUCompositeAlphaMode
import io.ygdrasil.wgpu.WGPUDevice
import io.ygdrasil.wgpu.WGPUExtent3D
import io.ygdrasil.wgpu.WGPULimits
import io.ygdrasil.wgpu.WGPUPipelineLayoutDescriptor
import io.ygdrasil.wgpu.WGPUQueue
import io.ygdrasil.wgpu.WGPURequestAdapterStatus_Success
import io.ygdrasil.wgpu.WGPURequestDeviceCallback
import io.ygdrasil.wgpu.WGPURequestDeviceCallbackInfo
import io.ygdrasil.wgpu.WGPUSType_ShaderSourceWGSL
import io.ygdrasil.wgpu.WGPUSampler
import io.ygdrasil.wgpu.WGPUShaderModule
import io.ygdrasil.wgpu.WGPUShaderModuleDescriptor
import io.ygdrasil.wgpu.WGPUShaderSourceWGSL
import io.ygdrasil.wgpu.WGPUSurface
import io.ygdrasil.wgpu.WGPUSurfaceCapabilities
import io.ygdrasil.wgpu.WGPUSurfaceConfiguration
import io.ygdrasil.wgpu.WGPUSurfaceTexture
import io.ygdrasil.wgpu.WGPUTexture
import io.ygdrasil.wgpu.WGPUTextureFormat
import io.ygdrasil.wgpu.WGPUTextureView
import io.ygdrasil.wgpu.WGPUTextureViewDescriptor
import io.ygdrasil.wgpu.wgpuAdapterGetLimits
import io.ygdrasil.wgpu.wgpuAdapterHasFeature
import io.ygdrasil.wgpu.wgpuAdapterRelease
import io.ygdrasil.wgpu.wgpuAdapterRequestDevice
import io.ygdrasil.wgpu.wgpuBufferGetMappedRange
import io.ygdrasil.wgpu.wgpuBufferRelease
import io.ygdrasil.wgpu.wgpuBufferUnmap
import io.ygdrasil.wgpu.wgpuDeviceCreateBindGroup
import io.ygdrasil.wgpu.wgpuDeviceCreateBindGroupLayout
import io.ygdrasil.wgpu.wgpuDeviceCreateBuffer
import io.ygdrasil.wgpu.wgpuDeviceCreateCommandEncoder
import io.ygdrasil.wgpu.wgpuDeviceCreateRenderPipeline
import io.ygdrasil.wgpu.wgpuDeviceCreateComputePipeline
import io.ygdrasil.wgpu.wgpuDeviceCreatePipelineLayout
import io.ygdrasil.wgpu.wgpuDeviceCreateSampler
import io.ygdrasil.wgpu.wgpuDeviceCreateShaderModule
import io.ygdrasil.wgpu.wgpuDeviceGetLimits
import io.ygdrasil.wgpu.wgpuDeviceGetQueue
import io.ygdrasil.wgpu.wgpuDeviceHasFeature
import io.ygdrasil.wgpu.wgpuDevicePoll
import io.ygdrasil.wgpu.wgpuDeviceRelease
import io.ygdrasil.wgpu.wgpuQueueRelease
import io.ygdrasil.wgpu.wgpuQueueSubmit
import io.ygdrasil.wgpu.wgpuQueueWriteBuffer
import io.ygdrasil.wgpu.wgpuQueueWriteTexture
import io.ygdrasil.wgpu.wgpuSamplerRelease
import io.ygdrasil.wgpu.wgpuShaderModuleRelease
import io.ygdrasil.wgpu.wgpuSurfaceConfigure
import io.ygdrasil.wgpu.wgpuSurfaceGetCapabilities
import io.ygdrasil.wgpu.wgpuSurfaceGetCurrentTexture
import io.ygdrasil.wgpu.wgpuSurfacePresent
import io.ygdrasil.wgpu.wgpuSurfaceRelease
import io.ygdrasil.wgpu.wgpuTextureCreateView
import io.ygdrasil.wgpu.wgpuTextureRelease
import io.ygdrasil.wgpu.wgpuTextureViewRelease
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

actual class Device(val segment: WGPUDevice) : Releasable {

    actual val queue: Queue by lazy { Queue(wgpuDeviceGetQueue(segment) ?: error("Failed to get queue.")) }

    actual val features: List<Feature> by lazy {
        val list = mutableListOf<Feature>()
        Feature.entries.forEach {
            if (wgpuDeviceHasFeature(segment, it.nativeVal)) {
                list += it
            }
        }
        list.toList()
    }

    actual val limits: Limits by lazy {
        memoryScope { scope ->
            val desc = WGPULimits.allocate(scope)
            wgpuDeviceGetLimits(segment, desc)
            Limits(
                maxTextureDimension1D = desc.maxTextureDimension1D.toInt(),
                maxTextureDimension2D = desc.maxTextureDimension2D.toInt(),
                maxTextureDimension3D = desc.maxTextureDimension3D.toInt(),
                maxTextureArrayLayers = desc.maxTextureArrayLayers.toInt(),
                maxBindGroups = desc.maxBindGroups.toInt(),
                maxBindGroupsPlusVertexBuffers = desc.maxBindGroupsPlusVertexBuffers.toInt(),
                maxBindingsPerBindGroup = desc.maxBindingsPerBindGroup.toInt(),
                maxDynamicUniformBuffersPerPipelineLayout =
                    desc.maxDynamicUniformBuffersPerPipelineLayout.toInt(),
                maxDynamicStorageBuffersPerPipelineLayout =
                    desc.maxDynamicStorageBuffersPerPipelineLayout.toInt(),
                maxSampledTexturesPerShaderStage =
                    desc.maxSampledTexturesPerShaderStage.toInt(),
                maxSamplersPerShaderStage = desc.maxSamplersPerShaderStage.toInt(),
                maxStorageBuffersPerShaderStage = desc.maxStorageBuffersPerShaderStage.toInt(),
                maxStorageTexturesPerShaderStage =
                    desc.maxStorageTexturesPerShaderStage.toInt(),
                maxUniformBuffersPerShaderStage = desc.maxUniformBuffersPerShaderStage.toInt(),
                maxUniformBufferBindingSize = desc.maxUniformBufferBindingSize.toLong(),
                maxStorageBufferBindingSize = desc.maxStorageBufferBindingSize.toLong(),
                minUniformBufferOffsetAlignment = desc.minUniformBufferOffsetAlignment.toInt(),
                minStorageBufferOffsetAlignment = desc.minStorageBufferOffsetAlignment.toInt(),
                maxVertexBuffers = desc.maxVertexBuffers.toInt(),
                maxBufferSize = desc.maxBufferSize.toLong(),
                maxVertexAttributes = desc.maxVertexAttributes.toInt(),
                maxVertexBufferArrayStride = desc.maxVertexBufferArrayStride.toInt(),
                maxInterStageShaderVariables = desc.maxInterStageShaderVariables.toInt(),
                maxColorAttachments = desc.maxColorAttachments.toInt(),
                maxColorAttachmentBytesPerSample = desc.maxColorAttachmentBytesPerSample.toInt(),
                maxComputeWorkgroupStorageSize = desc.maxComputeWorkgroupStorageSize.toInt(),
                maxComputeInvocationsPerWorkgroup = desc.maxComputeInvocationsPerWorkgroup.toInt(),
                maxComputeWorkgroupSizeX = desc.maxComputeWorkgroupSizeX.toInt(),
                maxComputeWorkgroupSizeY = desc.maxComputeWorkgroupSizeY.toInt(),
                maxComputeWorkgroupSizeZ = desc.maxComputeWorkgroupSizeZ.toInt(),
                maxComputeWorkgroupsPerDimension = desc.maxComputeWorkgroupsPerDimension.toInt(),
            )
        }
    }

    actual fun createShaderModule(src: String): ShaderModule {
        return memoryScope { scope ->
            ShaderModule(wgpuDeviceCreateShaderModule(segment, scope.map(src)) ?: error("fail to create shader module"))
        }
    }

    internal fun MemoryAllocator.map(input: String): WGPUShaderModuleDescriptor =
        WGPUShaderModuleDescriptor.allocate(this).also { output ->
            output.nextInChain = mapCode(input).handler
        }

    private fun MemoryAllocator.mapCode(input: String) = WGPUShaderModuleDescriptor.allocate(this).apply {
        nextInChain = WGPUShaderSourceWGSL.allocate(this@mapCode).apply {
            code.length = input.length.toULong()
            code.data = allocateFrom(input)
            chain.sType = WGPUSType_ShaderSourceWGSL
        }.handler
    }


    actual fun createRenderPipeline(desc: RenderPipelineDescriptor): RenderPipeline {
        return memoryScope { scope ->
            RenderPipeline(wgpuDeviceCreateRenderPipeline(segment, scope.map(desc)) ?: error("fail to create render pipeline"))
        }
    }

    actual fun createComputePipeline(desc: ComputePipelineDescriptor): ComputePipeline {
        return memoryScope { scope ->
            ComputePipeline(wgpuDeviceCreateComputePipeline(segment, scope.map(desc)) ?: error("fail to create compute pipeline"))
        }
    }

    actual fun createPipelineLayout(desc: PipelineLayoutDescriptor): PipelineLayout {
        return memoryScope { scope ->
            PipelineLayout(wgpuDeviceCreatePipelineLayout(segment, scope.map(desc)) ?: error("fail to create pipeline layout"))
        }
    }

    internal fun MemoryAllocator.map(input: PipelineLayoutDescriptor): WGPUPipelineLayoutDescriptor =
        WGPUPipelineLayoutDescriptor.allocate(this).also { output ->
            if (input.label != null) map(input.label, output.label)
            if (input.segments.isNotEmpty()) {
                output.bindGroupLayoutCount = input.segments.size.toULong()
                output.bindGroupLayouts = input.segments.map { it.handler }
                    .let { bufferOfAddresses(it) }
                    .let { ArrayHolder(it.handler) }
            }
        }



    actual fun createCommandEncoder(label: String?): CommandEncoder {
        return memoryScope { scope ->
            val descriptor = WGPUCommandEncoderDescriptor.allocate(scope)
            CommandEncoder(wgpuDeviceCreateCommandEncoder(segment, descriptor) ?: error("fail to create command encoder"))
        }
    }

    actual fun createBuffer(desc: BufferDescriptor): GPUBuffer {
        return memoryScope { scope ->
            GPUBuffer(wgpuDeviceCreateBuffer(segment, scope.map(desc)) ?: error("fail to create buffer"), desc.size)
        }
    }

    internal fun MemoryAllocator.map(input: BufferDescriptor) = WGPUBufferDescriptor.allocate(this).also { output ->
        map(input.label, output.label)
        output.size = input.size.toULong()
        output.usage = input.usage.usageFlag.toULong()
        output.mappedAtCreation = input.mappedAtCreation
    }


    actual fun createBindGroupLayout(desc: BindGroupLayoutDescriptor): BindGroupLayout {
        return memoryScope { scope ->
            BindGroupLayout(wgpuDeviceCreateBindGroupLayout(segment, scope.map(desc)) ?: error("fail to create bind group layout"))
        }
    }

    actual fun createBindGroup(desc: BindGroupDescriptor): BindGroup {
        return memoryScope { scope ->
            BindGroup(wgpuDeviceCreateBindGroup(segment, scope.map(desc)) ?: error("fail to create bind group"))
        }
    }

    actual fun createSampler(desc: SamplerDescriptor): Sampler {
        return memoryScope { scope ->
            Sampler(wgpuDeviceCreateSampler(segment, scope.map(desc)) ?: error("fail to create sampler"))
        }
    }

    actual fun createTexture(desc: TextureDescriptor): WebGPUTexture {
        return memoryScope { scope ->
            val textureSize = (desc.size.width * desc.size.height * desc.size.depth * desc.format.bytes).toLong()
            WebGPUTexture(wgpuDeviceCreateTexture(segment, scope.map(desc)) ?: error("fail to create texture"), textureSize)
        }
    }

    actual fun createGPUShortBuffer(
        label: String,
        data: ShortArray,
        usage: BufferUsage,
    ): GPUBuffer {
        val buffer =
            createBuffer(
                BufferDescriptor(label, data.size.toLong() * Short.SIZE_BYTES, usage, true)
            )
        buffer.getMappedRange().putShort(data)
        buffer.unmap()

        return buffer
    }

    actual fun createGPUShortBuffer(
        label: String,
        data: ShortBuffer,
        usage: BufferUsage,
    ): GPUBuffer {
        val buffer =
            createBuffer(
                BufferDescriptor(label, data.capacity.toLong() * Short.SIZE_BYTES, usage, true)
            )
        buffer.getMappedRange().putShort(data)
        buffer.unmap()

        return buffer
    }

    actual fun createGPUFloatBuffer(
        label: String,
        data: FloatArray,
        usage: BufferUsage,
    ): GPUBuffer {
        val buffer =
            createBuffer(
                BufferDescriptor(label, data.size.toLong() * Float.SIZE_BYTES, usage, true)
            )
        buffer.getMappedRange().putFloat(data)
        buffer.unmap()

        return buffer
    }

    actual fun createGPUFloatBuffer(
        label: String,
        data: FloatBuffer,
        usage: BufferUsage,
    ): GPUBuffer {
        val buffer =
            createBuffer(
                BufferDescriptor(label, data.capacity.toLong() * Float.SIZE_BYTES, usage, true)
            )
        buffer.getMappedRange().putFloat(data)
        buffer.unmap()

        return buffer
    }

    actual fun createGPUIntBuffer(label: String, data: IntArray, usage: BufferUsage): GPUBuffer {
        val buffer =
            createBuffer(BufferDescriptor(label, data.size.toLong() * Int.SIZE_BYTES, usage, true))
        buffer.getMappedRange().putInt(data)
        buffer.unmap()

        return buffer
    }

    actual fun createGPUIntBuffer(label: String, data: IntBuffer, usage: BufferUsage): GPUBuffer {
        val buffer =
            createBuffer(
                BufferDescriptor(label, data.capacity.toLong() * Int.SIZE_BYTES, usage, true)
            )
        buffer.getMappedRange().putInt(data)
        buffer.unmap()

        return buffer
    }

    actual fun createGPUByteBuffer(label: String, data: ByteArray, usage: BufferUsage): GPUBuffer {
        val buffer = createBuffer(BufferDescriptor(label, data.size.toLong(), usage, true))
        buffer.getMappedRange().putByte(data)
        buffer.unmap()

        return buffer
    }

    actual fun createGPUByteBuffer(label: String, data: ByteBuffer, usage: BufferUsage): GPUBuffer {
        val buffer = createBuffer(BufferDescriptor(label, data.capacity.toLong(), usage, true))
        buffer.getMappedRange().putByte(data)
        buffer.unmap()

        return buffer
    }

    actual override fun release() {
        wgpuDeviceRelease(segment)
    }

    override fun toString(): String {
        return "Device"
    }

    /**
     * Check for resource cleanups and mapping callbacks. Will block.
     *
     * @return `true` if the queue is empty, or `false` if there are more queue submissions still in
     *   flight. (Note that, unless access to the Queue is coordinated somehow, this information
     *   could be out of date by the time the caller receives it. Queues can be shared between
     *   threads, so other threads could submit new work at any time.)
     *
     * When running on WebGPU, this is a no-op. Devices are automatically polled.
     */
    actual fun poll(): Boolean {
        return wgpuDevicePoll(segment, true, null)
    }
}

actual class Adapter(var segment: WGPUAdapter) : Releasable {
    /** The features which can be used to create devices on this adapter. */
    actual val features: List<Feature> by lazy {
        val list = mutableListOf<Feature>()
        Feature.entries.forEach {
            if (wgpuAdapterHasFeature(segment, it.nativeVal)) {
                list += it
            }
        }
        list.toList()
    }

    /**
     * The best limits which can be used to create devices on this adapter. Each adapter limit must
     * be the same or better than its default value in supported limits.
     */
    actual val limits: Limits by lazy {
        memoryScope { scope ->
            val supported = WGPULimits.allocate(scope)
            wgpuAdapterGetLimits(segment, supported)

            Limits(
                maxTextureDimension1D = supported.maxTextureDimension1D.toInt(),
                maxTextureDimension2D = supported.maxTextureDimension2D.toInt(),
                maxTextureDimension3D = supported.maxTextureDimension3D.toInt(),
                maxTextureArrayLayers = supported.maxTextureArrayLayers.toInt(),
                maxBindGroups = supported.maxBindGroups.toInt(),
                maxBindGroupsPlusVertexBuffers = supported.maxBindGroupsPlusVertexBuffers.toInt(),
                maxBindingsPerBindGroup = supported.maxBindingsPerBindGroup.toInt(),
                maxDynamicUniformBuffersPerPipelineLayout =
                    supported.maxDynamicUniformBuffersPerPipelineLayout.toInt(),
                maxDynamicStorageBuffersPerPipelineLayout =
                    supported.maxDynamicStorageBuffersPerPipelineLayout.toInt(),
                maxSampledTexturesPerShaderStage =
                    supported.maxSampledTexturesPerShaderStage.toInt(),
                maxSamplersPerShaderStage = supported.maxSamplersPerShaderStage.toInt(),
                maxStorageBuffersPerShaderStage = supported.maxStorageBuffersPerShaderStage.toInt(),
                maxStorageTexturesPerShaderStage =
                    supported.maxStorageTexturesPerShaderStage.toInt(),
                maxUniformBuffersPerShaderStage = supported.maxUniformBuffersPerShaderStage.toInt(),
                maxUniformBufferBindingSize = supported.maxUniformBufferBindingSize.toLong(),
                maxStorageBufferBindingSize = supported.maxStorageBufferBindingSize.toLong(),
                minUniformBufferOffsetAlignment = supported.minUniformBufferOffsetAlignment.toInt(),
                minStorageBufferOffsetAlignment = supported.minStorageBufferOffsetAlignment.toInt(),
                maxVertexBuffers = supported.maxVertexBuffers.toInt(),
                maxBufferSize = supported.maxBufferSize.toLong(),
                maxVertexAttributes = supported.maxVertexAttributes.toInt(),
                maxVertexBufferArrayStride = supported.maxVertexBufferArrayStride.toInt(),
                maxInterStageShaderVariables = supported.maxInterStageShaderVariables.toInt(),
                maxColorAttachments = supported.maxColorAttachments.toInt(),
                maxColorAttachmentBytesPerSample =
                    supported.maxColorAttachmentBytesPerSample.toInt(),
                maxComputeWorkgroupStorageSize = supported.maxComputeWorkgroupStorageSize.toInt(),
                maxComputeInvocationsPerWorkgroup =
                    supported.maxComputeInvocationsPerWorkgroup.toInt(),
                maxComputeWorkgroupSizeX = supported.maxComputeWorkgroupSizeX.toInt(),
                maxComputeWorkgroupSizeY = supported.maxComputeWorkgroupSizeY.toInt(),
                maxComputeWorkgroupSizeZ = supported.maxComputeWorkgroupSizeZ.toInt(),
                maxComputeWorkgroupsPerDimension = supported.maxComputeWorkgroupsPerDimension.toInt(),
            )
        }
    }

    actual suspend fun requestDevice(descriptor: DeviceDescriptor?): Device {
        val output = atomic<WGPUDevice?>(null)
        memoryScope { scope ->

            val callback = WGPURequestDeviceCallback.allocate(scope) { status, device, message, userdata1, userdata2 ->
                if (status == WGPURequestAdapterStatus_Success) {
                    output.update { device }
                } else {
                    logger.log(Logger.Level.ERROR) {
                        "requestDevice status=$status, message=${message?.data?.toKString(message.length)}"
                    }
                }
            }

            val callbackInfo = WGPURequestDeviceCallbackInfo.allocate(scope).apply {
                this.callback = callback
                this.userdata2 = scope.bufferOfAddress(callback.handler).handler
            }

            wgpuAdapterRequestDevice(segment, scope.map(descriptor), callbackInfo)
        }
        return Device(output.value ?: error("fail to get device"))
    }

    actual override fun release() {
        wgpuAdapterRelease(segment)
    }

    override fun toString(): String {
        return "Adapter"
    }

    companion object {
        private val logger = Logger<Adapter>()
    }
}

actual class Queue(val segment: WGPUQueue) : Releasable {

    actual fun submit(vararg cmdBuffers: CommandBuffer) {
        memoryScope { scope ->
            wgpuQueueSubmit(
                segment,
                cmdBuffers.size.toULong(),
                scope.bufferOfAddresses(cmdBuffers.map { it.segment.handler }).handler.let(::ArrayHolder)
            )
        }
    }

    actual fun writeTexture(
        data: ByteBuffer,
        destination: TextureCopyView,
        layout: TextureDataLayout,
        copySize: Extent3D,
        size: Long,
    ) {
        data as ByteBufferImpl
        memoryScope { scope ->
            wgpuQueueWriteTexture(
                segment,
                destination.toNative(scope),
                data.segment.handler,
                size.toULong(),
                layout.toNative(scope),
                copySize.toNative(scope),
            )
        }
    }

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: ShortBuffer,
        offset: Long,
        dataOffset: Long,
        size: Long,
    ) {
        data as ShortBufferImpl

        wgpuQueueWriteBuffer(
            segment,
            buffer.segment,
            offset.toULong(),
            dataBuffer(dataOffset, data),
            (size * Short.SIZE_BYTES).toULong(),
        )
    }

    private fun dataBuffer(dataOffset: Long, data: GenericBuffer<*>) =
        if (dataOffset > 0) data.segment.handler.handler.asSlice(dataOffset)
            .let(::NativeAddress) else data.segment.handler

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: IntBuffer,
        offset: Long,
        dataOffset: Long,
        size: Long,
    ) {
        data as IntBufferImpl
        wgpuQueueWriteBuffer(
            segment,
            buffer.segment,
            offset.toULong(),
            dataBuffer(dataOffset, data),
            (size * Int.SIZE_BYTES).toULong(),
        )
    }

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: FloatBuffer,
        offset: Long,
        dataOffset: Long,
        size: Long,
    ) {
        data as FloatBufferImpl
        wgpuQueueWriteBuffer(
            segment,
            buffer.segment,
            offset.toULong(),
            dataBuffer(dataOffset, data),
            (size * Float.SIZE_BYTES).toULong(),
        )
    }

    actual fun writeBuffer(
        buffer: GPUBuffer,
        data: ByteBuffer,
        offset: Long,
        dataOffset: Long,
        size: Long,
    ) {
        data as ByteBufferImpl
        wgpuQueueWriteBuffer(
            segment,
            buffer.segment,
            offset.toULong(),
            dataBuffer(dataOffset, data),
            size.toULong(),
        )
    }

    actual fun writeTexture(
        data: ByteArray,
        destination: TextureCopyView,
        layout: TextureDataLayout,
        copySize: Extent3D,
        size: Long,
    ) {
        memoryScope { scope ->
            wgpuQueueWriteTexture(
                segment,
                destination.toNative(scope),
                data.toBuffer(scope),
                size.toULong(),
                layout.toNative(scope),
                copySize.toNative(scope),
            )
        }
    }

    actual override fun release() {
        wgpuQueueRelease(segment)
    }

    override fun toString(): String {
        return "Queue"
    }
}

actual class ShaderModule(val segment: WGPUShaderModule) : Releasable {
    actual override fun release() {
        wgpuShaderModuleRelease(segment)
    }

    override fun toString(): String {
        return "ShaderModule"
    }
}

actual class Surface(val segment: WGPUSurface) : Releasable {

    actual fun configure(configuration: SurfaceConfiguration) {
        memoryScope { scope ->
            wgpuSurfaceConfigure(segment, scope.map(configuration))
        }
    }

    private fun MemoryAllocator.map(input: SurfaceConfiguration): WGPUSurfaceConfiguration =
        WGPUSurfaceConfiguration.allocate(this).also { output ->
            output.device = input.device.segment
            output.usage = input.usage.usageFlag.toULong()
            output.format = input.format.nativeVal
            output.presentMode = input.presentMode.nativeVal
            output.alphaMode = input.alphaMode.nativeVal
            output.width = input.width.toUInt()
            output.height = input.height.toUInt()
        }

    actual fun getCurrentTexture(): SurfaceTexture {
        return memoryScope { scope ->
            val surfaceTexture = WGPUSurfaceTexture.allocate(scope)
            wgpuSurfaceGetCurrentTexture(segment, surfaceTexture)
            val texture = surfaceTexture.texture?.let { WebGPUTexture(it, it.handler.handler.byteSize())  }
            val status = surfaceTexture.status.let {
                    TextureStatus.from(it) ?: error("Invalid texture status: $it")
                }
            SurfaceTexture(texture, status)
        }
    }

    actual fun present() {
        wgpuSurfacePresent(segment)
    }

    actual fun getCapabilities(adapter: Adapter): SurfaceCapabilities {
        return memoryScope { scope ->

            val surfaceCapabilities = WGPUSurfaceCapabilities.allocate(scope)
            wgpuSurfaceGetCapabilities(segment, adapter.segment, surfaceCapabilities)

            val formats = surfaceCapabilities.formats ?: error("fail to get formats")
            var supportedFormats = surfaceCapabilities.toTextureFormats(formats)

            val alphaModes = surfaceCapabilities.alphaModes ?: error("fail to get alpha modes")
            var supportedAlphaMode = surfaceCapabilities.toAlphaMode(alphaModes)

            if (supportedFormats.isEmpty()) {
                logger.warn { "fail to get supported textures on surface, will inject rgba8unorm format" }
                supportedFormats = setOf(TextureFormat.RG8_UNORM)
            }

            if (supportedAlphaMode.isEmpty()) {
                logger.warn { "fail to get supported alpha mode on surface, will inject inherit alpha mode" }
                supportedAlphaMode = setOf(AlphaMode.INHERIT)
            }
            SurfaceCapabilities(supportedFormats.toList(), supportedAlphaMode.toList())
        }
    }

    private fun WGPUSurfaceCapabilities.toTextureFormats(
        formats: ArrayHolder<WGPUTextureFormat>
    ) = UIntArray(formatCount.toInt()) { 0u }
        .also {
            MemoryBuffer(formats.handler, formatCount * Int.SIZE_BYTES.toULong())
                .readUInts(it)
        }
        .map {
            TextureFormat.from(it)
                .also { if (it == null) logger.warn { "ignoring undefined format with value $it" } }
        }
        .filterNotNull()
        .toSet()

    private fun WGPUSurfaceCapabilities.toAlphaMode(
        alphaModes: ArrayHolder<WGPUCompositeAlphaMode>
    ) = UIntArray(formatCount.toInt()) { 0u }
        .also {
            MemoryBuffer(alphaModes.handler, formatCount * Int.SIZE_BYTES.toULong())
                .readUInts(it)
        }
        .map {
            AlphaMode.from(it)
                .also { if (it == null) logger.warn { "ignoring undefined alpha mode with value $it" } }
        }
        .filterNotNull()
        .toSet()


    actual fun getPreferredFormat(adapter: Adapter): TextureFormat {
        return getCapabilities(adapter).formats[0]
    }

    actual override fun release() {
        wgpuSurfaceRelease(segment)
    }

    companion object {
        private val logger = Logger<Surface>()
    }
}

actual class WebGPUTexture(val segment: WGPUTexture, val size: Long) : Releasable {

    private val info = TextureResourceInfo(this, size)

    actual fun createView(desc: TextureViewDescriptor?): TextureView {
        return if (desc != null) {
            memoryScope { scope ->
                TextureView(wgpuTextureCreateView(segment, scope.map(desc)) ?: error("Failed to create texture view"))
            }
        } else {
            TextureView(wgpuTextureCreateView(segment, null) ?: error("Failed to create texture view"))
        }
    }

    internal fun MemoryAllocator.map(input: TextureViewDescriptor) = WGPUTextureViewDescriptor.allocate(this)
        .also { output ->

            if (input.label != null) map(input.label, output.label)
            output.format = input.format.nativeVal
            output.dimension = input.dimension.nativeVal
            output.aspect = input.aspect.nativeVal
            output.baseMipLevel = input.baseMipLevel.toUInt()
            output.mipLevelCount = input.mipLevelCount.toUInt()
            output.baseArrayLayer = input.baseArrayLayer.toUInt()
            output.arrayLayerCount = input.arrayLayerCount.toUInt()
        }

    actual override fun release() {
        wgpuTextureRelease(segment)
        info.delete()
    }

}

actual class TextureView(val segment: WGPUTextureView) : IntoBindingResource {

    actual fun release() {
        wgpuTextureViewRelease(segment)
    }
}

actual class GPUBuffer(val segment: WGPUBuffer, actual val size: Long) : Releasable {

    private val info = BufferResourceInfo(this, size)

    actual fun getMappedRange(offset: Long, size: Long): ByteBuffer {
        val mappedRange = (wgpuBufferGetMappedRange(segment, offset.toULong(), size.toULong()) ?: error("Failed to get mapped range"))
            .let { MemoryBuffer(it, size.toULong()) }
        return ByteBufferImpl(size.toInt(), mappedRange.handler.let { MemoryBuffer(it, size.toULong())})
    }

    actual fun getMappedRange(): ByteBuffer = getMappedRange(0, size)

    actual fun unmap() {
        wgpuBufferUnmap(segment)
    }

    actual override fun release() {
        wgpuBufferRelease(segment)
        info.delete()
    }

}

actual class Sampler(val segment: WGPUSampler) : IntoBindingResource, Releasable {

    actual override fun release() {
        wgpuSamplerRelease(segment)
    }
}

fun Extent3D.toNative(scope: MemoryAllocator): WGPUExtent3D {
    val native = WGPUExtent3D.allocate(scope)

    native.width = width.toUInt()
    native.height = height.toUInt()
    native.depthOrArrayLayers = depth.toUInt()

    return native
}

actual class SurfaceCapabilities(
    actual val formats: List<TextureFormat>,
    actual val alphaModes: List<AlphaMode>,
)

actual class SurfaceTexture(
    actual val texture: WebGPUTexture?,
    actual val status: TextureStatus,
)

private fun ByteArray.toBuffer(scope: MemoryAllocator): NativeAddress {
    val memorySize = size.toULong() * Byte.SIZE_BYTES.toULong()
    return scope.allocateBuffer(memorySize)
        .also { buffer -> buffer.writeBytes(this) }
        .handler
}
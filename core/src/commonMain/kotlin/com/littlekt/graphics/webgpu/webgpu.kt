package com.littlekt.graphics.webgpu

import com.littlekt.Releasable
import com.littlekt.file.ByteBuffer
import com.littlekt.file.FloatBuffer
import com.littlekt.file.IntBuffer
import com.littlekt.file.ShortBuffer
import com.littlekt.graphics.webgpu.*

/**
 * An open connection to a graphic and/or compute device.
 *
 * Responsible for the creation of most rendering and compute resources. These are then used in
 * commands, which are submitted to a [Queue].
 *
 * A device may be requested from an adapter with [Adapter.requestDevice]
 */
expect class Device : Releasable {
    /** A [Queue] that has been ready from same adapter this device was created with. */
    val queue: Queue

    /**
     * A set containing the [Feature] values of the features supported by the device (i.e. the ones
     * with which it was created).
     */
    val features: List<Feature>

    /**
     * Exposes the [Limits] supported by the device (which are exactly the ones with which it was
     * created).
     */
    val limits: Limits

    /** @return a newly created [ShaderModule] using WGSL source code. */
    fun createShaderModule(src: String): ShaderModule

    /** @return a newly created [RenderPipeline]. */
    fun createRenderPipeline(desc: RenderPipelineDescriptor): RenderPipeline

    /** @return a newly created [ComputePipeline] */
    fun createComputePipeline(desc: ComputePipelineDescriptor): ComputePipeline

    /** @return a newly created [PipelineLayout]. */
    fun createPipelineLayout(desc: PipelineLayoutDescriptor): PipelineLayout

    /**
     * @param label debug label for a [CommandEncoder].
     * @return a newly created, empty [CommandEncoder].
     */
    fun createCommandEncoder(label: String? = null): CommandEncoder

    /** @return a newly created [GPUBuffer]. */
    fun createBuffer(desc: BufferDescriptor): GPUBuffer

    /** @return a newly created [BindGroupLayout]. */
    fun createBindGroupLayout(desc: BindGroupLayoutDescriptor): BindGroupLayout

    /** @return a newly created [BindGroup]. */
    fun createBindGroup(desc: BindGroupDescriptor): BindGroup

    /** @return a newly created [Sampler]. */
    fun createSampler(desc: SamplerDescriptor): Sampler

    /** @return a newly created [WebGPUTexture]. */
    fun createTexture(desc: TextureDescriptor): WebGPUTexture

    /**
     * This uses [createBuffer] internally, maps it at creation, writes the data, and unmaps it immediately.
     *
     * @return a newly created [GPUBuffer] using an [ShortArray].
     */
    fun createGPUShortBuffer(label: String, data: ShortArray, usage: BufferUsage): GPUBuffer

    /**
     * This uses [createBuffer] internally, maps it at creation, writes the data, and unmaps it immediately.
     *
     * @return a newly created [GPUBuffer] using an [ShortBuffer].
     */
    fun createGPUShortBuffer(label: String, data: ShortBuffer, usage: BufferUsage): GPUBuffer

    /**
     * This uses [createBuffer] internally, maps it at creation, writes the data, and unmaps it immediately.
     *
     * @return a newly created [GPUBuffer] using a [FloatArray].
     */
    fun createGPUFloatBuffer(label: String, data: FloatArray, usage: BufferUsage): GPUBuffer

    /**
     * This uses [createBuffer] internally, maps it at creation, writes the data, and unmaps it immediately.
     *
     * @return a newly created [GPUBuffer] using a [FloatBuffer].
     */
    fun createGPUFloatBuffer(label: String, data: FloatBuffer, usage: BufferUsage): GPUBuffer

    /**
     * This uses [createBuffer] internally, maps it at creation, writes the data, and unmaps it immediately.
     *
     * @return a newly created [GPUBuffer] using an [IntArray].
     */
    fun createGPUIntBuffer(label: String, data: IntArray, usage: BufferUsage): GPUBuffer

    /**
     * This uses [createBuffer] internally, maps it at creation, writes the data, and unmaps it immediately.
     *
     * @return a newly created [GPUBuffer] using an [IntBuffer].
     */
    fun createGPUIntBuffer(label: String, data: IntBuffer, usage: BufferUsage): GPUBuffer

    /**
     * This uses [createBuffer] internally, maps it at creation, writes the data, and unmaps it immediately.
     *
     * @return a newly created [GPUBuffer] using a [ByteArray].
     */
    fun createGPUByteBuffer(label: String, data: ByteArray, usage: BufferUsage): GPUBuffer

    /**
     * This uses [createBuffer] internally, maps it at creation, writes the data, and unmaps it immediately.
     *
     * @return a newly created [GPUBuffer] using a [ByteBuffer].
     */
    fun createGPUByteBuffer(label: String, data: ByteBuffer, usage: BufferUsage): GPUBuffer

    /**
     * Check for resource cleanups and mapping callbacks.
     *
     * @return `true` if the queue is empty, or `false` if there are more queue submissions still in
     *   flight. (Note that, unless access to the Queue is coordinated somehow, this information
     *   could be out of date by the time the caller receives it. Queues can be shared between
     *   threads, so other threads could submit new work at any time.)
     *
     * When running on WebGPU, this is a no-op. Devices are automatically polled.
     */
    fun poll(): Boolean

    override fun release()
}

/** A handle to a physical graphics and/or compute device. */
expect class Adapter : Releasable {
    /** The features which can be used to create devices on this adapter. */
    val features: List<Feature>

    /**
     * The best limits which can be used to create devices on this adapter. Each adapter limit must
     * be the same or better than its default value in supported limits.
     */
    val limits: Limits

    /**
     * Requests a connection to a physical device, creating a logical device.
     *
     * @param descriptor a [DeviceDescriptor] describing the required support a [Device] should have
     * @return the [Device] together with a [Queue] that executes command buffers.
     */
    suspend fun requestDevice(descriptor: DeviceDescriptor? = null): Device

    /** Release the memory held by the adapter. */
    override fun release()
}

/**
 * A descriptor used when requesting a device from an adapter via [Adapter.requestDevice]
 *
 * @param label A debug label of a [Device].
 * @param requiredFeatures A list of [Feature] required to be supported by a [Device].
 * @param requiredLimits The [Limits] required to support by a [Device] via [RequiredLimits]
 */
data class DeviceDescriptor(
    val label: String? = null,
    val requiredFeatures: List<Feature> = listOf(),
    val requiredLimits: RequiredLimits? = null,
)

/**
 * Each limit is a numeric limit on the usage of WebGPU on a device.
 *
 * Each limit has a default value. Every [Adapter] is guaranteed to support the default value or
 * better. The default is used if a value is not explicitly specified in requiredLimits.
 *
 * One limit value may be better than another. A better limit value always relaxes validation,
 * enabling strictly more programs to be valid. For each limit class, "better" is defined.
 *
 * Different limits have different limit classes:
 * - **maximum**: The limit enforces a maximum on some value passed into the API. Higher values are
 *   better. May only be set to values ≥ the default. Lower values are clamped to the default.
 * - **alignment**: The limit enforces a minimum alignment on some value passed into the API; that
 *   is, the value must be a multiple of the limit. Lower values are better. May only be set to
 *   powers of 2 which are ≤ the default. Values which are not powers of 2 are invalid. Higher
 *   powers of 2 are clamped to the default.
 *
 * **Note**: Setting "better" limits may not necessarily be desirable, as they may have a
 * performance impact. Because of this, and to improve portability across devices and
 * implementations, applications should generally request the "worst" limits that work for their
 * content (ideally, the default values).
 */
data class RequiredLimits(
    /** The maximum allowed value for the size.width of a texture created with dimension "1d". */
    val maxTextureDimension1D: Int? = null,
    /**
     * The maximum allowed value for the size.width and size.height of a texture created with
     * dimension "2d".
     */
    val maxTextureDimension2D: Int? = null,
    /**
     * The maximum allowed value for the size.width, size.height and size.depthOrArrayLayers of a
     * texture created with dimension "3d".
     */
    val maxTextureDimension3D: Int? = null,
    /**
     * The maximum allowed value for the size.depthOrArrayLayers of a texture created with dimension
     * "2d".
     */
    val maxTextureArrayLayers: Int? = null,
    /**
     * The maximum number of [BindGroupLayout] allowed in bindGroupLayouts when creating a
     * [PipelineLayout].
     */
    val maxBindGroups: Int? = null,
    /**
     * The maximum number of bind group and vertex buffer slots used simultaneously, counting any
     * empty slots below the highest index. Validated in createRenderPipeline() and in draw calls.
     */
    val maxBindGroupsPlusVertexBuffers: Int? = null,

    /**
     * The number of binding indices available when creating a [BindGroupLayout].
     *
     * **Note**: This limit is normative, but arbitrary. With the default binding slot limits, it is
     * impossible to use 1000 bindings in one bind group, but this allows
     * [BindGroupLayoutEntry.binding] values up to 999. This limit allows implementations to treat
     * binding space as an array, within reasonable memory space, rather than a sparse map
     * structure.
     */
    val maxBindingsPerBindGroup: Int? = null,
    /**
     * The maximum number of [BindGroupLayoutEntry] entries across a [PipelineLayout] which are
     * uniform buffers with dynamic offsets.
     */
    val maxDynamicUniformBuffersPerPipelineLayout: Int? = null,
    /**
     * The maximum number of [BindGroupLayoutEntry] entries across a [PipelineLayout] which are
     * storage buffers with dynamic offset
     */
    val maxDynamicStorageBuffersPerPipelineLayout: Int? = null,
    /**
     * For each possible [ShaderStage] stage, the maximum number of [BindGroupLayoutEntry] entries
     * across a [PipelineLayout] which are sampled textures.
     */
    val maxSampledTexturesPerShaderStage: Int? = null,
    /**
     * For each possible [ShaderStage] stage, the maximum number of [BindGroupLayoutEntry] entries
     * across a [PipelineLayout] which are samplers.
     */
    val maxSamplersPerShaderStage: Int? = null,
    /**
     * For each possible GPUShaderStage stage, the maximum number of GPUBindGroupLayoutEntry entries
     * across a GPUPipelineLayout which are storage buffers.
     */
    val maxStorageBuffersPerShaderStage: Int? = null,
    /**
     * For each possible GPUShaderStage stage, the maximum number of GPUBindGroupLayoutEntry entries
     * across a GPUPipelineLayout which are storage textures.
     */
    val maxStorageTexturesPerShaderStage: Int? = null,
    /**
     * For each possible GPUShaderStage stage, the maximum number of GPUBindGroupLayoutEntry entries
     * across a GPUPipelineLayout which are uniform buffers.
     */
    val maxUniformBuffersPerShaderStage: Int? = null,
    /**
     * The maximum GPUBufferBinding.size for bindings with a GPUBindGroupLayoutEntry entry for which
     * entry.buffer?.type is "uniform".
     */
    val maxUniformBufferBindingSize: Long? = null,
    /**
     * The maximum GPUBufferBinding.size for bindings with a GPUBindGroupLayoutEntry entry for which
     * entry.buffer?.type is "storage" or "read-only-storage".
     */
    val maxStorageBufferBindingSize: Long? = null,
    /**
     * The required alignment for GPUBufferBinding.offset and the dynamic offsets provided in
     * setBindGroup(), for bindings with a GPUBindGroupLayoutEntry entry for which
     * entry.buffer?.type is "uniform".
     */
    val minUniformBufferOffsetAlignment: Int? = null,
    /**
     * The required alignment for GPUBufferBinding.offset and the dynamic offsets provided in
     * setBindGroup(), for bindings with a GPUBindGroupLayoutEntry entry for which
     * entry.buffer?.type is "storage" or "read-only-storage".
     */
    val minStorageBufferOffsetAlignment: Int? = null,
    /** The maximum number of buffers when creating a GPURenderPipeline. */
    val maxVertexBuffers: Int? = null,
    /** The maximum size of size when creating a GPUBuffer. */
    val maxBufferSize: Long? = null,
    /**
     * The maximum number of attributes in total across buffers when creating a GPURenderPipeline.
     */
    val maxVertexAttributes: Int? = null,
    /** The maximum allowed arrayStride when creating a GPURenderPipeline. */
    val maxVertexBufferArrayStride: Int? = null,
    /**
     * The maximum allowed number of components of input or output variables for inter-stage
     * communication (like vertex outputs or fragment inputs).
     */
    val maxInterStageShaderComponents: Int? = null,
    /**
     * The maximum allowed number of input or output variables for inter-stage communication (like
     * vertex outputs or fragment inputs).
     */
    val maxInterStageShaderVariables: Int? = null,
    /**
     * The maximum allowed number of color attachments in
     * GPURenderPipelineDescriptor.fragment.targets, GPURenderPassDescriptor.colorAttachments, and
     * GPURenderPassLayout.colorFormats.
     */
    val maxColorAttachments: Int? = null,
    /**
     * The maximum number of bytes necessary to hold one sample (pixel or subpixel) of render
     * pipeline output data, across all color attachments.
     */
    val maxColorAttachmentBytesPerSample: Int? = null,
    /**
     * The maximum number of bytes of workgroup storage used for a compute stage GPUShaderModule
     * entry-point.
     */
    val maxComputeWorkgroupStorageSize: Int? = null,
    /**
     * The maximum value of the product of the workgroup_size dimensions for a compute stage
     * GPUShaderModule entry-point.
     */
    val maxComputeInvocationsPerWorkgroup: Int? = null,
    /**
     * The maximum value of the workgroup_size X dimension for a compute stage GPUShaderModule
     * entry-point.
     */
    val maxComputeWorkgroupSizeX: Int? = null,
    /**
     * The maximum value of the workgroup_size Y dimensions for a compute stage GPUShaderModule
     * entry-point.
     */
    val maxComputeWorkgroupSizeY: Int? = null,
    /**
     * The maximum value of the workgroup_size Z dimensions for a compute stage GPUShaderModule
     * entry-point.
     */
    val maxComputeWorkgroupSizeZ: Int? = null,
    /**
     * The maximum value for the arguments of dispatchWorkgroups(workgroupCountX, workgroupCountY,
     * workgroupCountZ).
     */
    val maxComputeWorkgroupsPerDimension: Int? = null,
)

/**
 * Each limit is a numeric limit on the usage of WebGPU on a device.
 *
 * Each limit has a default value. Every [Adapter] is guaranteed to support the default value or
 * better. The default is used if a value is not explicitly specified in requiredLimits.
 *
 * One limit value may be better than another. A better limit value always relaxes validation,
 * enabling strictly more programs to be valid. For each limit class, "better" is defined.
 *
 * Different limits have different limit classes:
 * - **maximum**: The limit enforces a maximum on some value passed into the API. Higher values are
 *   better. May only be set to values ≥ the default. Lower values are clamped to the default.
 * - **alignment**: The limit enforces a minimum alignment on some value passed into the API; that
 *   is, the value must be a multiple of the limit. Lower values are better. May only be set to
 *   powers of 2 which are ≤ the default. Values which are not powers of 2 are invalid. Higher
 *   powers of 2 are clamped to the default.
 *
 * **Note**: Setting "better" limits may not necessarily be desirable, as they may have a
 * performance impact. Because of this, and to improve portability across devices and
 * implementations, applications should generally request the "worst" limits that work for their
 * content (ideally, the default values).
 */
data class Limits(
    /** The maximum allowed value for the size.width of a texture created with dimension "1d". */
    val maxTextureDimension1D: Int,
    /**
     * The maximum allowed value for the size.width and size.height of a texture created with
     * dimension "2d".
     */
    val maxTextureDimension2D: Int,
    /**
     * The maximum allowed value for the size.width, size.height and size.depthOrArrayLayers of a
     * texture created with dimension "3d".
     */
    val maxTextureDimension3D: Int,
    /**
     * The maximum allowed value for the size.depthOrArrayLayers of a texture created with dimension
     * "2d".
     */
    val maxTextureArrayLayers: Int,
    /**
     * The maximum number of [BindGroupLayout] allowed in bindGroupLayouts when creating a
     * [PipelineLayout].
     */
    val maxBindGroups: Int,
    /**
     * The maximum number of bind group and vertex buffer slots used simultaneously, counting any
     * empty slots below the highest index. Validated in createRenderPipeline() and in draw calls.
     */
    val maxBindGroupsPlusVertexBuffers: Int,

    /**
     * The number of binding indices available when creating a [BindGroupLayout].
     *
     * **Note**: This limit is normative, but arbitrary. With the default binding slot limits, it is
     * impossible to use 1000 bindings in one bind group, but this allows
     * [BindGroupLayoutEntry.binding] values up to 999. This limit allows implementations to treat
     * binding space as an array, within reasonable memory space, rather than a sparse map
     * structure.
     */
    val maxBindingsPerBindGroup: Int,
    /**
     * The maximum number of [BindGroupLayoutEntry] entries across a [PipelineLayout] which are
     * uniform buffers with dynamic offsets.
     */
    val maxDynamicUniformBuffersPerPipelineLayout: Int,
    /**
     * The maximum number of [BindGroupLayoutEntry] entries across a [PipelineLayout] which are
     * storage buffers with dynamic offset
     */
    val maxDynamicStorageBuffersPerPipelineLayout: Int,
    /**
     * For each possible [ShaderStage] stage, the maximum number of [BindGroupLayoutEntry] entries
     * across a [PipelineLayout] which are sampled textures.
     */
    val maxSampledTexturesPerShaderStage: Int,
    /**
     * For each possible [ShaderStage] stage, the maximum number of [BindGroupLayoutEntry] entries
     * across a [PipelineLayout] which are samplers.
     */
    val maxSamplersPerShaderStage: Int,
    /**
     * For each possible GPUShaderStage stage, the maximum number of GPUBindGroupLayoutEntry entries
     * across a GPUPipelineLayout which are storage buffers.
     */
    val maxStorageBuffersPerShaderStage: Int,
    /**
     * For each possible GPUShaderStage stage, the maximum number of GPUBindGroupLayoutEntry entries
     * across a GPUPipelineLayout which are storage textures.
     */
    val maxStorageTexturesPerShaderStage: Int,
    /**
     * For each possible GPUShaderStage stage, the maximum number of GPUBindGroupLayoutEntry entries
     * across a GPUPipelineLayout which are uniform buffers.
     */
    val maxUniformBuffersPerShaderStage: Int,
    /**
     * The maximum GPUBufferBinding.size for bindings with a GPUBindGroupLayoutEntry entry for which
     * entry.buffer?.type is "uniform".
     */
    val maxUniformBufferBindingSize: Long,
    /**
     * The maximum GPUBufferBinding.size for bindings with a GPUBindGroupLayoutEntry entry for which
     * entry.buffer?.type is "storage" or "read-only-storage".
     */
    val maxStorageBufferBindingSize: Long,
    /**
     * The required alignment for GPUBufferBinding.offset and the dynamic offsets provided in
     * setBindGroup(), for bindings with a GPUBindGroupLayoutEntry entry for which
     * entry.buffer?.type is "uniform".
     */
    val minUniformBufferOffsetAlignment: Int,
    /**
     * The required alignment for GPUBufferBinding.offset and the dynamic offsets provided in
     * setBindGroup(), for bindings with a GPUBindGroupLayoutEntry entry for which
     * entry.buffer?.type is "storage" or "read-only-storage".
     */
    val minStorageBufferOffsetAlignment: Int,
    /** The maximum number of buffers when creating a GPURenderPipeline. */
    val maxVertexBuffers: Int,
    /** The maximum size of size when creating a GPUBuffer. */
    val maxBufferSize: Long,
    /**
     * The maximum number of attributes in total across buffers when creating a GPURenderPipeline.
     */
    val maxVertexAttributes: Int,
    /** The maximum allowed arrayStride when creating a GPURenderPipeline. */
    val maxVertexBufferArrayStride: Int,
    /**
     * The maximum allowed number of input or output variables for inter-stage communication (like
     * vertex outputs or fragment inputs).
     */
    val maxInterStageShaderVariables: Int,
    /**
     * The maximum allowed number of color attachments in
     * GPURenderPipelineDescriptor.fragment.targets, GPURenderPassDescriptor.colorAttachments, and
     * GPURenderPassLayout.colorFormats.
     */
    val maxColorAttachments: Int,
    /**
     * The maximum number of bytes necessary to hold one sample (pixel or subpixel) of render
     * pipeline output data, across all color attachments.
     */
    val maxColorAttachmentBytesPerSample: Int,
    /**
     * The maximum number of bytes of workgroup storage used for a compute stage GPUShaderModule
     * entry-point.
     */
    val maxComputeWorkgroupStorageSize: Int,
    /**
     * The maximum value of the product of the workgroup_size dimensions for a compute stage
     * GPUShaderModule entry-point.
     */
    val maxComputeInvocationsPerWorkgroup: Int,
    /**
     * The maximum value of the workgroup_size X dimension for a compute stage GPUShaderModule
     * entry-point.
     */
    val maxComputeWorkgroupSizeX: Int,
    /**
     * The maximum value of the workgroup_size Y dimensions for a compute stage GPUShaderModule
     * entry-point.
     */
    val maxComputeWorkgroupSizeY: Int,
    /**
     * The maximum value of the workgroup_size Z dimensions for a compute stage GPUShaderModule
     * entry-point.
     */
    val maxComputeWorkgroupSizeZ: Int,
    /**
     * The maximum value for the arguments of dispatchWorkgroups(workgroupCountX, workgroupCountY,
     * workgroupCountZ).
     */
    val maxComputeWorkgroupsPerDimension: Int,
)

/**
 * A handle to a command queue on a [Device].
 *
 * A `Queue` executes recorded [CommandBuffer] objects and provides convenience methods for writing
 * to buffers and textures. It can be created along with a [Device] by calling
 * [Adapter.requestDevice].
 */
expect class Queue : Releasable {

    /** Submit a series of finished command buffers for execution. */
    fun submit(vararg cmdBuffers: CommandBuffer)

    /**
     * Schedule a data write into [buffer] starting at [offset].
     *
     * This method is intended to have low performance costs. As such, the write is not immediately
     * submitted, and instead enqueued internally to happen at the start of the next [submit] call.
     *
     * This method fails if [data] overruns the size of [buffer] starting at [offset].
     *
     * @param size the number of elements
     */
    fun writeBuffer(
        buffer: GPUBuffer,
        data: ShortBuffer,
        offset: Long = 0,
        dataOffset: Long = 0,
        size: Long = data.capacity.toLong(),
    )

    /**
     * Schedule a data write into [buffer] starting at [offset].
     *
     * This method is intended to have low performance costs. As such, the write is not immediately
     * submitted, and instead enqueued internally to happen at the start of the next [submit] call.
     *
     * This method fails if [data] overruns the size of [buffer] starting at [offset].
     *
     * @param size the number of elements
     */
    fun writeBuffer(
        buffer: GPUBuffer,
        data: FloatBuffer,
        offset: Long = 0,
        dataOffset: Long = 0,
        size: Long = data.capacity.toLong(),
    )

    /**
     * Schedule a data write into [buffer] starting at [offset].
     *
     * This method is intended to have low performance costs. As such, the write is not immediately
     * submitted, and instead enqueued internally to happen at the start of the next [submit] call.
     *
     * This method fails if [data] overruns the size of [buffer] starting at [offset].
     *
     * @param size the number of elements
     */
    fun writeBuffer(
        buffer: GPUBuffer,
        data: IntBuffer,
        offset: Long = 0,
        dataOffset: Long = 0,
        size: Long = data.capacity.toLong(),
    )

    /**
     * Schedule a data write into [buffer] starting at [offset].
     *
     * This method is intended to have low performance costs. As such, the write is not immediately
     * submitted, and instead enqueued internally to happen at the start of the next [submit] call.
     *
     * This method fails if [data] overruns the size of [buffer] starting at [offset].
     *
     * @param size the number of elements
     */
    fun writeBuffer(
        buffer: GPUBuffer,
        data: ByteBuffer,
        offset: Long = 0,
        dataOffset: Long = 0,
        size: Long = data.capacity.toLong(),
    )

    /**
     * Schedule a write of some data into a texture.
     *
     * This method is intended to have low performance costs. As such, the write is not immediately
     * submitted, and instead enqueued internally to happen at the start of the next [submit] call.
     * However, [data] will be immediately copied into staging memory; so the caller may discard it
     * any time after this call completes.
     *
     * This method fails if [size] overruns the size of [destination], or if [data] is too short.
     *
     * @param data contains the texels to be written, which must be in the same format as the
     *   texture.
     * @param destination specifies the texture to write into, and the location within the texture
     *   (coordinate offset, mip level) that will be overwritten.
     * @param layout describes the memory layout of data, which does not necessarily have to have
     *   tightly packed rows.
     * @param size is the size, in texels, of the region to be written.
     */
    fun writeTexture(
        data: ByteBuffer,
        destination: TextureCopyView,
        layout: TextureDataLayout,
        copySize: Extent3D,
        size: Long = data.capacity.toLong(),
    )

    /**
     * Schedule a write of some data into a texture.
     *
     * This method is intended to have low performance costs. As such, the write is not immediately
     * submitted, and instead enqueued internally to happen at the start of the next [submit] call.
     * However, [data] will be immediately copied into staging memory; so the caller may discard it
     * any time after this call completes.
     *
     * This method fails if [size] overruns the size of [destination], or if [data] is too short.
     *
     * @param data contains the texels to be written, which must be in the same format as the
     *   texture.
     * @param destination specifies the texture to write into, and the location within the texture
     *   (coordinate offset, mip level) that will be overwritten.
     * @param layout describes the memory layout of data, which does not necessarily have to have
     *   tightly packed rows.
     * @param size is the size, in texels, of the region to be written.
     */
    fun writeTexture(
        data: ByteArray,
        destination: TextureCopyView,
        layout: TextureDataLayout,
        copySize: Extent3D,
        size: Long = data.size.toLong(),
    )

    override fun release()
}

/**
 * Handle to a compiled shader module.
 *
 * A `ShaderModule` represents a compiled shader module on the GPU. It can be created by pass source
 * code to [Device.createShaderModule]. Shader modules are used to define programmable stages of a
 * pipeline.
 */
expect class ShaderModule : Releasable {
    override fun release()
}

/**
 * Extent of a texture related operation.
 *
 * @param width width of the extent
 * @param height height of the extent
 * @param depth depth of the extent or number of array layers.
 */
data class Extent3D(val width: Int, val height: Int, val depth: Int)

/**
 * Origin of a copy to/from a texture.
 *
 * @param x x-position of the origin
 * @param y y-position of the origin
 * @param z z-position of the origin
 */
data class Origin3D(val x: Int, val y: Int, val z: Int)

/**
 * A handle to a presentable surface.
 *
 * A `Surface` represents a platform-specific surface (e.g. a window) onto which rendered images may
 * be presented.
 */
expect class Surface : Releasable {

    /** Initializes [Surface] for presentation. */
    fun configure(configuration: SurfaceConfiguration)

    /** @return the capabilities of the surface when used with the given [adapter]. */
    fun getCapabilities(adapter: Adapter): SurfaceCapabilities

    /** @return the optimal [TextureFormat] for displaying on the given [Adapter] */
    fun getPreferredFormat(adapter: Adapter): TextureFormat

    /**
     * In order to present the [SurfaceTexture] returned by this function, first a [Queue.submit]
     * needs to be done with some work rendering to this texture. Then [present] needs to be called.
     *
     * @return the next [SurfaceTexture] to be presented by the swapchain for drawing.
     */
    fun getCurrentTexture(): SurfaceTexture

    /**
     * Schedule the underlying [SurfaceTexture] to be presented on this surface.
     *
     * Needs to be called after any work on the texture is scheduled via [Queue.submit]
     */
    fun present()

    /** Release this memory held by this surface. */
    override fun release()
}

/** Defines the capabilities of a given surface and adapter. */
expect class SurfaceCapabilities {
    /**
     * List of supported formats to use with a given adapter. The first format is usually the
     * preferred.
     */
    val formats: List<TextureFormat>

    /** List of supported presentation modes to use with the given adapter. */
    val alphaModes: List<AlphaMode>
}

/**
 * Describes a [Surface]. For use with [Surface.configure].
 *
 * @param device the device of the surface
 * @param usage the usage of the swap chain. The only supported usage is
 *   [TextureUsage.RENDER_ATTACHMENT].
 * @param format the texture format of the swap chain. The only formats that are guaranteed are
 *   [TextureFormat.BGRA8_UNORM] and [TextureFormat.BGRA8_UNORM_SRGB].
 * @param presentMode Presentation mode of the swap chain. Fifo is the only mode guaranteed to be
 *   supported. FifoRelaxed, Imeediate, and Mailbox will crash if unsupported, while AutoVsync and
 *   AutoNoVsync will gracefully do a designed sets of fallbacks if their primary modes are
 *   unsupported.
 * @param alphaMode specifies how the alpha channel of textures should be handled during
 *   compositing.
 * @param width width of the swap chain
 * @param height height of the swap chain
 */
data class SurfaceConfiguration(
    val device: Device,
    val usage: TextureUsage,
    val format: TextureFormat,
    val presentMode: PresentMode,
    val alphaMode: AlphaMode,
    val width: Int,
    val height: Int,
)

/**
 * Describes a [TextureView].
 *
 * @param format the format of the texture view. Either must be the same as the texture format or in
 *   the list of `viewFormats` in the texture's descriptor.
 * @param dimension the dimension of the texture view. For 1D textures, this must be
 *   [TextureViewDimension.D1]. For 2D textures, it must be one of [TextureViewDimension.D2],
 *   [TextureViewDimension.D2_ARRAY], [TextureViewDimension.CUBE], and
 *   [TextureViewDimension.CUBE_ARRAY]. For 3D textures it must be [TextureViewDimension.D3].
 * @param aspect the aspect of the texture. Color textures must be [TextureAspect.ALL].
 * @param baseMipLevel base mip level.
 * @param mipLevelCount mip level count. `baseMipLevel + mipLevelCount` must b e less or equal to
 *   underlying texture mip count. If `0` considered to include the rest of the mipmap levels, but
 *   at least 1 in total.
 * @param baseArrayLayer base array layer.
 * @param arrayLayerCount Layer count. `baseArrayLayer + arrayLayerCount` must be less or equal to
 *   the underlying array count. If `0`, considered to include the rest of the array layers, but at
 *   least 1 in total.
 * @param label debug label of a texture view.
 */
data class TextureViewDescriptor(
    val format: TextureFormat,
    val dimension: TextureViewDimension,
    val aspect: TextureAspect = TextureAspect.ALL,
    val baseMipLevel: Int = 0,
    val mipLevelCount: Int = 1,
    val baseArrayLayer: Int = 0,
    val arrayLayerCount: Int = 1,
    val label: String? = null,
)

/**
 * Describes a [WebGPUTexture].
 *
 * @param size of the texture. All components must be greater than zero. For a regular 1D/2D
 *   texture, the unused sizes will be 1. For 2DArray textures, Z is the number of 2D textures in
 *   that array.
 * @param mipLevelCount mip count of the texture. For a texture with no extra mips, this must be 1.
 * @param sampleCount sample count ofa texture. If this is not 1, texture must have
 *   `TextureBindingLayout.multisampled` set to true.
 * @param dimension dimensions of the texture.
 * @param format format of the texture.
 * @param usage allowed usages of the texture. If used in other ways, the operation will fail.
 * @param label debug label of a texture.
 */
data class TextureDescriptor(
    val size: Extent3D,
    val mipLevelCount: Int,
    val sampleCount: Int,
    val dimension: TextureDimension,
    val format: TextureFormat,
    val usage: TextureUsage,
    val label: String? = null,
)

/** A texture that can be rendered to. Result of a successful call to [Surface.getCurrentTexture] */
expect class SurfaceTexture {
    /** The accessible view of the frame. */
    val texture: WebGPUTexture?

    /** The current status of the texture */
    val status: TextureStatus
}

/** Handle to a texture on the GPU. It can be created with [Device.createTexture]. */
expect class WebGPUTexture : Releasable {
    /** Creates a view of this texture. */
    fun createView(desc: TextureViewDescriptor? = null): TextureView

    override fun release()
}

/**
 * A handle to a texture view.
 *
 * A `TextureView` object describes a texture and associated metadata needed by a [RenderPipeline]
 * or [BindGroup].
 */
expect class TextureView : IntoBindingResource {
    fun release()
}

/**
 * Describes a [GPUBuffer]. For use with [Device.createBuffer].
 *
 * @param label debug label of a buffer.
 * @param size size of a buffer, in bytes.
 * @param usage usages of a buffer. If the buffer is used in any way that isn't specified here, the
 *   operation will fail.
 * @param mappedAtCreation allows a buffer to be mapped immediately after they are made. It does not
 *   have to [BufferUsage.MAP_READ] or [BufferUsage.MAP_WRITE], all buffers are allowed to be mapped
 *   at creation. If this is `true`, [size] must be a multiple of `4`.
 */
data class BufferDescriptor(
    val label: String,
    val size: Long,
    val usage: BufferUsage,
    val mappedAtCreation: Boolean,
)

/**
 * Handle to a GPU-accessible buffer.
 *
 * Create with [Device.createBuffer].
 *
 * @see [Device.createGPUFloatBuffer]
 * @see [Device.createGPUIntBuffer]
 * @see [Device.createGPUShortBuffer]
 * @see [Device.createGPUByteBuffer]
 */
expect class GPUBuffer : Releasable {

    /** The length of the buffer allocation in bytes. */
    val size: Long

    /**
     * Use only a portion of this buffer for a given operation.
     *
     * @return the underlying [ByteBuffer]
     */
    // see the empty param getMappedRange function for a reason why we aren't using default values
    // here.
    fun getMappedRange(offset: Long, size: Long): ByteBuffer

    /**
     * Use the entire buffer as the portion for a given operation.
     *
     * @return the underlying [ByteBuffer]
     */
    // we are using an empty parameter function here instead of a extension
    // because of a JS IR compiler error throwing an IrLinkageError when referencing size as a
    // default param
    fun getMappedRange(): ByteBuffer

    /** Flushes any pending write operations and unmaps the buffer from host memory. */
    fun unmap()

    override fun release()

}

/**
 * Describes a [Sampler]. For use with [Device.createSampler].
 *
 * @param compare if enabled, this is a comparison sampler using the given comparison function.
 * @param addressModeU how to detail with out of bounds accesses in the u (i.e. x) direction
 * @param addressModeV how to detail with out of bounds accesses in the v (i.e. y) direction
 * @param addressModeW how to detail with out of bounds accesses in the w (i.e. z) direction
 * @param magFilter how to filter the texture when it needs to be magnified (made larger)
 * @param minFilter how to filter the texture when it needs to be minified (made smaller)
 * @param mipmapFilter how to filter between mip map levels
 * @param lodMinClamp minimum level of detail (i.e. mip level) to use
 * @param lodMaxClamp maximum level of detail (i.e. mip level) to use
 * @param maxAnisotropy must be at least 1. if this is not 1, all filter must e linear.
 * @param label debug label of a sampler.
 */
data class SamplerDescriptor(
    val compare: CompareFunction? = null,
    val addressModeU: AddressMode = AddressMode.CLAMP_TO_EDGE,
    val addressModeV: AddressMode = AddressMode.CLAMP_TO_EDGE,
    val addressModeW: AddressMode = AddressMode.CLAMP_TO_EDGE,
    val magFilter: FilterMode = FilterMode.NEAREST,
    val minFilter: FilterMode = FilterMode.NEAREST,
    val mipmapFilter: FilterMode = FilterMode.NEAREST,
    val lodMinClamp: Float = 0f,
    val lodMaxClamp: Float = 100000000f,
    val maxAnisotropy: Short = 1,
    val label: String? = null,
)

/**
 * A handle to a sampler.
 *
 * A `Sampler` object defines how a pipeline will sample from a [TextureView]. Samplers define image
 * filters (including anisotropy) and address (wrapping) modes, among other things. See
 * [SamplerDescriptor].
 *
 * It can be created with [Device.createSampler].
 */
expect class Sampler : IntoBindingResource, Releasable {
    override fun release()
}

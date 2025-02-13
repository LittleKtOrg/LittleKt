package com.littlekt.graphics.webgpu

import com.littlekt.EngineStats
import com.littlekt.Releasable
import ffi.ArrayHolder
import ffi.MemoryAllocator
import ffi.memoryScope
import io.ygdrasil.wgpu.WGPUCommandBuffer
import io.ygdrasil.wgpu.WGPUCommandEncoder
import io.ygdrasil.wgpu.WGPUComputePassDescriptor
import io.ygdrasil.wgpu.WGPUComputePassEncoder
import io.ygdrasil.wgpu.WGPUComputePipeline
import io.ygdrasil.wgpu.WGPUComputePipelineDescriptor
import io.ygdrasil.wgpu.WGPUOrigin3D
import io.ygdrasil.wgpu.WGPURenderPassEncoder
import io.ygdrasil.wgpu.WGPURenderPipeline
import io.ygdrasil.wgpu.WGPUTexelCopyBufferLayout
import io.ygdrasil.wgpu.WGPUTexelCopyTextureInfo
import io.ygdrasil.wgpu.wgpuCommandBufferRelease
import io.ygdrasil.wgpu.wgpuCommandEncoderCopyBufferToTexture
import io.ygdrasil.wgpu.wgpuCommandEncoderFinish
import io.ygdrasil.wgpu.wgpuCommandEncoderRelease
import io.ygdrasil.wgpu.wgpuComputePassEncoderDispatchWorkgroups
import io.ygdrasil.wgpu.wgpuComputePassEncoderEnd
import io.ygdrasil.wgpu.wgpuComputePassEncoderRelease
import io.ygdrasil.wgpu.wgpuComputePassEncoderSetBindGroup
import io.ygdrasil.wgpu.wgpuComputePassEncoderSetPipeline
import io.ygdrasil.wgpu.wgpuComputePipelineRelease
import io.ygdrasil.wgpu.wgpuRenderPassEncoderDraw
import io.ygdrasil.wgpu.wgpuRenderPassEncoderDrawIndexed
import io.ygdrasil.wgpu.wgpuRenderPassEncoderEnd
import io.ygdrasil.wgpu.wgpuRenderPassEncoderRelease
import io.ygdrasil.wgpu.wgpuRenderPassEncoderSetBindGroup
import io.ygdrasil.wgpu.wgpuRenderPassEncoderSetIndexBuffer
import io.ygdrasil.wgpu.wgpuRenderPassEncoderSetPipeline
import io.ygdrasil.wgpu.wgpuRenderPassEncoderSetScissorRect
import io.ygdrasil.wgpu.wgpuRenderPassEncoderSetVertexBuffer
import io.ygdrasil.wgpu.wgpuRenderPassEncoderSetViewport
import io.ygdrasil.wgpu.wgpuRenderPipelineRelease
import io.ygdrasil.wgpu.WGPUTexelCopyBufferInfo
import io.ygdrasil.wgpu.wgpuCommandEncoderBeginRenderPass
import io.ygdrasil.wgpu.wgpuCommandEncoderCopyBufferToBuffer
import io.ygdrasil.wgpu.wgpuCommandEncoderBeginComputePass
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator
import java.lang.foreign.ValueLayout

actual class RenderPipeline(val segment: WGPURenderPipeline) : Releasable {

    actual override fun release() {
        wgpuRenderPipelineRelease(segment)
    }

    override fun toString(): String {
        return "RenderPipeline"
    }
}

actual class CommandBuffer(val segment: WGPUCommandBuffer) : Releasable {
    actual override fun release() {
        wgpuCommandBufferRelease(segment)
    }
}

fun TextureCopyView.toNative(scope: MemoryAllocator): WGPUTexelCopyTextureInfo {
    return scope.map(this)
}

fun TextureDataLayout.toNative(scope: MemoryAllocator): WGPUTexelCopyBufferLayout {
    return WGPUTexelCopyBufferLayout.allocate(scope).also { dataLayout ->
        dataLayout.offset = offset.toULong()
        dataLayout.bytesPerRow = bytesPerRow.toUInt()
        dataLayout.rowsPerImage = rowsPerImage.toUInt()
    }
}

fun BufferCopyView.toNative(scope: MemoryAllocator): WGPUTexelCopyBufferInfo {
    return scope.map(this)
}

internal fun MemoryAllocator.map(input: BufferCopyView) = WGPUTexelCopyBufferInfo.allocate(this).also { output ->
    output.buffer = input.buffer.segment
    map(input.layout, output.layout)
}

private fun map(input: TextureDataLayout, output: WGPUTexelCopyBufferLayout) {
    output.offset = input.offset.toULong()
    output.bytesPerRow = input.bytesPerRow.toUInt()
    output.rowsPerImage = input.rowsPerImage.toUInt()
}



actual class CommandEncoder(val segment: WGPUCommandEncoder) : Releasable {

    actual fun beginRenderPass(desc: RenderPassDescriptor): RenderPassEncoder {
        return memoryScope { scope ->
            RenderPassEncoder(
                wgpuCommandEncoderBeginRenderPass(segment, scope.map(desc)) ?: error("Failed to begin render pass."),
                desc.label,
            )
        }
    }

    actual fun finish(): CommandBuffer {
        return CommandBuffer(wgpuCommandEncoderFinish(segment, null) ?: error("Failed to finish command encoder."))
    }

    actual fun copyBufferToTexture(
        source: BufferCopyView,
        destination: TextureCopyView,
        copySize: Extent3D,
    ) {
        memoryScope { scope ->
            wgpuCommandEncoderCopyBufferToTexture(
                segment,
                source.toNative(scope),
                destination.toNative(scope),
                copySize.toNative(scope),
            )
        }
    }

    actual fun beginComputePass(label: String?): ComputePassEncoder {
        return memoryScope { scope ->
            val desc = WGPUComputePassDescriptor.allocate(scope)
            if(label != null) scope.map(label, desc.label)
            ComputePassEncoder(wgpuCommandEncoderBeginComputePass(segment, desc) ?: error("Failed to begin compute pass."))
        }
    }

    actual fun copyBufferToBuffer(
        source: GPUBuffer,
        destination: GPUBuffer,
        sourceOffset: Int,
        destinationOffset: Int,
        size: Long,
    ) {
        wgpuCommandEncoderCopyBufferToBuffer(
            segment,
            source.segment,
            sourceOffset.toULong(),
            destination.segment,
            destinationOffset.toULong(),
            size.toULong(),
        )
    }

    actual fun copyTextureToBuffer(source: TextureCopyView, dest: BufferCopyView, size: Extent3D) {
        TODO("IMPL")
    }

    actual override fun release() {
        wgpuCommandEncoderRelease(segment)
    }
}

actual class RenderPassEncoder(val segment: WGPURenderPassEncoder, actual val label: String? = null) :
    Releasable {

    actual fun setPipeline(pipeline: RenderPipeline) {
        wgpuRenderPassEncoderSetPipeline(segment, pipeline.segment)
        EngineStats.setPipelineCalls += 1
    }

    actual fun draw(vertexCount: Int, instanceCount: Int, firstVertex: Int, firstInstance: Int) {
        EngineStats.drawCalls += 1
        EngineStats.triangles += (vertexCount / 3) * instanceCount
        wgpuRenderPassEncoderDraw(segment, vertexCount.toUInt(), instanceCount.toUInt(), firstVertex.toUInt(), firstInstance.toUInt())
    }

    actual fun end() {
        wgpuRenderPassEncoderEnd(segment)
    }

    actual fun setVertexBuffer(slot: Int, buffer: GPUBuffer, offset: Long, size: Long) {
        wgpuRenderPassEncoderSetVertexBuffer(segment, slot.toUInt(), buffer.segment, offset.toULong(), size.toULong())
        EngineStats.setBufferCalls += 1
    }

    actual fun drawIndexed(
        indexCount: Int,
        instanceCount: Int,
        firstIndex: Int,
        baseVertex: Int,
        firstInstance: Int,
    ) {
        EngineStats.drawCalls += 1
        EngineStats.triangles += (indexCount / 3) * instanceCount
        wgpuRenderPassEncoderDrawIndexed(
            segment,
            indexCount.toUInt(),
            instanceCount.toUInt(),
            firstIndex.toUInt(),
            baseVertex,
            firstInstance.toUInt(),
        )
    }

    actual fun setIndexBuffer(
        buffer: GPUBuffer,
        indexFormat: IndexFormat,
        offset: Long,
        size: Long,
    ) {
        wgpuRenderPassEncoderSetIndexBuffer(
            segment,
            buffer.segment,
            indexFormat.nativeVal,
            offset.toULong(),
            size.toULong(),
        )
        EngineStats.setBufferCalls += 1
    }

    actual fun setBindGroup(index: Int, bindGroup: BindGroup, dynamicOffsets: List<Long>) {
        if (dynamicOffsets.isNotEmpty()) {
            memoryScope { scope ->
                val dynamicOffsets = dynamicOffsets.map { it.toInt()}
                    .toIntArray()
                val offsets = scope.allocateBuffer((dynamicOffsets.size * Long.SIZE_BYTES).toULong())
                    .also { buffer -> buffer.writeInts(dynamicOffsets) }
                    .let { ArrayHolder<UInt>(it.handler) }
                wgpuRenderPassEncoderSetBindGroup(
                    segment,
                    index.toUInt(),
                    bindGroup.segment,
                    dynamicOffsets.size.toULong(),
                    offsets,
                )
            }
        } else {
            wgpuRenderPassEncoderSetBindGroup(segment, index.toUInt(), bindGroup.segment, 0u, null)
        }
        EngineStats.setBindGroupCalls += 1
    }

    actual override fun release() {
        wgpuRenderPassEncoderRelease(segment)
    }

    actual fun setViewport(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        minDepth: Float,
        maxDepth: Float,
    ) {
        wgpuRenderPassEncoderSetViewport(
            segment,
            x.toFloat(),
            y.toFloat(),
            width.toFloat(),
            height.toFloat(),
            minDepth,
            maxDepth,
        )
    }

    actual fun setScissorRect(x: Int, y: Int, width: Int, height: Int) {
        wgpuRenderPassEncoderSetScissorRect(segment, x.toUInt(), y.toUInt(), width.toUInt(), height.toUInt())
    }

    override fun toString(): String {
        return "RenderPassEncoder(label=$label)"
    }
}

actual class ComputePipeline(val segment: WGPUComputePipeline) : Releasable {
    actual override fun release() {
        wgpuComputePipelineRelease(segment)
    }

    override fun toString(): String {
        return "ComputePipeline"
    }
}

actual class ComputePassEncoder(val segment: WGPUComputePassEncoder) : Releasable {
    actual fun setPipeline(pipeline: ComputePipeline) {
        wgpuComputePassEncoderSetPipeline(segment, pipeline.segment)
        EngineStats.setPipelineCalls += 1
    }

    actual fun setBindGroup(index: Int, bindGroup: BindGroup) {
        wgpuComputePassEncoderSetBindGroup(segment, index.toUInt(), bindGroup.segment, 0u, null)
        EngineStats.setBindGroupCalls += 1
    }

    actual fun dispatchWorkgroups(
        workgroupCountX: Int,
        workgroupCountY: Int,
        workgroupCountZ: Int,
    ) {
        wgpuComputePassEncoderDispatchWorkgroups(
            segment,
            workgroupCountX.toUInt(),
            workgroupCountY.toUInt(),
            workgroupCountZ.toUInt(),
        )
    }

    actual fun end() {
        wgpuComputePassEncoderEnd(segment)
    }

    actual override fun release() {
        wgpuComputePassEncoderRelease(segment)
    }
}

internal fun MemoryAllocator.map(input: TextureCopyView) =
    WGPUTexelCopyTextureInfo.allocate(this).also { output ->
        output.texture = input.texture.segment
        output.mipLevel = input.mipLevel.toUInt()
        map(input.origin, output.origin)
    }


internal fun map(input: Origin3D, output: WGPUOrigin3D) {
    output.x = input.x.toUInt()
    output.y = input.y.toUInt()
    output.z = input.z.toUInt()
}
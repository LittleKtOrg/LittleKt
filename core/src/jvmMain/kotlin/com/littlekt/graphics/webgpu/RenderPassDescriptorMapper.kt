package com.littlekt.graphics.webgpu

import com.littlekt.graphics.Color
import ffi.MemoryAllocator
import io.ygdrasil.wgpu.WGPUColor
import io.ygdrasil.wgpu.WGPURenderPassColorAttachment
import io.ygdrasil.wgpu.WGPURenderPassDepthStencilAttachment
import io.ygdrasil.wgpu.WGPURenderPassDescriptor

internal fun MemoryAllocator.map(input: RenderPassDescriptor): WGPURenderPassDescriptor =
    WGPURenderPassDescriptor.allocate(this).also { output ->
        if (input.label != null) map(input.label, output.label)

        if (input.colorAttachments.isNotEmpty()) {
            output.colorAttachmentCount = input.colorAttachments.size.toULong()
            output.colorAttachments = WGPURenderPassColorAttachment.allocateArray(
                this,
                output.colorAttachmentCount.toUInt()
            ) { index, value ->
                map(input.colorAttachments[index.toInt()], value)
            }
        }

        if (input.depthStencilAttachment != null) output.depthStencilAttachment = map(input.depthStencilAttachment)
        //TODO map this var occlusionQuerySet: GPUQuerySet?
        //TODO map this var timestampWrites: GPURenderPassTimestampWrites?
        //TODO map this var maxDrawCount: GPUSize64
        // check WGPURenderPassDescriptorMaxDrawCount
    }

internal fun map(input: RenderPassColorAttachmentDescriptor, output: WGPURenderPassColorAttachment) {
    output.view = input.view.segment
    output.loadOp = input.loadOp.nativeVal
    output.storeOp = input.storeOp.nativeVal
    // TODO find how to map this
    //if (input.depthSlice != null) WGPURenderPassColorAttachment.depthSlice = input.depthSlice)
    if (input.resolveTarget != null) output.resolveTarget = input.resolveTarget.segment
    if (input.clearColor != null) map(input.clearColor, output.clearValue)
}

internal fun MemoryAllocator.map(input: RenderPassDepthStencilAttachmentDescriptor): WGPURenderPassDepthStencilAttachment =
    WGPURenderPassDepthStencilAttachment.allocate(this).also { output ->
        output.view = input.view.segment
        output.depthClearValue = input.depthClearValue

        if (input.depthLoadOp != null) output.depthLoadOp = input.depthLoadOp.nativeVal
        if (input.depthStoreOp != null) output.depthStoreOp = input.depthStoreOp.nativeVal
        output.depthReadOnly = input.depthReadOnly
        output.stencilClearValue = input.stencilClearValue.toUInt()
        if (input.stencilLoadOp != null) output.stencilLoadOp = input.stencilLoadOp.nativeVal
        if (input.stencilStoreOp != null) output.stencilStoreOp = input.stencilStoreOp.nativeVal
        output.stencilReadOnly = input.stencilReadOnly
    }

internal fun map(input: Color, output: WGPUColor) {
    output.r = input.r.toDouble()
    output.g = input.g.toDouble()
    output.b = input.b.toDouble()
    output.a = input.a.toDouble()
}
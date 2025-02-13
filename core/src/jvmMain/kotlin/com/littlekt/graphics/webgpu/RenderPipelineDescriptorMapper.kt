package com.littlekt.graphics.webgpu

import ffi.MemoryAllocator
import io.ygdrasil.wgpu.WGPUBlendComponent
import io.ygdrasil.wgpu.WGPUBlendState
import io.ygdrasil.wgpu.WGPUColorTargetState
import io.ygdrasil.wgpu.WGPUDepthStencilState
import io.ygdrasil.wgpu.WGPUFragmentState
import io.ygdrasil.wgpu.WGPUMultisampleState
import io.ygdrasil.wgpu.WGPUPrimitiveState
import io.ygdrasil.wgpu.WGPURenderPipelineDescriptor
import io.ygdrasil.wgpu.WGPUStencilFaceState
import io.ygdrasil.wgpu.WGPUStringView
import io.ygdrasil.wgpu.WGPUVertexAttribute
import io.ygdrasil.wgpu.WGPUVertexBufferLayout
import io.ygdrasil.wgpu.WGPUVertexState
import io.ygdrasil.wgpu.toUInt

internal fun MemoryAllocator.map(input: RenderPipelineDescriptor) =
    WGPURenderPipelineDescriptor.allocate(this).also { output ->
        map(input.vertex, output.vertex)
        if (input.label != null) map(input.label, output.label)
        output.layout = input.layout.segment
        map(input.primitive, output.primitive)
        if (input.depthStencil != null) output.depthStencil = map(input.depthStencil)
        if (input.fragment != null) output.fragment = map(input.fragment)
        map(input.multisample, output.multisample)
    }

fun MemoryAllocator.map(input: ColorTargetState, output: WGPUColorTargetState) {
    output.format = input.format.nativeVal
    output.writeMask = input.writeMask.usageFlag.toULong()
    if (input.blendState != null) output.blend = map(input.blendState)
}

fun MemoryAllocator.map(input: BlendState): WGPUBlendState =
    WGPUBlendState
        .allocate(this).also { output ->
            map(input.color, output.color)
            map(input.alpha, output.alpha)

        }

fun map(
    input: BlendComponent,
    output: WGPUBlendComponent
) {
    output.operation = input.operation.nativeVal
    output.srcFactor = input.srcFactor.nativeVal
    output.dstFactor = input.dstFactor.nativeVal
}

private fun MemoryAllocator.map(input: FragmentState): WGPUFragmentState =
    WGPUFragmentState.allocate(this)
        .also { fragmentState ->
            fragmentState.module = input.module.segment
            map(input.entryPoint, fragmentState.entryPoint)
            if (input.targets.isNotEmpty()) {
                fragmentState.targetCount = input.targets.size.toULong()
                val colorTargets =
                    WGPUColorTargetState.allocateArray(this, input.targets.size.toUInt(), { index, value ->
                        val colorTargetState = input.targets[index.toInt()]
                        map(colorTargetState, value)

                    })
                fragmentState.targets = colorTargets
            }
        }

private fun MemoryAllocator.map(input: DepthStencilState): WGPUDepthStencilState =
    WGPUDepthStencilState.allocate(this)
        .also { output ->
            output.format = input.format.nativeVal
            output.depthWriteEnabled = input.depthWriteEnabled.toUInt()
            output.depthCompare = input.depthCompare.nativeVal
            map(input.stencil.front, output.stencilFront)
            map(input.stencil.back, output.stencilBack)
            output.stencilReadMask = input.stencil.readMask.toUInt()
            output.stencilWriteMask = input.stencil.writeMask.toUInt()
            output.depthBias = input.bias.constant
            output.depthBiasSlopeScale = input.bias.slopeScale
            output.depthBiasClamp = input.bias.clamp
        }

fun map(input: StencilFaceState, output: WGPUStencilFaceState) {
    output.compare = input.compare.nativeVal
    output.failOp = input.failOp.nativeVal
    output.depthFailOp = input.depthFailOp.nativeVal
    output.passOp = input.passOp.nativeVal
}

private fun map(input: MultisampleState, output: WGPUMultisampleState) {
    output.count = input.count.toUInt()
    output.mask = input.mask.toUInt()
    output.alphaToCoverageEnabled = input.alphaToCoverageEnabled
}

private fun map(input: PrimitiveState, output: WGPUPrimitiveState) {
    output.topology = input.topology.nativeVal
    if (input.stripIndexFormat != null) output.stripIndexFormat = input.stripIndexFormat.nativeVal
    output.frontFace = input.frontFace.nativeVal
    output.cullMode = input.cullMode.nativeVal
    //TODO check how to map unclippedDepth https://docs.rs/wgpu/latest/wgpu/struct.PrimitiveState.html
}

private fun MemoryAllocator.map(input: VertexState, output: WGPUVertexState) {
    output.module = input.module.segment
    map(input.entryPoint, output.entryPoint)
    // TODO learn how to map this
    output.constantCount = 0uL
    if (input.buffers.isNotEmpty()) {
        output.buffers = WGPUVertexBufferLayout.allocateArray(this, input.buffers.size.toUInt(), { index, value ->
            val vertexBufferLayout = input.buffers[index.toInt()]
            map(vertexBufferLayout, value)

        })
        output.bufferCount = input.buffers.size.toULong()
    }
}

private fun map(
    input: WebGPUVertexAttribute,
    output: WGPUVertexAttribute
) {
    output.format = input.format.nativeVal
    output.offset = input.offset.toULong()
    output.shaderLocation = input.shaderLocation.toUInt()
}

private fun MemoryAllocator.map(
    input: WebGPUVertexBufferLayout,
    output: WGPUVertexBufferLayout
) {
    output.arrayStride = input.arrayStride.toULong()
    if (input.attributes.isNotEmpty()) {
        output.attributes = WGPUVertexAttribute.allocateArray(this, input.attributes.size.toUInt(), { index, value ->
            val vertexAttribute = input.attributes[index.toInt()]
            map(vertexAttribute, value)
        })
        output.attributeCount = input.attributes.size.toULong()
    }
    output.stepMode = input.stepMode.nativeVal
}

internal fun MemoryAllocator.map(input: String, output: WGPUStringView) {
    output.data = allocateFrom(input)
    output.length = input.length.toULong()
}
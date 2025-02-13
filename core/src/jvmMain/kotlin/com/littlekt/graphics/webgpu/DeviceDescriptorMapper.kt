@file:OptIn(ExperimentalUnsignedTypes::class)

package com.littlekt.graphics.webgpu

import ffi.ArrayHolder
import ffi.MemoryAllocator
import io.ygdrasil.wgpu.WGPUDeviceDescriptor
import io.ygdrasil.wgpu.WGPULimits

internal fun MemoryAllocator.map(input: DeviceDescriptor?): WGPUDeviceDescriptor? = when (input) {
    null -> null
    else -> WGPUDeviceDescriptor.allocate(this).also { output ->
        if (input.label != null) map(input.label, output.label)
        output.requiredLimits = map(input.requiredLimits)

        output.requiredFeatureCount = input.requiredFeatures.size.toULong()
        if (input.requiredFeatures.isNotEmpty()) {
            output.requiredFeatures = allocateBuffer((Int.SIZE_BYTES * input.requiredFeatures.size).toULong()).also { buffer ->
                buffer.writeUInts(input.requiredFeatures.map { it.nativeVal }.toUIntArray())
            }.handler.let(::ArrayHolder)
        }

    }
}

private fun MemoryAllocator.map(input: RequiredLimits?): WGPULimits? = when (input) {
    null -> null
    else -> WGPULimits.allocate(this).also { output ->
        input.maxTextureDimension1D?.let {
            output.maxTextureDimension1D = it.toUInt()
        }
        input.maxTextureDimension2D?.let {
            output.maxTextureDimension2D = it.toUInt()
        }
        input.maxTextureDimension3D?.let {
            output.maxTextureDimension3D = it.toUInt()
        }
        input.maxTextureArrayLayers?.let {
            output.maxTextureArrayLayers = it.toUInt()
        }
        input.maxBindGroups?.let { output.maxBindGroups = it.toUInt() }
        input.maxBindGroupsPlusVertexBuffers?.let {
            output.maxBindGroupsPlusVertexBuffers = it.toUInt()
        }
        input.maxBindingsPerBindGroup?.let {
            output.maxBindingsPerBindGroup = it.toUInt()
        }
        input.maxDynamicUniformBuffersPerPipelineLayout?.let {
            output.maxDynamicUniformBuffersPerPipelineLayout = it.toUInt()
        }
        input.maxDynamicStorageBuffersPerPipelineLayout?.let {
            output.maxDynamicStorageBuffersPerPipelineLayout = it.toUInt()
        }
        input.maxSampledTexturesPerShaderStage?.let {
            output.maxSampledTexturesPerShaderStage = it.toUInt()
        }
        input.maxSamplersPerShaderStage?.let {
            output.maxSamplersPerShaderStage = it.toUInt()
        }
        input.maxStorageBuffersPerShaderStage?.let {
            output.maxStorageBuffersPerShaderStage = it.toUInt()
        }
        input.maxStorageTexturesPerShaderStage?.let {
            output.maxStorageTexturesPerShaderStage = it.toUInt()
        }
        input.maxUniformBuffersPerShaderStage?.let {
            output.maxUniformBuffersPerShaderStage = it.toUInt()
        }
        input.maxUniformBufferBindingSize?.let {
            output.maxUniformBufferBindingSize = it.toULong()
        }
        input.maxStorageBufferBindingSize?.let {
            output.maxStorageBufferBindingSize = it.toULong()
        }
        input.minUniformBufferOffsetAlignment?.let {
            output.minUniformBufferOffsetAlignment = it.toUInt()
        }
        input.minStorageBufferOffsetAlignment?.let {
            output.minStorageBufferOffsetAlignment = it.toUInt()
        }
        input.maxVertexBuffers?.let {
            output.maxVertexBuffers = it.toUInt()
        }
        input.maxBufferSize?.let { output.maxBufferSize = it.toULong() }
        input.maxVertexAttributes?.let {
            output.maxVertexAttributes = it.toUInt()
        }
        input.maxVertexBufferArrayStride?.let {
            output.maxVertexBufferArrayStride = it.toUInt()
        }

    }
}


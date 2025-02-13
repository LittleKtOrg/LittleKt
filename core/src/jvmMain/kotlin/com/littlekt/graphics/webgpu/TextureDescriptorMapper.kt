package com.littlekt.graphics.webgpu

import ffi.ArrayHolder
import ffi.MemoryAllocator
import io.ygdrasil.wgpu.WGPUExtent3D
import io.ygdrasil.wgpu.WGPUTextureDescriptor

internal fun MemoryAllocator.map(input: TextureDescriptor) = WGPUTextureDescriptor.allocate(this).also { output ->
    if (input.label != null) map(input.label, output.label)
    map(input.size, output.size)
    output.format = input.format.nativeVal
    output.usage = input.usage.usageFlag.toULong()
    output.mipLevelCount = input.mipLevelCount.toUInt()
    output.sampleCount = input.sampleCount.toUInt()
    output.dimension = input.dimension.nativeVal
}

internal fun map(input: Extent3D, output: WGPUExtent3D) {
    output.width = input.width.toUInt()
    output.height = input.height.toUInt()
    output.depthOrArrayLayers = input.depth.toUInt()
}


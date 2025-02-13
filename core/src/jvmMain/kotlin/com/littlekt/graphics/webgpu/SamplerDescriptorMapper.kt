package com.littlekt.graphics.webgpu

import ffi.MemoryAllocator
import io.ygdrasil.wgpu.WGPUSamplerDescriptor

internal fun MemoryAllocator.map(input: SamplerDescriptor): WGPUSamplerDescriptor =
    WGPUSamplerDescriptor.allocate(this).also { output ->
        if (input.label != null) map(input.label, output.label)

        output.addressModeU = input.addressModeU.nativeVal
        output.addressModeV = input.addressModeV.nativeVal
        output.addressModeW = input.addressModeW.nativeVal

        output.magFilter = input.magFilter.nativeVal
        output.minFilter = input.minFilter.nativeVal
        output.mipmapFilter = input.mipmapFilter.nativeVal

        output.lodMinClamp = input.lodMinClamp
        output.lodMaxClamp = input.lodMaxClamp

        if (input.compare != null) output.compare = input.compare.nativeVal
        output.maxAnisotropy = input.maxAnisotropy.toUShort()

    }

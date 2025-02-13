package com.littlekt.graphics.webgpu

import ffi.MemoryAllocator
import io.ygdrasil.wgpu.WGPUBindGroupLayoutDescriptor
import io.ygdrasil.wgpu.WGPUBindGroupLayoutEntry
import io.ygdrasil.wgpu.WGPUChainedStruct
import io.ygdrasil.wgpu.WGPUNativeSType_BindGroupEntryExtras

fun MemoryAllocator.map(input: BindGroupLayoutDescriptor): WGPUBindGroupLayoutDescriptor =
    WGPUBindGroupLayoutDescriptor.allocate(this).also { output ->
        if (input.label != null) map(input.label, output.label)

        if (input.entries.isNotEmpty()) {
            output.entryCount = input.entries.size.toULong()
            val entries = WGPUBindGroupLayoutEntry.allocateArray(this, input.entries.size.toUInt()) { index, entry ->
                map(input.entries[index.toInt()], entry)

            }
            output.entries = entries
        }
    }

fun MemoryAllocator.map(input: BindGroupLayoutEntry, output: WGPUBindGroupLayoutEntry) {

    output.binding = input.binding.toUInt()
    output.visibility = input.visibility.usageFlag.toULong()

    when (val bindingType = input.bindingLayout) {
        is BufferBindingLayout -> {
            val buffer = output.buffer
            buffer.hasDynamicOffset = bindingType.hasDynamicOffset
            buffer.minBindingSize = bindingType.minBindingSize.toULong()
            buffer.type = bindingType.type.nativeVal

            val chain = WGPUChainedStruct.allocate(this)
            chain.sType = WGPUNativeSType_BindGroupEntryExtras
            buffer.nextInChain = chain.handler
        }

        is SamplerBindingLayout -> {
            val sampler = output.sampler
            sampler.type = bindingType.type.nativeVal

            val chain = WGPUChainedStruct.allocate(this)
            chain.sType = WGPUNativeSType_BindGroupEntryExtras
            sampler.nextInChain = chain.handler
        }

        is TextureBindingLayout -> {
            val texture = output.texture
            texture.multisampled = bindingType.multisampled
            texture.sampleType = bindingType.sampleType.nativeVal
            texture.viewDimension = bindingType.viewDimension.nativeVal

            val chain = WGPUChainedStruct.allocate(this)
            chain.sType = WGPUNativeSType_BindGroupEntryExtras
            texture.nextInChain = chain.handler
        }
    }

}
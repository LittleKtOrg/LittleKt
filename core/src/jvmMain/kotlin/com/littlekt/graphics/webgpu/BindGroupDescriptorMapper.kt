package com.littlekt.graphics.webgpu

import ffi.MemoryAllocator
import io.ygdrasil.wgpu.WGPUBindGroupDescriptor
import io.ygdrasil.wgpu.WGPUBindGroupEntry

internal fun MemoryAllocator.map(input: BindGroupDescriptor): WGPUBindGroupDescriptor =
    WGPUBindGroupDescriptor.allocate(this).also { output ->
        if (input.label != null) map(input.label, output.label)
        output.layout = input.layout.segment
        if (input.entries.isNotEmpty()) {
            output.entryCount = input.entries.size.toULong()
            output.entries = WGPUBindGroupEntry.allocateArray(this, input.entries.size.toUInt()) { index, entry ->
                map(input.entries[index.toInt()], entry)
            }
        }
    }

private fun map(input: BindGroupEntry, output: WGPUBindGroupEntry) {
    output.binding = input.binding.toUInt()

    when (val resource = input.resource) {
        is BufferBinding -> {
            output.size = resource.size.toULong() ?: 0u
            output.offset = resource.offset.toULong()
            output.buffer = resource.buffer.segment
        }

        is Sampler -> output.sampler = resource.segment
        is TextureView -> output.textureView = resource.segment
    }
}


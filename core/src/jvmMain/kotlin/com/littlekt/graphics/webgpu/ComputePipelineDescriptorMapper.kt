package com.littlekt.graphics.webgpu

import ffi.MemoryAllocator
import io.ygdrasil.wgpu.WGPUComputePipelineDescriptor
import io.ygdrasil.wgpu.WGPUProgrammableStageDescriptor

fun MemoryAllocator.map(input: ComputePipelineDescriptor): WGPUComputePipelineDescriptor =
    WGPUComputePipelineDescriptor.allocate(this).also { output ->
        output.layout = input.layout.segment
        if (input.label != null) map(input.label, output.label)
        map(input.compute, output.compute)
    }

fun MemoryAllocator.map(input: ProgrammableStage, output: WGPUProgrammableStageDescriptor) {
    output.module = input.module.segment
    map(input.entryPoint, output.entryPoint)
}

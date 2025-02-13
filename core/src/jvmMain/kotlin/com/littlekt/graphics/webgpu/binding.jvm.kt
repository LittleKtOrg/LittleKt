package com.littlekt.graphics.webgpu

import com.littlekt.Releasable
import io.ygdrasil.wgpu.WGPUBindGroup
import io.ygdrasil.wgpu.WGPUBindGroupLayout
import io.ygdrasil.wgpu.WGPUPipelineLayout
import io.ygdrasil.wgpu.wgpuBindGroupLayoutRelease
import io.ygdrasil.wgpu.wgpuBindGroupRelease
import io.ygdrasil.wgpu.wgpuPipelineLayoutRelease

actual interface IntoBindingResource

actual class BindGroup(val segment: WGPUBindGroup) : Releasable {

    actual override fun release() {
        wgpuBindGroupRelease(segment)
    }

    override fun toString(): String {
        return "BindGroup"
    }
}

actual abstract class BindingLayout

actual class BufferBinding
actual constructor(val buffer: GPUBuffer, val offset: Long, val size: Long) : IntoBindingResource

actual class BufferBindingLayout
actual constructor(
    val type: BufferBindingType,
    val hasDynamicOffset: Boolean,
    val minBindingSize: Long
) : BindingLayout()

actual class TextureBindingLayout
actual constructor(
    val sampleType: TextureSampleType,
    val viewDimension: TextureViewDimension,
    val multisampled: Boolean
) : BindingLayout()

actual class SamplerBindingLayout actual constructor(val type: SamplerBindingType) :
    BindingLayout()

actual class BindGroupLayout internal constructor(val segment: WGPUBindGroupLayout) : Releasable {

    actual override fun release() {
        wgpuBindGroupLayoutRelease(segment)
    }

    override fun toString(): String {
        return "BindGroupLayout"
    }
}

actual class PipelineLayout(val segment: WGPUPipelineLayout) : Releasable {

    actual override fun release() {
        wgpuPipelineLayoutRelease(segment)
    }

    override fun toString(): String {
        return "PipelineLayout"
    }
}

actual class PipelineLayoutDescriptor {
    val segments: List<WGPUBindGroupLayout>
    val label: String?

    actual constructor(
        bindGroupLayouts: List<BindGroupLayout>,
        label: String?
    ) {
        segments = bindGroupLayouts.map { it.segment }
        this.label = label
    }

    actual constructor(
        bindGroupLayout: BindGroupLayout,
        label: String?,
    ) : this(listOf(bindGroupLayout))
}

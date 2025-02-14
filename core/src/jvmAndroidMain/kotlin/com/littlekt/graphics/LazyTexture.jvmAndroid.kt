package com.littlekt.graphics

import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.SamplerDescriptor

/** Creates a platform default [LazyTexture]. */
actual fun LazyTexture(device: Device, samplerDescriptor: SamplerDescriptor): LazyTexture =
    LazyPixmapTexture(device, samplerDescriptor)

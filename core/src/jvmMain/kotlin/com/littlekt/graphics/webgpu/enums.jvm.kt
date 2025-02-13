package com.littlekt.graphics.webgpu

import io.ygdrasil.wgpu.WGPUPrimitiveTopology_LineList
import io.ygdrasil.wgpu.WGPUPrimitiveTopology_LineStrip
import io.ygdrasil.wgpu.WGPUPrimitiveTopology_PointList
import io.ygdrasil.wgpu.WGPUPrimitiveTopology_TriangleList
import io.ygdrasil.wgpu.*

val PrimitiveTopology.nativeVal: UInt
    get() =
        when (this) {
            PrimitiveTopology.POINT_LIST -> WGPUPrimitiveTopology_PointList
            PrimitiveTopology.LINE_LIST -> WGPUPrimitiveTopology_LineList
            PrimitiveTopology.LINE_STRIP -> WGPUPrimitiveTopology_LineStrip
            PrimitiveTopology.TRIANGLE_LIST -> WGPUPrimitiveTopology_TriangleList
            PrimitiveTopology.TRIANGLE_STRIP -> WGPUPrimitiveTopology_TriangleStrip
        }

val FrontFace.nativeVal: UInt
    get() =
        when (this) {
            FrontFace.CCW -> WGPUFrontFace_CCW
            FrontFace.CW -> WGPUFrontFace_CW
        }

val CullMode.nativeVal: UInt
    get() =
        when (this) {
            CullMode.NONE -> WGPUCullMode_None
            CullMode.FRONT -> WGPUCullMode_Front
            CullMode.BACK -> WGPUCullMode_Back
        }

val TextureViewDimension.nativeVal: UInt
    get() =
        when (this) {
            TextureViewDimension.D1 -> WGPUTextureViewDimension_1D
            TextureViewDimension.D2 -> WGPUTextureViewDimension_2D
            TextureViewDimension.D2_ARRAY -> WGPUTextureViewDimension_2DArray
            TextureViewDimension.CUBE -> WGPUTextureViewDimension_Cube
            TextureViewDimension.CUBE_ARRAY -> WGPUTextureViewDimension_CubeArray
            TextureViewDimension.D3 -> WGPUTextureViewDimension_3D
        }

val TextureAspect.nativeVal: UInt
    get() =
        when (this) {
            TextureAspect.ALL -> WGPUTextureAspect_All
            TextureAspect.STENCIL_ONLY -> WGPUTextureAspect_StencilOnly
            TextureAspect.DEPTH_ONLY -> WGPUTextureAspect_DepthOnly
        }

val TextureDimension.nativeVal: UInt
    get() =
        when (this) {
            TextureDimension.D1 -> WGPUTextureDimension_1D
            TextureDimension.D2 -> WGPUTextureDimension_2D
            TextureDimension.D3 -> WGPUTextureDimension_3D
        }

val TextureFormat.nativeVal: UInt
    get() =
        when (this) {
            /** 8 Bit Red channel only. `[0, 255]` converted to/from float `[0, 1]` in shader. */
            TextureFormat.R8_UNORM -> WGPUTextureFormat_R8Unorm

            /**
             * 8 Bit Red channel only. `[-127, 127]` converted to/from float `[-1, 1]` in shader.
             */
            TextureFormat.R8_SNORM -> WGPUTextureFormat_R8Snorm

            /** Red channel only. 8 bit integer per channel. Unsigned in shader. */
            TextureFormat.R8_UINT -> WGPUTextureFormat_R8Uint

            /** Red channel only. 8 bit integer per channel. Signed in shader. */
            TextureFormat.R8_SINT -> WGPUTextureFormat_R8Sint

            /** Red channel only. 16 bit integer per channel. Unsigned in shader. */
            TextureFormat.R16_UINT -> WGPUTextureFormat_R16Uint

            /** Red channel only. 16 bit integer per channel. Signed in shader. */
            TextureFormat.R16_SINT -> WGPUTextureFormat_R16Sint

            /** Red channel only. 16 bit float per channel. Float in shader. */
            TextureFormat.R16_FLOAT -> WGPUTextureFormat_RG16Float

            /**
             * Red and green channels. 8 bit integer per channel. `[0, 255]` converted to/from float
             * `[0, 1]` in shader.
             */
            TextureFormat.RG8_UNORM -> WGPUTextureFormat_RG8Unorm

            /**
             * Red and green channels. 8 bit integer per channel. `[-127, 127]` converted to/from
             * float `[-1, 1]` in shader.
             */
            TextureFormat.RG8_SNORM -> WGPUTextureFormat_RG8Snorm

            /** Red and green channels. 8 bit integer per channel. Unsigned in shader. */
            TextureFormat.RG8_UINT -> WGPUTextureFormat_RG8Uint

            /** Red and green channel s. 8 bit integer per channel. Signed in shader. */
            TextureFormat.RG8_SINT -> WGPUTextureFormat_RG8Sint

            /** Red channel only. 32 bit integer per channel. Unsigned in shader. */
            TextureFormat.R32_UINT -> WGPUTextureFormat_R32Uint

            /** Red channel only. 32 bit integer per channel. Signed in shader. */
            TextureFormat.R32_SINT -> WGPUTextureFormat_R32Sint

            /** Red channel only. 32 bit float per channel. Float in shader. */
            TextureFormat.R32_FLOAT -> WGPUTextureFormat_R32Float

            /** Red and green channels. 16 bit integer per channel. Unsigned in shader. */
            TextureFormat.RG16_UINT -> WGPUTextureFormat_RG16Uint

            /** Red and green channels. 16 bit integer per channel. Signed in shader. */
            TextureFormat.RG16_SINT -> WGPUTextureFormat_RG16Sint

            /** Red and green channels. 16 bit float per channel. Float in shader. */
            TextureFormat.RG16_FLOAT -> WGPUTextureFormat_RG16Float

            /**
             * Red, green, blue, and alpha channels. 8 bit integer per channel. `[0, 255]` converted
             * to/from float `[0, 1]` in shader.
             */
            TextureFormat.RGBA8_UNORM -> WGPUTextureFormat_RGBA8Unorm

            /**
             * Red, green, blue, and alpha channels. 8 bit integer per channel. Srgb-color `[0,
             * 255]` converted to/from linear-color float `[0, 1]` in shader.
             */
            TextureFormat.RGBA8_UNORM_SRGB -> WGPUTextureFormat_RGBA8UnormSrgb

            /**
             * Red, green, blue, and alpha channels. 8 bit integer per channel. `[-127, 127]`
             * converted to/from float `[-1, 1]` in shader.
             */
            TextureFormat.RGBA8_SNORM -> WGPUTextureFormat_RGBA8Snorm

            /**
             * Red, green, blue, and alpha channels. 8 bit integer per channel. Unsigned in shader.
             */
            TextureFormat.RGBA8_UINT -> WGPUTextureFormat_RGBA8Uint

            /**
             * Red, green, blue, and alpha channels. 8 bit integer per channel. Signed in shader.
             */
            TextureFormat.RGBA8_SINT -> WGPUTextureFormat_RGBA8Sint

            /**
             * Blue, green, red, and alpha channels. 8 bit integer per channel. `[0, 255]` converted
             * to/from float `[0, 1]` in shader.
             */
            TextureFormat.BGRA8_UNORM -> WGPUTextureFormat_BGRA8Unorm

            /**
             * Blue, green, red, and alpha channels. 8 bit integer per channel. Srgb-color `[0,
             * 255]` converted to/from linear-color float `[0, 1]` in shader.
             */
            TextureFormat.BGRA8_UNORM_SRGB -> WGPUTextureFormat_BGRA8UnormSrgb

            /**
             * Red, green, blue, and alpha channels. 10 bit integer for RGB channels, 2 bit integer
             * for alpha channel. `[0, 1023]` (`[0, 3]` for alpha) converted to/from float `[0, 1]`
             * in shader.
             */
            TextureFormat.RGB10A2_UNORM -> WGPUTextureFormat_RGB10A2Unorm

            /**
             * Red, green, and blue channels. 11 bit float with no sign bit for RG channels. 10 bit
             * float with no sign bit for blue channel. Float in shader.
             */
            TextureFormat.RG11B10_FLOAT -> WGPUTextureFormat_RG11B10Ufloat

            /** Red and green channels. 32 bit integer per channel. Unsigned in shader. */
            TextureFormat.RG32_UINT -> WGPUTextureFormat_RG32Uint

            /** Red and green channels. 32 bit integer per channel. Signed in shader. */
            TextureFormat.RG32_SINT -> WGPUTextureFormat_RG32Sint

            /** Red and green channels. 32 bit float per channel. Float in shader. */
            TextureFormat.RG32_FLOAT -> WGPUTextureFormat_RG32Float

            /**
             * Red, green, blue, and alpha channels. 16 bit integer per channel. Unsigned in shader.
             */
            TextureFormat.RGBA16_UINT -> WGPUTextureFormat_RGBA16Uint

            /**
             * Red, green, blue, and alpha channels. 16 bit integer per channel. Signed in shader.
             */
            TextureFormat.RGBA16_SINT -> WGPUTextureFormat_RGBA16Sint

            /** Red, green, blue, and alpha channels. 16 bit float per channel. Float in shader. */
            TextureFormat.RGBA16_FLOAT -> WGPUTextureFormat_RGBA16Float

            /**
             * Red, green, blue, and alpha channels. 32 bit integer per channel. Unsigned in shader.
             */
            TextureFormat.RGBA32_UINT -> WGPUTextureFormat_RGBA32Uint

            /**
             * Red, green, blue, and alpha channels. 32 bit integer per channel. Signed in shader.
             */
            TextureFormat.RGBA32_SINT -> WGPUTextureFormat_RGBA32Sint

            /** Red, green, blue, and alpha channels. 32 bit float per channel. Float in shader. */
            TextureFormat.RGBA32_FLOAT -> WGPUTextureFormat_RGBA32Float

            /** Special depth format with 32 bit floating point depth. */
            TextureFormat.DEPTH32_FLOAT -> WGPUTextureFormat_Depth32Float

            /** Special depth format with at least 24 bit integer depth. */
            TextureFormat.DEPTH24_PLUS -> WGPUTextureFormat_Depth24Plus

            /**
             * Special depth/stencil format with at least 24 bit integer depth and 8 bits integer
             * stencil.
             */
            TextureFormat.DEPTH24_PLUS_STENCIL8 -> WGPUTextureFormat_Depth24PlusStencil8
        }

fun TextureFormat.Companion.from(nativeVal: UInt) =
    TextureFormat.entries.firstOrNull { it.nativeVal == nativeVal }

val BlendOperation.nativeVal: UInt
    get() =
        when (this) {
            BlendOperation.ADD -> WGPUBlendOperation_Add
            BlendOperation.SUBTRACT -> WGPUBlendOperation_Subtract
            BlendOperation.REVERSE_SUBTRACT -> WGPUBlendOperation_ReverseSubtract
            BlendOperation.MIN -> WGPUBlendOperation_Min
            BlendOperation.MAX -> WGPUBlendOperation_Max
        }

val StencilOperation.nativeVal: UInt
    get() =
        when (this) {
            StencilOperation.KEEP -> WGPUStencilOperation_Keep
            StencilOperation.ZERO -> WGPUStencilOperation_Zero
            StencilOperation.REPLACE -> WGPUStencilOperation_Replace
            StencilOperation.INVERT -> WGPUStencilOperation_Invert
            StencilOperation.INCREMENT_CLAMP -> WGPUStencilOperation_IncrementClamp
            StencilOperation.DECREMENT_CLAMP -> WGPUStencilOperation_DecrementClamp
            StencilOperation.INCREMENT_WRAP -> WGPUStencilOperation_IncrementWrap
            StencilOperation.DECREMENT_WRAP -> WGPUStencilOperation_DecrementWrap
        }

val BlendFactor.nativeVal: UInt
    get() =
        when (this) {
            BlendFactor.ZERO -> WGPUBlendFactor_Zero
            BlendFactor.ONE -> WGPUBlendFactor_One
            BlendFactor.SRC_COLOR -> WGPUBlendFactor_Src
            BlendFactor.ONE_MINUS_SRC_COLOR -> WGPUBlendFactor_OneMinusSrc
            BlendFactor.SRC_ALPHA -> WGPUBlendFactor_SrcAlpha
            BlendFactor.ONE_MINUS_SRC_ALPHA -> WGPUBlendFactor_OneMinusSrcAlpha
            BlendFactor.DST_COLOR -> WGPUBlendFactor_Dst
            BlendFactor.ONE_MINUS_DST_COLOR -> WGPUBlendFactor_OneMinusDst
            BlendFactor.DST_ALPHA -> WGPUBlendFactor_DstAlpha
            BlendFactor.ONE_MINUS_DST_ALPHA -> WGPUBlendFactor_OneMinusDstAlpha
            BlendFactor.SRC_ALPHA_SATURATED -> WGPUBlendFactor_SrcAlphaSaturated
            BlendFactor.CONSTANT_COLOR -> WGPUBlendFactor_Constant
            BlendFactor.ONE_MINUS_CONSTANT_COLOR -> WGPUBlendFactor_OneMinusConstant
        }

val IndexFormat.nativeVal: UInt
    get() =
        when (this) {
            /// Supported on Web and Desktop
            IndexFormat.UINT16 -> WGPUIndexFormat_Uint16

            /// Not supported on web for WGPU.
            IndexFormat.UINT32 -> WGPUIndexFormat_Uint32
        }

val VertexFormat.nativeVal: UInt
    get() =
        when (this) {
            VertexFormat.UINT8x2 -> WGPUVertexFormat_Uint8x2
            VertexFormat.UINT8x4 -> WGPUVertexFormat_Uint8x4
            VertexFormat.SINT8x2 -> WGPUVertexFormat_Sint8x2
            VertexFormat.SINT8x4 -> WGPUVertexFormat_Sint8x4
            VertexFormat.UNORM8x2 -> WGPUVertexFormat_Unorm8x2
            VertexFormat.UNORM8x4 -> WGPUVertexFormat_Unorm8x4
            VertexFormat.SNORM8x2 -> WGPUVertexFormat_Snorm8x2
            VertexFormat.SNORM8x4 -> WGPUVertexFormat_Snorm8x4
            VertexFormat.UINT16x2 -> WGPUVertexFormat_Uint16x2
            VertexFormat.UINT16x4 -> WGPUVertexFormat_Uint16x4
            VertexFormat.SINT16x2 -> WGPUVertexFormat_Sint16x2
            VertexFormat.SINT16x4 -> WGPUVertexFormat_Sint16x4
            VertexFormat.UNORM16x2 -> WGPUVertexFormat_Unorm16x2
            VertexFormat.UNORM16x4 -> WGPUVertexFormat_Unorm16x4
            VertexFormat.SNORM16x2 -> WGPUVertexFormat_Snorm16x2
            VertexFormat.SNORM16x4 -> WGPUVertexFormat_Snorm16x4
            VertexFormat.FLOAT16x2 -> WGPUVertexFormat_Float16x2
            VertexFormat.FLOAT16x4 -> WGPUVertexFormat_Float16x4
            VertexFormat.FLOAT32 -> WGPUVertexFormat_Float32
            VertexFormat.FLOAT32x2 -> WGPUVertexFormat_Float32x2
            VertexFormat.FLOAT32x3 -> WGPUVertexFormat_Float32x3
            VertexFormat.FLOAT32x4 -> WGPUVertexFormat_Float32x4
            VertexFormat.UINT32 -> WGPUVertexFormat_Uint32
            VertexFormat.UINT32x2 -> WGPUVertexFormat_Uint32x2
            VertexFormat.UINT32x3 -> WGPUVertexFormat_Uint32x3
            VertexFormat.UINT32x4 -> WGPUVertexFormat_Uint32x4
            VertexFormat.SINT32 -> WGPUVertexFormat_Sint32
            VertexFormat.SINT32x2 -> WGPUVertexFormat_Sint32x2
            VertexFormat.SINT32x3 -> WGPUVertexFormat_Sint32x3
            VertexFormat.SINT32x4 -> WGPUVertexFormat_Sint32x4
        }

val VertexStepMode.nativeVal: UInt
    get() =
        when (this) {
            VertexStepMode.VERTEX -> WGPUVertexStepMode_Vertex
            VertexStepMode.INSTANCE -> WGPUVertexStepMode_Instance
        }

val LoadOp.nativeVal: UInt
    get() =
        when (this) {
            LoadOp.CLEAR -> WGPULoadOp_Clear
            LoadOp.LOAD -> WGPULoadOp_Load
        }

val StoreOp.nativeVal: UInt
    get() =
        when (this) {
            StoreOp.DISCARD -> WGPUStoreOp_Discard
            StoreOp.STORE -> WGPUStoreOp_Store
        }

val BufferBindingType.nativeVal: UInt
    get() =
        when (this) {
            BufferBindingType.UNIFORM -> WGPUBufferBindingType_Uniform
            BufferBindingType.STORAGE -> WGPUBufferBindingType_Storage
            BufferBindingType.READ_ONLY_STORAGE -> WGPUBufferBindingType_ReadOnlyStorage
        }

val AddressMode.nativeVal: UInt
    get() =
        when (this) {
            AddressMode.CLAMP_TO_EDGE -> WGPUAddressMode_ClampToEdge
            AddressMode.REPEAT -> WGPUAddressMode_Repeat
            AddressMode.MIRROR_REPEAT -> WGPUAddressMode_MirrorRepeat
        }

val FilterMode.nativeVal: UInt
    get() =
        when (this) {
            FilterMode.NEAREST -> WGPUFilterMode_Nearest
            FilterMode.LINEAR -> WGPUFilterMode_Linear
        }

val CompareFunction.nativeVal: UInt
    get() =
        when (this) {
            CompareFunction.NEVER -> WGPUCompareFunction_Never
            CompareFunction.LESS -> WGPUCompareFunction_Less
            CompareFunction.EQUAL -> WGPUCompareFunction_Equal
            CompareFunction.LESS_EQUAL -> WGPUCompareFunction_LessEqual
            CompareFunction.GREATER -> WGPUCompareFunction_Greater
            CompareFunction.NOT_EQUAL -> WGPUCompareFunction_NotEqual
            CompareFunction.GREATER_EQUAL -> WGPUCompareFunction_GreaterEqual
            CompareFunction.ALWAYS -> WGPUCompareFunction_Always
        }

val TextureSampleType.nativeVal: UInt
    get() =
        when (this) {
            TextureSampleType.FLOAT -> WGPUTextureSampleType_Float
            TextureSampleType.SINT -> WGPUTextureSampleType_Sint
            TextureSampleType.UINT -> WGPUTextureSampleType_Uint
            TextureSampleType.DEPTH -> WGPUTextureSampleType_Depth
        }

val SamplerBindingType.nativeVal: UInt
    get() =
        when (this) {
            SamplerBindingType.FILTERING -> WGPUSamplerBindingType_Filtering
            SamplerBindingType.NON_FILTERING -> WGPUSamplerBindingType_NonFiltering
            SamplerBindingType.COMPARISON -> WGPUSamplerBindingType_Comparison
        }

val AlphaMode.nativeVal: UInt
    get() =
        when (this) {
            AlphaMode.AUTO -> WGPUCompositeAlphaMode_Auto
            AlphaMode.OPAQUE -> WGPUCompositeAlphaMode_Opaque
            AlphaMode.PREMULTIPLIED -> WGPUCompositeAlphaMode_Premultiplied
            AlphaMode.UNPREMULTIPLIED -> WGPUCompositeAlphaMode_Unpremultiplied
            AlphaMode.INHERIT -> WGPUCompositeAlphaMode_Inherit
        }

fun AlphaMode.Companion.from(nativeVal: UInt): AlphaMode? =
    AlphaMode.entries.firstOrNull { it.nativeVal == nativeVal }

val PresentMode.nativeVal: UInt
    get() =
        when (this) {
            PresentMode.FIFO -> WGPUPresentMode_Fifo
            PresentMode.FIFO_RELAXED -> WGPUPresentMode_FifoRelaxed
            PresentMode.IMMEDIATE -> WGPUPresentMode_Immediate
            PresentMode.MAILBOX -> WGPUPresentMode_Mailbox
        }

val TextureStatus.nativeVal: UInt
    get() =
        when (this) {
            TextureStatus.SUCCESS -> WGPUSurfaceGetCurrentTextureStatus_SuccessOptimal
            TextureStatus.TIMEOUT -> WGPUSurfaceGetCurrentTextureStatus_Timeout
            TextureStatus.OUTDATED -> WGPUSurfaceGetCurrentTextureStatus_Outdated
            TextureStatus.LOST -> WGPUSurfaceGetCurrentTextureStatus_Lost
            TextureStatus.OUT_OF_MEMORY -> WGPUSurfaceGetCurrentTextureStatus_OutOfMemory
            TextureStatus.DEVICE_LOST -> WGPUSurfaceGetCurrentTextureStatus_DeviceLost
        }

fun TextureStatus.Companion.from(nativeVal: UInt): TextureStatus? =
    TextureStatus.entries.firstOrNull { it.nativeVal == nativeVal }

@JvmInline
value class Backend(val flag: ULong) {

    infix fun or(other: Backend): Backend = Backend(flag or other.flag)

    infix fun and(other: Backend): Backend = Backend(flag and other.flag)

    fun isInvalid(): Boolean = this or all != all

    companion object {
        val VULKAN: Backend = Backend(WGPUInstanceBackend_Vulkan)
        val GL: Backend = Backend(WGPUInstanceBackend_GL)
        val METAL: Backend = Backend(WGPUInstanceBackend_Metal)
        val DX11: Backend = Backend(WGPUInstanceBackend_DX11)
        val DX12: Backend = Backend(WGPUInstanceBackend_DX12)
        val ALL: Backend = Backend(WGPUInstanceBackend_All)

        private val all = VULKAN or GL or METAL or DX11 or DX12
    }
}

val Feature.nativeVal: UInt
    get() =
        when (this) {
            Feature.DEPTH_CLIP_CONTROL -> WGPUFeatureName_DepthClipControl
            Feature.DEPTH32FLOAT_STENCIL18 -> WGPUFeatureName_Depth32FloatStencil8
            Feature.TEXTURE_COMPRESSION_BC -> WGPUFeatureName_TextureCompressionBC
            Feature.TEXTURE_COMPRESSION_ETC2 -> WGPUFeatureName_TextureCompressionETC2
            Feature.TEXTURE_COMPRESSION_ASTC -> WGPUFeatureName_TextureCompressionASTC
            Feature.TIMESTAMP_QUERY -> WGPUFeatureName_TimestampQuery
            Feature.INDIRECT_FIRST_INSTANCE -> WGPUFeatureName_IndirectFirstInstance
            Feature.SHADER_F16 -> WGPUFeatureName_ShaderF16
            Feature.RG11B10UFLOAT_RENDERABLE -> WGPUFeatureName_RG11B10UfloatRenderable
            Feature.BGRA8UNORM_STORAGE -> WGPUFeatureName_BGRA8UnormStorage
            Feature.FLOAT32_FILTERABLE -> WGPUFeatureName_Float32Filterable
            Feature.CLIP_DISTANCES ->
                WGPUFeatureName_Undefined // TODO check if this exists in WGPU in the future
            Feature.DUAL_SOURCE_BLENDING ->
                WGPUFeatureName_Undefined // TODO check if this exists in WGPU in the future
        }

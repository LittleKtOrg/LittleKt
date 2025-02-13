package com.littlekt

import com.littlekt.graphics.Cursor
import com.littlekt.graphics.HdpiMode
import com.littlekt.graphics.SystemCursor
import com.littlekt.graphics.webgpu.Adapter
import com.littlekt.graphics.webgpu.AlphaMode
import com.littlekt.graphics.webgpu.Backend
import com.littlekt.graphics.webgpu.Device
import com.littlekt.graphics.webgpu.PresentMode
import com.littlekt.graphics.webgpu.Surface
import com.littlekt.graphics.webgpu.SurfaceCapabilities
import com.littlekt.graphics.webgpu.SurfaceConfiguration
import com.littlekt.graphics.webgpu.TextureFormat
import com.littlekt.graphics.webgpu.TextureUsage
import com.littlekt.log.Logger
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import darwin.CAMetalLayer
import darwin.NSWindow
import ffi.MemoryAllocator
import ffi.NativeAddress
import ffi.memoryScope
import io.ygdrasil.wgpu.WGPUAdapter
import io.ygdrasil.wgpu.WGPUInstance
import io.ygdrasil.wgpu.WGPUNativeSType_InstanceExtras
import io.ygdrasil.wgpu.WGPURequestAdapterCallback
import io.ygdrasil.wgpu.WGPURequestAdapterCallbackInfo
import io.ygdrasil.wgpu.WGPURequestAdapterOptions
import io.ygdrasil.wgpu.WGPURequestAdapterStatus
import io.ygdrasil.wgpu.WGPURequestAdapterStatus_Success
import io.ygdrasil.wgpu.WGPUSType_SurfaceSourceAndroidNativeWindow
import io.ygdrasil.wgpu.WGPUSType_SurfaceSourceMetalLayer
import io.ygdrasil.wgpu.WGPUSType_SurfaceSourceWaylandSurface
import io.ygdrasil.wgpu.WGPUSType_SurfaceSourceWindowsHWND
import io.ygdrasil.wgpu.WGPUSType_SurfaceSourceXlibWindow
import io.ygdrasil.wgpu.WGPUStringView
import io.ygdrasil.wgpu.WGPUSurface
import io.ygdrasil.wgpu.WGPUSurfaceDescriptor
import io.ygdrasil.wgpu.WGPUSurfaceSourceAndroidNativeWindow
import io.ygdrasil.wgpu.WGPUSurfaceSourceMetalLayer
import io.ygdrasil.wgpu.WGPUSurfaceSourceWaylandSurface
import io.ygdrasil.wgpu.WGPUSurfaceSourceWindowsHWND
import io.ygdrasil.wgpu.WGPUSurfaceSourceXlibWindow
import io.ygdrasil.wgpu.wgpuCreateInstance
import io.ygdrasil.wgpu.wgpuInstanceCreateSurface
import io.ygdrasil.wgpu.wgpuInstanceRelease
import io.ygdrasil.wgpu.wgpuInstanceRequestAdapter
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWNativeCocoa.glfwGetCocoaWindow
import org.lwjgl.glfw.GLFWNativeWayland
import org.lwjgl.glfw.GLFWNativeWin32
import org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Display
import org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Window
import org.rococoa.ID
import org.rococoa.Rococoa
import java.lang.foreign.MemorySegment

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class LwjglGraphics(private val context: LwjglContext) : Graphics, Releasable {
    private val systemCursors = mutableMapOf<SystemCursor, Long>()

    internal var _logicalWidth: Int = 0
    internal var _logicalHeight: Int = 0

    internal var _backBufferWidth: Int = 0
    internal var _backBufferHeight: Int = 0

    override val width: Int
        get() =
            if (context.configuration.hdpiMode == HdpiMode.PIXELS) backBufferWidth
            else _logicalWidth

    override val height: Int
        get() =
            if (context.configuration.hdpiMode == HdpiMode.PIXELS) backBufferHeight
            else _logicalHeight

    override val backBufferWidth: Int
        get() = _backBufferWidth

    override val backBufferHeight: Int
        get() = _backBufferHeight

    internal lateinit var instance: Instance

    override lateinit var surface: Surface
    override lateinit var adapter: Adapter
    override lateinit var device: Device

    override val preferredFormat by lazy { surface.getPreferredFormat(adapter) }
    private var hasSurfaceCapabilities = false
    override val surfaceCapabilities: SurfaceCapabilities by lazy {
        hasSurfaceCapabilities = true
        surface.getCapabilities(adapter)
    }

    override fun configureSurface(
        usage: TextureUsage,
        format: TextureFormat,
        presentMode: PresentMode,
        alphaMode: AlphaMode,
    ) {
        surface.configure(
            SurfaceConfiguration(device, usage, format, presentMode, alphaMode, width, height)
        )
    }

    internal suspend fun requestAdapterAndDevice(powerPreference: PowerPreference) {
        val output = atomic<WGPUAdapter?>(null)

        memoryScope { scope ->

            val options = WGPURequestAdapterOptions.allocate(scope)
            options.compatibleSurface = surface.segment
            options.powerPreference = powerPreference.nativeVal

            val callback = WGPURequestAdapterCallback.allocate(scope, object : WGPURequestAdapterCallback {
                override fun invoke(
                    status: WGPURequestAdapterStatus,
                    adapter: WGPUAdapter?,
                    message: WGPUStringView?,
                    userdata1: NativeAddress?,
                    userdata2: NativeAddress?
                ) {
                    if (status == WGPURequestAdapterStatus_Success) {
                        output.update { adapter }
                    } else {
                        logger.error {
                            "requestAdapter status=$status, message=${message?.data?.toKString(message.length)}"
                        }
                    }
                }
            })

            val callbackInfo = WGPURequestAdapterCallbackInfo.allocate(scope).apply {
                this.callback = callback
                this.userdata2 = scope.bufferOfAddress(callback.handler).handler
            }

            wgpuInstanceRequestAdapter(instance.segment, options, callbackInfo)

        }


        adapter = Adapter(output.value ?: error("Failed to request adapter"))
        requestDevice()
    }

    private suspend fun requestDevice() {
        device = adapter.requestDevice()
    }

    override fun supportsExtension(extension: String): Boolean {
        return GLFW.glfwExtensionSupported(extension)
    }

    override fun setCursor(cursor: Cursor) {
        GLFW.glfwSetCursor(context.windowHandle, cursor.cursorHandle)
    }

    override fun setCursor(cursor: SystemCursor) {
        var handle = systemCursors[cursor]
        if (handle == null) {
            handle =
                when (cursor) {
                    SystemCursor.ARROW -> GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR)
                    SystemCursor.I_BEAM -> GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR)
                    SystemCursor.CROSSHAIR ->
                        GLFW.glfwCreateStandardCursor(GLFW.GLFW_CROSSHAIR_CURSOR)

                    SystemCursor.HAND -> GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR)
                    SystemCursor.HORIZONTAL_RESIZE ->
                        GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR)

                    SystemCursor.VERTICAL_RESIZE ->
                        GLFW.glfwCreateStandardCursor(GLFW.GLFW_VRESIZE_CURSOR)
                }
            if (handle == 0L) return
            systemCursors[cursor] = handle
        }
        GLFW.glfwSetCursor(context.windowHandle, handle)
    }

    internal fun createInstance(configuration: JvmConfiguration) {
        instance = memoryScope { scope ->
            Instance(wgpuCreateInstance(scope.map(configuration.preferredBackends)) ?: error("Failed to create instance"))
        }
    }

    internal fun MemoryAllocator.map(backend: Backend) =
        io.ygdrasil.wgpu.WGPUInstanceDescriptor.allocate(this).also { output ->
            output.nextInChain = io.ygdrasil.wgpu.WGPUInstanceExtras.allocate(this).also { nextInChain ->
                nextInChain.backends = backend.flag
                nextInChain.chain.sType = WGPUNativeSType_InstanceExtras
            }.handler
        }

    internal fun configureSurfaceToWindow(windowHandle: Long) {
        val isMac = System.getProperty("os.name").lowercase().contains("mac")
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val isLinux = System.getProperty("os.name").lowercase().contains("linux")
        surface =
            Surface(
                when {
                    isWindows -> {
                        val hwnd = GLFWNativeWin32.glfwGetWin32Window(windowHandle).toNativeAddress()
                        val hinstance = Kernel32.INSTANCE.GetModuleHandle(null).pointer.toNativeAddress()
                        instance.segment.getSurfaceFromWindows(hinstance, hwnd)
                    }

                    isLinux -> {
                        val platform = GLFW.glfwGetPlatform()
                        when (platform) {
                            GLFW.GLFW_PLATFORM_X11 -> {
                                val display = glfwGetX11Display().toNativeAddress()
                                val x11_window = glfwGetX11Window(windowHandle).toULong()
                                instance.segment.getSurfaceFromX11Window(display, x11_window) ?: error("fail to get surface on Linux")
                            }

                            GLFW.GLFW_PLATFORM_WAYLAND -> {
                                val display = GLFWNativeWayland.glfwGetWaylandDisplay().toNativeAddress()
                                val surface = GLFWNativeWayland.glfwGetWaylandWindow(windowHandle).toNativeAddress()
                                instance.segment.getSurfaceFromWaylandWindow(display, surface) ?: error("fail to get surface on Linux")
                            }

                            else -> {
                                logger.log(Logger.Level.ERROR) {
                                    "Linux platform not supported. Supported backends: [X11, Wayland]"
                                }
                                null
                            }
                        }
                    }

                    isMac -> {
                        val nsWindowPtr = glfwGetCocoaWindow(windowHandle)
                        val nswindow = Rococoa.wrap(ID.fromLong(nsWindowPtr), NSWindow::class.java)
                        nswindow.contentView()?.setWantsLayer(true)
                        val layer = CAMetalLayer.layer()
                        nswindow.contentView()?.setLayer(layer.id().toLong().toPointer())
                        instance.segment.getSurfaceFromMetalLayer(layer.id().toLong().toNativeAddress())
                    }

                    else -> {
                        logger.log(Logger.Level.ERROR) { "Platform not supported." }
                        null
                    }
                } ?: error("Failed to create surface"),
            )
    }

    override fun release() {
        device.queue.release()
        device.release()
        adapter.release()
        surface.release()
        wgpuInstanceRelease(instance.segment)
    }

    companion object {
        private val logger = Logger<LwjglGraphics>()
    }
}


private fun WGPUInstance.getSurfaceFromMetalLayer(metalLayer: NativeAddress): WGPUSurface? = memoryScope { scope ->

    val surfaceDescriptor = WGPUSurfaceDescriptor.allocate(scope).apply {
        nextInChain = WGPUSurfaceSourceMetalLayer.allocate(scope).apply {
            chain.sType = WGPUSType_SurfaceSourceMetalLayer
            layer = metalLayer
        }.handler
    }

    return wgpuInstanceCreateSurface(this, surfaceDescriptor)
}

private fun WGPUInstance.getSurfaceFromX11Window(display: NativeAddress, window: ULong): WGPUSurface? = memoryScope { scope ->

    val surfaceDescriptor = WGPUSurfaceDescriptor.allocate(scope).apply {
        nextInChain = WGPUSurfaceSourceXlibWindow.allocate(scope).apply {
            chain.sType = WGPUSType_SurfaceSourceXlibWindow
            this.display = display
            this.window = window
        }.handler
    }

    return wgpuInstanceCreateSurface(this, surfaceDescriptor)
}

private fun WGPUInstance.getSurfaceFromAndroidWindow(window: NativeAddress): WGPUSurface? = memoryScope { scope ->

    val surfaceDescriptor = WGPUSurfaceDescriptor.allocate(scope).apply {
        nextInChain = WGPUSurfaceSourceAndroidNativeWindow.allocate(scope).apply {
            chain.sType = WGPUSType_SurfaceSourceAndroidNativeWindow
            this.window = window
        }.handler
    }

    return wgpuInstanceCreateSurface(this, surfaceDescriptor)
}

private fun WGPUInstance.getSurfaceFromWaylandWindow(display: NativeAddress, surface: NativeAddress): WGPUSurface? = memoryScope { scope ->

    val surfaceDescriptor = WGPUSurfaceDescriptor.allocate(scope).apply {
        nextInChain = WGPUSurfaceSourceWaylandSurface.allocate(scope).apply {
            chain.sType = WGPUSType_SurfaceSourceWaylandSurface
            this.display = display
            this.surface = surface
        }.handler
    }

    return wgpuInstanceCreateSurface(this, surfaceDescriptor)
}

private fun WGPUInstance.getSurfaceFromWindows(hinstance: NativeAddress, hwnd: NativeAddress): WGPUSurface? = memoryScope { scope ->

    val surfaceDescriptor = WGPUSurfaceDescriptor.allocate(scope).apply {
        nextInChain = WGPUSurfaceSourceWindowsHWND.allocate(scope).apply {
            chain.sType = WGPUSType_SurfaceSourceWindowsHWND
            this.hwnd = hwnd
            this.hinstance = hinstance
        }.handler
    }

    return wgpuInstanceCreateSurface(this, surfaceDescriptor)
}

private fun Long.toNativeAddress() = let { MemorySegment.ofAddress(it) }
    .let { NativeAddress(it) }

private fun Pointer.toNativeAddress() = let { MemorySegment.ofAddress(Pointer.nativeValue(this)) }
    .let { NativeAddress(it) }

private fun Long.toPointer(): Pointer = Pointer(this)

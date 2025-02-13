package com.littlekt

import com.littlekt.async.KtScope
import com.littlekt.async.MainDispatcher
import com.littlekt.async.mainThread
import com.littlekt.audio.OpenALAudioContext
import com.littlekt.file.*
import com.littlekt.file.Base64.decodeFromBase64
import com.littlekt.file.vfs.VfsFile
import com.littlekt.file.vfs.readPixmap
import com.littlekt.input.LwjglInput
import com.littlekt.log.Logger
import com.littlekt.resources.internal.InternalResources
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.now
import ffi.globalMemory
import io.ygdrasil.wgpu.WGPULogCallback
import io.ygdrasil.wgpu.WGPULogLevel_Debug
import io.ygdrasil.wgpu.WGPULogLevel_Error
import io.ygdrasil.wgpu.WGPULogLevel_Info
import io.ygdrasil.wgpu.WGPULogLevel_Trace
import io.ygdrasil.wgpu.WGPULogLevel_Warn
import io.ygdrasil.wgpu.wgpuSetLogCallback
import io.ygdrasil.wgpu.wgpuSetLogLevel
import java.lang.foreign.Arena
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.runBlocking
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
class LwjglContext(override val configuration: JvmConfiguration) : Context() {

    override val stats: AppStats = AppStats()
    override val graphics: LwjglGraphics = LwjglGraphics(this)
    override val logger: Logger = Logger(configuration.title)
    override val vfsResources: Vfs = JvmResourcesVfs(this, logger)
    override val vfsUrl: Vfs = JvmUrlVfs(this, logger)
    override val vfsApplication: Vfs = JvmApplicationVfs(this, logger, ".")
    override val input: LwjglInput = LwjglInput(this)
    override val resourcesVfs: VfsFile
        get() = vfsResources.root

    override val urlVfs: VfsFile
        get() = vfsUrl.root

    override val applicationVfs: VfsFile
        get() = vfsApplication.root

    override val kvStorage: KeyValueStorage = JvmKeyValueStorage(logger, "./.storage")

    override val platform: Platform = Platform.DESKTOP

    override val clipboard: JvmClipboard by lazy { JvmClipboard(windowHandle) }

    internal var windowHandle: Long = 0
        private set

    internal val audioContext = OpenALAudioContext()

    private val windowShouldClose: Boolean
        get() = GLFW.glfwWindowShouldClose(windowHandle)

    private val tempBuffer: IntBuffer
    private val tempBuffer2: IntBuffer

    private val scope = Arena.ofConfined()

    private val wgpuLogger = Logger("WGPU")

    init {
        MemoryStack.stackPush().use { stack ->
            tempBuffer = stack.mallocInt(1) // int*
            tempBuffer2 = stack.mallocInt(1) // int*
        }
        KtScope.initiate()
        mainThread = Thread.currentThread()
        if (configuration.enableWGPULogging) {
            initLogging()
        }
    }

    override fun start(build: (app: Context) -> ContextListener) = runBlocking {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        check(GLFW.glfwInit()) { "Unable to initialize GLFW" }

        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(
            GLFW.GLFW_VISIBLE,
            GLFW.GLFW_FALSE,
        ) // the window will stay hidden after creation
        GLFW.glfwWindowHint(
            GLFW.GLFW_RESIZABLE,
            configuration.resizeable.glfw,
        ) // the window will be resizable
        GLFW.glfwWindowHint(GLFW.GLFW_MAXIMIZED, configuration.maximized.glfw)
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API) // prevent opengl

        val isMac = System.getProperty("os.name").lowercase().contains("mac")

        // Create the window
        windowHandle =
            GLFW.glfwCreateWindow(
                configuration.width,
                configuration.height,
                configuration.title,
                MemoryUtil.NULL,
                MemoryUtil.NULL,
            )
        if (windowHandle == MemoryUtil.NULL)
            throw RuntimeException("Failed to create the GLFW window")

        graphics.createInstance(configuration)
        graphics.configureSurfaceToWindow(windowHandle)
        graphics.requestAdapterAndDevice(configuration.powerPreference)

        updateFramebufferInfo()

        // Get the resolution of the primary monitor
        val vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())

        check(vidmode != null) { "Unable to retrieve GLFW video mode" }

        // Center the window
        GLFW.glfwSetWindowPos(
            windowHandle,
            configuration.windowPosX ?: ((vidmode.width() - graphics.width) / 2),
            configuration.windowPosY ?: ((vidmode.height() - graphics.height) / 2),
        )

        // set window icon
        if (!isMac) {
            if (configuration.icons.isNotEmpty()) {
                val buffer = GLFWImage.malloc(configuration.icons.size)
                configuration.icons.forEach {
                    val pixmap = resourcesVfs[it].readPixmap()
                    val icon = GLFWImage.malloc()
                    icon.set(
                        pixmap.width,
                        pixmap.height,
                        ByteBuffer.allocateDirect(pixmap.pixels.capacity)
                            .put(pixmap.pixels.toArray())
                            .flip(),
                    )
                    buffer.put(icon)
                    icon.free()
                }
                buffer.flip()
                GLFW.glfwSetWindowIcon(windowHandle, buffer)
                buffer.free()
            } else {
                val pixmap = ktHead32x32.decodeFromBase64().readPixmap()
                val icon = GLFWImage.malloc()
                icon.set(
                    pixmap.width,
                    pixmap.height,
                    ByteBuffer.allocateDirect(pixmap.pixels.capacity)
                        .put(pixmap.pixels.toArray())
                        .flip(),
                )
                val buffer = GLFWImage.malloc(1)
                buffer.put(icon)
                icon.free()
                buffer.flip()
                GLFW.glfwSetWindowIcon(windowHandle, buffer)
                buffer.free()
            }
        }

        // Make the window visible
        GLFW.glfwShowWindow(windowHandle)
        input.attachToWindow(windowHandle)

        if (configuration.loadInternalResources) {
            InternalResources.createInstance(this@LwjglContext)
            InternalResources.INSTANCE.load()
        }

        val listener: ContextListener = build(this@LwjglContext)

        GLFW.glfwSetFramebufferSizeCallback(windowHandle) { _, _, _ ->
            updateFramebufferInfo()

            resizeCalls.fastForEach { resize -> resize(graphics.width, graphics.height) }
        }

        listener.run { start() }
        listener.run {
            updateFramebufferInfo()
            resizeCalls.fastForEach { resize -> resize(graphics.width, graphics.height) }
        }

        while (!windowShouldClose) {
            calcFrameTimes(now().milliseconds)
            MainDispatcher.INSTANCE.executePending(available)
            update(dt)
        }

        releaseCalls.fastForEach { release -> release() }
        destroy()
    }

    private fun initLogging() {
        val callback = WGPULogCallback.allocate(globalMemory) { level, cMessage, userdata ->
            val message = cMessage?.data?.toKString(cMessage.length) ?: "empty message"
            when (level) {
                WGPULogLevel_Error -> logger.error { message }
                WGPULogLevel_Warn -> logger.warn { message }
                WGPULogLevel_Info -> logger.info { message }
                WGPULogLevel_Debug -> logger.debug { message }
                WGPULogLevel_Trace -> logger.trace { message }
                else -> logger.warn { "Unknown log level $level with message $message" }
            }
        }

        wgpuSetLogLevel(WGPULogLevel_Trace)
        wgpuSetLogCallback(callback, globalMemory.bufferOfAddress(callback.handler).handler)
    }

    private fun updateFramebufferInfo() {
        GLFW.glfwGetWindowSize(windowHandle, tempBuffer, tempBuffer2)

        graphics._logicalWidth = tempBuffer[0]
        graphics._logicalHeight = tempBuffer2[0]

        GLFW.glfwGetFramebufferSize(windowHandle, tempBuffer, tempBuffer2)

        graphics._backBufferWidth = tempBuffer[0]
        graphics._backBufferHeight = tempBuffer2[0]
    }

    private suspend fun update(dt: Duration) {
        audioContext.update()

        EngineStats.resetPerFrameCounts()

        input.update()
        stats.update(dt)

        updateCalls.fastForEach { update -> update(dt) }
        postUpdateCalls.fastForEach { postUpdate -> postUpdate(dt) }

        input.reset()
        GLFW.glfwPollEvents()

        invokeAnyRunnable()
    }

    private fun invokeAnyRunnable() {
        if (postRunnableCalls.isNotEmpty()) {
            postRunnableCalls.fastForEach { postRunnable -> postRunnable.invoke() }
            postRunnableCalls.clear()
        }
    }

    override fun close() {
        GLFW.glfwSetWindowShouldClose(windowHandle, true)
    }

    override fun destroy() {
        super.destroy()
        // Free the window callbacks and destroy the window
        Callbacks.glfwFreeCallbacks(windowHandle)
        GLFW.glfwDestroyWindow(windowHandle)

        graphics.release()

        // Terminate GLFW and free the error callback
        GLFW.glfwTerminate()
        GLFW.glfwSetErrorCallback(null)?.free()

        scope.close()

        audioContext.release()
    }

    private val Boolean.glfw: Int
        get() = if (this) GLFW.GLFW_TRUE else GLFW.GLFW_FALSE
}

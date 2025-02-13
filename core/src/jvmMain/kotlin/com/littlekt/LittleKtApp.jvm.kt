package com.littlekt

import com.littlekt.graphics.HdpiMode
import com.littlekt.graphics.webgpu.Backend
import ffi.LibraryLoader

internal val ktHead32x32 =
    "iVBORw0KGgoAAAANSUhEUgAAACAAAAAhCAYAAAC4JqlRAAAAAXNSR0IArs4c6QAABKNJREFUWIWVl39oVWUYxz/vzgnCsFqxS2tBymKgCF6vOs3Zbu6/zHBCf5gOQnYURBKMhit/YJszZVGwFmLeIULzL8EtMIPAebOprDYnQ8sRNQldXCulH4PkHN/+OOc9e8+vu7sHLpxz7vc83+/zfZ73PecIoiFD5yIGM9tIzFkWAsmbow+wsgX/F3PzrMmtbAEpJVLKSE6lREop2fry3dgMuXwqoHo25FK6XHru4xcqEEIACF+Ap6wkAfcyzYmulI/0lJRT5S3TVXpEkVC23Vi+V95c1ir/LHuKv8oe51/xGOUjPZSP9ABg4jCVaZIPlrwhVaVxcev3c/6xGaqyaCz87mCSOMq1+6cyTQCJLf36+pv+faYCWtkCZ681RsC5fIpfmlrJaWTqeub5logQgDn5FHL7EcTRFDO1QV8FTN6/FLAt5yXq+PVtdr56l1w+5TtlZQtk5rUwMtEZSWplC2y9sQWA4wtPFHW3LOkPRT73xCcAdJ2toLGyl8bK3oCQJBG6ELn9SECElS1w5ae9gDvZclHVNla+cNAnBjwLdyOlRAhBY2VvhKBvcvOMFuvFiKO7fbxaWYlLRrc6l0/5AvomN8dWWoqI8AwBwiwGPn6hwp9kRSxHcwGsSFsR0XGbWpLIWAEq9CRhYv26SKcCGCFErIiwC5AwhMp2dVMSuS5Cx8jRHEKIxI0oVoDaA0rZkEoJJULPGdcGvwWVT9QlJoJgr4s5ouNUhCY/IMQXkJnXQi6f4uO6PNdjks7UhjhxIm0F+m5lC7T/8z6VmstlAAdWXyWXT3F49WV2DWZjk4q05f+KhY6LE105/CmTS3cAcC9jSRPAFgYHVl+l9dslgakOOCAlVQ173AuLm+FaT5R9cTPPrvkZgNvnO9zV4OVTg62KmVy6AxvbdcDBxBZGoGK9Tzq5ShxbvRDcPt8BQFXDHtRjXs+rwsHAxnQFfHBxoXAwqXl6jQ8Ir4Y7A4e4M3AIgHWddqwAdV3HKuJcPhVony1MHIzpZWhjMv7HAAB9X3wVS6AqKxZx/yviM22vA/Dj8vewMXluuEtMCxAGLS+N++D2LdlpF671sK7T5tTgQzJr2xjrrvaT3/97yj8e664ms7aNU4MPXTe8OZGjOZ9cpC1sDGzvKRBwQInYsP80i+YHd7Gx7mre3TSfse5qJvr3+eRPzp3ji5jo3xfAhR3YsP80w7UH3ZnDnbnANL1Vf0ua3nh8+E2NPzTz1rcz8cP3Lmi83z33ROjhX69Z754vWObjlIih2sMY2Cwd2isiArZnb0tT2pg4mNKm8+K0CJWU8f4IcSRCWJG2GFjRhSHd2k0caodaowLCIsJOFIukjUekLc6tOIaBwyPYGNKhfmiXzxt5HDsY3nuSe/5O/TginSxCWZtEfmblSWwct1YJMrSHxO4o27K/ScNrw2yd0Mk/f/E0j/IfhmorNq9c2RbgjH0f+Cz/jHC8pWJj4GB6ThR/DujkJ1edAXDv9/KEyWGG770kJyDZcoBjq770K1aDt/Hyxliukj44w4NpSofDFxdEcB/V5TWMi2++9FpRjll98er7hCnVdqIc0o8ddg42lJT7fz+5dOVMdmd7AAAAAElFTkSuQmCC"

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
actual class LittleKtProps {
    var width: Int = 960
    var height: Int = 540
    var title: String = "LitteKt"
    var icons: List<String> = listOf()
    var resizeable = true
    var maximized = false

    var windowPosX: Int? = null
    var windowPosY: Int? = null

    var hdpiMode: HdpiMode = HdpiMode.LOGICAL

    var traceWgpu = false
    var enableWGPULogging = false

    /** Bitmask for backends. Defaults to [Backend.ALL]. */
    var preferredBackends = Backend.ALL

    var loadInternalResources: Boolean = true

    var powerPreference = PowerPreference.HIGH_POWER
}

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
actual fun createLittleKtApp(action: LittleKtProps.() -> Unit): LittleKtApp {
    val props = LittleKtProps().apply(action)
    System.setProperty("jextract.trace.downcalls", props.traceWgpu.toString())
    loadNativesFromClasspath()
    return LittleKtApp(
        LwjglContext(
            JvmConfiguration(
                props.title,
                props.width,
                props.height,
                props.icons,
                props.resizeable,
                props.maximized,
                props.windowPosX,
                props.windowPosY,
                props.hdpiMode,
                props.enableWGPULogging,
                props.preferredBackends,
                props.loadInternalResources,
                props.powerPreference
            )
        )
    )
}

private fun loadNativesFromClasspath() {
    LibraryLoader.load()
}

/**
 * @author Colton Daily
 * @date 11/17/2021
 */
class JvmConfiguration(
    override val title: String,
    val width: Int,
    val height: Int,
    val icons: List<String>,
    val resizeable: Boolean,
    val maximized: Boolean,
    val windowPosX: Int?,
    val windowPosY: Int?,
    val hdpiMode: HdpiMode,
    val enableWGPULogging: Boolean,
    val preferredBackends: Backend,
    override val loadInternalResources: Boolean,
    val powerPreference: PowerPreference = PowerPreference.HIGH_POWER
) : ContextConfiguration()

val PowerPreference.nativeVal: UInt
    get() =
        when (this) {
            PowerPreference.LOW_POWER -> 1u
            PowerPreference.HIGH_POWER -> 2u
        }

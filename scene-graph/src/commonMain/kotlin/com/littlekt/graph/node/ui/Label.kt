package com.littlekt.graph.node.ui

import com.littlekt.graph.SceneGraph
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.addTo
import com.littlekt.graph.node.annotation.SceneGraphDslMarker
import com.littlekt.graph.node.resource.Theme
import com.littlekt.graphics.*
import com.littlekt.graphics.g2d.Batch
import com.littlekt.graphics.g2d.font.BitmapFont
import com.littlekt.graphics.g2d.font.BitmapFontCache
import com.littlekt.graphics.g2d.font.GlyphLayout
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import com.littlekt.math.geom.Angle
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.roundToInt

/** Adds a [Label] to the current [Node] as a child and then triggers the [callback] */
@OptIn(ExperimentalContracts::class)
inline fun Node.label(callback: @SceneGraphDslMarker Label.() -> Unit = {}): Label {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return Label().also(callback).addTo(this)
}

/** Adds a [Label] to the current [SceneGraph.root] as a child and then triggers the [callback] */
@OptIn(ExperimentalContracts::class)
inline fun SceneGraph<*>.label(callback: @SceneGraphDslMarker Label.() -> Unit = {}): Label {
    contract { callsInPlace(callback, InvocationKind.EXACTLY_ONCE) }
    return root.label(callback)
}

/**
 * A [Control] that renders text with a specified [BitmapFont].
 *
 * @author Colton Daily
 * @date 1/8/2022
 */
open class Label : Control() {

    private var cache: BitmapFontCache = BitmapFontCache(font)
    private val layout = GlyphLayout()

    private var _fontScale = MutableVec2f(1f)

    /** The scale of the font. */
    var fontScale: Vec2f
        get() = _fontScale
        set(value) {
            if (value == _fontScale) return
            if (value.x == 0f || value.y == 0f) {
                _fontScale.set(value)
                if (value.x == 0f) {
                    _fontScale.x = 0.1f
                }
                if (value.y == 0f) {
                    _fontScale.y = 0.1f
                }
            } else {
                _fontScale.set(value)
            }
            onMinimumSizeChanged()
        }

    /** The scale of the font on the x-axis. */
    var fontScaleX: Float
        get() = _fontScale.x
        set(value) {
            if (value == _fontScale.x) return
            if (value == 0f) {
                _fontScale.x = value + 0.1f
            } else {
                _fontScale.x = value
            }
            onMinimumSizeChanged()
        }

    /** The scale of the font on the y-axis. */
    var fontScaleY: Float
        get() = _fontScale.y
        set(value) {
            if (value == _fontScale.y) return
            if (value == 0f) {
                _fontScale.y = value + 0.1f
            } else {
                _fontScale.y = value
            }
            onMinimumSizeChanged()
        }

    /** The color of the text. */
    var fontColor: Color
        get() = getThemeColor(themeVars.fontColor)
        set(value) {
            colorOverrides[themeVars.fontColor] = value
        }

    /** The [BitmapFont] that this label should use to draw with. */
    var font: BitmapFont
        get() = getThemeFont(themeVars.font)
        set(value) {
            fontOverrides[themeVars.font] = value
            cache = BitmapFontCache(value)
        }

    /** The vertical alignment of the text. */
    var verticalAlign: VAlign = VAlign.BOTTOM
        set(value) {
            if (value == field) return
            field = value
            onMinimumSizeChanged()
        }

    /** The horizontal alignment of the text. */
    var horizontalAlign: HAlign = HAlign.LEFT
        set(value) {
            if (value == field) return
            field = value
            onMinimumSizeChanged()
        }

    /** The label's text. */
    var text: String = ""
        set(value) {
            if (value == field) return
            field = value
            onMinimumSizeChanged()
        }

    /** The overflow text to display if the text is cut off. E.g `...`. */
    var ellipsis: String? = null
        set(value) {
            if (value == field) return
            field = value
            onMinimumSizeChanged()
        }

    /** `true` to allow the text to wrap to the next line. */
    var wrap: Boolean = false
        set(value) {
            if (value == field) return
            field = value
            onMinimumSizeChanged()
        }

    /** Convert the text to all uppercase. */
    var uppercase: Boolean = false
        set(value) {
            if (value == field) return
            field = value
            onMinimumSizeChanged()
        }

    init {
        mouseFilter = MouseFilter.IGNORE
        verticalSizing = SizeFlag.SHRINK_CENTER
    }

    override fun onHierarchyChanged(flag: Int) {
        super.onHierarchyChanged(flag)
        if (flag == SIZE_DIRTY && parent is Container) {
            onMinimumSizeChanged()
        }
    }

    override fun render(batch: Batch, camera: Camera, shapeRenderer: ShapeRenderer) {
        cache.let {
            tempColor.set(color).mul(fontColor)
            it.tint(tempColor)
            if (globalRotation != Angle.ZERO || globalScaleX != 1f || globalScaleY != 1f) {
                applyTransform(batch)
                it.setPosition(0f, 0f)
                it.draw(batch)
                resetTransform(batch)
            } else {
                it.setPosition(globalX, globalY)
                it.draw(batch)
            }
        }
    }

    override fun calculateMinSize() {
        if (!minSizeInvalid) return

        layout()

        _internalMinWidth = layout.width
        _internalMinHeight = layout.height

        minSizeInvalid = false
    }

    private fun layout() {
        val text = if (uppercase) text.uppercase() else text

        var tx = 0f
        var ty = 0f
        val textWidth: Float
        val textHeight: Float

        if (wrap || text.contains("\n")) {
            layout.setText(
                font,
                text,
                Color.WHITE,
                width,
                scaleX = fontScaleX,
                scaleY = fontScaleY,
                align = horizontalAlign,
                wrap = wrap,
                truncate = ellipsis,
            )
            textWidth = layout.width
            textHeight = layout.height

            if (horizontalAlign != HAlign.LEFT) {
                tx +=
                    if (horizontalAlign == HAlign.RIGHT) {
                        width - textWidth
                    } else {
                        (width - textWidth) / 2
                    }
            }
        } else {
            textWidth = width
            textHeight = font.capHeight
        }

        when (verticalAlign) {
            VAlign.TOP -> {
                ty += height
                ty -= textHeight
                ty -= font.metrics.ascent
            }
            VAlign.BOTTOM -> {
                ty += font.metrics.descent
            }
            else -> {
                ty += height * 0.5f
                ty -= textHeight * 0.5f
                ty += font.metrics.descent * 0.5f
            }
        }
        ty = ty.roundToInt().toFloat()

        layout.setText(
            font,
            text,
            Color.WHITE,
            textWidth,
            scaleX = fontScaleX,
            scaleY = fontScaleY,
            horizontalAlign,
            wrap,
            ellipsis,
        )
        cache.setText(layout, tx - originX, ty - originY, fontScaleX, fontScaleY)
        _internalMinWidth = layout.width
        _internalMinHeight = layout.height
    }

    class ThemeVars {
        val fontColor = "fontColor"
        val font = "font"
    }

    companion object {
        private val tempColor = MutableColor()

        /** [Theme] related variable names when setting theme values for a [Label] */
        val themeVars = ThemeVars()
    }
}

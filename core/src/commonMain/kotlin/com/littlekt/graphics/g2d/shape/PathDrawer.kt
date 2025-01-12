package com.littlekt.graphics.g2d.shape

import com.littlekt.graphics.g2d.shape.Joiner.prepareFlatEndpoint
import com.littlekt.graphics.g2d.shape.Joiner.preparePointyJoin
import com.littlekt.graphics.g2d.shape.Joiner.prepareSmoothJoin
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Vec2f
import com.littlekt.math.isFuzzyEqual
import com.littlekt.util.datastructure.FloatArrayList
import com.littlekt.util.datastructure.fastForEach

/**
 * @author Colton Daily
 * @date 7/19/2022
 */
internal class PathDrawer(batchManager: BatchManager, private val lineDrawer: LineDrawer) :
    Drawer(batchManager) {
    private val path = FloatArrayList()
    private val tempPath = FloatArrayList()

    fun path(
        userPath: List<Vec2f>,
        thickness: Float,
        joinType: JoinType,
        open: Boolean = true,
    ) {
        userPath.fastForEach {
            tempPath += it.x
            tempPath += it.y
        }
        path(tempPath.data, thickness, joinType, 0, tempPath.size, open)
        tempPath.clear()
    }

    fun path(
        userPath: FloatArray,
        thickness: Float,
        joinType: JoinType,
        start: Int = 0,
        end: Int = userPath.size,
        open: Boolean = true,
    ) {
        if (userPath.size < 4) return

        path += userPath[start]
        path += userPath[start + 1]
        for (i in start + 2 until end step 2) {
            if (
                !isFuzzyEqual(userPath[i - 2], userPath[i], 0.001f) ||
                    !isFuzzyEqual(userPath[i - 1], userPath[i + 1], 0.001f)
            ) {
                path += userPath[i]
                path += userPath[i + 1]
            }
        }
        if (path.size < 4) {
            path.clear()
            return
        }

        if (path.size == 4) {
            lineDrawer.line(path[0], path[1], path[2], path[3], thickness, false)
            path.clear()
            return
        }

        val wasCaching = batchManager.startCaching()
        if (joinType == JoinType.NONE) {
            drawPathNoJoin(path.data, path.size, thickness, open)
        } else {
            drawPathWithJoin(path.data, path.size, thickness, open, joinType == JoinType.POINTY)
        }
        if (!wasCaching) {
            batchManager.endCaching()
        }
        path.clear()
    }

    fun drawPathNoJoin(path: FloatArray, size: Int, thickness: Float, open: Boolean) {
        val n = if (open) size - 2 else size
        for (i in 0 until n step 2) {
            lineDrawer.line(
                path[i],
                path[i + 1],
                path[(i + 2) % size],
                path[(i + 3) % size],
                thickness,
                false
            )
        }
        if (!open) {
            lineDrawer.line(
                path[size - 2],
                path[size - 1],
                path[0],
                path[1],
                thickness,
                false
            )
        }
    }

    fun drawPathWithJoin(
        path: FloatArray,
        size: Int,
        thickness: Float,
        open: Boolean,
        pointyJoin: Boolean
    ) {
        val halfThickness = 0.5f * thickness
        val color = batchManager.color
        batchManager.ensureSpaceForQuad()

        for (i in 2 until size - 2 step 2) {
            a.set(path[i - 2], path[i - 1])
            b.set(path[i], path[i + 1])
            c.set(path[i + 2], path[i + 3])

            if (pointyJoin) {
                preparePointyJoin(a, b, c, d, e, halfThickness)
            } else {
                prepareSmoothJoin(a, b, c, d, e, halfThickness, false)
            }
            vert3(d)
            vert4(e)

            if (i == 2) {
                if (open) {
                    prepareFlatEndpoint(path[2], path[3], path[0], path[1], d, e, halfThickness)
                    vert1(e)
                    vert2(d)
                } else {
                    vec1.set(path[size - 2], path[size - 1])
                    if (pointyJoin) {
                        preparePointyJoin(vec1, a, b, d0, e0, halfThickness)
                    } else {
                        prepareSmoothJoin(vec1, a, b, d0, e0, halfThickness, true)
                    }
                    vert1(e0)
                    vert2(d0)
                }
            }

            val x3: Float
            val y3: Float
            val x4: Float
            val y4: Float
            if (pointyJoin) {
                x3 = x3()
                y3 = y3()
                x4 = x4()
                y4 = y4()
            } else {
                prepareSmoothJoin(a, b, c, d, e, halfThickness, true)
                x3 = d.x
                y3 = d.y
                x4 = e.x
                y4 = e.y
            }

            color(color, color, color, color)
            batchManager.pushQuad()
            if (!pointyJoin) {
                drawSmoothJoinFill(a, b, c, d, e, halfThickness)
            }
            batchManager.ensureSpaceForQuad()
            vert1(x4, y4)
            vert2(x3, y3)
        }

        if (open) {
            // draw last link on path
            prepareFlatEndpoint(b, c, d, e, halfThickness)
            vert3(e)
            vert4(d)
            color(color, color, color, color)
            batchManager.pushQuad()
        } else {
            if (pointyJoin) {
                // draw last link on path
                a.set(path[0], path[1])
                preparePointyJoin(b, c, a, d, e, halfThickness)
                vert3(d)
                vert4(e)
                color(color, color, color, color)
                batchManager.pushQuad()

                // draw connection back to first vertex
                batchManager.ensureSpaceForQuad()
                vert1(d)
                vert2(e)
                vert3(e0)
                vert4(d0)
                color(color, color, color, color)
                batchManager.pushQuad()
            } else {
                // draw last link on path
                a.set(b)
                b.set(c)
                c.set(path[0], path[1])
                prepareSmoothJoin(a, b, c, d, e, halfThickness, false)
                vert3(d)
                vert4(e)
                color(color, color, color, color)
                batchManager.pushQuad()
                drawSmoothJoinFill(a, b, c, d, e, halfThickness)

                // draw connection back to first vertex
                batchManager.ensureSpaceForQuad()
                prepareSmoothJoin(a, b, c, d, e, halfThickness, true)
                vert3(e)
                vert4(d)
                a.set(path[2], path[3])
                prepareSmoothJoin(b, c, a, d, e, halfThickness, false)
                vert1(d)
                vert2(e)
                color(color, color, color, color)
                batchManager.pushQuad()
                drawSmoothJoinFill(b, c, a, d, e, halfThickness)
            }
        }
    }

    companion object {
        private val d0 = MutableVec2f()
        private val e0 = MutableVec2f()
    }
}

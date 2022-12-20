package com.lehaine.littlekt.math.spatial

import com.lehaine.littlekt.graphics.IndexedVertexList
import com.lehaine.littlekt.graphics.PrimitiveType
import com.lehaine.littlekt.math.MutableVec3f
import com.lehaine.littlekt.math.Ray
import com.lehaine.littlekt.math.Vec3f
import com.lehaine.littlekt.math.isFuzzyZero
import kotlin.math.min

open class Edge<T : Vec3f>(val pt0: T, val pt1: T) {
    val e: Vec3f
    val length: Float

    val minX: Float
    val minY: Float
    val minZ: Float
    val maxX: Float
    val maxY: Float
    val maxZ: Float

    private val tmpVec = MutableVec3f()
    private val tmpResult = MutableVec3f()

    init {
        e = pt1.subtract(pt0, MutableVec3f()).norm()
        length = pt0.distance(pt1)

        minX = minOf(pt0.x, pt1.x)
        minY = minOf(pt0.y, pt1.y)
        minZ = minOf(pt0.z, pt1.z)
        maxX = maxOf(pt0.x, pt1.x)
        maxY = maxOf(pt0.y, pt1.y)
        maxZ = maxOf(pt0.z, pt1.z)
    }

    open fun rayDistanceSqr(ray: Ray): Float {
        return ray.sqrDistanceToPoint(nearestPointOnEdge(ray, tmpResult))
    }

    open fun nearestPointOnEdge(ray: Ray, result: MutableVec3f): MutableVec3f {
        val dot = e * ray.direction
        val n = 1f - dot * dot
        if (n.isFuzzyZero()) {
            // edge and ray are parallel
            return result.set(if (pt0.sqrDistance(ray.origin) < pt1.sqrDistance(ray.origin)) pt0 else pt1)
        }

        ray.origin.subtract(pt0, tmpVec)
        val a = tmpVec * e
        val b = tmpVec * ray.direction
        val l = (a - b * dot) / n
        return if (l > 0) e.scale(min(l, length), result).add(pt0) else result.set(pt0)
    }

    open fun nearestPointOnEdge(point: Vec3f, result: MutableVec3f): MutableVec3f {
        pt1.subtract(pt0, result)
        val l = (point * result - pt0 * result) / (result * result)
        when {
            l < 0f -> result.set(pt0)
            l > 1f -> result.set(pt1)
            else -> result.scale(l).add(pt0)
        }
        return result
    }

    companion object {
        fun getEdges(lineMeshData: IndexedVertexList): List<Edge<Vec3f>> {
            if (lineMeshData.primitiveType != PrimitiveType.LINES) {
                throw IllegalArgumentException("Supplied meshData must have primitiveType GL_LINES")
            }
            val edges = mutableListOf<Edge<Vec3f>>()
            for (i in 0 until lineMeshData.numIndices step 2) {
                val i0 = lineMeshData.indices[i]
                val i1 = lineMeshData.indices[i + 1]
                val p0 = Vec3f(lineMeshData.vertexIt.apply { index = i0 })
                val p1 = Vec3f(lineMeshData.vertexIt.apply { index = i1 })
                edges += Edge(p0, p1)
            }
            return edges
        }
    }
}
package com.littlekt.graphics

import com.littlekt.graphics.util.IndexedMeshGeometry
import com.littlekt.graphics.webgpu.BufferUsage
import com.littlekt.graphics.webgpu.Device
import kotlin.math.min

/**
 * A wrapper that handles the creation of a vertex GPU buffer and a index GPU buffer as well as
 * uploading data to these buffers, and recreating them if they run out of size.
 *
 * @param device the current device
 * @param geometry the underlying [IndexedMeshGeometry] of this mesh for uploading data.
 * @author Colton Daily
 * @date 4/10/2024
 */
class IndexedMesh<T : IndexedMeshGeometry>(device: Device, geometry: T) :
    Mesh<T>(device, geometry) {

    /** The GPU index buffer for this mesh, if created. */
    var ibo =
        device.createGPUShortBuffer(
            "ibo",
            geometry.indices,
            BufferUsage.INDEX or BufferUsage.COPY_DST
        )
        private set

    init {
        val size = min(ibo.size / Short.SIZE_BYTES, geometry.indices.limit.toLong())
        logger.trace { "Writing IBO to queue of size: $size" }
        device.queue.writeBuffer(ibo, geometry.indices, size = size)
        geometry.indicesDirty = false
    }

    /**
     * Update the vertex and index buffers, if the underlying geometry has changed. If the
     * underlying geometry is bigger than the GPU buffers, the GPU buffers will be recreated and the
     * new data copied over.
     */
    override fun update() {
        if (geometry.dirty) {
            if (geometry.verticesDirty) {
                if (vbo.size < geometry.vertices.capacity) {
                    logger.trace {
                        "Destroying and creating VBO from size: ${vbo.size} to  size: ${geometry.vertices.capacity}"
                    }
                    vbo.release()
                    vbo =
                        device.createGPUByteBuffer(
                            "vbo",
                            geometry.vertices,
                            BufferUsage.VERTEX or BufferUsage.COPY_DST
                        )
                    // need to remake ibo because indices won't correspond correctly to the new
                    // vertices
                    destroyAndRecreateIbo()
                } else {
                    val size =
                        min(
                            vbo.size,
                            geometry.numVertices *
                                    geometry.vertexStride.toLong()
                        )
                    logger.trace { "Writing VBO to queue of size: $size" }
                    device.queue.writeBuffer(vbo, geometry.vertices, size = size)
                }
                geometry.verticesDirty = false
            }
            if (geometry.indicesDirty) {
                if (ibo.size < geometry.indices.capacity * Short.SIZE_BYTES) {
                    destroyAndRecreateIbo()
                } else {
                    val size = min(ibo.size / Short.SIZE_BYTES, geometry.indices.limit.toLong())
                    logger.trace { "Writing IBO to queue of size: $size" }
                    device.queue.writeBuffer(ibo, geometry.indices, size = size)
                }
                geometry.indicesDirty = false
            }
        }
    }

    private fun destroyAndRecreateIbo() {
        logger.trace { "Destroy and recreating IBO." }
        ibo.release()
        if (geometry.indicesType == IndexedMeshGeometry.IndicesType.QUAD) {
            logger.trace { "Regenerating indices as quads." }
            geometry.indicesAsQuad()
        } else if (geometry.indicesType == IndexedMeshGeometry.IndicesType.TRI) {
            logger.trace { "Regenerating indices as tris." }
            geometry.indicesAsTri()
        }
        logger.trace { "Creating IBO of size: ${geometry.indices.capacity}" }
        ibo =
            device.createGPUShortBuffer(
                "ibo",
                geometry.indices,
                BufferUsage.INDEX or BufferUsage.COPY_DST
            )
    }

    override fun release() {
        vbo.release()
        ibo.release()
    }
}

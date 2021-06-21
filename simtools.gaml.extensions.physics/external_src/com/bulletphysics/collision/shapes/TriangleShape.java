/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library Copyright (c) 2003-2008 Erwin Coumans
 * http://www.bulletphysics.com/
 *
 * This software is provided 'as-is', without any express or implied warranty. In no event will the authors be held
 * liable for any damages arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose, including commercial applications, and to alter
 * it and redistribute it freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software.
 * If you use this software in a product, an acknowledgment in the product documentation would be appreciated but is not
 * required. 2. Altered source versions must be plainly marked as such, and must not be misrepresented as being the
 * original software. 3. This notice may not be removed or altered from any source distribution.
 */

package com.bulletphysics.collision.shapes;

import static com.bulletphysics.Pools.VECTORS;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.broadphase.BroadphaseNativeType;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.linearmath.VectorUtil;

/**
 * Single triangle shape.
 *
 * @author jezek2
 */
public class TriangleShape extends PolyhedralConvexShape {

	public final Vector3f[] vertices1/* [3] */ = new Vector3f[] { new Vector3f(), new Vector3f(), new Vector3f() };

	// JAVA NOTE: added
	public TriangleShape() {}

	public TriangleShape(final Vector3f p0, final Vector3f p1, final Vector3f p2) {
		vertices1[0].set(p0);
		vertices1[1].set(p1);
		vertices1[2].set(p2);
	}

	// JAVA NOTE: added
	public void init(final Vector3f p0, final Vector3f p1, final Vector3f p2) {
		vertices1[0].set(p0);
		vertices1[1].set(p1);
		vertices1[2].set(p2);
	}

	@Override
	public int getNumVertices() {
		return 3;
	}

	public Vector3f getVertexPtr(final int index) {
		return vertices1[index];
	}

	@Override
	public void getVertex(final int index, final Vector3f vert) {
		vert.set(vertices1[index]);
	}

	@Override
	public BroadphaseNativeType getShapeType() {
		return BroadphaseNativeType.TRIANGLE_SHAPE_PROXYTYPE;
	}

	@Override
	public int getNumEdges() {
		return 3;
	}

	@Override
	public void getEdge(final int i, final Vector3f pa, final Vector3f pb) {
		getVertex(i, pa);
		getVertex((i + 1) % 3, pb);
	}

	@Override
	public void getAabb(final Transform t, final Vector3f aabbMin, final Vector3f aabbMax) {
		// btAssert(0);
		getAabbSlow(t, aabbMin, aabbMax);
	}

	@Override
	public Vector3f localGetSupportingVertexWithoutMargin(final Vector3f dir, final Vector3f out) {
		Vector3f dots = VECTORS.get();
		dots.set(dir.dot(vertices1[0]), dir.dot(vertices1[1]), dir.dot(vertices1[2]));
		out.set(vertices1[VectorUtil.maxAxis(dots)]);
		VECTORS.release(dots);

		return out;
	}

	@Override
	public void batchedUnitVectorGetSupportingVertexWithoutMargin(final Vector3f[] vectors,
			final Vector3f[] supportVerticesOut, final int numVectors) {
		Vector3f dots = VECTORS.get();

		for (int i = 0; i < numVectors; i++) {
			Vector3f dir = vectors[i];
			dots.set(dir.dot(vertices1[0]), dir.dot(vertices1[1]), dir.dot(vertices1[2]));
			supportVerticesOut[i].set(vertices1[VectorUtil.maxAxis(dots)]);
		}
		VECTORS.release(dots);
	}

	@Override
	public void getPlane(final Vector3f planeNormal, final Vector3f planeSupport, final int i) {
		getPlaneEquation(i, planeNormal, planeSupport);
	}

	@Override
	public int getNumPlanes() {
		return 1;
	}

	public void calcNormal(final Vector3f normal) {
		Vector3f tmp1 = VECTORS.get();
		Vector3f tmp2 = VECTORS.get();

		tmp1.sub(vertices1[1], vertices1[0]);
		tmp2.sub(vertices1[2], vertices1[0]);

		normal.cross(tmp1, tmp2);
		normal.normalize();
		VECTORS.release(tmp1, tmp2);
	}

	public void getPlaneEquation(final int i, final Vector3f planeNormal, final Vector3f planeSupport) {
		calcNormal(planeNormal);
		planeSupport.set(vertices1[0]);
	}

	@Override
	public void calculateLocalInertia(final float mass, final Vector3f inertia) {
		assert false;
		inertia.set(0f, 0f, 0f);
	}

	@Override
	public boolean isInside(final Vector3f pt, final float tolerance) {
		Vector3f normal = VECTORS.get();
		try {
			calcNormal(normal);
			// distance to plane
			float dist = pt.dot(normal);
			float planeconst = vertices1[0].dot(normal);
			dist -= planeconst;
			if (dist >= -tolerance && dist <= tolerance) {
				// inside check on edge-planes
				int i;
				for (i = 0; i < 3; i++) {
					Vector3f pa = VECTORS.get(), pb = VECTORS.get();
					Vector3f edgeNormal = VECTORS.get();
					Vector3f edge = VECTORS.get();
					try {
						getEdge(i, pa, pb);
						edge.sub(pb, pa);
						edgeNormal.cross(edge, normal);
						edgeNormal.normalize();
						/* float */ dist = pt.dot(edgeNormal);
						float edgeConst = pa.dot(edgeNormal);
						dist -= edgeConst;
						if (dist < -tolerance) return false;
					} finally {
						VECTORS.release(pa, edgeNormal, edge, pb);
					}
				}

				return true;
			}

			return false;
		} finally {
			VECTORS.release(normal);
		}
	}

	@Override
	public String getName() {
		return "Triangle";
	}

	@Override
	public int getNumPreferredPenetrationDirections() {
		return 2;
	}

	@Override
	public void getPreferredPenetrationDirection(final int index, final Vector3f penetrationVector) {
		calcNormal(penetrationVector);
		if (index != 0) { penetrationVector.scale(-1f); }
	}

}

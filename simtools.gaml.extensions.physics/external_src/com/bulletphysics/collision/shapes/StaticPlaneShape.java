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
import com.bulletphysics.collision.broadphase.DispatcherInfo;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.linearmath.TransformUtil;
import com.bulletphysics.linearmath.VectorUtil;

/**
 * StaticPlaneShape simulates an infinite non-moving (static) collision plane.
 *
 * @author jezek2
 */
public class StaticPlaneShape extends ConcaveShape {

	protected final Vector3f localAabbMin = new Vector3f();
	protected final Vector3f localAabbMax = new Vector3f();

	protected final Vector3f planeNormal = new Vector3f();
	protected float planeConstant;
	protected final Vector3f localScaling = new Vector3f(0f, 0f, 0f);

	public StaticPlaneShape(final Vector3f planeNormal, final float planeConstant) {
		this.planeNormal.normalize(planeNormal);
		this.planeConstant = planeConstant;
	}

	public Vector3f getPlaneNormal(final Vector3f out) {
		out.set(planeNormal);
		return out;
	}

	public float getPlaneConstant() {
		return planeConstant;
	}

	@Override
	public void processAllTriangles( final TriangleCallback callback, final Vector3f aabbMin,
			final Vector3f aabbMax) {
		Vector3f tmp = VECTORS.get();
		Vector3f tmp1 = VECTORS.get();
		Vector3f tmp2 = VECTORS.get();
		Vector3f center = VECTORS.get();
		Vector3f halfExtents = VECTORS.get();
		Vector3f tangentDir0 = VECTORS.get(), tangentDir1 = VECTORS.get();
		Vector3f projectedCenter = VECTORS.get();
		Vector3f[] triangle = new Vector3f[] { VECTORS.get(), VECTORS.get(), VECTORS.get() };

		try {
			halfExtents.sub(aabbMax, aabbMin);
			halfExtents.scale(0.5f);
			float radius = halfExtents.length();
			center.add(aabbMax, aabbMin);
			center.scale(0.5f);
			// this is where the triangles are generated, given AABB and plane equation (normal/constant)
			// tangentDir0/tangentDir1 can be precalculated
			TransformUtil.planeSpace1(planeNormal, tangentDir0, tangentDir1);
			// Vector3f supVertex0 = VECTORS.get(), supVertex1 = VECTORS.get();
			tmp.scale(planeNormal.dot(center) - planeConstant, planeNormal);
			projectedCenter.sub(center, tmp);
			tmp1.scale(radius, tangentDir0);
			tmp2.scale(radius, tangentDir1);
			VectorUtil.add(triangle[0], projectedCenter, tmp1, tmp2);
			tmp1.scale(radius, tangentDir0);
			tmp2.scale(radius, tangentDir1);
			tmp.sub(tmp1, tmp2);
			VectorUtil.add(triangle[1], projectedCenter, tmp);
			tmp1.scale(radius, tangentDir0);
			tmp2.scale(radius, tangentDir1);
			tmp.sub(tmp1, tmp2);
			triangle[2].sub(projectedCenter, tmp);
			callback.processTriangle( triangle, 0, 0);
			tmp1.scale(radius, tangentDir0);
			tmp2.scale(radius, tangentDir1);
			tmp.sub(tmp1, tmp2);
			triangle[0].sub(projectedCenter, tmp);
			tmp1.scale(radius, tangentDir0);
			tmp2.scale(radius, tangentDir1);
			tmp.add(tmp1, tmp2);
			triangle[1].sub(projectedCenter, tmp);
			tmp1.scale(radius, tangentDir0);
			tmp2.scale(radius, tangentDir1);
			VectorUtil.add(triangle[2], projectedCenter, tmp1, tmp2);
			callback.processTriangle( triangle, 0, 1);
		} finally {
			VECTORS.release(triangle[0], triangle[1], triangle[2], projectedCenter, tangentDir0, tangentDir1,
					halfExtents, center, tmp2, tmp1, tmp);
		}
	}

	@Override
	public void getAabb(final Transform t, final Vector3f aabbMin, final Vector3f aabbMax) {
		aabbMin.set(-1e30f, -1e30f, -1e30f);
		aabbMax.set(1e30f, 1e30f, 1e30f);
	}

	@Override
	public BroadphaseNativeType getShapeType() {
		return BroadphaseNativeType.STATIC_PLANE_PROXYTYPE;
	}

	@Override
	public void setLocalScaling( final Vector3f scaling) {
		localScaling.set(scaling);
	}

	@Override
	public Vector3f getLocalScaling(final Vector3f out) {
		out.set(localScaling);
		return out;
	}

	@Override
	public void calculateLocalInertia(final float mass, final Vector3f inertia) {
		// moving concave objects not supported
		inertia.set(0f, 0f, 0f);
	}

	@Override
	public String getName() {
		return "STATICPLANE";
	}

}

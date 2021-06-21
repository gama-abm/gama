/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * This source file is part of GIMPACT Library.
 *
 * For the latest info, see http://gimpact.sourceforge.net/
 *
 * Copyright (c) 2007 Francisco Leon Najera. C.C. 80087371. email: projectileman@yahoo.com
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

package com.bulletphysics.extras.gimpact;

import static com.bulletphysics.Pools.AABBS;
import static com.bulletphysics.Pools.VECTORS;

import java.util.ArrayList;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.broadphase.DispatcherInfo;
import com.bulletphysics.collision.dispatch.CollisionWorld.RayResultCallback;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.StridingMeshInterface;
import com.bulletphysics.collision.shapes.TriangleCallback;
import com.bulletphysics.extras.gimpact.BoxCollision.AABB;
import com.bulletphysics.linearmath.Transform;

/**
 *
 * @author jezek2
 */
public class GImpactMeshShape extends GImpactShapeInterface {

	protected ArrayList<GImpactMeshShapePart> mesh_parts = new ArrayList<>();

	public GImpactMeshShape(final StridingMeshInterface meshInterface) {
		buildMeshParts(meshInterface);
	}

	public int getMeshPartCount() {
		return mesh_parts.size();
	}

	public GImpactMeshShapePart getMeshPart(final int index) {
		return mesh_parts.get(index);
	}

	@Override
	public void setLocalScaling( final Vector3f scaling) {
		localScaling.set(scaling);

		int i = mesh_parts.size();
		while (i-- != 0) {
			GImpactMeshShapePart part = mesh_parts.get(i);
			part.setLocalScaling( scaling);
		}

		needs_update = true;
	}

	@Override
	public void setMargin(final float margin) {
		collisionMargin = margin;

		int i = mesh_parts.size();
		while (i-- != 0) {
			GImpactMeshShapePart part = mesh_parts.get(i);
			part.setMargin(margin);
		}

		needs_update = true;
	}

	@Override
	public void postUpdate() {
		int i = mesh_parts.size();
		while (i-- != 0) {
			GImpactMeshShapePart part = mesh_parts.get(i);
			part.postUpdate();
		}

		needs_update = true;
	}

	@Override
	public void calculateLocalInertia(final float mass, final Vector3f inertia) {
		// #ifdef CALC_EXACT_INERTIA
		inertia.set(0f, 0f, 0f);

		int i = getMeshPartCount();
		float partmass = mass / i;

		Vector3f partinertia = VECTORS.get();

		while (i-- != 0) {
			getMeshPart(i).calculateLocalInertia(partmass, partinertia);
			inertia.add(partinertia);
		}

		VECTORS.release(partinertia);
		//// #else
		//
		//// Calc box inertia
		//
		// btScalar lx= m_localAABB.m_max[0] - m_localAABB.m_min[0];
		// btScalar ly= m_localAABB.m_max[1] - m_localAABB.m_min[1];
		// btScalar lz= m_localAABB.m_max[2] - m_localAABB.m_min[2];
		// const btScalar x2 = lx*lx;
		// const btScalar y2 = ly*ly;
		// const btScalar z2 = lz*lz;
		// const btScalar scaledmass = mass * btScalar(0.08333333);
		//
		// inertia = scaledmass * (btVector3(y2+z2,x2+z2,x2+y2));
		//// #endif
	}

	@Override
	PrimitiveManagerBase getPrimitiveManager() {
		assert false;
		return null;
	}

	@Override
	public int getNumChildShapes() {
		assert false;
		return 0;
	}

	@Override
	public boolean childrenHasTransform() {
		assert false;
		return false;
	}

	@Override
	public boolean needsRetrieveTriangles() {
		assert false;
		return false;
	}

	@Override
	public boolean needsRetrieveTetrahedrons() {
		assert false;
		return false;
	}

	@Override
	public void getBulletTriangle(final int prim_index, final TriangleShapeEx triangle) {
		assert false;
	}

	@Override
	void getBulletTetrahedron(final int prim_index, final TetrahedronShapeEx tetrahedron) {
		assert false;
	}

	@Override
	public void lockChildShapes() {
		assert false;
	}

	@Override
	public void unlockChildShapes() {
		assert false;
	}

	@Override
	public void getChildAabb(final int child_index, final Transform t, final Vector3f aabbMin, final Vector3f aabbMax) {
		assert false;
	}

	@Override
	public CollisionShape getChildShape(final int index) {
		assert false;
		return null;
	}

	@Override
	public Transform getChildTransform(final int index) {
		assert false;
		return null;
	}

	@Override
	public void setChildTransform(final int index, final Transform transform) {
		assert false;
	}

	@Override
	ShapeType getGImpactShapeType() {
		return ShapeType.TRIMESH_SHAPE;
	}

	@Override
	public String getName() {
		return "GImpactMesh";
	}

	@Override
	public void rayTest(final Vector3f rayFrom, final Vector3f rayTo, final RayResultCallback resultCallback) {}

	@Override
	public void processAllTriangles( final TriangleCallback callback, final Vector3f aabbMin,
			final Vector3f aabbMax) {
		int i = mesh_parts.size();
		while (i-- != 0) {
			mesh_parts.get(i).processAllTriangles( callback, aabbMin, aabbMax);
		}
	}

	protected void buildMeshParts(final StridingMeshInterface meshInterface) {
		for (int i = 0; i < meshInterface.getNumSubParts(); i++) {
			GImpactMeshShapePart newpart = new GImpactMeshShapePart(meshInterface, i);
			mesh_parts.add(newpart);
		}
	}

	@Override
	protected void calcLocalAABB() {
		AABB tmpAABB = AABBS.get();

		localAABB.invalidate();
		int i = mesh_parts.size();
		while (i-- != 0) {
			mesh_parts.get(i).updateBound();
			localAABB.merge(mesh_parts.get(i).getLocalBox(tmpAABB));
		}
	}

}

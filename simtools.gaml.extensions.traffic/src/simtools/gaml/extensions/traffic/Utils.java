package simtools.gaml.extensions.traffic;

import static simtools.gaml.extensions.traffic.DrivingSkill.getCurrentTarget;
import static simtools.gaml.extensions.traffic.DrivingSkill.getDistanceToGoal;
import static simtools.gaml.extensions.traffic.DrivingSkill.getMinSafetyDistance;
import static simtools.gaml.extensions.traffic.DrivingSkill.getNextRoad;
import static simtools.gaml.extensions.traffic.DrivingSkill.getNumLanesOccupied;
import static simtools.gaml.extensions.traffic.DrivingSkill.getVehicleLength;
import static simtools.gaml.extensions.traffic.DrivingSkill.isViolatingOneway;
import static simtools.gaml.extensions.traffic.DrivingSkill.readyToCross;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.OrderedBidiMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.locationtech.jts.geom.Coordinate;

import msi.gama.metamodel.agent.IAgent;
import msi.gama.metamodel.shape.GamaPoint;
import msi.gama.runtime.IScope;

public class Utils {
	/**
	 * Attempts to make lane changing probabilities timestep-agnostic
	 *
	 * @param probaInOneSecond a probability with respect to one second
	 * @param timeStep         the duration of a simulation step
	 * @return the rescaled probability
	 */
	public static double rescaleProba(final double probaInOneSecond,
			final double timeStep) {
		return Math.min(probaInOneSecond * timeStep, 1.0);
	}

	public static Triple<IAgent, Double, Boolean> findLeader(final IScope scope,
										final IAgent vehicle,
										final IAgent target,
										final IAgent road,
										final int segment,
										final int lowestLane,
										final double distToSegmentEnd,
										final double distToCurrentTarget) {
		double vL = getVehicleLength(vehicle);
		double minSafetyDist = getMinSafetyDistance(vehicle);
		boolean violatingOneway = isViolatingOneway(vehicle);
		int numRoadLanes = RoadSkill.getNumLanes(road);
		int numLanesOccupied = getNumLanesOccupied(vehicle);

		double tmpDistQuery;
		IAgent leader = null;
		double minGap = Double.MAX_VALUE;
		boolean sameDirection = false;
		for (int i = 0; i < numLanesOccupied; i += 1) {
			int lane = lowestLane + i;
			OrderedBidiMap<Double, IAgent> distMap = 
					RoadSkill.getVehiclesOnLaneSegment(scope, road, lane).inverseBidiMap();
			boolean wrongDirection = lane < numRoadLanes ? false : true;
			wrongDirection = violatingOneway ? !wrongDirection : wrongDirection;
			tmpDistQuery = !wrongDirection ? distToCurrentTarget : 
					RoadSkill.getTotalLength(road) - distToCurrentTarget;
			// Dirty workaround for prev/nextKey() not working when key is not present:
			// we insert the vehicle into the other lane even though it's not on that lane yet
			// and then remove it later
			boolean inserted = false;
			if (!distMap.containsKey(tmpDistQuery)) {
				distMap.put(tmpDistQuery, vehicle);
				inserted = true;
			}
			
			Double k = !wrongDirection ? distMap.previousKey(tmpDistQuery)
					: distMap.nextKey(tmpDistQuery);
			if (inserted) {
				distMap.remove(tmpDistQuery);
			}
			// No leader on this lane
			if (k == null) {
				continue;
			} 
			double tmpLeaderDist = k;
			IAgent tmpLeader = distMap.get(tmpLeaderDist);
			if (tmpLeader == null || tmpLeader.dead()) {
				continue;
			}
			double otherVL = getVehicleLength(tmpLeader);
			double gap = Math.abs(tmpLeaderDist - tmpDistQuery) - 0.5 * vL - 0.5 * otherVL;

			if (gap < 0) {
				return null;
			} else if (gap < minGap) {
				minGap = gap;
				leader = tmpLeader;
				sameDirection = target == getCurrentTarget(tmpLeader);
			}
			
		}
		if (leader != null && !leader.dead()) {
			 return ImmutableTriple.of(leader, minGap, sameDirection);
		}

		// The methods continue down here if no leading vehicle is found on the current road

		IAgent nextRoad = getNextRoad(vehicle);
		// Check if vehicle is approaching an intersection
		// Slowing down at final target, since at this point we don't know which road will be taken next
		// OR might need to slow down at the intersection if it is not possible to enter the next road
		if (nextRoad == null || !readyToCross(scope, vehicle, target, nextRoad)) {
			// Return a virtual leading vehicle of length 0 to simulate deceleration at intersections
			// NOTE: the added minSafetyDist is necessary for the vehicle to ignore the safety dist when stopping at an endpoint
			// TODO: make the vehicles stop in front of the lights,
			// this would require changes in the drive loop as well
			return ImmutableTriple.of(target, distToCurrentTarget + minSafetyDist, false);
		} else if (nextRoad != road) {
			boolean willViolateOneway = target == RoadSkill.getTargetNode(nextRoad);
			IAgent nextTarget = !willViolateOneway ?
				RoadSkill.getTargetNode(nextRoad) : RoadSkill.getSourceNode(nextRoad);
			int numLanesNext = RoadSkill.getNumLanes(nextRoad);
			int numLanesTotalNext = RoadSkill.getNumLanesTotal(nextRoad);
			int lowestLaneToCheck = Math.min(lowestLane, numLanesTotalNext - numLanesOccupied);
			
			for (int i = 0; i < numLanesOccupied; i += 1) {
				int lane = lowestLaneToCheck + i;
				OrderedBidiMap<Double, IAgent> distMap = 
						RoadSkill.getVehiclesOnLaneSegment(scope, nextRoad, lane).inverseBidiMap();
				boolean wrongDirection = lane < numLanesNext ? false : true;
				wrongDirection = willViolateOneway ? !wrongDirection : wrongDirection;

				if (distMap.isEmpty()) {
					continue;
				}
				
				double tmpLeaderDist = !wrongDirection ? distMap.lastKey() : distMap.firstKey();
				IAgent tmpLeader = distMap.get(tmpLeaderDist);
				if (tmpLeader == null || tmpLeader.dead()) {
					continue;
				}
				double extraGap = !wrongDirection ? 
						RoadSkill.getTotalLength(nextRoad) - tmpLeaderDist : tmpLeaderDist;
				double otherVL = getVehicleLength(tmpLeader);
				double gap = distToCurrentTarget + extraGap - 0.5 * vL - 0.5 * otherVL;
				
				if (gap < 0) {
					return null;
				}
				if (gap < minGap) {
					minGap = gap;
					leader = tmpLeader;
					sameDirection = nextTarget == getCurrentTarget(tmpLeader);
				}
			}
		}	

		if (leader != null) {
			return ImmutableTriple.of(leader, minGap, sameDirection);
		} else {
			// the road ahead seems to be completely clear
			return ImmutableTriple.of(null, 1e6, false);
		}
	}
	
	
	public static Triple<IAgent, Double, Boolean> findFollower(final IScope scope,
										final IAgent vehicle,
										final IAgent target,
										final IAgent road,
										final int segment,
										final int lowestLane,
										final double distToSegmentEnd,
										final double distToCurrentTarget) {
		double vL = getVehicleLength(vehicle);
		boolean violatingOneway = isViolatingOneway(vehicle);
		int numRoadLanes = RoadSkill.getNumLanes(road);
		int numLanesOccupied = getNumLanesOccupied(vehicle);

		double tmpDistQuery;
		IAgent follower = null;
		double minGap = Double.MAX_VALUE;
		boolean sameDirection = false;
		for (int i = 0; i < numLanesOccupied; i += 1) {
			int lane = lowestLane + i;
			OrderedBidiMap<Double, IAgent> distMap = 
					RoadSkill.getVehiclesOnLaneSegment(scope, road, lane).inverseBidiMap();
			boolean wrongDirection = lane < numRoadLanes ? false : true;
			wrongDirection = violatingOneway ? !wrongDirection : wrongDirection;
			tmpDistQuery = !wrongDirection ? distToCurrentTarget : 
					RoadSkill.getTotalLength(road) - distToCurrentTarget;
			// Dirty workaround for prev/nextKey() not working when key is not present:
			// we insert the vehicle into the other lane even though it's not on that lane yet
			// and then remove it later
			boolean inserted = false;
			if (!distMap.containsKey(tmpDistQuery)) {
				distMap.put(tmpDistQuery, vehicle);
				inserted = true;
			}
			
			Double k = !wrongDirection ? distMap.nextKey(tmpDistQuery)
					: distMap.previousKey(tmpDistQuery);
			if (inserted) {
				distMap.remove(tmpDistQuery);
			}
			// No follower on this lane
			if (k == null) {
				continue;
			} 
			double tmpLeaderDist = k;
			IAgent tmpFollower = distMap.get(tmpLeaderDist);
			if (tmpFollower == null || tmpFollower.dead()) {
				continue;
			}
			double otherVL = getVehicleLength(tmpFollower);
			double gap = Math.abs(tmpLeaderDist - tmpDistQuery) - 0.5 * vL - 0.5 * otherVL;

			if (gap < 0) {
				return null;
			} else if (gap < minGap) {
				minGap = gap;
				follower = tmpFollower;
				sameDirection = target == getCurrentTarget(tmpFollower);
			}
			
		}
		if (follower != null && !follower.dead()) {
			return ImmutableTriple.of(follower, minGap, sameDirection);
		} else {
			//TODO: find followers on previous roads
			return ImmutableTriple.of(null, null, null);
		}
	}

	/**
	 * Find the leading vehicle (closest vehicle ahead) and the back vehicle
	 * (closest vehicle behind) which are moving on the same lanes as the current
	 * vehicle.
	 *
	 * For each of the above vehicle, the method returns a triplet containing:
	 *    1. The vehicle agent itself
	 *    2. The bumper-to-bumper gap between that vehicle and the current vehicle
	 *    3. Whether that vehicle is moving in the same direction
	 *
	 * If no leading vehicle is found on the current segment,
	 * it tries to find one in the next segment or the first segment of the next road.
	 * If none is found still, the bumper-to-bumper gap is set to a big value (to eliminate the deceleration term in IDM).
	 *
	 * On the other hand, if no back vehicle is found on the current segment,
	 * the method does NOT consider the previous segment or the last segment of the previous road.
	 * (a possible TODO?)
	 *
	 * If either the leading or back vehicle overlaps with the current one, the return value will be null.
	 *
	 * @param scope
	 * @param vehicle          the vehicle whose leading and back vehicle is to be found
	 * @param target           the target of the concerned vehicle
	 * @param road             the road which the vehicle is moving on
	 * @param segment          the index of the current road segment that the vehicle is on
	 * @param distToSegmentEnd the distance from the vehicle to the segment endpoint
	 * @param lowestLane       the current "lowest" lane index of the vehicle
	 * @return a pair containing two triplets, one for the leading vehicle and one
	 *         for the back vehicle
	 */
//	public static ImmutablePair<Triple<IAgent, Double, Boolean>, Triple<IAgent, Double, Boolean>>
//			findLeadingAndBackVehicle(final IScope scope,
//										final IAgent vehicle,
//										final IAgent target,
//										final IAgent road,
//										final int segment,
//										final double distToSegmentEnd,
//										final int lowestLane) {
//		double vL = getVehicleLength(vehicle);
//		double minSafetyDist = getMinSafetyDistance(vehicle);
//		boolean violatingOneway = isViolatingOneway(vehicle);
//
//		int endPtIdx = !violatingOneway ? segment + 1 : segment;
//		GamaPoint segmentEndPt = new GamaPoint(road.getInnerGeometry().getCoordinates()[endPtIdx]);
//
//		int numLanesOccupied = getNumLanesOccupied(vehicle);
//		Set<IAgent> neighbors = new HashSet<>();
//		for (int i = 0; i < numLanesOccupied; i += 1) {
//			neighbors.addAll(
//				RoadSkill.getVehiclesOnLaneSegment(scope, road, lowestLane + i, segment)
//			);
//		}
//
//		// finding the closest vehicle ahead & behind
//		IAgent leadingVehicle = null;
//		double minLeadingDist = Double.MAX_VALUE;
//		boolean leadingSameDirection = false;
//		IAgent backVehicle = null;
//		double minBackDist = Double.MAX_VALUE;
//		boolean backSameDirection = false;
//		Triple<IAgent, Double, Boolean> leadingTriple;
//		Triple<IAgent, Double, Boolean> backTriple;
//
//		// TODO: rework this spaghetti bowl
//		// Should probably construct a map of Agent -> gapBetweenCentroids first 
//		// (including agents from other segments & roads),
//		// then iterate through the keys to find leader & follower
//		for (IAgent otherVehicle : neighbors) {
//			if (otherVehicle == vehicle || otherVehicle == null || otherVehicle.dead()) {
//				continue;
//			}
//			boolean sameDirection = target == getCurrentTarget(otherVehicle);
//			double otherVL = getVehicleLength(otherVehicle);
//			double otherDistToSegmentEnd;
//			if (sameDirection) {
//				otherDistToSegmentEnd = getDistanceToGoal(otherVehicle);
//			} else {
//				GamaPoint otherLocation = (GamaPoint) otherVehicle.getLocation();
//				otherDistToSegmentEnd = otherLocation.distance(segmentEndPt);
//			}
//
//			// Calculate bumper-to-bumper distances
//			double gapBetweenCentroids = distToSegmentEnd - otherDistToSegmentEnd;
//			double gap = Math.abs(gapBetweenCentroids) - 0.5 * vL - 0.5 * otherVL;
//
//			if (Math.abs(gapBetweenCentroids) < 0.5 * vL + 0.5 * otherVL) {
//				// Overlap with another vehicle
//				return null;
//			} else if (gapBetweenCentroids > 0 && gap < minLeadingDist) {
//				leadingVehicle = otherVehicle;
//				minLeadingDist = Math.abs(gap);
//				leadingSameDirection = sameDirection;
//			} else if (gapBetweenCentroids < 0 && gap < minBackDist) {
//				backVehicle = otherVehicle;
//				minBackDist = Math.abs(gap);
//				backSameDirection = sameDirection;
//			}
//		}
//
//		if (backVehicle == null) {
//			int numSegments = RoadSkill.getNumSegments(road);
//			// TODO: check for previous roads as well (all roads connected to last target?)
//			if ((!violatingOneway && segment > 0) ||
//					(violatingOneway && segment < numSegments - 1)) {
//				int prevSegment;
//				Coordinate prevPoint;
//				Coordinate coords[] = road.getInnerGeometry().getCoordinates();
//				if (!violatingOneway) {
//					prevSegment = segment - 1;
//					prevPoint = coords[segment];
//				} else {
//					prevSegment = segment + 1;
//					prevPoint = coords[segment + 1];
//				}
//				GamaPoint loc = (GamaPoint) vehicle.getLocation();
//				double distToPrevTarget = loc.distance(prevPoint);
//				Set<IAgent> prevNeighbors = new HashSet<>();
//				for (int i = 0; i < numLanesOccupied; i += 1) {
//					prevNeighbors.addAll(
//						RoadSkill.getVehiclesOnLaneSegment(scope, road, lowestLane + i, prevSegment)
//					);
//				}
//				for (IAgent otherVehicle : prevNeighbors) {
//					boolean sameDirection = target == getCurrentTarget(otherVehicle);
//					double otherVL = getVehicleLength(otherVehicle);
//					double otherDistToSegmentEnd;
//					if (sameDirection) {
//						otherDistToSegmentEnd = getDistanceToGoal(otherVehicle);
//					} else {
//						GamaPoint otherLocation = (GamaPoint) otherVehicle.getLocation();
//						otherDistToSegmentEnd = otherLocation.distance(segmentEndPt);
//					}
//
//					double gap = otherDistToSegmentEnd + distToPrevTarget - 0.5 * vL - 0.5 * otherVL;
//					if (gap < 0) {
//						//overlap
//						return null;
//					} else if (gap < minBackDist) {
//						backVehicle = otherVehicle;
//						minBackDist = gap;
//						backSameDirection = sameDirection;
//					}
//				}
//			}
//		}
//
//		if (backVehicle == null) {
//			backTriple = null;
//		} else {
//			backTriple = ImmutableTriple.of(backVehicle, minBackDist, backSameDirection);
//		}
//
//		// No leading vehicle is found on the current segment
//		if (leadingVehicle == null) {
//			IAgent nextRoad = getNextRoad(vehicle);
//			// Check if vehicle is approaching an intersection
//			int numSegments = RoadSkill.getNumSegments(road);
//			if ((!violatingOneway && segment == numSegments - 1) ||
//					(violatingOneway && segment == 0)) {
//				// Return a virtual leading vehicle of length 0 to simulate deceleration at intersections
//				// NOTE: the added minSafetyDist is necessary for the vehicle to ignore the safety dist when stopping at an endpoint
//				// TODO: make the vehicles stop in front of the lights,
//				// this would require changes in the drive loop as well
//				IAgent stoppingNode = target;
//				leadingTriple = ImmutableTriple.of(stoppingNode, distToSegmentEnd + minSafetyDist, false);
//				// Slowing down at final target, since at this point we don't know which road will be taken next
//				if (nextRoad == null) {
//					return ImmutablePair.of(leadingTriple, backTriple);
//				// Might need to slow down at the intersection if it is not possible to enter the next road
//				} else {
//					if (!readyToCross(scope, vehicle, stoppingNode, nextRoad)) {
//						return ImmutablePair.of(leadingTriple, backTriple);
//					}
//				}
//			}
//
//			// Continue to find leading vehicle on next segment or next road in path
//			IAgent roadToCheck = null;
//			IAgent targetToCheck = null;
//			int lowestLaneToCheck = 0;
//			int segmentToCheck = 0;
//			if ((!violatingOneway && segment < numSegments - 1) ||
//					(violatingOneway && segment > 0)) {
//				roadToCheck = road;
//				targetToCheck = target;
//				segmentToCheck = !violatingOneway ? segment + 1 : segment - 1;
//				lowestLaneToCheck = lowestLane;
//			} else if (road != nextRoad) {  // road == nextRoad when vehicle is at an intersection
//				roadToCheck = nextRoad;
//				boolean willViolateOneway = target == RoadSkill.getTargetNode(nextRoad);
//				targetToCheck = !willViolateOneway ?
//					RoadSkill.getTargetNode(nextRoad) : RoadSkill.getSourceNode(nextRoad);
//				segmentToCheck = !willViolateOneway ? 0 : RoadSkill.getNumSegments(nextRoad) - 1;
//				// TODO: is this the right lane to check?
//				int numLanesTotal = RoadSkill.getNumLanes(roadToCheck);
//				IAgent linkedRoadToCheck = RoadSkill.getLinkedRoad(roadToCheck);
//				if (linkedRoadToCheck != null) {
//					numLanesTotal += RoadSkill.getNumLanes(linkedRoadToCheck);
//				}
//				lowestLaneToCheck = Math.min(lowestLane, numLanesTotal - numLanesOccupied);
//			}
//
//			if (roadToCheck != null) {
//				Set<IAgent> furtherVehicles = new HashSet<>();
//				for (int i = 0; i < numLanesOccupied; i += 1) {
//					furtherVehicles.addAll(
//						RoadSkill.getVehiclesOnLaneSegment(scope,
//							roadToCheck, lowestLaneToCheck + i, segmentToCheck)
//					);
//				}
//
//				for (IAgent otherVehicle : furtherVehicles) {
//					if (otherVehicle == vehicle || otherVehicle == null || otherVehicle.dead()) {
//						continue;
//					}
//					// check if the other vehicle going in opposite direction
//					double targetToOtherVehicle;
//					boolean sameDirection = targetToCheck == getCurrentTarget(otherVehicle);
//					if (sameDirection) {
//						Coordinate coords[] = roadToCheck.getInnerGeometry().getCoordinates();
//						// TODO: verify the correctness of this, regarding one-way
//						double segmentLength = coords[segmentToCheck].distance(coords[segmentToCheck + 1]);
//						targetToOtherVehicle = segmentLength - getDistanceToGoal(otherVehicle);
//					} else {
//						targetToOtherVehicle = getDistanceToGoal(otherVehicle);
//					}
//					double otherVL = getVehicleLength(otherVehicle);
//					double gap = distToSegmentEnd + targetToOtherVehicle - 0.5 * vL - 0.5 * otherVL;
//
//					if (gap < 0) {
//						// crash
//						return null;
//					} else if (gap < minLeadingDist) {
//						leadingVehicle = otherVehicle;
//						minLeadingDist = gap;
//						leadingSameDirection = sameDirection;
//					}
//				}
//			}
//		}
//
//		if (leadingVehicle == null || leadingVehicle.dead()) {
//			// the road ahead seems to be completely clear
//			leadingTriple = ImmutableTriple.of(null, 1e6, false);
//			return ImmutablePair.of(leadingTriple, backTriple);
//		} else {
//			// Found a leading vehicle on the next segment/next road
//			leadingTriple = ImmutableTriple.of(leadingVehicle, minLeadingDist, leadingSameDirection);
//			return ImmutablePair.of(leadingTriple, backTriple);
//		}
//	}
}

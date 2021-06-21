package gama.extensions.physics.common;

public interface IPhysicalConstants {

	/*
	 * Constants from Bullet
	 */

	/**
	 * Use ActivationStates
	 *
	 * @author drogoul
	 *
	 */
	enum ActivationState {
		UNKNOWN(-1), ACTIVE(1), SLEEPING(2), WANTING_DEACTIVATION(3), ALWAYS_ON(4), ALWAYS_OFF(5);

		private final int bulletCode;

		ActivationState(final int i) {
			bulletCode = i;
		}

		public static ActivationState of(final int code) {
			for (ActivationState a : values()) {
				if (a.bulletCode == code) return a;
			}
			return UNKNOWN;
		}
	}

	double EPS = 0.000001;

	/*
	 * Species and skills names
	 */

	String PHYSICAL_WORLD = "physical_world";
	String STATIC_BODY = "static_body";
	String DYNAMIC_BODY = "dynamic_body";

	/*
	 * Action names
	 */

	String REGISTER = "register";
	String APPLY = "apply";
	String CONTACT_ADDED = "contact_added_with";
	String CONTACT_REMOVED = "contact_removed_with";
	String UPDATE_BODY = "update_body";
	/*
	 * Arguments to actions
	 */
	String OTHER = "other";
	String BODIES = "bodies";
	String CLEARANCE = "clearance";
	String IMPULSE = "impulse";
	String FORCE = "force";
	String TORQUE = "torque";

	/*
	 * Attributes
	 */
	String USE_NATIVE = "use_native_library";
	String ROTATION = "rotation";
	String VELOCITY = "velocity";
	String FRICTION = "friction";
	String RESTITUTION = "restitution";
	String DAMPING = "damping";
	String ANGULAR_DAMPING = "angular_damping";
	String CONTACT_DAMPING = "contact_damping";
	String ANGULAR_VELOCITY = "angular_velocity";
	String MASS = "mass";
	String BODY = "%%rigid_body%%";
	String TERRAIN = "terrain";
	String AABB = "aabb";
	String GRAVITY = "gravity";
	String AUTOMATED_REGISTRATION = "automated_registration";
	String MAX_SUBSTEPS = "max_substeps";
	String ACCURATE_COLLISION_DETECTION = "accurate_collision_detection";

}

package ummisco.gaml.extensions.maths.random;

import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.WeibullDistribution;

import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.example;
import msi.gama.precompiler.GamlAnnotations.no_test;
import msi.gama.precompiler.GamlAnnotations.operator;
import msi.gama.precompiler.GamlAnnotations.test;
import msi.gama.precompiler.GamlAnnotations.usage;
import msi.gama.precompiler.IConcept;
import msi.gama.precompiler.IOperatorCategory;
import msi.gama.precompiler.Reason;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;

public class Random2 {

	@operator (
			value = "gamma_rnd",
			can_be_const = false,
			category = { IOperatorCategory.RANDOM },
			concept = { IConcept.RANDOM })
	@doc (
			value = "returns a random value from a gamma distribution with specified values of the shape and scale parameters",
			examples = { @example (
					value = "gamma_rnd(9,0.5)",
					equals = "0.731",
					test = false) },
			see = { "binomial", "gauss_rnd", "lognormal_rnd", "poisson", "rnd", "skew_gauss", "truncated_gauss",
					"weibull_rnd", "gamma_trunc_rnd" })
	@no_test (Reason.IMPOSSIBLE_TO_TEST)
	public static Double OpGammaDist(final IScope scope, final Double shape, final Double scale)
			throws GamaRuntimeException {
		final GammaDistribution dist = new GammaDistribution(new ForwardingGenerator(scope.getRandom().getGenerator()),
				shape, scale, GammaDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
		return dist.sample();
	}

	@operator (
			value = "weibull_rnd",
			can_be_const = false,
			category = { IOperatorCategory.RANDOM },
			concept = { IConcept.RANDOM })
	@doc (
			value = "returns a random value from a Weibull distribution with specified values of the shape (alpha) and scale (beta) parameters. See https://mathworld.wolfram.com/WeibullDistribution.html for more details (equations 1 and 2). ",
			examples = { @example (
					value = "weibull_rnd(2,3) ",
					equals = "0.731",
					test = false) },
			see = { "binomial", "gamma_rnd", "gauss_rnd", "lognormal_rnd", "poisson", "rnd", "skew_gauss",
					"truncated_gauss", "weibull_trunc_rnd" })
	@no_test (Reason.IMPOSSIBLE_TO_TEST)
	public static Double OpWeibullDist(final IScope scope, final Double shape, final Double scale)
			throws GamaRuntimeException {
		final WeibullDistribution dist =
				new WeibullDistribution(new ForwardingGenerator(scope.getRandom().getGenerator()), shape, scale);

		return dist.sample();
	}

	@operator (
			value = "lognormal_rnd",
			can_be_const = false,
			category = { IOperatorCategory.RANDOM },
			concept = { IConcept.RANDOM })
	@doc (
			value = "returns a random value from a Log-Normal distribution with specified values of the shape (alpha) and scale (beta) parameters. See https://en.wikipedia.org/wiki/Log-normal_distribution for more details. ",
			examples = { @example (
					value = "lognormal_rnd(2,3) ",
					equals = "0.731",
					test = false) },
			see = { "binomial", "gamma_rnd", "gauss_rnd", "poisson", "rnd", "skew_gauss", "truncated_gauss",
					"weibull_rnd", "lognormal_trunc_rnd" })
	@no_test (Reason.IMPOSSIBLE_TO_TEST)
	public static Double OpLogNormalDist(final IScope scope, final Double shape, final Double scale)
			throws GamaRuntimeException {
		final LogNormalDistribution dist =
				new LogNormalDistribution(new ForwardingGenerator(scope.getRandom().getGenerator()), shape, scale);
		return dist.sample();
	}

	@operator (
			value = "lognormal_trunc_rnd",
			can_be_const = false,
			category = { IOperatorCategory.RANDOM },
			concept = { IConcept.RANDOM })
	@doc (
			value = "returns a random value from a truncated Log-Normal distribution (in a range or given only one boundary) with specified values of the shape (alpha) and scale (beta) parameters. See https://en.wikipedia.org/wiki/Log-normal_distribution for more details. ",
			usages = { @usage (
					value = "when 2 float operands are specified, they are taken as mininimum and maximum values for the result",
					examples = { @example (
							value = "lognormal_trunc_rnd(2,3,0,5)",
							test = false) }) },
			see = { "lognormal_rnd", "gamma_trunc_rnd", "weibull_trunc_rnd", "truncated_gauss" })
	@test ("lognormal_trunc_rnd(2,3,0,5) <= 5.0")
	@test ("lognormal_trunc_rnd(2,3,0.0,5.0) >= 0.0")
	public static Double OpLogNormalTruncDist(final IScope scope, final Double shape, final Double scale,
			final Double min, final Double max) throws GamaRuntimeException {
		double tmpResult = 0;

		do {
			tmpResult = OpLogNormalDist(scope, shape, scale);
		} while (tmpResult > max || tmpResult < min);
		return tmpResult;
	}

	@operator (
			value = "lognormal_trunc_rnd",
			can_be_const = false,
			category = { IOperatorCategory.RANDOM },
			concept = { IConcept.RANDOM })
	@doc (
			usages = { @usage (
					value = "when 1 float and a boolean (isMax) operands are specified, the float value represents the single boundary (max if the boolean is true, min otherwise),",
					examples = { @example (
							value = "lognormal_trunc_rnd(2,3,5,true)",
							test = false) }) },
			see = { "lognormal_rnd", "gamma_trunc_rnd", "weibull_trunc_rnd", "truncated_gauss" })
	@test ("lognormal_trunc_rnd(2,3,5,true) <= 5.0")
	@test ("lognormal_trunc_rnd(2,3,0.0,false) >= 0.0")
	public static Double OpLogNormalTruncDist(final IScope scope, final Double shape, final Double scale,
			final Double minmax, final Boolean isMax) throws GamaRuntimeException {
		double tmpResult = 0;

		if (isMax) {
			// minmax is a max here
			do {
				tmpResult = OpLogNormalDist(scope, shape, scale);
			} while (tmpResult > minmax);
		} else {
			// minmax is a min here
			do {
				tmpResult = OpLogNormalDist(scope, shape, scale);
			} while (tmpResult < minmax);
		}
		return tmpResult;
	}

	@operator (
			value = "weibull_trunc_rnd",
			can_be_const = false,
			category = { IOperatorCategory.RANDOM },
			concept = { IConcept.RANDOM })
	@doc (
			value = "returns a random value from a truncated Weibull distribution (in a range or given only one boundary) with specified values of the shape (alpha) and scale (beta) parameters. See https://mathworld.wolfram.com/WeibullDistribution.html for more details (equations 1 and 2). ",
			usages = { @usage (
					value = "when 2 float operands are specified, they are taken as mininimum and maximum values for the result",
					examples = { @example (
							value = "weibull_trunc_rnd(2,3,0.0,5.0)",
							test = false) }) },
			see = { "weibull_rnd", "gamma_trunc_rnd", "lognormal_trunc_rnd", "truncated_gauss" })
	@test ("weibull_trunc_rnd(2,3,0,5) <= 5.0")
	@test ("weibull_trunc_rnd(2,3,0.0,5.0) >= 0.0")
	public static Double OpWeibullTruncDist(final IScope scope, final Double shape, final Double scale,
			final Double min, final Double max) throws GamaRuntimeException {
		double tmpResult = 0;

		do {
			tmpResult = OpWeibullDist(scope, shape, scale);
		} while (tmpResult > max || tmpResult < min);
		return tmpResult;
	}

	@operator (
			value = "weibull_trunc_rnd",
			can_be_const = false,
			category = { IOperatorCategory.RANDOM },
			concept = { IConcept.RANDOM })
	@doc (
			usages = { @usage (
					value = "when 1 float and a boolean (isMax) operands are specified, the float value represents the single boundary (max if the boolean is true, min otherwise),",
					examples = { @example (
							value = "weibull_trunc_rnd(2,3,5,true)",
							test = false) }) },
			see = { "weibull_rnd", "gamma_trunc_rnd", "lognormal_trunc_rnd", "truncated_gauss" })
	@test ("weibull_trunc_rnd(2,3,5,true) <= 5.0")
	@test ("weibull_trunc_rnd(2,3,0.0,false) >= 0.0")
	public static Double OpWeibullTruncDist(final IScope scope, final Double shape, final Double scale,
			final Double minmax, final Boolean isMax) throws GamaRuntimeException {
		double tmpResult = 0;

		if (isMax) {
			// minmax is a max here
			do {
				tmpResult = OpWeibullDist(scope, shape, scale);
			} while (tmpResult > minmax);
		} else {
			// minmax is a min here
			do {
				tmpResult = OpWeibullDist(scope, shape, scale);
			} while (tmpResult < minmax);
		}
		return tmpResult;
	}

	@operator (
			value = "gamma_trunc_rnd",
			can_be_const = false,
			category = { IOperatorCategory.RANDOM },
			concept = { IConcept.RANDOM })
	@doc (
			value = "returns a random value from a truncated gamma distribution (in a range or given only one boundary) with specified values of the shape and scale parameters.",
			usages = { @usage (
					value = "when 2 float operands are specified, they are taken as mininimum and maximum values for the result",
					examples = { @example (
							value = "gamma_trunc_rnd(2,3,0,5)",
							test = false) }) },
			see = { "gamma_rnd", "weibull_trunc_rnd", "lognormal_trunc_rnd", "truncated_gauss" })
	@test ("gamma_trunc_rnd(2,3,0,5) <= 5.0")
	@test ("gamma_trunc_rnd(2,3,0.0,5.0) >= 0.0")
	public static Double OpGammaTruncDist(final IScope scope, final Double shape, final Double scale, final Double min,
			final Double max) throws GamaRuntimeException {
		double tmpResult = 0;

		do {
			tmpResult = OpGammaDist(scope, shape, scale);
		} while (tmpResult > max || tmpResult < min);
		return tmpResult;
	}

	@operator (
			value = "gamma_trunc_rnd",
			can_be_const = false,
			category = { IOperatorCategory.RANDOM },
			concept = { IConcept.RANDOM })
	@doc (
			usages = { @usage (
					value = "when 1 float and a boolean (isMax) operands are specified, the float value represents the single boundary (max if the boolean is true, min otherwise),",
					examples = { @example (
							value = "gamma_trunc_rnd(2,3,5,true)",
							test = false) }) },
			see = { "gamma_rnd", "weibull_trunc_rnd", "lognormal_trunc_rnd", "truncated_gauss" })
	@test ("gamma_trunc_rnd(2,3,5,true) <= 5.0")
	@test ("gamma_trunc_rnd(2,3,0.0,false) >= 0.0")
	public static Double OpGammaTruncDist(final IScope scope, final Double shape, final Double scale,
			final Double minmax, final Boolean isMax) throws GamaRuntimeException {
		double tmpResult = 0;

		if (isMax) {
			// minmax is a max here
			do {
				tmpResult = OpGammaDist(scope, shape, scale);
			} while (tmpResult > minmax);
		} else {
			// minmax is a min here
			do {
				tmpResult = OpGammaDist(scope, shape, scale);
			} while (tmpResult < minmax);
		}
		return tmpResult;
	}

	@operator (
			value = "weibull_density",
			can_be_const = false,
			category = { IOperatorCategory.RANDOM },
			concept = { IConcept.RANDOM })
	@doc (
			value = "weibull_density(x,shape,scale) returns the probability density function (PDF) at the specified point x "
					+ "of the Weibull distribution with the given shape and scale.",
			examples = { @example (
					value = "weibull_rnd(1,2,3) ",
					equals = "0.731",
					test = false) },
			see = { "binomial", "gamma_rnd", "gauss_rnd", "lognormal_rnd", "poisson", "rnd", "skew_gauss",
					"lognormal_density", "gamma_density" })
	@no_test (Reason.IMPOSSIBLE_TO_TEST)
	public static Double OpWeibullDistDensity(final IScope scope, final Double x, final Double shape,
			final Double scale) throws GamaRuntimeException {
		final WeibullDistribution dist =
				new WeibullDistribution(new ForwardingGenerator(scope.getRandom().getGenerator()), shape, scale);

		return dist.density(x);
	}

	@operator (
			value = "lognormal_density",
			can_be_const = false,
			category = { IOperatorCategory.RANDOM },
			concept = { IConcept.RANDOM })
	@doc (
			value = "lognormal_density(x,shape,scale) returns the probability density function (PDF) at the specified point x "
					+ "of the logNormal distribution with the given shape and scale.",
			examples = { @example (
					value = "lognormal_density(1,2,3) ",
					equals = "0.731",
					test = false) },
			see = { "binomial", "gamma_rnd", "gauss_rnd", "poisson", "rnd", "skew_gauss", "truncated_gauss",
					"weibull_rnd", "weibull_density", "gamma_density" })
	@no_test (Reason.IMPOSSIBLE_TO_TEST)
	public static Double OpLogNormalDist(final IScope scope, final Double x, final Double shape, final Double scale)
			throws GamaRuntimeException {
		final LogNormalDistribution dist =
				new LogNormalDistribution(new ForwardingGenerator(scope.getRandom().getGenerator()), shape, scale);
		return dist.density(x);
	}

	@operator (
			value = "gamma_density",
			can_be_const = false,
			category = { IOperatorCategory.RANDOM },
			concept = { IConcept.RANDOM })
	@doc (
			value = "gamma_density(x,shape,scale) returns the probability density function (PDF) at the specified point x "
					+ "of the Gamma distribution with the given shape and scale.",
			examples = { @example (
					value = "gamma_density(1,9,0.5)",
					equals = "0.731",
					test = false) },
			see = { "binomial", "gauss_rnd", "lognormal_rnd", "poisson", "rnd", "skew_gauss", "truncated_gauss",
					"weibull_rnd", "weibull_density", "lognormal_density" })
	@no_test (Reason.IMPOSSIBLE_TO_TEST)
	public static Double OpGammaDist(final IScope scope, final Double x, final Double shape, final Double scale)
			throws GamaRuntimeException {
		final GammaDistribution dist = new GammaDistribution(new ForwardingGenerator(scope.getRandom().getGenerator()),
				shape, scale, GammaDistribution.DEFAULT_INVERSE_ABSOLUTE_ACCURACY);
		return dist.density(x);
	}

}

/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.util.math;

/**
 * 
 * @author Zach Tosi
 * @author Scott Hotton
 * @author Jeff Yoshimi
 * 
 */
public enum SquashingFunction {

	/** Arctangent. */
	ARCTAN {

		@Override
		public String toString() {
			return "Arctan";
		}

		@Override
		public double valueOf(double val, double ceil, double floor,
				double slope) {
			return atan(val, ceil, floor, slope);
		}

		@Override
		public double inverseVal(double val, double ceil, double floor,
				double slope) {
			return invAtan(val, ceil, floor, slope);
		}

		@Override
		public double derivVal(double val, double ceil, double floor,
				double slope) {
			return derivAtan(val, ceil, floor, slope);
		}

	},

	/** Logistic Function. */
	LOGISTIC {

		@Override
		public String toString() {
			return "Logistic";
		}

		@Override
		public double valueOf(double val, double ceil, double floor,
				double slope) {
			return logistic(val, ceil, floor, slope);
		}

		@Override
		public double inverseVal(double val, double ceil, double floor,
				double slope) {
			return invLogistic(val, ceil, floor, slope);
		}

		@Override
		public double derivVal(double val, double ceil, double floor,
				double slope) {
			return derivLogistic(val, ceil, floor, slope);
		}
	},

	/** Hyperbolic Tangent. */
	TANH {

		@Override
		public String toString() {
			return "Tanh";
		}

		@Override
		public double valueOf(double val, double ceil, double floor,
				double slope) {
			return tanh(val, ceil, floor, slope);
		}

		@Override
		public double inverseVal(double val, double ceil, double floor,
				double slope) {
			return invTanh(val, ceil, floor, slope);
		}

		@Override
		public double derivVal(double val, double ceil, double floor,
				double slope) {
			return derivTanh(val, ceil, floor, slope);
		}
	},

	NULL_STRING {

		@Override
		public String toString() {
			return "...";
		}

		@Override
		public double valueOf(double val, double ceil, double floor,
				double slope) {
			return 0;
		}

		@Override
		public double inverseVal(double val, double ceil, double floor,
				double slope) {
			return 0;
		}

		@Override
		public double derivVal(double val, double ceil, double floor,
				double slope) {
			return 0;
		}
	};

	/**
	 * Gives the value of the given squashing function for some input value, a
	 * ceiling, floor, and slope.
	 * 
	 * @param val
	 *            the base value to pass the function
	 * @param ceil
	 *            the upper limit of the curve
	 * @param floor
	 *            the lower limit of the curve
	 * @param slope
	 *            the slope of the curve at zero
	 * @return the output of the given squashing function
	 */
	public abstract double valueOf(double val, double ceil, double floor,
			double slope);

	/**
	 * Gives the value of the inverse of the given squashing function for some
	 * input value, a ceiling, floor, and slope.
	 * 
	 * @param val
	 *            the base value to pass the inverse function
	 * @param ceil
	 *            the upper limit of the squashing function being inverted
	 * @param floor
	 *            the lower limit of the squashing function being inverted
	 * @param slope
	 *            the slope of the squashing function being inverted at zero
	 * @return the output of the given squashing function's inverse
	 */
	public abstract double inverseVal(double val, double ceil,
			double floor, double slope);

	/**
	 * Gives the value of the derivative of the given squashing function for
	 * some input value, a ceiling, floor, and slope. All parameters are fed to
	 * the squashing function and the return value represents the derivative of
	 * THAT function.
	 * 
	 * @param val
	 *            the base value to pass the function
	 * @param ceil
	 *            the upper limit of the curve
	 * @param floor
	 *            the lower limit of the curve
	 * @param slope
	 *            the slope of the curve at zero
	 * @return the output of the given squashing function's derivative
	 */
	public abstract double derivVal(double val, double ceil,
			double floor, double slope);

	/*
	 * ****************************************************************
	 * _____________________Function Methods_ ________________________*
	 * ****************************************************************
	 */

	/**
	 * The hyperbolic tangent given an upper and lower limit and slope for a
	 * particular value
	 * 
	 * @param val
	 * @param ceil
	 * @param floor
	 * @param slope
	 * @return
	 */
	public static double tanh(double val, double ceil, double floor,
			double slope) {
		double diff = ceil - floor;
		double a = (2 * slope) / diff;
		return (diff / 2) * Math.tanh(a * val) + ((ceil + floor) / 2);
	}

	/**
	 * Returns the results of the standard sigmoidal function.
	 * 
	 * @param input
	 *            argument
	 * @param neuron
	 *            undergoing update
	 * @return results of sigm
	 */
	public static double logistic(double val, double ceil, double floor,
			double slope) {
		double diff = ceil - floor;
		return diff * logisticFunc(slope * val / diff) + floor;
	}

	/**
	 * Returns the standard logistic. Helper method so that the full logistic
	 * function doesn't have to be written out every time it's invoked.
	 * 
	 * @param x
	 *            input argument
	 * @return result of logistic
	 */
	private static double logisticFunc(double x) {
		return 1 / (1 + Math.exp(-x));
	}

	/**
	 * Returns the result of the arctangent function
	 * 
	 * @param input
	 *            argument
	 * @param neuron
	 *            undergoing update
	 * @return results of atan
	 */
	public static double atan(double val, double ceil, double floor,
			double slope) {
		double diff = ceil - floor;
		double a = (Math.PI * slope) / diff;
		return (diff / Math.PI) * Math.atan(a * val)
				+ ((ceil + floor) / 2);
	}

	/*
	 * ****************************************************************
	 * _____________________Inverse Functions ________________________*
	 * ****************************************************************
	 */

	/**
	 * The inverse hyperbolic tangent given an upper and lower limit and slope
	 * for a particular value for the hyperbolic tangent before being inverted.
	 * 
	 * @param val
	 * @param ceil
	 * @param floor
	 * @param slope
	 * @return
	 */
	public static double invTanh(double val, double ceil, double floor,
			double slope) {
		double z = 0.5 * (((val - floor) / (ceil - floor)) - 0.5);
		return (Math.log((1 + z)) / (1 - z));
	}

	/**
	 * Returns the results of the inverse of the standard sigmoidal function
	 * 
	 * @param input
	 *            argument
	 * @param neuron
	 *            from which the value is being mapped
	 * @return the inverse sigmoid
	 */
	public static double invLogistic(double val, double ceil,
			double floor, double slope) {
		double diff = ceil - floor;
		return diff * -Math.log(diff / (val - floor) - 1) / slope;
	}

	/**
	 * Returns the result of the inverse arctangent or tangent function
	 * 
	 * @param input
	 *            argument
	 * @param neuron
	 *            from which the value is being mapped
	 * @return the inverse of the atan activation function
	 */
	public static double invAtan(double val, double ceil, double floor,
			double slope) {
		double a = (Math.PI * slope) / (ceil - floor);
		double diff = ceil - floor;
		double z = ((val - ((ceil + floor) / 2)) * (Math.PI / diff));
		return Math.tan(z) / a;
	}

	/*
	 * ****************************************************************
	 * _____________________Derivative Functions _____________________*
	 * ****************************************************************
	 */

	/**
	 * The derivative of the hyperbolic tangent given the original function's
	 * upper and lower bounds, and slope for a given value.
	 * 
	 * @param val
	 * @param ceil
	 * @param floor
	 * @param slope
	 * @return
	 */
	public static double derivTanh(double val, double ceil, double floor,
			double slope) {
		double diff = ceil - floor;
		double a = (2 * slope) / diff;
		return diff / 2 * a * Math.pow(1 / Math.cosh(a * val), 2);
	}

	/**
	 * The derivative of the arc tangent given the original function's upper and
	 * lower bounds, and slope for a given value.
	 * 
	 * @param val
	 * @param ceil
	 * @param floor
	 * @param slope
	 * @return
	 */
	public static double derivAtan(double val, double ceil, double floor,
			double slope) {
		double diff = ceil - floor;
		double a = (Math.PI * slope) / diff;
		return a * (diff / Math.PI) * (1 / (1 + Math.pow(a * val, 2)));
	}

	/**
	 * The derivative of the logistic function given the original function's
	 * upper and lower bounds, and slope for a given value.
	 * 
	 * @param val
	 * @param ceil
	 * @param floor
	 * @param slope
	 * @return
	 */
	public static double derivLogistic(double val, double ceil,
			double floor, double slope) {
		double diff = ceil - floor;
		return slope * logisticFunc(slope * val / diff)
				* (1 - logisticFunc(slope * val / diff));
	}

}
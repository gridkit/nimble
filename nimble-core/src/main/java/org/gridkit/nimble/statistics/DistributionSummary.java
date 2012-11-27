package org.gridkit.nimble.statistics;

import java.io.Serializable;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.apache.commons.math3.util.Precision;

/**
 * This class represents distribution characteristics
 * of random function using common statistical estimates.
 * 
 * All used estimates a cummulative
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface DistributionSummary extends Summary.CountSummary, Summary.SumSummary {

    /**
     * Returns the <a href="http://www.xycoon.com/arithmetic_mean.htm">
     * arithmetic mean </a> of the available values
     * @return The mean or Double.NaN if no values have been added.
     */
    public double getMean();
    
    /**
     * Returns the variance of the available values.
     * @return The variance, Double.NaN if no values have been added
     * or 0.0 for a single value set.
     */
    
    public double getVariance();
    /**
     * Returns the standard deviation of the available values.
     * @return The standard deviation, Double.NaN if no values have been added
     * or 0.0 for a single value set.
     */
    
    public double getStandardDeviation();
    /**
     * Returns the maximum of the available values
     * @return The max or Double.NaN if no values have been added.
     */
    
    public double getMax();
    /**
    * Returns the minimum of the available values
    * @return The min or Double.NaN if no values have been added.
    */
    
    public double getMin();
    /**
     * Returns the number of available values
     * @return The number of available values
     */
    
    public long getN();
    /**
     * Returns the sum of the values that have been added to Univariate.
     * @return The sum or Double.NaN if no values have been added
     */
    
    public double getSum();
	
	public static class Values implements DistributionSummary, Serializable {

		private static final long serialVersionUID = 20121017L;

		/** The sample mean */
	    private final double mean;

	    /** The sample variance */
	    private final double variance;

	    /** The number of observations in the sample */
	    private final long n;

	    /** The maximum value */
	    private final double max;

	    /** The minimum value */
	    private final double min;

	    /** The sum of the sample values */
	    private final double sum;

	    /**
	      * Constructor
	      *
	      * @param mean  the sample mean
	      * @param variance  the sample variance
	      * @param n  the number of observations in the sample
	      * @param max  the maximum value
	      * @param min  the minimum value
	      * @param sum  the sum of the values
	     */
	    public Values(double mean, double variance, long n, double max, double min, double sum) {
	        this.mean = mean;
	        this.variance = variance;
	        this.n = n;
	        this.max = max;
	        this.min = min;
	        this.sum = sum;
	    }

	    public Values(StatisticalSummary summary) {
	    	this(summary.getMean(), summary.getVariance(), summary.getN(), summary.getMax(), summary.getMin(), summary.getSum());
	    }

	    public Values(DistributionSummary summary) {
	    	this(summary.getMean(), summary.getVariance(), summary.getN(), summary.getMax(), summary.getMin(), summary.getSum());
	    }

	    @Override
		public boolean isEmpty() {
			return n == 0;
		}

		/**
	     * @return Returns the max.
	     */
	    public double getMax() {
	        return max;
	    }

	    /**
	     * @return Returns the mean.
	     */
	    public double getMean() {
	        return mean;
	    }

	    /**
	     * @return Returns the min.
	     */
	    public double getMin() {
	        return min;
	    }

	    /**
	     * @return Returns the number of values.
	     */
	    public long getN() {
	        return n;
	    }

	    /**
	     * @return Returns the sum.
	     */
	    public double getSum() {
	        return sum;
	    }

	    /**
	     * @return Returns the standard deviation
	     */
	    public double getStandardDeviation() {
	        return FastMath.sqrt(variance);
	    }

	    /**
	     * @return Returns the variance.
	     */
	    public double getVariance() {
	        return variance;
	    }

	    /**
	     * Returns true iff <code>object</code> is a
	     * <code>StatisticalSummaryValues</code> instance and all statistics have
	     *  the same values as this.
	     *
	     * @param object the object to test equality against.
	     * @return true if object equals this
	     */
	    @Override
	    public boolean equals(Object object) {
	        if (object == this ) {
	            return true;
	        }
	        if (object instanceof StatisticalSummaryValues == false) {
	            return false;
	        }
	        StatisticalSummaryValues stat = (StatisticalSummaryValues) object;
	        return Precision.equalsIncludingNaN(stat.getMax(),      getMax())  &&
	               Precision.equalsIncludingNaN(stat.getMean(),     getMean()) &&
	               Precision.equalsIncludingNaN(stat.getMin(),      getMin())  &&
	               Precision.equalsIncludingNaN(stat.getN(),        getN())    &&
	               Precision.equalsIncludingNaN(stat.getSum(),      getSum())  &&
	               Precision.equalsIncludingNaN(stat.getVariance(), getVariance());
	    }

	    /**
	     * Returns hash code based on values of statistics
	     *
	     * @return hash code
	     */
	    @Override
	    public int hashCode() {
	        int result = 31 + MathUtils.hash(getMax());
	        result = result * 31 + MathUtils.hash(getMean());
	        result = result * 31 + MathUtils.hash(getMin());
	        result = result * 31 + MathUtils.hash(getN());
	        result = result * 31 + MathUtils.hash(getSum());
	        result = result * 31 + MathUtils.hash(getVariance());
	        return result;
	    }

	    /**
	     * Generates a text report displaying values of statistics.
	     * Each statistic is displayed on a separate line.
	     *
	     * @return String with line feeds displaying statistics
	     */
	    @Override
	    public String toString() {
	        StringBuffer outBuffer = new StringBuffer();
	        outBuffer.append("Summary{");
	        outBuffer.append("n: ").append(getN());
	        outBuffer.append(", min: ").append(getMin());
	        outBuffer.append(", max: ").append(getMax());
	        outBuffer.append(", mean: ").append(getMean());
	        outBuffer.append(", std dev: ").append(getStandardDeviation());
	        outBuffer.append("}");
	        return outBuffer.toString();
	    }
	}	
}

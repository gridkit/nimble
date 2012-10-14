package org.gridkit.nimble.statistics;

/**
 * This class represents distribution characteristics
 * of random function using common statistical estimates.
 * 
 * All used estimates a cummulative
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface DistributionSummary {

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
	
	public static class Values implements DistributionSummary {

		double mean;
		double varianc;
		
		@Override
		public double getMean() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getVariance() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getStandardDeviation() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getMax() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getMin() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public long getN() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getSum() {
			// TODO Auto-generated method stub
			return 0;
		}
	}	
}

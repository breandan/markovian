package edu.washington.util;

public class SPNMath {
	private static final double LOGZERO = Double.NEGATIVE_INFINITY;

	public static double log_sum_exp(double l1, double l2){
		if(l1 == LOGZERO && l2 == LOGZERO)
			return LOGZERO;
		if(l1 > l2)
			return l1 + Math.log(1 + Math.exp(l2 - l1));
		else
			return l2 + Math.log(1 + Math.exp(l1 - l2));
	}
	
	public static double exp(double val) {
		if(val == LOGZERO) return 0;
		if(val > 70) return Math.exp(val);
		if(val < -70) return Math.exp(val);
	    final long tmp = (long) (1512775 * val + 1072632447);
	    return Double.longBitsToDouble(tmp << 32);
	}
	
	public static double log_minus_exp(double l1, double l2){
		if(l1 == LOGZERO || l1 <= l2)
			return LOGZERO;
		if(l2 == LOGZERO)
			return l1;
//		if(l1 < l2)
//			throw new ArithmeticException("Trying to subtract a large number ("+l2+") from a smaller number ("+l1+")  abs diff = "+Math.abs(l1 - l2));
		return l1 + Math.log(1 - Math.exp(l2 - l1));
	}
}

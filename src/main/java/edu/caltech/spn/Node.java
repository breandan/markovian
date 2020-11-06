package edu.caltech.spn;

// SPN node
public abstract class Node {
	public static double ZERO_LOGVAL_=Double.NEGATIVE_INFINITY;	
	double logval_;
	double logDerivative_=Node.ZERO_LOGVAL_;
		
	public double getLogVal() {return logval_;}
	public void setVal(double v) {if (v==0) logval_=ZERO_LOGVAL_; else logval_=Math.log(v);}
	
	// evaluate root
	public void eval() {}
	
	// propagate derivative to children
	public void passDerivative(){}		
}
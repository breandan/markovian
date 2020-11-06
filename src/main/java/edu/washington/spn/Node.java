package edu.washington.spn;

import java.io.Serializable;


public abstract class Node implements Serializable {
	public static double ZERO_LOGVAL_=Double.NEGATIVE_INFINITY;
	
	public boolean dirty = true;
//	public final static int TYPE_VAR_=0;
//	public final static int TYPE_PROD_=1;
//	public final static int TYPE_SUM_=2;
//	
//	public Node(int type, int regId, int nodeIdx) {
//	public Node(int type) {
//		type_=type;
////		regId_=regId;
////		nodeIdx_=nodeIdx;
//	}
	
	// ref: type, region, nodeIdx (among nodes in same layer)
	public Integer shapeID = -1;
//	public int tie = -1;
	public int firstX=-1, firstY=-1;
	
	int type_;
//	int regId_;
//	int nodeIdx_;
//	public int getType() {return type_;}
//	public int getRegionId() {return regId_;}
//	public int getNodeIdx() {return nodeIdx_;}
	
	// val
	public double logval_ = 0;
	public double getLogVal() {return logval_;}
//	public void setVal(double v) {if (v==0) logval_=ZERO_LOGVAL_; else logval_=Math.log(v);}
	public void eval() {}
	
	public double der = 0;
	public double logder_=Node.ZERO_LOGVAL_;
	public void passDer(){}	// sent to chd
	public void passSampledDer() {}
	
	// cond der
//	double jntder_=Node.ZERO_LOGVAL_;	// P(X,Y)
//	double marder_=Node.ZERO_LOGVAL_;	// P(X)
//	public void passCondDer(){}
}
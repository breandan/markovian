package edu.caltech.common;

public class Parameter {
	// EM
	public static int maxIter_=30;	
	public static double thresholdLLHChg_=0.1;
	public static int batch_size_=50;
	public static double sparsePrior_=1;
	
	// SPN
	public static int numSumPerRegion_=20;
	public static int inputDim1_=64;
	public static int inputDim2_=64;
	public static int baseResolution_=4;
	public static double smoothSumCnt_=0.01;	
	public static int numComponentsPerVar_=4;	

	// Eval
	public static int maxTestSize_=50;	
	public static String domain_=null;
	public static int numSlavePerClass_=50;
	public static int numSlaveGrp_=1;	
}	

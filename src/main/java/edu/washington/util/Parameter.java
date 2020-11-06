package edu.washington.util;

public class Parameter {

	public static final double convergenceThreshold = 0.1;// 0.1;
	public static double invVar = 1; //00;
	public static int spn_width = 64;
	public static int spn_height = 64;
	public static boolean mpe = false;
	
	public static final double smoothSumCnt_ = 0.01;
	public static final double measurementPenalty = 1;
	public static final int batchsize = 50;
	public static double penalty = 1.0;
	public static double zero_tolerance = 0.01;
	public static double edgePenalty = 0.03;
//	public static int maxCandidatesMemory = 16000;
	public static int maxEdgesMemory = 9000000;
	public static int maxFanIn = 128 ;
	public static boolean resetBWrounds = false;
	public static int numRoundsTrainInSL = 1;
	
	public static double initLSNC = 1;
	public static double initSNC  = 1;
	public static int slRoundLimit = 10000;
	public static double regularization_coefficient = 0.0;
	public static boolean normImPatch = true;
	public static int patchWidth = 4;
	public static int patchStride = patchWidth;
	public static String filename = "untitled";
	public static String loadFilename = "load";
	public static int maxEpochs = 1000;
	public static int dictionarySize = 1000;
	public static double featThreshold = 0.0;
	
	public static int featGrid = 8;
	public static int numFeats = 200;
	
//	public static int numMOLOGcomp = 2;


}

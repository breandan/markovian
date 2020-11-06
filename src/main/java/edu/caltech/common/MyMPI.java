package edu.caltech.common;

import java.util.Random;

import mpi.MPI;

public class MyMPI {
	// master
	public static int rank_=MPI.COMM_WORLD.Rank();
	public static boolean isClassMaster_ = false;
	public static int masterRank_ = -1;
	public static int mySlave_ = -1; // class master
	
	// slave
	public static int myOffset_ = -1;	// slave
	public static int myStart_ = -1;	// slave
	
	public static void setConstantsForImgs() {
		MyMPI.isClassMaster_ = (rank_==0);
		masterRank_ = 0; 
		
		myOffset_=rank_-1;	// slave
		myStart_=1;	// slave
		mySlave_=1; // class master
		
		setRandomSeedByRank();
	}

	public static void setConstantsForImgsParallel() {
		// every block of ns+1 process serves one processing at a time
		MyMPI.isClassMaster_ = (rank_%(Parameter.numSlavePerClass_+1)==0);
		
		// slave 
		myOffset_=(rank_-1) % (Parameter.numSlavePerClass_+1);
		myStart_=rank_-myOffset_;
		masterRank_ = rank_-myOffset_-1;
		
		// master
		mySlave_=rank_+1;		
		setRandomSeedByRank();
	}

	static void setRandomSeedByRank() {
		Utils.seed_=MyMPI.rank_;
		Utils.random_=new Random(Utils.seed_);
	}
	
	// buffer
	public static int buf_idx_=0;	
	public static int buf_size_=10000000;
	public static int[] buf_int_=new int[buf_size_];	
	public static double[] buf_double_=new double[100];
	public static char[] buf_char_=new char[100];
	
	// MPI util
	public static double recvDouble(int src, int tag) {
		MPI.COMM_WORLD.Recv(MyMPI.buf_double_, 0, 1, MPI.DOUBLE, src, tag);
		return MyMPI.buf_double_[0];
	}
	public static void sendDouble(int dest, int tag, double d) {
		MyMPI.buf_double_[0]=d;
		MPI.COMM_WORLD.Send(MyMPI.buf_double_, 0, 1, MPI.DOUBLE, dest, tag);
	}
	public static char recvChar(int src, int tag) {
		MPI.COMM_WORLD.Recv(MyMPI.buf_char_, 0, 1, MPI.CHAR, src, tag);
		return MyMPI.buf_char_[0];
	}
	public static void sendChar(int dest, int tag, char c) {
		MyMPI.buf_char_[0]=c;
		MPI.COMM_WORLD.Send(MyMPI.buf_char_, 0, 1, MPI.CHAR, dest, tag);
	}
}

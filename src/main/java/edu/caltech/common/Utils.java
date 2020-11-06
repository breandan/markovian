package edu.caltech.common;

import java.io.*;
import java.util.Random;
import edu.caltech.spn.Instance;

public class Utils {
	// return intensity value in gray scale
	public static int getIntVal(Instance inst, double p) {
		return (int)(p*inst.std_+inst.mean_);				
	}
	
	// time
	static Timer timer_=new Timer();
	static {timer_.timerStart();}
	public static void logTime(String msg) {
		long sec=timer_.getTimeElapsed()/1000;
		println("<TIME> "+msg+" "+sec+"s");
		timer_.timerStart();
	}
	public static void logTimeMS(String msg) {
		long sec=timer_.getTimeElapsed();
		println("<TIME> "+msg+" "+sec+" ms");
		timer_.timerStart();
	}	

	// logging
	public static PrintStream out_=System.out;
	public static void setOut(PrintStream out) {out_=out;}
	public static void print(String s) {out_.print(getPrefix()+s);}
	public static void println(String s) {print(s+"\n");}
	public static void println() {print("\n");}
	static String getPrefix() {
		return "[Rank="+MyMPI.rank_+"] ";
	}
	public static String leftPad(String s,int len,char c) {
		for (int i=0; i<len-s.length(); i++) s=c+s;
		return s;
	}
	
	// numeric
	public static double round(double x) {return round(x,2);}
	public static double round(double x, int n) {
		double k=1.0;
		for (int i=0; i<n; i++)	k*=10;
		int y=(int)(x*k);
		return y*1.0/k;
	}
	public static double addLog(double l1, double l2) {
		if (l1>l2) {
			return l1+Math.log(1+Math.exp(l2-l1));
		}
		else {
			return l2+Math.log(1+Math.exp(l1-l2));
		}
	}

	// random
	public static long seed_=-1;	// reproducible; different seed for different slave
	public static Random random_;
}
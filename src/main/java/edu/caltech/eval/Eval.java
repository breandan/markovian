package edu.caltech.eval;

import java.io.*;
import java.util.*;

import edu.caltech.common.*;

public class Eval {
	static String expDir_="/projects/dm/2/hoifung/projects/dspn/release";
	
	public static void main(String[] args) throws Exception {
		evalCaltech();
		evalOlivetti();		
	}
	
	static void evalOlivetti() throws Exception {
		int size=Parameter.inputDim1_, padLen=10;
		String oliveDir=expDir_+"/results/olivetti";
		double lm=-1; try {lm=cmpMSELeft(oliveDir+"/completions/olive-left.dat", size, padLen);} catch(Exception e){}
		double bm=-1; try {bm=cmpMSEBottom(oliveDir+"/completions/olive-btm.dat", size, padLen);} catch(Exception e){}
		
		System.out.println("\n\nOlivetti MSE\tLeft="+Math.round(lm)+"\tBottom="+Math.round(bm));
	}
	
	static void evalCaltech() throws Exception {
		String caltechDir=expDir_+"/results/caltech/completions";

		int size=64, padLen=10;
		File dir=new File(caltechDir);
		String[] rsts=dir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.indexOf("-left.dat")>0;
			}
		});
		Arrays.sort(rsts);

		double ttlLtMse=0, ttlBmMse=0;
		int numCat=0;
		
		System.out.println("CAT\tLeft\tBottom");
		for (int di=0; di<rsts.length; di++) {
			String fn=rsts[di];
			String cat=fn.substring(0,fn.indexOf("-left.dat"));
			if (cat.equals("BACKGROUND_Google")) continue;
			try {
				double lm=cmpMSELeft(caltechDir+"/"+cat+"-left.dat", size, padLen);
				double bm=cmpMSEBottom(caltechDir+"/"+cat+"-btm.dat", size, padLen);
				numCat++;	ttlLtMse+=lm; ttlBmMse+=bm;
				System.out.println(cat+" "+Math.round(lm)+" "+Math.round(bm));
			}
			catch(Exception e) {
				throw e;
			}
		}
		System.out.println("\nCaltech-"+numCat+"\t"+Math.round(ttlLtMse/numCat)+"\t"+Math.round(ttlBmMse/numCat));		
	}

	static double cmpMSELeft(String fn, int size, int padLen) throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(fn));
		double p=0; int c=0;
		String s;
		int idx=0;
		while ((s=in.readLine())!=null) {
			idx++;			
			if (idx==size+padLen) {
				idx=0;
			}
			else if (idx>size) {
				continue;
			}
			else {
				String[] ts=s.split(",");
				for (int i=0; i<size/2; i++) {
					double q1=Double.parseDouble(ts[i]), q2=Double.parseDouble(ts[i+size+padLen]);
					p+=(q1-q2)*(q1-q2);
					c++;
				}
			}
		}
		in.close();
		return p/c;
	}
	
	static double cmpMSEBottom(String fn, int size, int padLen) throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(fn));
		double p=0; int c=0;
		String s;
		int idx=0;
		while ((s=in.readLine())!=null) {
			idx++;			
			if (idx==size+padLen) {
				idx=0;
			}
			else if (idx>size) {
				continue;
			}
			else if (idx>size/2) {
				String[] ts=s.split(",");
				for (int i=0; i<size; i++) {
					double q1=Double.parseDouble(ts[i]), q2=Double.parseDouble(ts[i+(size+padLen)]);
					p+=(q1-q2)*(q1-q2);
					c++;
				}
			}
		}
		in.close();
		return p/c;
	}
}

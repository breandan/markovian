package edu.caltech.eval;

import java.util.*;
import java.io.*;

import edu.caltech.common.*;
import edu.caltech.spn.Instance;

public class Dataset {
	// tmp
	static int[][] tmp_=new int[Parameter.inputDim1_][Parameter.inputDim2_];	// buffer for proc imgs 
	
	// data
	static String expDir_="../..";
	static String olivettiRawFileName_=expDir_+"/data/olivetti/olivetti.raw";
	static String calDataDir_=expDir_+"/data/caltech";
	static String calRstDir_=expDir_+"/results/caltech/completions";
	static String calMdlDir_=expDir_+"/results/caltech/models";
	static FilenameFilter calFileNameFilter_=new FilenameFilter() {
		public boolean accept(File dir, String name) {
			if (name.indexOf(".raw.rescale")<0) return false; else return true;
		}
	};
	static int RESCALE_LEN_=100;

	ArrayList<Instance> train_, test_;

	// dataset
	public ArrayList<Instance> getTrain() {return train_;}
	public ArrayList<Instance> getTest() {return test_;}

	// divide train/test
	static Set<Integer> genTestIdx(int maxSize, int testSize) {		
		Set<Integer> tis=new TreeSet<Integer>();
		for (int i=maxSize-testSize; i<maxSize; i++) {
			tis.add(i); if (tis.size()==testSize) break;
		}
		return tis;
	}	
	
	static void setInstance(int[][] buf, Instance inst) {	
		double tf=0, varf=0; int cf=0;
		for (int i=0; i<Parameter.inputDim1_; i++)
			for (int j=0; j<Parameter.inputDim2_; j++) {
				tf+=buf[i][j]; varf+=buf[i][j]*buf[i][j]; cf++;
			}
		tf/=cf; varf/=cf; inst.mean_=tf; inst.std_=Math.sqrt(varf-tf*tf);
		inst.vals_=new double[Parameter.inputDim1_][Parameter.inputDim2_];		
		for (int i=0; i<Parameter.inputDim1_; i++)
			for (int j=0; j<Parameter.inputDim2_; j++) {
				inst.vals_[i][j]=(buf[i][j]-inst.mean_)/inst.std_;
			}
	}	
	
	// --------------------------------------------------------- // 
	// Caltech
	// --------------------------------------------------------- //
	public void loadCaltech(String dirName) throws Exception {
		File d=new File(calDataDir_+"/"+dirName);			
		System.out.println("Loading "+d.getAbsolutePath());
		String[] fns=d.list(calFileNameFilter_);
		Arrays.sort(fns);
		int maxSize=fns.length;
		int testSize=maxSize/3;
		if (testSize>Parameter.maxTestSize_) testSize=Parameter.maxTestSize_;
		train_=new ArrayList<Instance>();
		test_=new ArrayList<Instance>();
		for (int fi=0; fi<fns.length; fi++) {			
			Instance inst=readCalInstance(d.getAbsolutePath()+"/"+fns[fi]);
			if (fi<maxSize-testSize) train_.add(inst);
			else test_.add(inst);
		}
		if (!MyMPI.isClassMaster_ && MyMPI.myOffset_==0) 
			Utils.println(dirName+": train.size="+train_.size()+" test.size="+test_.size());
	}	
		
	static Instance readCalInstance(String fn) throws Exception {
		int delta=(RESCALE_LEN_-Parameter.inputDim1_)/2;
		
		Instance inst=new Instance();
		BufferedReader in;
		in = new BufferedReader(new FileReader(fn));

		String s;
		String[] ts;
		int idx=0;		
		while ((s=in.readLine())!=null) {
			s=s.trim();
			if (s.length()==0) continue;			
			ts=s.split(" ");
			
			int trueIdx=idx-delta;
			if (trueIdx>=0 && trueIdx<Parameter.inputDim1_)
			for (int k=0; k<Parameter.inputDim2_; k++) {
				int p=(int) Double.parseDouble(ts[k+delta]);
				tmp_[trueIdx][k]=p;
			}			
			idx++;
		}
		in.close();		
		setInstance(tmp_, inst);
		return inst;
	}	
	
	// --------------------------------------------------------- // 
	// Olivetti
	// --------------------------------------------------------- // 
	public void loadOlivetti() throws Exception {
		Set<Integer> tis=genTestIdx(400,Parameter.maxTestSize_);
		BufferedReader in;
		in = new BufferedReader(new FileReader(olivettiRawFileName_));
		double[][] faces=new double[4096][400];
		String s;
		String[] ts;
		int idx=0;		
		while ((s=in.readLine())!=null) {
			s=s.trim();
			if (s.length()==0) continue; 
			
			ts=s.split("  ");
			for (int i=0; i<400; i++) faces[idx][i]=Double.parseDouble(ts[i]);
			idx++;
		}
		in.close();
		
		//
		train_=new ArrayList<Instance>();
		test_=new ArrayList<Instance>();
		
		for (int pi=0; pi<400; pi++) {
			Instance inst=readOlivettiInstance(faces, pi);
			if (tis.contains(pi)) test_.add(inst);
			else train_.add(inst);
		}
	}
	
	static Instance readOlivettiInstance(double[][] faces, int pi) {
		Instance inst=new Instance();
		for (int i=0; i<Parameter.inputDim1_; i++)
			for (int j=0; j<Parameter.inputDim2_; j++) {
				int k=j*Parameter.inputDim1_+i;
				tmp_[i][j]=(int)faces[k][pi];
			}
		setInstance(tmp_, inst);
		return inst;
	}	
}

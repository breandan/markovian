package edu.caltech.eval;

import edu.caltech.spn.SPN;
import edu.caltech.spn.GenerativeLearning;

import java.io.*;
import java.util.*;

import edu.caltech.common.*;

import mpi.MPI;

public class Run {
	// domains
	static String DOM_OLIVETTI_="O";
	static String DOM_CALTECH_="C";
	
	// directory
	static String expDir_="/projects/dm/2/hoifung/projects/dspn/release";
	
	static String oliveDataDir_=expDir_+"/data/olivetti";
	static String oliveRstDir_=expDir_+"/results/olivetti/completions";
	static String oliveMdlDir_=expDir_+"/results/olivetti/models";
	
	static String calDataDir_=expDir_+"/data/caltech";
	static String calRstDir_=expDir_+"/results/caltech/completions";
	static String calMdlDir_=expDir_+"/results/caltech/models";

	static FilenameFilter caltechImgNameFilter_=new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			if (name.indexOf(".raw.rescale")>0) return true;
			return false;
		}
	};
	
	// main
	public static void main(String[] args) throws Exception {
		MPI.Init(args);
		proc(args);
	}

	static void proc(String[] args) throws Exception {	
		procArgs(args);		
		if (Parameter.domain_.equals(DOM_OLIVETTI_)) {
			runOlivetti();
		}
		else if (Parameter.domain_.equals(DOM_CALTECH_)) {
			runCaltech();
		}		
		MPI.Finalize();
	}
	
	static void procArgs(String[] args) {
		for (int i=0; i<args.length; i++) {
			if (args[i].equals("-d")) {
				Parameter.domain_=args[++i];
			}
			else if (args[i].equals("-ncv")) {
				Parameter.numComponentsPerVar_=Integer.parseInt(args[++i]);
			}
			else if (args[i].equals("-nsr")) {
				Parameter.numSumPerRegion_=Integer.parseInt(args[++i]);
			}
			else if (args[i].equals("-sp")) {				
				Parameter.sparsePrior_=Double.parseDouble(args[++i]);
			}
			else if (args[i].equals("-br")) {				
				Parameter.baseResolution_=Integer.parseInt(args[++i]);
			}
			else if (args[i].equals("-ct")) {				
				Parameter.thresholdLLHChg_=Double.parseDouble(args[++i]);
			}
			else if (args[i].equals("-bs")) {
				Parameter.batch_size_=Integer.parseInt(args[++i]);
			}
			else if (args[i].equals("-ns")) {
				Parameter.numSlavePerClass_=Integer.parseInt(args[++i]);
			}
			else if (args[i].equals("-nsg")) {
				Parameter.numSlaveGrp_=Integer.parseInt(args[++i]);
			}			
		}
		
		if (Parameter.domain_==null) {
			if (MyMPI.rank_==0) 
				Utils.println("\n\nOptions: [-d <domain>]" +
					" [-sp <sparsePrior>]" +					
					" [-br <baseResolution>]" +
					" [-ncv <numComponentsPerVar>]" +
					" [-nsr <numSumPerRegion>]" +
					" [-ct <convergencyThrehold>]" +
					" [-bs <batchSize>]" +
					" [-ns <numSlavePerCat>] " + 
					" [-nsg <numSlaveGrp>]");
			System.exit(0);
		}
	}
	
	static void runCaltech() throws Exception {
		int myId=MyMPI.rank_/(Parameter.numSlavePerClass_+1);
				
		Map<String,Map<String,String>> cat_info=getCaltechInfo();
		int numCat=-1;		
		for (String cat: cat_info.keySet()) {
			numCat++;
			if (numCat%Parameter.numSlaveGrp_!=myId) continue;
			
			MyMPI.setConstantsForImgsParallel();			
			
			Utils.println("learning "+cat+" numCat="+numCat);
			Dataset data=new Dataset();
			data.loadCaltech(cat);	
			
			// learn
			GenerativeLearning l=new GenerativeLearning();			
			l.learn(data.getTrain());
			SPN dspn=l.getDSPN();				
			if (MyMPI.myOffset_==0) dspn.saveDSPN(calMdlDir_+"/"+cat);
			Utils.logTimeMS("unsup learn for completion done: "+cat);
			
			// complete left/bottom
			ImageCompletion.completeLeft(dspn, data.getTest(), cat, calRstDir_);
			ImageCompletion.completeBottom(dspn, data.getTest(), cat, calRstDir_);
		}		
	}
	static Map<String,Map<String,String>> getCaltechInfo() throws Exception {
		// cat - <key, val>
		Map<String,Map<String,String>> cat_info=new TreeMap<String, Map<String,String>>();
		File dir=new File(calDataDir_);
		String[] dirs=dir.list();
		for (int i=0; i<dirs.length; i++) {
			if (!(new File(calDataDir_+"/"+dirs[i])).isDirectory()) continue;
			cat_info.put(dirs[i], new HashMap<String,String>());
		}
		return cat_info;
	}

	
	static void runOlivetti() throws Exception {
		MyMPI.setConstantsForImgs();		
		Dataset data=new Dataset();
		data.loadOlivetti();
		
		// learn
		GenerativeLearning l=new GenerativeLearning();			
		l.learn(data.getTrain());
		SPN dspn=l.getDSPN();
		if (MyMPI.myOffset_==0) {
			dspn.saveDSPN(oliveMdlDir_+"/olive");
		}
		
		// complete
		ImageCompletion.completeLeft(dspn, data.getTest(), "olive", oliveRstDir_);
		ImageCompletion.completeBottom(dspn, data.getTest(), "olive", oliveRstDir_);
	}
}

package edu.washington.exp;

import edu.washington.slalg.VarInstSplit;
import edu.washington.spn.GraphSPN;
import edu.washington.spn.Node;
import edu.washington.spn.SmoothedMultinomialNode;
import edu.washington.util.Parameter;
import edu.washington.data.Dataset;
import edu.washington.data.Discretized;
import edu.washington.data.Partition;

public class RunSLSPN {
	static Dataset d;
	public static int data_id;
	public static String queryfile;
	public static String evidencefile;
	
	public static Class ds[] = new Class[] {Discretized.EachMovie.class,
			Discretized.MSWeb.class, 
			Discretized.KDD.class,
			Discretized.S20NG.class,
			Discretized.Abalone.class,
			Discretized.Adult.class,
			Discretized.Audio.class,
			Discretized.Book.class,
			Discretized.Covertype.class,
			Discretized.Jester.class,
			Discretized.MSNBC.class, // 10
			Discretized.Netflix.class,
			Discretized.NLTCS.class,
			Discretized.Plants.class,
			Discretized.R52.class,
			Discretized.School.class,
			Discretized.Traffic.class,
			Discretized.WebKB.class,
			Discretized.Wine.class, // 18
			Discretized.Accidents.class,
			Discretized.Ad.class,
			Discretized.BBC.class,
			Discretized.C20NG.class,
			Discretized.CWebKB.class,
			Discretized.DNA.class,
			Discretized.Kosarek.class,
			Discretized.Retail.class,
			Discretized.Pumsb_Star.class,
			Discretized.CR52.class}; // 28
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long tic = System.currentTimeMillis();
		parseParameters(args);

		try {
			d = (Dataset) ds[data_id].newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("\nCP "+VarInstSplit.clusterPenalty+"\tGF "+VarInstSplit.gfactor+"\tIT "+VarInstSplit.indepInstThresh);

		// Run SL
		VarInstSplit slalg = new VarInstSplit();
		GraphSPN spn = slalg.learnStructure(d);

		long toc = System.currentTimeMillis();
		
		double highestValidation = Double.NEGATIVE_INFINITY;

		double smoo = 1.0;
		for(smoo = 2.0; smoo>=0.02; smoo*=0.5){
			SmoothedMultinomialNode.smooth = smoo; //0.0000001
			for(Node n : spn.order){
				if(n instanceof SmoothedMultinomialNode){
					SmoothedMultinomialNode smn = (SmoothedMultinomialNode) n;
					smn.reset();
				}
			}
			System.out.println("\nCP "+VarInstSplit.clusterPenalty+"\tGF "+VarInstSplit.gfactor+"\tIT "+VarInstSplit.indepInstThresh+"\tSmoo"+SmoothedMultinomialNode.smooth);

			//				spn.printDepth(10);
			double llh = spn.llh(Partition.Training);
			System.out.println("Train LLH: "+llh);

			llh = spn.llh(Partition.Validation);
			System.out.println("Validation LLH: "+llh);
			if(llh > highestValidation){
				highestValidation = llh;
				spn.write(edu.washington.util.Parameter.filename);
				System.out.println("***");
			}
		} // end smoothing loop


		spn = GraphSPN.load(edu.washington.util.Parameter.filename, d);
		double vllh = spn.llh(Partition.Validation);
		double tllh = spn.llh(Partition.Testing);

		// print all params separated by tabs
		System.out.print(data_id+" ");
		System.out.print(d);
		System.out.print("\tGF: "+VarInstSplit.gfactor);
		System.out.print("\tCP: "+VarInstSplit.clusterPenalty);
		System.out.print("\tValid: "+vllh);
		System.out.print("\tTest: "+tllh);
		System.out.println("\tTime: "+1.0*(toc-tic)/1000);
	}


	public static void parseParameters(String[] args){
		int pos = 0;

		while(pos<args.length){

			// Data source
			if(args[pos].equals("DATA")){
				data_id = Integer.parseInt(args[++pos]);
				System.out.println(args[pos-1]+"\t"+args[pos]);
			}
			
			// Gfactor
			if(args[pos].equals("GF")){
				VarInstSplit.gfactor = Double.parseDouble(args[++pos]);
				System.out.println(args[pos-1]+"\t"+args[pos]);
			}

			// Cluster penalty
			if(args[pos].equals("CP")){
				VarInstSplit.clusterPenalty = Double.parseDouble(args[++pos]);
				System.out.println(args[pos-1]+"\t"+args[pos]);
			}

			// If there are this many or fewer instances, assume that all variables are independent
			// This just saves us from needlessly computing the pairwise independence test
			if(args[pos].equals("INDEPINST")){
				VarInstSplit.indepInstThresh = Integer.parseInt(args[++pos]);
				System.out.println(args[pos-1]+"\t"+args[pos]);
			}

			// Split MI comp. PLL
			if(args[pos].equals("COMPPLL")){
				VarInstSplit.compPLL = true;
				System.out.println(args[pos]);
			}

			// Filename to save model
			if(args[pos].equals("N")){
				Parameter.filename = args[++pos];
				System.out.println(args[pos-1]+"\t"+args[pos]);
			}

			// Query file
			if(args[pos].equals("Q")){
				queryfile = args[++pos];
				System.out.println(args[pos-1]+"\t"+args[pos]);
			}
			
			// Evidence file
			if(args[pos].equals("EV")){
				evidencefile = args[++pos];
				System.out.println(args[pos-1]+"\t"+args[pos]);
			}
			
			
			pos++;
		}

	}

}

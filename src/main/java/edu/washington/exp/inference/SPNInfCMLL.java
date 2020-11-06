package edu.washington.exp.inference;

import edu.washington.spn.GraphSPN;
import edu.washington.util.Parameter;
import edu.washington.data.Dataset;
import edu.washington.data.Partition;
import edu.washington.data.QueryDataset;
import edu.washington.exp.RunSLSPN;

public class SPNInfCMLL {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RunSLSPN.parseParameters(args);
		String prefix = "edu/caltech/data/";
		Dataset d = new QueryDataset(prefix+RunSLSPN.queryfile, prefix+RunSLSPN.evidencefile);
		
		int numQueryVars = ((QueryDataset) d).getNumQueryVars(0, null);
		System.out.println(numQueryVars);
		int queryVarList[] = new int[numQueryVars];
		
		GraphSPN spn = GraphSPN.load(Parameter.filename, d);
		
		double LL = 0, LLsq = 0;
		long tic = System.currentTimeMillis();
		for(int inst=0; inst<d.getNumTesting(); inst++){
			double ill = 0;
			// P(q | ev) = P(ev,q) / P(e)
			
			((QueryDataset) d).showQuery(inst, Partition.Testing);
			int c=0;
			int vals[] = ((QueryDataset) d).getValues();
			for(int j=0; j<vals.length; j++){
				if(vals[j] > -1){
					queryVarList[c++] = j;
//					System.out.print(j+" ");
				}
			}
//			System.out.println();
			
			for(c=0; c<numQueryVars; c++){
				((QueryDataset) d).showEvidencePlusAQuery(inst, Partition.Testing, queryVarList[c]);
				ill += spn.upwardPass();
			}
			
			((QueryDataset) d).showEvidence(inst, Partition.Testing);
			ill -= numQueryVars * spn.upwardPass();
			
			System.out.println(ill);
			LL += ill;
			LLsq += ill*ill;
		}
		long toc = System.currentTimeMillis();
		LL /= d.getNumTesting();
		LLsq /= d.getNumTesting();
		
		System.out.println("avg = "+LL+" +/- "+Math.sqrt(LLsq - LL*LL));
		System.out.println("Total time: "+(1.0*(toc-tic)/1000)+"s");
		// avg = -21.734815 +/- 0.363440
		// Total time: 424.504467s

	}

}

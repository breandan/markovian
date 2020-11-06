package edu.washington.exp.inference;

import edu.washington.spn.GraphSPN;
import edu.washington.util.Parameter;
import edu.washington.data.Dataset;
import edu.washington.data.Partition;
import edu.washington.data.QueryDataset;
import edu.washington.exp.RunSLSPN;

public class SPNInf {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RunSLSPN.parseParameters(args);
		String prefix = "edu/caltech/data/";
		Dataset d = new QueryDataset(prefix+RunSLSPN.queryfile, prefix+RunSLSPN.evidencefile);
		
		GraphSPN spn = GraphSPN.load(Parameter.filename, d);
		
		double LL = 0, LLsq = 0;
		long tic = System.currentTimeMillis();
		for(int inst=0; inst<d.getNumTesting(); inst++){
			double ill = 0;
			// P(q | ev) = P(ev,q) / P(e)
			((QueryDataset) d).showJoint(inst, Partition.Testing);
			ill += spn.upwardPass();
			((QueryDataset) d).showEvidence(inst, Partition.Testing);
			ill -= spn.upwardPass();
			
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

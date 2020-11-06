package edu.washington.exp.inference;

import edu.washington.spn.GraphSPN;
import edu.washington.util.Parameter;
import edu.washington.data.Dataset;
import edu.washington.data.Partition;
import edu.washington.exp.RunSLSPN;

public class SPNInfPLL {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RunSLSPN.parseParameters(args);
		String prefix = "edu/caltech/data/";
		
		Dataset d = null;
		try {
			d = (Dataset) RunSLSPN.ds[RunSLSPN.data_id].newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		GraphSPN spn = GraphSPN.load(Parameter.filename, d);
		
		double LL = 0, LLsq = 0;
		long tic = System.currentTimeMillis();
		for(int inst=0; inst<d.getNumTesting(); inst++){
			
			double ill = 0;
			// P(q | ev) = P(ev,q) / P(e)
			
			d.show(inst, Partition.Testing);
			ill += spn.upwardPass() * d.getNumFeatures();
			int rightvals[] = d.getValues().clone();
			int refvals[] = d.getValues();
			for(int marg=0; marg < d.getNumFeatures(); marg++){
				if(marg>0){
					refvals[marg-1] =rightvals[marg-1];
				}
				refvals[marg] = -1;
				ill -= spn.upwardPass();
			}
			refvals[d.getNumFeatures()-1] = rightvals[d.getNumFeatures()-1];
			
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

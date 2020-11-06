package edu.washington.spn;

import edu.washington.data.Dataset;
import edu.washington.data.Partition;

public class SmoothedMultinomialNode extends Node implements DatasetDep {
	private static final long serialVersionUID = 1L;
	public static double smooth = 0.0000001;
	private double logvals[];
	private final int attr;
	private Dataset d;
	private final int[] instances;
	
	public SmoothedMultinomialNode(Dataset d, int variable, int instances[]) {
		this.d = d;
		attr = variable;
		this.instances = instances; 
		
		reset();
	}
	
	public void setDataset(Dataset d) {
		this.d = d;
	}
	
	public void reset(){
		int numvals = d.getAttrSizes()[attr];
		double counts[] = new double[numvals];
		double countsum=numvals*smooth;
		
		for(int val=0; val<numvals; val++){
			counts[val] = smooth;
		}
		
		for(int inst : instances){
			d.show(inst, Partition.Training);
			int setting = d.getValues()[attr];
			counts[setting]++;
			countsum++;
		}
		
		logvals = new double[numvals];
		for(int val=0; val<numvals; val++){
			logvals[val] = Math.log(counts[val]) - Math.log(countsum);
		}
	}
	
	@Override
	public void eval() {
		int setting = d.getValues()[attr];
		double new_logval_;
		
		if(setting == -1){
			new_logval_ = 0;
		} else {
			new_logval_ = logvals[setting];
		}
		
		if(new_logval_ == logval_){
			dirty = false;
		} else {
			logval_ = new_logval_;
			dirty = true;
		}
		
	}
	
}

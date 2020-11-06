package edu.washington.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public abstract class SparseDataset extends Dataset {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int numVar;
	int numTraining, numTesting, numValidation;
	int data[][];
	protected int attrSizes[];
	List<List<Integer>> sp_attr_inst, sp_inst_attr;
	
	int currentInstance = 0;
	
	void makesparse() {
		int numInst = data.length;
		int numAttr = data[0].length;
		
		sp_attr_inst = new ArrayList<List<Integer>>();
		sp_inst_attr = new ArrayList<List<Integer>>();
		
		for(int i=0; i<numInst; i++){
			sp_inst_attr.add(new ArrayList<Integer>());
		}
		for(int a=0; a<numAttr; a++){
			sp_attr_inst.add(new ArrayList<Integer>());
		}
		
		for(int i=0; i<numInst; i++){
			for(int a=0; a<numAttr; a++){
				if(data[i][a] != 0){
					sp_inst_attr.get(i).add(a);
					sp_attr_inst.get(a).add(i);
				}
			}
		}
		
	}
	
	public int[][] count(Integer v, Integer ov, HashSet<Integer> instances) {
		List<Integer> nonzero_insts_forv = sp_attr_inst.get(v);
		List<Integer> nonzero_insts_forov = sp_attr_inst.get(ov);
		
		int counts[][] = new int[attrSizes[v]][attrSizes[ov]];
		int noncounts = instances.size(); 
		
		int i=0;
		int instv = nonzero_insts_forv.isEmpty() ? Integer.MAX_VALUE : nonzero_insts_forv.get(i);
		
		int j=0;
		int instov = nonzero_insts_forov.isEmpty() ? Integer.MAX_VALUE : nonzero_insts_forov.get(j);
		
		while(i<nonzero_insts_forv.size() || j<nonzero_insts_forov.size()){
			
			while(instov<instv && j<nonzero_insts_forov.size()){
				// if instov is in instances, count it by itself
				if(instances.contains(instov)){
					counts[0][data[instov][ov]]++;
					noncounts--;
				}
				
				j++;
				if(j<nonzero_insts_forov.size()){
					instov = nonzero_insts_forov.get(j);
				} else {
					instov = Integer.MAX_VALUE;
				}
			}
			
			// if instov == instv and in instances, count both
			if(instov == instv && instances.contains(instv)){
				counts[data[instv][v]][data[instov][ov]]++;
				noncounts--;
				j++;
				if(j<nonzero_insts_forov.size()){
					instov = nonzero_insts_forov.get(j);
				} else {
					instov = Integer.MAX_VALUE;
				}
			}
			// if instov > instv and in instances, count v by itself
			else if(instov > instv && instances.contains(instv)){
				counts[data[instv][v]][0]++;
				noncounts--;
			}
			
			i++;
			if(i<nonzero_insts_forv.size()){
				instv = nonzero_insts_forv.get(i);
			} else {
				instv = Integer.MAX_VALUE;
			}
		}
		counts[0][0] = noncounts;
		return counts;
	}

	public int ham(int seed, int inst, HashSet<Integer> set_variables) {
		List<Integer> nonzero_vars_fori = sp_inst_attr.get(seed);
		List<Integer> nonzero_vars_forj = sp_inst_attr.get(inst);
		
		int ham=0;
		
		int i=0;
		int vari = nonzero_vars_fori.isEmpty() ? Integer.MAX_VALUE : nonzero_vars_fori.get(i);
		
		int j=0;
		int varj = nonzero_vars_forj.isEmpty() ? Integer.MAX_VALUE : nonzero_vars_forj.get(j);
		
		while(i<nonzero_vars_fori.size() || j<nonzero_vars_forj.size()){
			
			while(varj<vari && j<nonzero_vars_forj.size()){
				// if instov is in instances, count it by itself
				if(set_variables.contains(varj)){
					ham++;
				}
				
				j++;
				if(j<nonzero_vars_forj.size()){
					varj = nonzero_vars_forj.get(j);
				} else {
					varj = Integer.MAX_VALUE;
				}
			}
			
			// if instov == instv and in instances, count both
			if(varj == vari && set_variables.contains(vari)){
				if(data[seed][vari] != data[inst][varj]){
					ham++;
				}
				j++;
				if(j<nonzero_vars_forj.size()){
					varj = nonzero_vars_forj.get(j);
				} else {
					varj = Integer.MAX_VALUE;
				}
			}
			// if instov > instv and in instances, count v by itself
			else if(varj > vari && set_variables.contains(vari)){
				ham++;
			}
			
			i++;
			if(i<nonzero_vars_fori.size()){
				vari = nonzero_vars_fori.get(i);
			} else {
				vari = Integer.MAX_VALUE;
			}
		}
		
		return ham;
	}

	public List<List<Integer>> get_sp_inst_attr() {
		return sp_inst_attr;
	}

	@Override
	public void show(int i, Partition testing, boolean b) {
		switch (testing) {
		case Training:
			currentInstance = i;
			break;
		case Validation:
			currentInstance = numTraining + i;
			break;
		case Testing:
			currentInstance = numTraining + numValidation + i;
			break;
		default:
			break;
		}
	}

	@Override
	public int getUniqueInstanceID() {
		return currentInstance;
	}
	
	@Override
	public int getNumFeatures() {
		return numVar;
	}

	@Override
	public int[] getAttrSizes() {
		return attrSizes;
	}

	@Override
	public int[] getValues() {
		return data[currentInstance];
	}
	
	@Override
	public int getNumTesting() {
		return numTesting;
	}

	@Override
	public int getNumTraining() {
		return numTraining;
	}

	@Override
	public int getNumValidation() {
		return numValidation;
	}
	
	@Override
	public double getNumFolds() {
		return 1;
	}

	@Override
	public int getNumClasses() {
		return 0;
	}

	@Override
	public int trueLabel() {
		return 0;
	}
}

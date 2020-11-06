package edu.washington.slalg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import edu.washington.spn.GraphSPN;
import edu.washington.spn.Node;
import edu.washington.spn.ProdNode;
import edu.washington.spn.SmoothedMultinomialNode;
import edu.washington.spn.SumNode;
import edu.washington.util.SPNMath;
import edu.washington.data.Dataset;
import edu.washington.data.Partition;
import edu.washington.data.SparseDataset;

public class VarInstSplit implements SLAlg {
	private static final int MAX_NB_CLUSTERS = 2000;
	public static boolean verbose = true;
	public static double pval = 0.3;
	public static double clusterPenalty = 2;
	public static int indepInstThresh = 1;
	public static boolean compPLL = true;
	Dataset d;
	public static boolean onlyAddMostDependent = false;
	public static double gfactor = 1.0;

	@SuppressWarnings("unused")
	@Override
	public GraphSPN learnStructure(Dataset d) {
		this.d = d;
		int numCmsg = 6;

		// Queue of <#id, {variables}x{instances}>
		// ... decide whether it's +/x and then recurse
		// List of nodes to create #0 <+, w1x#1, w2x#2>
		//						   #2 <x, #3, #4>
		// when gets to single var/inst, just create SPN node
		// when queue of slices to expand is empty, create nodes from build list

		// Queue for slices of variable-instance space to pursue
		Queue<VarInstSlice> toProcess = new LinkedList<VarInstSlice>();

		// When you build a node, you need to let its future parents know where it is
		HashMap<Integer, Node> idNodeMap = new HashMap<Integer, Node>();

		// Nodes that will be built once all leaf distributions are created
		List<BuildItem> buildList = new ArrayList<BuildItem>();

		int allInstances[] = new int[d.getNumTraining()];
		for(int i=0; i<allInstances.length; i++) allInstances[i] = i;

		int allVariables[] = new int[d.getNumFeatures()];
		for(int i=0; i<allVariables.length; i++) allVariables[i] = i;

		int nextID = 0;

		if(verbose) System.out.println("Building structure from "+allVariables.length+" vars over "+allInstances.length+" instances");

		// Start with all variables over all instances
		VarInstSlice wholeTraingingset = new VarInstSlice(nextID++, allInstances, allVariables);
		toProcess.add(wholeTraingingset);

		GraphSPN spn = new GraphSPN(d);

		while(!toProcess.isEmpty()){
			VarInstSlice currentSlice = toProcess.remove();
			int id = currentSlice.id;

			if(currentSlice.variables.length == 1){
				Node varnode = new SmoothedMultinomialNode(d, currentSlice.variables[0], currentSlice.instances);
				spn.order.add(varnode);
				idNodeMap.put(id, varnode);
				continue;
			}

			if(currentSlice.instances.length<=indepInstThresh && currentSlice.variables.length>1){
				int idx = 0;
				int ch_ids[] = new int[currentSlice.variables.length];

				for(int var : currentSlice.variables){
					int ch_id1 = nextID++;
					ch_ids[idx++] = ch_id1;

					VarInstSlice ch1 = new VarInstSlice(ch_id1, currentSlice.instances, new int[] {var});
					toProcess.add(ch1);
				}
				BuildItem newprod = new BuildItem(id, false, ch_ids, null);
				buildList.add(newprod);
				continue;
			}

			// measure appx. independence
			int indset[] = null;

			CountCache cc = null;
			if(nextID > 1){
				long tic = System.currentTimeMillis();
				cc = new CountCache();

				indset = greedyIndSet(d, currentSlice,cc);

				long toc = System.currentTimeMillis();
				//				System.out.println((toc-tic)+"ms");
			} else {
				indset = new int[0]; // Don't try to measure independence on the first node (would be a trivial dataset)
			}


			if(indset.length > 1){
				System.out.println("Indset size "+indset.length);
			}
			// If found an appx. independent set
			if(indset.length < currentSlice.variables.length && indset.length > 0){
				if(currentSlice.instances.length<4 && indset.length > 1) 
					System.out.println("Found indset (x) ("+indset.length+"/"+currentSlice.variables.length+") over "+currentSlice.instances.length+"I");

				int ch_id1 = nextID++;
				int ch_id2 = nextID++;
				BuildItem newprod = new BuildItem(id, false, new int[] {ch_id1, ch_id2}, null);
				buildList.add(newprod);

				VarInstSlice ch1 = new VarInstSlice(ch_id1, currentSlice.instances, indset);
				VarInstSlice ch2 = new VarInstSlice(ch_id2, currentSlice.instances, setminus(currentSlice.variables, indset));
				toProcess.add(ch1);
				toProcess.add(ch2);
			} else {
				int instsets[][] = null;
				ClustersWLL bestClustering = clusterNBInsts(d, currentSlice, cc);

				instsets = bestClustering.clusters;

				if(numCmsg-- > 0)
					System.out.println(currentSlice.instances.length+"I "+currentSlice.variables.length+"V -> "+instsets.length+" clusters");

				double ch_weights[] = new double[instsets.length];
				int ch_ids[] = new int[instsets.length];
				for(int c=0; c<instsets.length; c++){
					int ch_id1 = nextID++;
					double w1 = 1.0*instsets[c].length/currentSlice.instances.length;

					ch_ids[c] = ch_id1;
					ch_weights[c] = w1;

					VarInstSlice ch1 = new VarInstSlice(ch_id1, instsets[c], currentSlice.variables);
					toProcess.add(ch1);
				}
				BuildItem newsum = new BuildItem(id, true, ch_ids, ch_weights);
				buildList.add(newsum);	

			}
		}

		if(verbose) System.out.println("SPN has "+spn.order.size()+" nodes");

		if(verbose) System.out.println("Building from build list ("+buildList.size()+" items)...");
		while(!buildList.isEmpty()){
			BuildItem bi = buildList.remove(buildList.size()-1);

			if(bi.sum){
				SumNode sn = new SumNode(spn);
				for(int c=0; c<bi.ids.length; c++){
					Node ch = idNodeMap.get(bi.ids[c]);
					if(ch instanceof SumNode){
						SumNode snc = (SumNode) ch;
						for(int sc=0; sc<snc.keyOrder.size(); sc++){
							sn.addChdOnly(snc.keyOrder.get(sc), bi.weights[c] * snc.getW().get(sc));
						}
						spn.order.remove(snc);
					} else {
						sn.addChdOnly(ch, bi.weights[c]);
					}
				}
				spn.order.add(sn);
				idNodeMap.put(bi.id, sn);
			} else {
				ProdNode pn = new ProdNode();
				for(int c=0; c<bi.ids.length; c++){
					Node ch = idNodeMap.get(bi.ids[c]);
					if(ch instanceof ProdNode){
						ProdNode pnc = (ProdNode) ch;
						for(Node pc : pnc.allChildren()){
							pn.addChd(pc);
						}
						spn.order.remove(pnc);
					} else {
						pn.addChd(ch);
					}
				}
				spn.order.add(pn);
				idNodeMap.put(bi.id, pn);
			}
		}

		if(verbose) System.out.println("SPN has "+spn.order.size()+" nodes");

		return spn;
	}

	private ClustersWLL clusterNBInsts(Dataset d, VarInstSlice currentSlice, CountCache cc) {
		int numRuns = 10;
		int numEMits = 4;

		double bestLL = Double.NEGATIVE_INFINITY;
		int bestClusters[][] = null ;

		HashSet<Integer> vars = new HashSet<Integer>();
		for(Integer var : currentSlice.variables){
			vars.add(var);
		}


		ArrayList<Integer> instanceOrder = new ArrayList<Integer>();
		for(int inst : currentSlice.instances){
			instanceOrder.add(inst);
		}

		double newClusterLL = (new Cluster(vars)).newLL();		
		double newClusterPenalizedLL = -clusterPenalty * vars.size()+newClusterLL;
		for(int r=0; r<numRuns; r++){
			System.out.println("EM Run "+r+" with "+instanceOrder.size()+" insts "+vars.size()+" vars");
			List<Cluster> nbcs = new ArrayList<Cluster>();

			Collections.shuffle(instanceOrder);

			// data structure for assignment of inst to cluster
			// {instance}->num of cluster
			Map<Integer, Cluster> inst_cluster_map = new HashMap<Integer, Cluster>();
			double LL=Double.NEGATIVE_INFINITY;

			//			newClusterPenalizedLL *= 1.0 + Math.random()*0.4 - 0.2;
			for(int it=0; it<numEMits; it++){
				// LL = 0
				//				System.out.print("EM Iteration "+it);
				LL = 0;

				double minBest = Double.POSITIVE_INFINITY;
				int inc=0;
				for(int inst : instanceOrder){
					inc++;
					// remove SS from previously assigned cluster
					Cluster prev_cluster = inst_cluster_map.remove(inst);
					if(prev_cluster != null){
						prev_cluster.removeInst(inst);
						if(prev_cluster.isEmpty()){
							nbcs.remove(prev_cluster);
						}
					}

					// set best LL to be newClusterPenalty
					//double newClusterPenalty = -clusterPenalty * nbcs.size();

					double bestCLL = newClusterPenalizedLL;
					Cluster bestCluster = null;

					// find best cluster
					// quit when LL is less than best
					for(Cluster c : nbcs){
						double cll = c.ll(inst, Partition.Training, bestCLL);
						//						System.out.println("  "+inst+" vs "+c+" "+cll);
						if(cll > bestCLL){
							bestCLL = cll;
							bestCluster = c;							
						}
					}

					if(bestCLL < minBest){
						minBest = bestCLL;
					}

					// make new cluster if bestLL is not greater than penalty
					if(bestCluster == null){
						bestCluster = new Cluster(vars);
						nbcs.add(bestCluster);

						if(nbcs.size() > MAX_NB_CLUSTERS && currentSlice.instances.length > 10000){
							System.out.println("Too many clusters, increase CP penalty");
							System.exit(0);
						}
					}
					//					if(inc % 1000 == 0) System.out.println(inc+"inst w/ "+nbcs.size());

					// add SS to newly assigned cluster
					bestCluster.addInst(inst);
					inst_cluster_map.put(inst, bestCluster);

					LL += bestCLL;
				}
				//				System.out.println("\t"+nbcs.size()+" clusters\t"+LL+" LL");

				// Always ensure there's more than one cluster
				if(nbcs.size() == 1){
					it = 0;
					newClusterPenalizedLL *= 0.5;
				}
			} // End EM

			// if LL > best...
			// bestLL
			// bestClusters

			LL = penalizedLL(nbcs, currentSlice.instances, currentSlice.variables.length);

			if(LL > bestLL){
				bestLL = LL;
				HashMap<Cluster, Integer> clusterToId = new HashMap<VarInstSplit.Cluster, Integer>();
				int nextID = 0;
				List<List<Integer>> instSets = new ArrayList<List<Integer>>();
				for(Cluster c : nbcs){
					clusterToId.put(c, nextID++);
					instSets.add(new ArrayList<Integer>());
				}
				bestClusters = new int[nbcs.size()][];
				for(Entry<Integer, Cluster> e : inst_cluster_map.entrySet()){
					int cid = clusterToId.get(e.getValue());
					instSets.get(cid).add(e.getKey());
				}
				for(int cid=0; cid<bestClusters.length; cid++){
					bestClusters[cid] = new int[instSets.get(cid).size()];
					int i=0;
					for(Integer inst : instSets.get(cid)){
						bestClusters[cid][i++] = inst;
					}
				}
			} // end if this run of em is better

		} // end multiple runs of EM

		return new ClustersWLL(bestClusters, bestLL);
	}

	private double penalizedLL(List<Cluster> nbcs, int[] instances, int numVars) {
		double LL = 0;
		double clusterPriors[] = new double[nbcs.size()];
		for(int c=0; c<nbcs.size(); c++){
			clusterPriors[c] = 1.0 * nbcs.get(c).size / instances.length;
		}
		for(int trinst : instances){
			double logprob = Double.NEGATIVE_INFINITY;
			for(int c=0; c<nbcs.size(); c++){
				logprob = SPNMath.log_sum_exp(logprob, Math.log(clusterPriors[c]) + nbcs.get(c).ll(trinst, Partition.Training, Double.NEGATIVE_INFINITY));
			}
			LL += logprob;
		}

		LL -= clusterPenalty * numVars * nbcs.size();


		return LL;
	}

	class ClustersWLL {
		public int splitVar = 0;
		int clusters[][];
		double LL;
		public ClustersWLL(int[][] clusters, double lL) {
			this.clusters = clusters;
			LL = lL;
		}
		public ClustersWLL(int[][] clusters2, double pLL, Integer bestVar) {
			this.clusters = clusters2;
			LL = pLL;
			this.splitVar = bestVar;
		}
	}

	private class Cluster {
		double smoo = 0.1;
		double nbsmoo = 0.1;
		// data structure for sufficient statistics
		// {num clusters}x{attrs}x{attr val}
		Map<Integer,List<Integer>> sstats = new HashMap<Integer,List<Integer>>();
		Map<Integer,Integer> sstats_nonzero = new HashMap<Integer,Integer>();
		final HashSet<Integer> vars;
		int size = 0;

		public Cluster(HashSet<Integer> vars) {
			this.vars = vars;
		}

		public void removeInst(int inst) {
			d.show(inst, Partition.Training);
			int vals[] = d.getValues();

			size--;
			// find all non-zero attrs of inst, decrement counts in sstats
			for(Integer var : ((SparseDataset) d).get_sp_inst_attr().get(inst)){
				if(!vars.contains(var)){
					continue;
				}
				// Decrement count for the value of the non-zero attr
				sstats.get(var).set(vals[var], sstats.get(var).get(vals[var])-1);
				// Decrement count for the non-zero attr
				int newcount = sstats_nonzero.get(var)-1;
				sstats_nonzero.put(var, newcount);
				if(newcount == 0){
					sstats.remove(var);
					sstats_nonzero.remove(var);
				}
			}
		}

		public void addInst(int inst) {
			d.show(inst, Partition.Training);
			int vals[] = d.getValues();


			// find all non-zero attrs of inst, decrement counts in sstats
			for(Integer var : ((SparseDataset) d).get_sp_inst_attr().get(inst)){
				if(!vars.contains(var)){
					continue;
				}

				// Create list to count values of non-zero attrs (if not already created)
				if(!sstats.containsKey(var)){
					List<Integer> counts = new ArrayList<Integer>();
					for(int a=0; a<d.getAttrSizes()[var]; a++){
						counts.add(0);
					}
					sstats.put(var, counts);
				}

				// Increment count for the value of the non-zero attr
				sstats.get(var).set(vals[var], sstats.get(var).get(vals[var])+1);
				// Increment count for the non-zero attr
				sstats_nonzero.put(var, sstats_nonzero.containsKey(var) ? sstats_nonzero.get(var) + 1 : 1);
			}
			size++;
		}

		public double ll(int inst, Partition part, double bestCLL) {
			double l=0;

			d.show(inst, part);
			int att_sizes[] = d.getAttrSizes();
			int vals[] = d.getValues();

			for(Integer var : vars){
				// Lookup counts for this variable
				if(sstats_nonzero.containsKey(var)){ // Some vals of attr are non-zero in cluster
					if(vals[var]==0){
						int w = size - sstats_nonzero.get(var);
						l += Math.log((w + smoo) / (size + att_sizes[var]*smoo));
					} else {
						int w = sstats.get(var).get(vals[var]);
						l += Math.log((w + smoo) / (size + att_sizes[var]*smoo));
					}
				} else { // All values of attr are zero in the cluster
					if(vals[var]==0){
						l += Math.log((size + smoo) / (size + att_sizes[var]*smoo));
					} else {
						l += Math.log((smoo) / (size + att_sizes[var]*smoo));
					}
				}


				if(l < bestCLL){
					return l;
				}
			}


			return l;
		}

		double newLL(){
			double ll=0;
			int att_sizes[] = d.getAttrSizes();

			for(Integer var : vars){
				//				ll += Math.log((1 + smoo/att_sizes[var]) / (1 + smoo));
				ll += Math.log((1 + nbsmoo) / (1 + att_sizes[var]*nbsmoo));
			}
			return ll;
		}

		public boolean isEmpty() {
			return size==0;
		}

	}

	private int[] setminus(int[] whole, int[] part) {
		int toreturn[] = new int[whole.length-part.length];

		HashSet<Integer> temp = new HashSet<Integer>();
		for(int i : whole){ temp.add(i); }
		for(int i : part){ temp.remove(i); }

		int idx=0;
		for(int i : temp){
			toreturn[idx++] = i;
		}
		return toreturn;
	}

	private int[] greedyIndSet(Dataset d, VarInstSlice currentSlice, CountCache cc) {
		if(currentSlice.instances.length == 1){
			int returnset[] = new int[] {currentSlice.variables[0]};
			return returnset;
		}

		HashSet<Integer> vars = new HashSet<Integer>();
		for(int i : currentSlice.variables){
			vars.add(i);
		}

		int numCalls = 0;

		HashSet<Integer> indset = new HashSet<Integer>();
		Integer seed = edu.washington.util.SPNUtil.RandomElement(vars);
		vars.remove(seed);
		indset.add(seed);

		Queue<Integer> toprocess = new LinkedList<Integer>();
		toprocess.add(seed);

		while(!toprocess.isEmpty()){
			Integer v = toprocess.remove();
			List<Integer> toremove = new ArrayList<Integer>();

			for(Integer ov : vars){
				numCalls++;
				if(!independent(v,ov,d,currentSlice.instances,cc)){
					toremove.add(ov);
					indset.add(ov);
					toprocess.add(ov);
				}
			}

			for(Integer ov : toremove){
				vars.remove(ov);
			}
		}

		int returnset[] = new int[indset.size()];
		int i=0;
		for(Integer v : indset){
			returnset[i] = v;
			i++;
		}

		return returnset;
	}

	class Count {
		int counts[][];

		public Count(int c[][]) {
			this.counts = c;
		}
	}

	class CountCache extends HashMap<String,Count>{
		private static final long serialVersionUID = 1L;

	}

	private boolean independent(Integer v, Integer ov, Dataset d, int[] instances, CountCache cc) {
		if(ov < v){
			Integer temp = v;
			v = ov;
			ov = temp;
		}

		int attrSize[] = d.getAttrSizes();
		int counts[][] = new int[attrSize[v]][attrSize[ov]];
		int vtot[] = new int[attrSize[v]];
		int ovtot[] = new int[attrSize[ov]];

		if(d instanceof SparseDataset){
			HashSet<Integer> set_instances = new HashSet<Integer>();
			for(int i: instances){
				set_instances.add(i);
			}
			counts = ((SparseDataset) d).count(v, ov, set_instances);
		}
		else {
			for(int i : instances){
				d.show(i, Partition.Training);
				int vals[] = d.getValues();
				counts[vals[v]][vals[ov]]++;
			}
		}

		if(cc != null){
			cc.put(v+","+ov,new Count(counts));
		}

		for(int j=0; j<attrSize[v]; j++){
			for(int k=0; k<attrSize[ov]; k++){
				vtot[j] += counts[j][k];
				ovtot[k] += counts[j][k];
			}
		}

		int vskip=0, ovskip=0;
		for(int j=0; j<attrSize[v]; j++){
			if(vtot[j] == 0){
				vskip++;
			}
		}

		for(int k=0; k<attrSize[ov]; k++){
			if(ovtot[k] == 0){
				ovskip++;
			}
		}

		double gval = 0;
		for(int j=0; j<attrSize[v]; j++){
			for(int k=0; k<attrSize[ov]; k++){
				double ecount = 1.0*vtot[j]*ovtot[k]/instances.length;
				if(counts[j][k] == 0.0) continue;
				//				chisq += Math.pow(counts[j][k] - ecount, 2.0)/ecount;
				gval += 1.0 * counts[j][k] * Math.log(1.0 * counts[j][k] / ecount); 
			}
		}
		gval *= 2;

		int dof = (attrSize[v]-vskip-1)*(attrSize[ov]-ovskip-1);

		// If less than threshold, observed values could've been produced by noise on top of independent vars
		return gval < 2 * dof * gfactor + 0.001; 
	}

	private class VarInstSlice {
		public final int id, instances[], variables[];

		public VarInstSlice(int id, int[] instances, int[] variables) {
			this.id = id;
			this.instances = instances;
			this.variables = variables;
		}

	}

	private class BuildItem {
		public final int id;
		public final boolean sum;
		public final int ids[];
		public final double weights[];

		public BuildItem(int id, boolean sum, int[] ids, double[] weights) {
			super();
			this.id = id;
			this.sum = sum;
			this.ids = ids;
			this.weights = weights;
		}
	}
}

package edu.caltech.spn;

import java.util.*;

import edu.caltech.common.*;

// rectangular region for images 
public class Region {
	int id_;
	int a1_, a2_, b1_, b2_;
	int a_, b_;	// a=a2-a1, b=b2-b1	
	int interval_;	// for coarse resolution	

	// for pixel region only: gaussian units
	double[] means_;
	double[] vars_;
	double[] cnts_;
	double ttlCnt_=0;
	
	// data structure for a parse
	Map<Integer,Integer> inst_type_=new HashMap<Integer,Integer>();
	Map<Integer,String> inst_decomp_=new HashMap<Integer, String>();
	Map<String,ProdNode> decomp_prod_=new HashMap<String,ProdNode>();
	
	// each region is alloted a set of sum nodes
	ArrayList<SumNode> types_=new ArrayList<SumNode>();

	// for MAP computation
	int defMapTypeIdx_;
	String[] mapDecomps_;		
	double defMapSumPrb_;
	double defMapProdPrb_;

	//
	double invar_=Math.sqrt(20);	
	
	// 
	private Region(int id, int a1, int a2, int b1, int b2) {
		id_=id;
		a1_=a1;	a2_=a2;
		b1_=b1;	b2_=b2;
		a_=a2_-a1_; b_=b2_-b1_;
		
		if (a_>Parameter.baseResolution_ || b_>Parameter.baseResolution_) {
			if (a_%Parameter.baseResolution_!=0 || b_%Parameter.baseResolution_!=0) {
				Utils.println("ERR: base_res="+Parameter.baseResolution_+" "+a1+", "+a2+", "+b1+", "+b2);
				System.exit(-1);
			}
		}
		if (a_<=Parameter.baseResolution_ && b_<=Parameter.baseResolution_) interval_=1; else interval_=Parameter.baseResolution_;
	}
	public static Map<Integer,Region> id_regions_=new HashMap<Integer,Region>();
	
	// NOTE: dimension limited by range of 32-bit integer to around 215
	// for larger dimension, use long for id, or switch to string
	public static int getRegionId(int a1, int a2, int b1, int b2) {
		int id=((a1*Parameter.inputDim1_+a2-1)*Parameter.inputDim2_+b1)*Parameter.inputDim2_+b2-1;
		if (!id_regions_.containsKey(id)) id_regions_.put(id, new Region(id, a1, a2, b1, b2)); 
		return id;
	}
	
	public static Region getRegion(int id) {
		Region r = id_regions_.get(id);
		if (r==null) {
			int b2=id%Parameter.inputDim2_+1;
			int x=id/Parameter.inputDim2_;
			int b1=x%Parameter.inputDim2_;
			x=x/Parameter.inputDim2_;
			int a2=x%Parameter.inputDim1_+1;
			int a1=x/Parameter.inputDim1_;
			r=Region.getRegion(Region.getRegionId(a1, a2, b1, b2));
		}
		return r;
	}	
	public int getId() {return id_;}
	public String myStr() {
		String s="<"+a1_+","+a2_+","+b1_+","+b2_+">";
		return s;
	}
	
	// initialization
	public void resetTypes(int numTypes) {
		// clean up
		types_.clear();		
		inst_type_.clear();
		inst_decomp_.clear();
		decomp_prod_.clear();
		mapDecomps_=null;
		for (int i=0; i<numTypes; i++) {
			types_.add(new SumNode());
		}		
	}

	public void setTypes(int numTypes) {
		if (numTypes<types_.size()) {resetTypes(numTypes); return;}
		int nn=numTypes-types_.size();
		for (int i=0; i<nn; i++) {
			types_.add(new SumNode());
		}
	}
		
	// set value for input layer	
	public void setBase(double val) {
		setBaseGauss(val);
	}
	
	double cmpGauss(double v, double mean) {
		double m=mean-v;
		return -(m*m/2);
	}
	
	public void setBaseGauss(double v) {
		defMapTypeIdx_=-1;
		double mp=0;
		for (int i=0; i<types_.size(); i++) {
			SumNode n=types_.get(i);			
			n.logval_=cmpGauss(v,means_[i]);
			if (defMapTypeIdx_==-1 || n.logval_>mp) {
				defMapTypeIdx_=i;
				mp=n.logval_;
			}
		}		
	}

	public void setBaseForSumOut() {
		defMapTypeIdx_=-1;		
		for (int i=0; i<Parameter.numComponentsPerVar_; i++) {
			SumNode n=types_.get(i);
			n.logval_=0;
		}
	}

	// compute MAP state at inference time
	public void inferMAP(int instIdx, Instance inst) {
		if (mapDecomps_==null) mapDecomps_=new String[types_.size()];
		
		// compute prod values
		for (String di: decomp_prod_.keySet()) {
			ProdNode n=decomp_prod_.get(di);
			n.eval();
		}

		// evaluate children for sum nodes 
		for (int ti=0; ti<types_.size(); ti++) {
			if (types_.get(ti).chds_.size()==0) continue;
			SumNode n=types_.get(ti);
			n.eval();
			
			double maxChdPrb=0;
			ArrayList<String> mapDecompOpt=new ArrayList<String>();			
			for (String di: n.chds_.keySet()) {				
				Node c=n.chds_.get(di);				
				double m=(c.logval_==Node.ZERO_LOGVAL_)?Node.ZERO_LOGVAL_:c.logval_+Math.log(n.getChdCnt(di));
			
				if (mapDecompOpt.isEmpty() || m>maxChdPrb) {
					mapDecompOpt.clear();
					maxChdPrb=m;
				}
				if (m==maxChdPrb) {
					mapDecompOpt.add(di);
				}
			}
					
			// randomly break tie
			mapDecomps_[ti]=mapDecompOpt.get(Utils.random_.nextInt(mapDecompOpt.size()));
			mapDecompOpt.clear();			
		}
	}
	
	// compute MAP state at learning time: could tap a previous unused node
	public void inferMAPForLearning(int instIdx, Instance inst) {
		ArrayList<String> defMapDecompOpts=new ArrayList<String>();
		String defMapDecomp=null; if (mapDecomps_==null) mapDecomps_=new String[types_.size()];
		
		defMapTypeIdx_=-1;
		defMapSumPrb_=100;
		defMapProdPrb_=100;
		defMapDecompOpts.clear();

		// sum: choose a previous unused node
		ArrayList<Integer> blanks=null;
		for (int i=0; i<types_.size(); i++) {
			SumNode n=types_.get(i);
			if (n.chds_.size()==0) {
				if (blanks==null) blanks=new ArrayList<Integer>();
				blanks.add(i);
			}
		}
		int chosenBlankIdx=-1;
		if (blanks!=null) {
			if (blanks.size()>1) {
				int ci=Utils.random_.nextInt(blanks.size());
				chosenBlankIdx=blanks.get(ci);
			}
			else chosenBlankIdx=blanks.get(0);
			blanks.clear();
		}	 
		
		// find MAP decomposition
		for (int i=a1_+interval_; i<a2_; i+=interval_) {
			int ri1=Region.getRegionId(a1_, i, b1_, b2_);
			int ri2=Region.getRegionId(i, a2_, b1_, b2_);
			Region r1=Region.getRegion(ri1);
			Region r2=Region.getRegion(ri2);
			SumNode n1=r1.types_.get(r1.defMapTypeIdx_);
			SumNode n2=r2.types_.get(r2.defMapTypeIdx_);
			double lp;
			
			if (n1.logval_==Node.ZERO_LOGVAL_ || n2.logval_==Node.ZERO_LOGVAL_)
				lp=Node.ZERO_LOGVAL_;
			else 
				lp=n1.logval_+n2.logval_;			
			
			if (defMapDecompOpts.isEmpty() || lp>defMapProdPrb_) {
				defMapProdPrb_=lp;
				defMapDecompOpts.clear(); 
			}
			if (lp==defMapProdPrb_) {
				String di=Decomposition.getIdStr(ri1, ri2, r1.defMapTypeIdx_, r2.defMapTypeIdx_);
				defMapDecompOpts.add(di);
			}
		}
		for (int i=b1_+interval_; i<b2_; i+=interval_) {
			int ri1=Region.getRegionId(a1_, a2_, b1_, i);
			int ri2=Region.getRegionId(a1_, a2_, i, b2_);
			Region r1=Region.getRegion(ri1);
			Region r2=Region.getRegion(ri2);
			
			SumNode n1=r1.types_.get(r1.defMapTypeIdx_);
			SumNode n2=r2.types_.get(r2.defMapTypeIdx_);
			double lp;
			if (n1.logval_==Node.ZERO_LOGVAL_ || n2.logval_==Node.ZERO_LOGVAL_)
				lp=Node.ZERO_LOGVAL_;
			else 
				lp=n1.logval_+n2.logval_;			
			
			if (defMapDecompOpts.isEmpty() || lp>defMapProdPrb_) {
				defMapProdPrb_=lp;
				defMapDecompOpts.clear(); 
			}
			if (lp==defMapProdPrb_) {
				String di=Decomposition.getIdStr(ri1, ri2, r1.defMapTypeIdx_, r2.defMapTypeIdx_);
				defMapDecompOpts.add(di);
			}			
		}
		
		// random break ties for a previously unused node
		defMapDecomp=defMapDecompOpts.get(Utils.random_.nextInt(defMapDecompOpts.size()));
		defMapDecompOpts.clear();
		
		// evaluate product nodes
		for (String di: decomp_prod_.keySet()) {
			ProdNode n=decomp_prod_.get(di);
			n.eval();
		}
		
		// evaluate existing sum nodes and children
		ArrayList<Integer> mapTypes=new ArrayList<Integer>();
		for (int ti=0; ti<types_.size(); ti++) {
			if (types_.get(ti).chds_.size()==0) continue;
			SumNode n=types_.get(ti);
			n.eval();
			
			double maxSumPrb=0;
			ArrayList<String> mapDecompOpt=new ArrayList<String>();
			
			for (String di: n.chds_.keySet()) {				
				Node c=n.chds_.get(di);
				double l=n.logval_+Math.log(n.cnt_);
				double m=c.logval_;
				double nl;
				
				if (l>m) {
					nl=l+Math.log(1+Math.exp(m-l));
				}
				else {
					nl=m+Math.log(1+Math.exp(l-m));
				}

				if (mapDecompOpt.isEmpty() || nl>maxSumPrb) {
					mapDecompOpt.clear();
					maxSumPrb=nl;
				}
				if (nl==maxSumPrb) {
					mapDecompOpt.add(di);
				}
			}

			if (!n.chds_.containsKey(defMapDecomp)) {
				double nl=defMapProdPrb_;
				if (n.logval_!=Node.ZERO_LOGVAL_) {
					nl=Math.log(n.cnt_)+n.logval_;
					
					if (defMapProdPrb_>nl) {
						nl=defMapProdPrb_+Math.log(1+Math.exp(nl-defMapProdPrb_));
					}
					else {
						nl=nl+Math.log(Math.exp(defMapProdPrb_-nl)+1);
					}
				}
				nl-=Parameter.sparsePrior_;
				
				if (mapDecompOpt.isEmpty() || nl>maxSumPrb) {
					mapDecompOpt.clear();
					maxSumPrb=nl;
					mapDecompOpt.add(defMapDecomp);
				}
			}
						
			n.logval_=maxSumPrb-Math.log(n.cnt_+1);
			
			// randomly break tie
			mapDecomps_[ti]=mapDecompOpt.get(Utils.random_.nextInt(mapDecompOpt.size()));
			mapDecompOpt.clear();
			
			if (mapTypes.isEmpty() || n.logval_>defMapSumPrb_) {
				defMapSumPrb_=n.logval_;
				mapTypes.clear();
			}
			if (n.logval_==defMapSumPrb_) {
				mapTypes.add(ti);
			}			
		}
		
		if (chosenBlankIdx>=0) {
			SumNode n=types_.get(chosenBlankIdx);
			n.logval_=defMapProdPrb_-Math.log(n.cnt_+1)-Parameter.sparsePrior_;
			mapDecomps_[chosenBlankIdx]=defMapDecomp;
			
			if (mapTypes.isEmpty() || n.logval_>defMapSumPrb_) {
				defMapSumPrb_=n.logval_;
				mapTypes.clear(); mapTypes.add(chosenBlankIdx);
			}
		}
	
		defMapTypeIdx_=mapTypes.get(Utils.random_.nextInt(mapTypes.size()));
		mapTypes.clear();
	}

	// downward trace-back step
	public void setCurrParseToMAP(int instIdx) {
		if (a_==1 && b_==1) {
			return;
		}
		
		// type node
		if (types_.size()==1) inst_type_.put(instIdx, 0);	// only one choice
		
		int chosenType=inst_type_.get(instIdx);
		String di=mapDecomps_[chosenType];
		
		inst_decomp_.put(instIdx,di);
		Decomposition d=Decomposition.getDecomposition(di);
		Region r1=Region.getRegion(d.regionId1_);
		Region r2=Region.getRegion(d.regionId2_);

		r1.inst_type_.put(instIdx, d.typeId1_);
		r2.inst_type_.put(instIdx, d.typeId2_);
			
		// record update if slave
		if (!MyMPI.isClassMaster_ && SPN.isRecordingUpdate_) {
			MyMPI.buf_int_[MyMPI.buf_idx_++]=id_;
			MyMPI.buf_int_[MyMPI.buf_idx_++]=chosenType;
			MyMPI.buf_int_[MyMPI.buf_idx_++]=d.regionId1_;
			MyMPI.buf_int_[MyMPI.buf_idx_++]=d.regionId2_;
			MyMPI.buf_int_[MyMPI.buf_idx_++]=d.typeId1_;
			MyMPI.buf_int_[MyMPI.buf_idx_++]=d.typeId2_;
		}
		
		// if product node not created, create it now
		ProdNode np=decomp_prod_.get(di);
		if (np==null) {
			np=new ProdNode();
			decomp_prod_.put(di, np);
			np.addChd(r1.types_.get(d.typeId1_));
			np.addChd(r2.types_.get(d.typeId2_));
		}

		r1.setCurrParseToMAP(instIdx);
		r2.setCurrParseToMAP(instIdx);
	}

	// clear an existing parse for incremental EM 
	public void clearCurrParse(int instIdx) {
		if (!inst_type_.containsKey(instIdx)) return;
		if (a_==1 && b_==1) return;
		int cti=inst_type_.get(instIdx);
		String di=inst_decomp_.get(instIdx);

		inst_type_.remove(instIdx);
		inst_decomp_.remove(instIdx);		
		Decomposition d=Decomposition.getDecomposition(di);
		
		// record update if slave
		if (!MyMPI.isClassMaster_ && SPN.isRecordingUpdate_) {
			MyMPI.buf_int_[MyMPI.buf_idx_++]=id_;
			MyMPI.buf_int_[MyMPI.buf_idx_++]=cti;
			MyMPI.buf_int_[MyMPI.buf_idx_++]=d.regionId1_;
			MyMPI.buf_int_[MyMPI.buf_idx_++]=d.regionId2_;
			MyMPI.buf_int_[MyMPI.buf_idx_++]=d.typeId1_;
			MyMPI.buf_int_[MyMPI.buf_idx_++]=d.typeId2_;
		}
		
		Region r1=Region.getRegion(d.regionId1_);
		r1.clearCurrParse(instIdx);
		Region r2=Region.getRegion(d.regionId2_);
		r2.clearCurrParse(instIdx);		
	}

	// clear parse from other slaves
	public void clearCurrParseFromBuf(int chosenType, int ri1, int ri2, int ti1, int ti2) {
		if (a_==1 && b_==1) return;
		
		String di=Decomposition.getIdStr(ri1, ri2, ti1, ti2);				
		SumNode n=types_.get(chosenType);
		n.removeChdOnly(di, 1);
	}

	public void setCurrParseFromBuf(int chosenType, int ri1, int ri2, int ti1, int ti2) {
		if (a_==1 && b_==1) return;
		
		String di=Decomposition.getIdStr(ri1, ri2, ti1, ti2);
		SumNode n=types_.get(chosenType);
		Decomposition d=Decomposition.getDecomposition(di);

		// if prodnode not created, create it now
		ProdNode np=decomp_prod_.get(di);
		if (np==null) {
			np=new ProdNode();
			decomp_prod_.put(di, np);
			Region r1=Region.getRegion(d.regionId1_);
			Region r2=Region.getRegion(d.regionId2_);
						
			np.addChd(r1.types_.get(d.typeId1_));
			np.addChd(r2.types_.get(d.typeId2_));
		}
		n.addChdOnly(di, 1, np);
	}			
}

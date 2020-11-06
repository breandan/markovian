package edu.caltech.spn;


import java.util.*;

import edu.caltech.common.*;

public class SumNode extends Node {
	public Map<String,Node> chds_;	
	Map<String,Double> chdCnts_;
	double cnt_;

	public SumNode() {
		chds_=new HashMap<String,Node>();
		chdCnts_=new HashMap<String,Double>();
		cnt_=Parameter.smoothSumCnt_;
	}
	
	public void eval() {
		double v=0;
		String maxi=null;
		double maxl=0;
		for (String i: chds_.keySet()) {
			double l=chds_.get(i).getLogVal();
			if (l==Node.ZERO_LOGVAL_) continue;
			if (maxi==null || maxl<l) {
				maxi=i; maxl=l;
			}
		}
		if (maxi==null) {logval_=Node.ZERO_LOGVAL_; return;}
		for (String i: chds_.keySet()) {
			if (!chdCnts_.containsKey(i)) continue;
			double l=chds_.get(i).getLogVal();
			if (l==Node.ZERO_LOGVAL_) continue;
			v+=getChdCnt(i)*Math.exp(l-maxl);
		}		
		logval_=Math.log(v/cnt_)+maxl;
	}
		
	public void passDerivative() {
		if (logDerivative_==Node.ZERO_LOGVAL_) return;
		
		for (String di: chds_.keySet()) {
			Node n=chds_.get(di);
			double l=logDerivative_+Math.log(getChdCnt(di)/cnt_);
			if (n.logDerivative_==Node.ZERO_LOGVAL_) n.logDerivative_=l;
			else n.logDerivative_=Utils.addLog(l, n.logDerivative_);
		}
	}
	
	public double getChdCnt(String di) {
		return chdCnts_.get(di);
	}
	
	public void setChdCnt(String di, double cnt) {
		chdCnts_.put(di,cnt);
	}
		
	public void addChdOnly(String decompIdx, double cnt, Node n) {
		if (!chds_.containsKey(decompIdx)) {
			chds_.put(decompIdx,n);
		}
		if (!chdCnts_.containsKey(decompIdx)) {
			chdCnts_.put(decompIdx,cnt);
		}
		else chdCnts_.put(decompIdx,cnt+getChdCnt(decompIdx));
		cnt_+=cnt;
	}
		
	public void removeChdOnly(String decompIdx, double cnt) {
		double cc=getChdCnt(decompIdx);
		cc-=cnt;
		if (cc==0) {
			chds_.remove(decompIdx);
			chdCnts_.remove(decompIdx);
		}
		else chdCnts_.put(decompIdx, cc);
		cnt_-=cnt;
	}	
}

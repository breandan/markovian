package edu.caltech.spn;

import java.util.*;

import edu.caltech.common.*;

// Product Node
public class ProdNode extends Node {
	ArrayList<Node> chds_;	
	
	public ProdNode() {
		chds_=new ArrayList<Node>();
	}
	
	public void passDerivative() {		
		if (logDerivative_==Node.ZERO_LOGVAL_) return;
		
		if (logval_==Node.ZERO_LOGVAL_) {
			int cnt=0;
			for (Node n: chds_) if (n.logval_==Node.ZERO_LOGVAL_) {cnt++; if (cnt>1) return;}
		}
		
		for (Node n: chds_) {
			if (n.logval_==Node.ZERO_LOGVAL_) {
				double l=0;
				for (Node m:chds_) {
					if (m.logval_!=Node.ZERO_LOGVAL_) l+=m.logval_;
				}
				l+=logDerivative_;
				if (n.logDerivative_==Node.ZERO_LOGVAL_) n.logDerivative_=l;
				else n.logDerivative_=Utils.addLog(n.logDerivative_, l);
			}
			else if (logval_!=Node.ZERO_LOGVAL_) {				
				double l=logDerivative_+logval_-n.logval_;
				if (n.logDerivative_==Node.ZERO_LOGVAL_) n.logDerivative_=l;
				else n.logDerivative_=Utils.addLog(n.logDerivative_, l);
			}
		}
	}
	
	public void eval() {
		logval_=0;
		for (Node n: chds_) {
			double v=n.getLogVal();
			if (v==Node.ZERO_LOGVAL_) {logval_=Node.ZERO_LOGVAL_; return;}	
			logval_+=v;
		}
	}
	
	public void addChd(Node n) {
		chds_.add(n);
	}
}

package edu.washington.spn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProdNode extends Node implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<Node> chds_;
	private Node a,b;


	public ProdNode() {
		chds_ = new ArrayList<Node>();
	}

	public ProdNode(Collection<? extends Node> setCopy) {
		this();
		chds_.addAll(setCopy);
		for(Node n : chds_){
			firstX = Math.min(firstX, n.firstX);
			firstY = Math.min(firstY, n.firstY);
		}
		if(chds_.size() == 2){
			a = chds_.get(0);
			b = chds_.get(1);
		} 

	}

	@Override
	public void eval() {
		dirty = false;
		for(int c=0; c<chds_.size(); c++){
			if(chds_.get(c).dirty){
				dirty = true;
				break;
			}
		}
		
		if(!dirty){
			return;
		}
		
		logder_=Node.ZERO_LOGVAL_;
		if(a != null){
			logval_ = a.logval_ + b.logval_;
			return;
		}
		logval_=0;

		for (Node n: chds_) {
			logval_+=n.getLogVal();
		}


	}

	public void addChd(Node n) {
		chds_.add(n);
		dirty = true;
	}


	boolean dirty=true;
	int code=0;
	@Override
	public int hashCode() {
		if(dirty)
			code = (new HashSet(chds_)).hashCode();
		dirty = false;
		return code;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof ProdNode) {
			ProdNode pnc = (ProdNode) obj;
			if(pnc.chds_.size() != this.chds_.size())
				return false;
			
			if(pnc.hashCode() != this.hashCode())
				return false;
			
			Set<Node> theirSet = new HashSet<Node>(pnc.chds_);
			Set<Node> mySet = new HashSet<Node>(this.chds_);
			
			return theirSet.equals(mySet);
		}
		return false;
	}

	@Override
	public String toString() {
		return "PNC "+super.toString();
	}

	public List<Node> allChildren() {
		return chds_;
	}

}

package edu.washington.spn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import edu.washington.util.Parameter;

public class SumNode extends Node  implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3379653242101724567L;

	protected final List<Node> chds_ = new ArrayList<Node>();
	protected final List<Double> w_ = new ArrayList<Double>();

	private final HashSet<Node> contains = new HashSet<Node>();

	public final ArrayList<Node> keyOrder = new ArrayList<Node>();
	protected double cnt_=Parameter.smoothSumCnt_;

	public Node winner;
	protected String winnerTag;

	public final GraphSPN gs;

	public boolean allProducts = false; 

	public SumNode(GraphSPN gs) {
		this.gs = gs;
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
		
		//		System.out.println();
		logder_=Node.ZERO_LOGVAL_;
		winner = null;
		double sumVal=0;

		// Max child (for numerical purposes)
		double maxl=Node.ZERO_LOGVAL_;

		for(int c=0; c<chds_.size(); c++){
			double l=chds_.get(c).getLogVal();
			double lw = Math.log(w_.get(c));
			if(l+lw>maxl) maxl = l+lw;
		}

		// Compute unnormalized prob
		double log_unnorm = Node.ZERO_LOGVAL_;
		double max_childlog = Node.ZERO_LOGVAL_;
		double max_weight = 0.00001;

		for(int c=0; c<chds_.size(); c++){
			double childlog=chds_.get(c).getLogVal();
			if (childlog==Node.ZERO_LOGVAL_) continue;
			double w = w_.get(c);
			double log_multiplied = childlog + Math.log(w);
			sumVal+= edu.washington.util.SPNMath.exp(log_multiplied-maxl);

			if((max_childlog+Math.log(max_weight)) < log_multiplied){
				max_childlog = childlog;
				max_weight = w;
				winner = chds_.get(c);
			}

		}
		log_unnorm = Math.log(sumVal)+maxl; //log_sum_exp(log_unnorm, log_multiplied);

		logval_ = log_unnorm - Math.log(cnt_);
	}



	public void addChdOnly(Node n, double cc) {
		if(cc > 0){
			chds_.add(n);
			w_.add(cc);
			contains.add(n);
			keyOrder.add(n);
			cnt_+=cc;
		}
	}

	public String tag = "";
	@Override
	public String toString() {
		// Temporary toString
		return tag;
	}



	public List<Node> getChds(){
		return chds_;
	}

	public List<Double> getW(){
		return w_;
	}

	public boolean contains(Node child) {
		return contains.contains(child);
	}

	public double getWeight(Node child) {
		int idx = chds_.indexOf(child);
		return w_.get(idx);
	}

	public void removeChildrenFromSet(HashSet<Node> wantToRemove) {
		for(Node n : chds_)
			wantToRemove.remove(n);
	}

	public void setCnt(double newcnt) {
		cnt_ = newcnt;
	}


}

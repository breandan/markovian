package edu.washington.spn;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import edu.washington.util.SPNUtil;
import edu.washington.data.Dataset;
import edu.washington.data.Partition;

public class GraphSPN implements Serializable {
	private static final long serialVersionUID = 1L;

	transient public Dataset data;

	public static int penaltyInterval = 100;
	public LinkedSet<Node> order = new LinkedSet<Node>();

	public GraphSPN(Dataset d) {
		this.data = d;
	}	

	public double llh(Partition p) {
		double ll = 0;

		for(int i=0; i<data.getNumItems(p); i++){
			data.show(i, p, true);

			ll += upwardPass();
		}

		return 1.0 * ll / data.getNumItems(p);
	}

	

	public double  llhTraining() {
		return llh(Partition.Training);
	}
	

	public Map<Node, Node> topDown(Node root, boolean ignorePriors) {
		Map<Node, Node> parse = new HashMap<Node, Node>();
		Queue<Node> toVisit = new LinkedList<Node>();

		toVisit.add(root);

		while(!toVisit.isEmpty()){
			Node n = toVisit.remove();

			// SumNode
			if(n instanceof SumNode){
				SumNode s = (SumNode)n;

				Node child = s.winner;
				// Can learn only if (1) c is original or (2) c is in s's allowedDownwardCandidates
				parse.put(s, child);

				if(child instanceof Node){
					Node mc = (Node) child;
					toVisit.add(mc);

				}
			} // end if SumNode

			// ProdNode
			else if (n instanceof ProdNode){
				// Send all active candidates to all children of a product node
				ProdNode p = (ProdNode)n;
				for(Node child : p.allChildren()){
					toVisit.add(child);
				}
			}


			else {
				parse.put(n, null);
			}

		} // end top-down while loop
		return parse;
	}

	public void topDownMarginal(Node root) {
		getRoot().logder_ = 0;
		// Traverse the spn in reverse order
		//		int count=0;
		for(ListNode<Node> ln = order.tail; ln != null; ln = ln.prev){
			Node n = ln.item;
			n.passDer();
		}
	}

	public double upwardPass() {

		ListNode<Node> ln = order.head;
		while(ln!=null){
			ln.item.eval();
			ln = ln.next;
		}


		return getRoot().logval_;
	}

	

	public List<Node> generateSample(Random rnd){
		Node root = getRoot();

		Queue<Node> toVisit = new LinkedList<Node>();
		List<Node> visited = new ArrayList<Node>();

		toVisit.add(root);


		while(!toVisit.isEmpty()){
			Node n = toVisit.remove();
			visited.add(n);

			visited.add(n);
			if(n instanceof SumNode){
				toVisit.add(SPNUtil.RandomSumChild((SumNode)n, rnd));
			}
			if(n instanceof ProdNode){
				toVisit.addAll(((ProdNode) n).allChildren());
			}

		}

		return visited;
	}	



	public Node getRoot() {
		return order.tail.item;
	}


	public List<Node> labeledNodes = new ArrayList<Node>();

	public List<Integer> varorder;
	

	public void write(String filename){
		try{
			//use buffering
			OutputStream file = new FileOutputStream( filename );
			OutputStream buffer = new BufferedOutputStream( file );
			ObjectOutputStream output = new ObjectOutputStream( buffer );
			try{
				output.writeObject(this);
			} catch(Exception e){
				throw new RuntimeException("Serialization error. Path to bad object: ");
			}
			finally{
				output.close();
			}
		}  
		catch(IOException ex){
			ex.printStackTrace();




			(new File(filename)).delete();
		}
	}

	public static GraphSPN load(String filename, Dataset newData){
		GraphSPN gspn = null;
		try{
			ObjectInput input = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filename)));
			try {
				gspn = (GraphSPN) input.readObject();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				input.close();
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		gspn.data = newData;

		for(Node n : gspn.order){
			if(n instanceof DatasetDep){
				DatasetDep datasetDep = (DatasetDep) n;
				datasetDep.setDataset(newData);
			}
			
		}
		return gspn;
	}





	public long edgeCount(){
		long totalNumWeights = 0;
		for(Node n : order){
			if(n instanceof SumNode){
				SumNode sn = (SumNode) n;
				totalNumWeights += sn.getW().size();
			}
			if(n instanceof ProdNode){
				ProdNode prodNode = (ProdNode) n;
				totalNumWeights += prodNode.allChildren().size();
			}
		}
		return totalNumWeights;
	}




	public void clearDer() {
		for(Node n : order){
			n.logder_ = Double.NEGATIVE_INFINITY;
			n.der = 0;
		}
	}


	public void printDepth(int i) {
		Map<Node, Node> parse = new HashMap<Node, Node>();
		LinkedList<Node> toVisit = new LinkedList<Node>();
		LinkedList<Integer> depth = new LinkedList<Integer>();

		toVisit.add(getRoot());
		depth.add(0);

		while(!toVisit.isEmpty()){
			Node n = toVisit.remove();
			int d  = depth.remove();
			if(d>i) continue;

			// SumNode
			if(n instanceof SumNode){
				for(int j=0; j<d; j++){ System.out.print(" ");}
				SumNode s = (SumNode)n;
				System.out.print("SN\t"+s.getChds().size());
				for(int c=0; c<s.getChds().size(); c++){
					System.out.print("\t"+String.format("%.2f",s.getW().get(c)));
				}
				for(int c=s.getChds().size()-1; c>=0; c--){
					toVisit.addFirst(s.getChds().get(c));
					depth.addFirst(d+1);
				}
				System.out.println();
			} 
			else if(n instanceof ProdNode){
				for(int j=0; j<d; j++){ System.out.print(" ");}
				ProdNode pn = (ProdNode) n;
				System.out.print("P\t"+pn.allChildren().size());
				for(Node chd : pn.allChildren()){
					toVisit.addFirst(chd);
					depth.addFirst(d+1);
				}
				System.out.println();
			}
		}
	}


}

package edu.caltech.eval;

import java.util.*;
import java.io.*;

import edu.caltech.common.*;
import mpi.*;
import edu.caltech.spn.*;

public class ImageCompletion {
	static int PAD_LEN_=10;
	
	// -------------------------------------------------------------
	// Completion
	// -------------------------------------------------------------
	public static void completeLeft(ArrayList<Instance> test, String mdlDir, String mdlName, String rstDir) throws Exception {
		Utils.println("complete left half for mdl="+mdlDir+"/"+mdlName);		
		SPN spn=SPN.loadDSPN(mdlDir+"/"+mdlName+".mdl");
		completeLeft(spn, test, mdlName, rstDir);
	}	
	public static void completeLeft(SPN spn, ArrayList<Instance> test, String mdlName, String rstDir) throws Exception {
		Utils.println("---> complete left half and output "+rstDir+"/"+mdlName+"-left.dat");		
		
		int size=(int)Math.ceil(test.size()*1.0/Parameter.numSlavePerClass_);
		
		if (!MyMPI.isClassMaster_) {
			int master=MyMPI.masterRank_;
			MyMPI.buf_idx_=0;
			for (int i=MyMPI.myOffset_*size; i<test.size() && i<(MyMPI.myOffset_+1)*size; i++) {
				Instance inst=test.get(i);
				spn.completeLeftImg(inst);
			}
			sendImg(master);
		}
		else {
			BufferedWriter out=new BufferedWriter(new FileWriter(rstDir+"/"+mdlName+"-left.dat"));
			for (int si=1; si<=Parameter.numSlavePerClass_; si++) {
				int src=si+MyMPI.rank_;
				recvImg(src);				
				MyMPI.buf_idx_=0;
				for (int i=(si-1)*size; i<test.size() && i<si*size; i++) {
					Instance inst=test.get(i);
					outputRstToImg(out,i,inst);
				}
			}
			out.close();
		}	
	}
	
	public static void completeBottom(ArrayList<Instance> test, String mdlDir, String mdlName, String rstDir) throws Exception {
		Utils.println("complete bottom half for mdl="+mdlDir+"/"+mdlName);		
		SPN spn=SPN.loadDSPN(mdlDir+"/"+mdlName+".mdl");
		completeBottom(spn, test, mdlName, rstDir);
	}
	public static void completeBottom(SPN spn, ArrayList<Instance> test, String mdlName, String rstDir) throws Exception {
		Utils.println("---> complete bottom half and output "+rstDir+"/"+mdlName+"-btm.dat");		
		
		int size=(int)Math.ceil(test.size()*1.0/Parameter.numSlavePerClass_);
		
		if (!MyMPI.isClassMaster_) {
			int master=MyMPI.masterRank_;
			MyMPI.buf_idx_=0;
			for (int i=MyMPI.myOffset_*size; i<test.size() && i<(MyMPI.myOffset_+1)*size; i++) {
				Instance inst=test.get(i);
				spn.completeBottomImg(inst);
			}
			sendImg(master);
		}
		else {
			BufferedWriter out=new BufferedWriter(new FileWriter(rstDir+"/"+mdlName+"-btm.dat"));
			for (int si=1; si<=Parameter.numSlavePerClass_; si++) {
				int src=si+MyMPI.rank_;
				recvImg(src);
				MyMPI.buf_idx_=0;
				for (int i=(si-1)*size; i<test.size() && i<si*size; i++) {
					Instance inst=test.get(i);
					outputRstToImg(out,i,inst);		
				}
			}
			out.close();
		}	
	}

	static void outputRstToImg(BufferedWriter out, int instIdx, Instance inst) throws Exception {
		// output orginal and completed images side by side		
		int dim1=inst.vals_.length, dim2=inst.vals_[0].length;
		int sz=dim1*dim2;
		for (int ri=0; ri<dim1; ri++) {
			int off=ri*dim2;
			
			// original
			for (int ci=0; ci<dim2; ci++) {
				int v=Utils.getIntVal(inst, inst.vals_[ri][ci]);
				if (ci>0) out.write(','); 
				out.write(""+v);
			}

			// pad
			for (int i=0; i<PAD_LEN_; i++) out.write(",0");
				
			// completion
			for (int ci=0; ci<dim2; ci++) {
				out.write(","+MyMPI.buf_int_[MyMPI.buf_idx_+off+ci]);
			}
			
			out.write("\n");
		}
		
		// pad
		for (int k=0; k<PAD_LEN_; k++) {
			out.write("0");
			int len=dim2*2+PAD_LEN_;
			for (int i=1; i<len; i++) {
				out.write(",0");			
			}
			out.write("\n");
		}		
		
		MyMPI.buf_idx_+=sz;
	}
	
	static void sendImg(int dest) {
		MPI.COMM_WORLD.Send(MyMPI.buf_int_, 0, MyMPI.buf_idx_, MPI.INT, dest, 0);
	}
	
	static int recvImg(int src) {
		Status status=MPI.COMM_WORLD.Recv(MyMPI.buf_int_, 0, MyMPI.buf_size_, MPI.INT, src, 0);
		return status.count;
	}	
}

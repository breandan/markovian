package edu.washington.exp.inference;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.washington.data.Dataset;
import edu.washington.data.Partition;
import edu.washington.exp.RunSLSPN;

public class GenQEV {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RunSLSPN.parseParameters(args);
		Dataset d = null;

		try {
			d = (Dataset) RunSLSPN.ds[RunSLSPN.data_id].newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}

		List<Integer> testOrder = new ArrayList<Integer>();
		for(int inst=0; inst<d.getNumTesting(); inst++){
			testOrder.add(inst);
		}

		List<Integer> varOrder = new ArrayList<Integer>();
		for(int var=0; var<d.getNumFeatures(); var++){
			varOrder.add(var);
		}

		// Pick up to 1000 random test examples
		Collections.shuffle(testOrder);
		if(testOrder.size() > 1000){
			testOrder = testOrder.subList(0,1000);
		}

		System.out.println("Generating "+testOrder.size()+" queries each");
		
		String prefix = "edu/caltech/data/" +d+"/";

		{
			String setprefix = "VE";
			double percentQuery 	= 0.3;
			for(double percentEvidence = 0.0; percentEvidence <= 0.5; percentEvidence += 0.1){
				writeQEV(d, testOrder, varOrder, prefix, setprefix, percentQuery, percentEvidence);
			}
		}
		
		{
			String setprefix = "VQ";
			double percentEvidence 	= 0.3;
			for(double percentQuery = 0.1; percentQuery <= 0.5; percentQuery += 0.1){
				writeQEV(d, testOrder, varOrder, prefix, setprefix, percentQuery, percentEvidence);
			}
		}
	}

	protected static void writeQEV(Dataset d, List<Integer> testOrder,
			List<Integer> varOrder, String prefix, String setprefix,
			double percentQuery, double percentEvidence) {
		String filenameQ = prefix + setprefix +"_Q"+String.format("%.2f", percentQuery)+"_E"+String.format("%.2f", percentEvidence)+".q";
		String filenameE = prefix + setprefix+"_Q"+String.format("%.2f", percentQuery)+"_E"+String.format("%.2f", percentEvidence)+".ev";

		BufferedWriter bwQ =null;
		BufferedWriter bwE =null;
		try {
			bwQ = new BufferedWriter(new FileWriter(filenameQ));
			bwE = new BufferedWriter(new FileWriter(filenameE));
		} catch (Exception e){
			e.printStackTrace();
		}

		int numQuery = (int) (percentQuery * d.getNumFeatures());
		int numEvidence = (int) (percentEvidence * d.getNumFeatures());
		// For each test example, pick random query and evidence
		for(int inst : testOrder){
			d.show(inst, Partition.Testing);
			Collections.shuffle(varOrder);
			int v=0;
			int Qarray[] = new int[d.getNumFeatures()];
			int Earray[] = new int[d.getNumFeatures()];
			for(int j=0; j<numQuery; j++){
				Qarray[varOrder.get(v++)] = 1;
			}
			for(int j=0; j<numEvidence; j++){
				Earray[varOrder.get(v++)] = 1;
			}

			StringBuilder sbQ = new StringBuilder();
			StringBuilder sbE = new StringBuilder();
			int vals[] = d.getValues();

			for(int j=0; j<d.getNumFeatures(); j++){
				if(Qarray[j] == 1){
					sbQ.append(vals[j]);
				} else {
					sbQ.append("*");
				}

				if(Earray[j] == 1){
					sbE.append(vals[j]);
				} else {
					sbE.append("*");
				}

				if(j<d.getNumFeatures()-1){
					sbQ.append(",");
					sbE.append(",");
				}
			}

			//			System.out.println("Q" + sbQ.toString());
			//			System.out.println("E" + sbE.toString());

			try {
				bwQ.write(sbQ + "\n");
				bwE.write(sbE + "\n");
			} catch (Exception e) {
				e.printStackTrace();
			}

		} // End for over testOrder

		try{
			bwQ.close();
			bwE.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}

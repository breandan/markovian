package edu.washington.data;

import java.io.BufferedReader;
import java.io.FileReader;

public class QueryDataset extends Dataset {

	int q_data[][];
	int ev_data[][];
	int vals[];
	
	int numTraining, numValidation, numTesting;
	int numvars = 0;
	
	public QueryDataset(String queryfile, String evidencefile) {
		int numqueries = 1;
		String delim = ",";
		
		try {
			// Read training
			System.out.println("Loading query...");
			BufferedReader br = new BufferedReader(new FileReader(queryfile));
			String line = br.readLine();
			numvars = line.split(delim).length;
			while ((line = br.readLine()) != null) { numqueries++; }
			br.close();
			System.out.println("Found "+numqueries+" queries with "+numvars+" vars");
			numTraining = 0;
			numValidation = 0;
			numTesting = numqueries;
			
			q_data = new int[numqueries][numvars];
			ev_data = new int[numqueries][numvars];
			
			int c=0;
			br = new BufferedReader(new FileReader(queryfile));
			while ((line = br.readLine()) != null) {
				String toks[] = line.split(delim);
				for(int f=0; f<numvars; f++){
					if(toks[f].equals("*")){
						q_data[c][f] = -1;
					} else {
						q_data[c][f] = Integer.parseInt(toks[f]);
					}
				}
				c++;
			}
			br.close();
			
			
			
			System.out.println("Loading evidence...");
			br = new BufferedReader(new FileReader(evidencefile));
			c=0;
			
			while ((line = br.readLine()) != null) {
				String toks[] = line.split(delim);
				for(int f=0; f<numvars; f++){
					if(toks[f].equals("*")){
						ev_data[c][f] = -1;
					} else {
						ev_data[c][f] = Integer.parseInt(toks[f]);
					}
				}
				c++;
			}
			br.close();
		} catch(Exception e){
			e.printStackTrace();
		}
		
		
	}
	
	@Override
	public double getNumFolds() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumClasses() {
		// TODO Auto-generated method stub
		return 0;
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
	public int trueLabel() {
		return 0;
	}

	@Override
	public void show(int i, Partition testing, boolean b) {
		// TODO Auto-generated method stub
	}

	public void showJoint(int i, Partition testing) {
		vals = q_data[i].clone();
		
		for(int j=0; j<numvars; j++){
			if(ev_data[i][j] > -1){
				vals[j] = ev_data[i][j]; 
			}
		}
	}
	
	public int getNumQueryVars(int i, Partition part){
		int c=0;
		for(int j=0; j<numvars; j++){
			if(q_data[i][j] > -1){
				c++;
			}
		}
		return c;
	}
	
	public void showQuery(int i, Partition testing) {
		vals = q_data[i];
	}
	
	public void showEvidencePlusAQuery(int inst, Partition testing, int var) {
		vals = ev_data[inst].clone();
		vals[var] = q_data[inst][var]; 
	}

	public void showEvidence(int i, Partition testing) {
		vals = ev_data[i];
	}
	
	@Override
	public int getUniqueInstanceID() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNumFeatures() {
		return numvars;
	}

	@Override
	public int[] getAttrSizes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] getValues() {
		return vals;
	}

}

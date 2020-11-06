package edu.washington.data;

import java.io.Serializable;

public abstract class Dataset implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int currentFold = 0;
	public int numFeats;
	
	public abstract double getNumFolds();

	public void setFold(int f) {
		currentFold = f;
	}
	
	public abstract int getNumClasses();

	public abstract int getNumTesting();
	public abstract int getNumTraining();
	public abstract int getNumValidation();

	public abstract int trueLabel();

	public void show(int i, Partition testing){
		show(i, testing, true);
	}
	
	public abstract void show(int i, Partition testing, boolean b);



	public int getNumItems(Partition p) {
		switch (p) {
		case Training: return getNumTraining(); 
		case Validation: return getNumValidation(); 
		case Testing: return getNumTesting();
		default:
			break;
		}
		return 0;
	}

	public abstract int getUniqueInstanceID();

	public abstract int getNumFeatures();
	public abstract int[] getAttrSizes();
	public abstract int[] getValues();

	
	

}

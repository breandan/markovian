package edu.caltech.spn;

import java.util.*;

// represent a decomposition of a region into two sub-regions
public class Decomposition {
	static Map<String,Decomposition> id_decomp_=new HashMap<String,Decomposition>();	
	static Decomposition blankDecomp_=new Decomposition("",-1,-1,-1,-1);

	String id_;
	int regionId1_, regionId2_, typeId1_, typeId2_;
		
	private Decomposition(String id, int regionId1, int regionId2, int typeId1, int typeId2) {
		id_=id;
		regionId1_=regionId1;	regionId2_=regionId2;
		typeId1_=typeId1;	typeId2_=typeId2;
	}
	public static Decomposition getDecomposition(int regionId1, int regionId2, int typeId1, int typeId2) {
		String id=getIdStr(regionId1,regionId2,typeId1,typeId2);
		Decomposition d=id_decomp_.get(id);
		if (d==null) id_decomp_.put(id, d); 
		return d;
	}
	public static Decomposition getDecomposition(String id) {
		Decomposition d=id_decomp_.get(id);
		if (d==null) {
			if (id==null) {
				// blank; doesn't matter
				return blankDecomp_;
			}
			String[] ts=id.split(" ");
			int regionId1=Integer.parseInt(ts[0]);
			int regionId2=Integer.parseInt(ts[1]);
			int typeId1=Integer.parseInt(ts[2]);
			int typeId2=Integer.parseInt(ts[3]);
			d=new Decomposition(id, regionId1, regionId2, typeId1, typeId2);			
		}
		return d;
	}	
	public String getId() {return id_;}	
	public static void remove(String id) {
		id_decomp_.remove(id);
	}	
	public static String getIdStr(int regionId1, int regionId2, int typeId1, int typeId2) {
		String id=regionId1+" "+regionId2+" "+typeId1+" "+typeId2;
		return id;
	}
}

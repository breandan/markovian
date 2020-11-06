package edu.washington.slalg;

import edu.washington.spn.GraphSPN;
import edu.washington.data.Dataset;

public interface SLAlg {
	public GraphSPN learnStructure(Dataset d); 
}

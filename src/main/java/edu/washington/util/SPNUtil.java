package edu.washington.util;

import java.util.List;
import java.util.Random;
import java.util.Set;

import edu.washington.spn.Node;
import edu.washington.spn.SumNode;

public class SPNUtil {
	public static Random rand = new Random();
	
	
	
	public static <T> double AddIdIfGEQ(double reference, double new_value, List<T> ids, T new_id){
			if(ids.isEmpty() || new_value > reference){
				ids.clear();
				reference = new_value;
			}
			if(new_value == reference)
				ids.add(new_id);
			return reference;
		}

//	public static int RandomElement(List<Integer> list){
//		return list.get((int) (Math.random() * list.size()));
//	}
	
	public static Node RandomSumChild(SumNode sn, Random rnd){
		double d = rnd.nextDouble();
		double runsum = 0;
		for(int c=0; c<sn.keyOrder.size(); c++){
			double cw = sn.getW().get(c);
			if(runsum <= d && d < (runsum + cw)){
				return sn.keyOrder.get(c);
			}
			runsum += cw;
		}
		return sn.keyOrder.get(sn.keyOrder.size()-1);
	}
	
	public static <T> T RandomElement(List<T> list){
		return list.get((int) (rand.nextDouble() * list.size()));
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T RandomElement(Set<T> set){
		return (T) set.toArray()[(int) (rand.nextDouble() * set.size())];
	}
	
	public static byte flipCoin(double d) {
		if(rand.nextDouble() < d)
			return 1;
		else
			return 0;
	}
}


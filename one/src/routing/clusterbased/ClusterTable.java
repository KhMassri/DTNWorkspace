package routing.clusterbased;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.SimClock;


public class ClusterTable {
	//public static final int INFINITE_SET_SIZE = Integer.MAX_VALUE;
	/** meeting probabilities (probability that the next node one meets is X) */
	private Map<Integer,NodeInfo> table;
	
public ClusterTable(){
	
	table=new HashMap<Integer,NodeInfo>();
		
}

public void addNode(Integer k,NodeInfo info){
	
	table.put(k, info);
}

public void remove(Integer k){
	
	table.remove(k);
}



public double getProbFor(Integer k){
	return table.get(k).getProb();
	
}
public String getClusterIdFor(Integer k){
	return table.get(k).getClusterId();
	
}
public int getTimeFor(Integer k){
	return table.get(k).getTime();
}   
    


class NodeInfo{
	
	double prob;
	String clusterId;
	int time;
	
	double getProb(){return prob;}
	String getClusterId(){return clusterId;}
	int getTime(){return time;}
}


}

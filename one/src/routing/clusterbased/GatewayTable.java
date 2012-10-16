package routing.clusterbased;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.SimClock;

public class GatewayTable {

		private Map<String,GatewayInfo> table;
		
	public GatewayTable(){
		
		table=new HashMap<String,GatewayInfo>();
			
	}

	public void addNode(String c,GatewayInfo info){
		
		table.put(c, info);
	}

	public void remove(String c){
		
		table.remove(c);
	}



	public double getProbFor(String c){
		return table.get(c).getProb();
		
	}
	public Integer getGatewayFor(String c){
		return table.get(c).getGateway();
		
	}
	public int getTimeFor(String c){
		return table.get(c).getTime();
	}   
	    


	
	
	class GatewayInfo{
		
		double prob;
		Integer gateway;
		int time;
		
		double getProb(){return prob;}
		Integer getGateway(){return gateway;}
		int getTime(){return time;}
	}


	
	
}

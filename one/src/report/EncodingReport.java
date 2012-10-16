package report;

import java.util.ArrayList;
import java.util.List;

import core.ConnectionListener;
import core.DTNHost;
import core.UpdateListener;

public class EncodingReport extends Report implements ConnectionListener,
	UpdateListener {

	private int[] encounters;
	ArrayList<Integer> dest = new ArrayList<Integer>();
	ArrayList<Integer> pod = new ArrayList<Integer>();
	ArrayList<Integer> doneTime = new ArrayList<Integer>();
	ArrayList<Integer> nrofCarriers = new ArrayList<Integer>();
	ArrayList<Integer> enc = new ArrayList<Integer>();
	
	public EncodingReport() {
		
	}
	
	public void hostsConnected(DTNHost host1, DTNHost host2) {
		if (encounters == null) {
			return;
		}
		encounters[host1.getAddress()]++;
		encounters[host2.getAddress()]++;
		
	}

	public void hostsDisconnected(DTNHost host1, DTNHost host2) {}

	public void updated(List<DTNHost> hosts) {
		if (encounters == null) {
			encounters = new int[hosts.size()];
		}
	}

	public void encodingDone(DTNHost dest, int pod,int nrofCarrier){
		
		this.dest.add(dest.getAddress());
		this.pod.add(pod);
		doneTime.add((int) this.getSimTime());
		nrofCarriers.add(nrofCarrier);
		enc.add(encounters[dest.getAddress()]);
		
	
	}
	
	@Override
	public void done() {
		
		for(int i=0;i<pod.size();i++){
			
			
			write("Time: "+doneTime.get(i));
			write("Encounters_at_dec: "+enc.get(i));
			write("Total_Encounters: "+encounters[dest.get(i)]);
			write("Active_Encounters: "+this.nrofCarriers.get(i));
			
		}
		super.done();
	}

	public int[] getEncounters() {
		return encounters;
	}

	public void setEncounters(int[] encounters) {
		this.encounters = encounters;
	}


	
}

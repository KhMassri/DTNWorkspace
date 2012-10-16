package report;

import java.util.Vector;
import core.ConnectionListener;
import core.DTNHost;
import core.SimClock;

import java.util.ArrayList;
import java.util.Vector;
public class ContactsPerNodeReport extends Report implements ConnectionListener {

	Vector<Integer> hostAdresses=new Vector<Integer>();
	Vector<Integer> contacts=new Vector<Integer>();
	
	Vector<Integer> times=new Vector<Integer>();
	public ContactsPerNodeReport(){
		
		init();
		
	}
	
	@Override
	public void hostsConnected(DTNHost host1, DTNHost host2) {
		// TODO Auto-generated method stub
		if (isWarmup()) {
			return;
		}
		newEvent();
		if((host1.isSink()||host2.isSink())&&(host1.getAddress()==10||host2.getAddress()==10))
			times.add(SimClock.getIntTime());
		
	if(hostAdresses.contains(host1.getAddress()))
		contacts.set(hostAdresses.indexOf(host1.getAddress()),contacts.get(hostAdresses.indexOf(host1.getAddress()))+1);
	else
		{hostAdresses.add(host1.getAddress());
		contacts.add(1);
		}
	
	if(hostAdresses.contains(host2.getAddress()))
		contacts.set(hostAdresses.indexOf(host2.getAddress()),contacts.get(hostAdresses.indexOf(host2.getAddress()))+1);
	else
		{hostAdresses.add(host2.getAddress());
		contacts.add(1);
		}
	
	}

	@Override
	public void hostsDisconnected(DTNHost host1, DTNHost host2) {
		// TODO Auto-generated method stub

	}
	
	public void done(){
		write("Node   nrOfContacts");
		for(int i=0;i<hostAdresses.size();i++)
			write(hostAdresses.get(i)+"  "+contacts.get(i));
		int t=0;
		for(int i=0;i<43200;i++)
			if(times.contains(i)){
			write(i-t+" ");
			t=i;
		}
		
		super.done();
			
		}
	


}

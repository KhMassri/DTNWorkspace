package report;

import java.util.Vector;

import core.DTNHost;
import core.Message;
import core.MessageListener;

public class MessagesPerNodeReport extends Report implements MessageListener {

	Vector<Integer> hostAdresses=new Vector<Integer>();
	Vector<Integer> generated=new Vector<Integer>();
	Vector<Integer> delivered=new Vector<Integer>();
	Vector<Integer> sent=new Vector<Integer>();
	
	public MessagesPerNodeReport(){
		
		super.init();
	}
	
	
	@Override
	public void newMessage(Message m) {
		if (isWarmup()) {
			addWarmupID(m.getId());
			return;
		}
		int ad=m.getFrom().getAddress();
		if(hostAdresses.contains(ad))
			generated.set(hostAdresses.indexOf(ad),generated.get(hostAdresses.indexOf(ad))+1);
		else
			{hostAdresses.add(ad);
			generated.add(1);
			delivered.add(0);
			sent.add(0);
			
			}

	}

	
	@Override
	public void messageTransferred(Message m, DTNHost from, DTNHost to,
			boolean firstDelivery) {
		if (isWarmup() || isWarmupID(m.getId())) return;
		
		
		if (firstDelivery)
		{
			int ad=to.getAddress();
			if(hostAdresses.contains(ad))
				delivered.set(hostAdresses.indexOf(ad),delivered.get(hostAdresses.indexOf(ad))+1);
			else
				{hostAdresses.add(ad);
				delivered.add(1);
				generated.add(0);
				sent.add(0);
				}
			}
		int fromAd=from.getAddress();
		
		if(hostAdresses.contains(fromAd))
			sent.set(hostAdresses.indexOf(fromAd),sent.get(hostAdresses.indexOf(fromAd))+1);
		else
			{hostAdresses.add(fromAd);
			sent.add(1);
			generated.add(0);
			delivered.add(0);
			}
			
			

	}
	
	public void done(){
		write("Node  nrOfGenerated  nrOfDelivered  nrOfSent");
		for(int i=0;i<hostAdresses.size();i++)
			write(hostAdresses.get(i)+"  "+generated.get(i)+"  "+delivered.get(i)+"  "+sent.get(i));
		
		super.done();
			
		}
	
	
	
	@Override
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
		// TODO Auto-generated method stub

	}

	@Override
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		// TODO Auto-generated method stub

	}

	@Override
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
		// TODO Auto-generated method stub

	}


}

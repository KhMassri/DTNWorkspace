/**
 * 
 */
package routing;




import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import movement.MovementModel;

import core.Connection;
import core.DTNHost;
import core.DTNSim;
import core.Message;
import core.Settings;

/**
 * @author khalil
 * 
 */
public class SinkRouter extends ActiveRouter {

	private  List<String> sinkedMessages=new ArrayList<String>();
	private  List<Set<String>> codeWords= new ArrayList<Set<String>>();
	int c = 0;



	/**
	 * @param s
	 */
	public SinkRouter(Settings s) {
		super(s);
	}

	protected SinkRouter(SinkRouter r) {
		super(r);
	}



	public void changedConnection(Connection con) {
		return;
	}


	@Override
	public void update() {
		super.update();

		if (isTransferring() || !canStartTransfer()) {
			return; // transferring, don't try other connections yet

		}	
	}

	public Message messageTransferred(String id, DTNHost from){


		String[] ids = id.split("&");
		if(ids.length<=1)
		{

			Message m = super.messageTransferred(id, from);
			//System.out.println((Double)m.getProperty("FaultToleranceValue"));
			sinkedMessages.add(m.getId());
			System.out.println("-------------------------------Total sinked =  "+sinkedMessages.size());
			checkCodeWords(m,from);
			return m;

		}


		else

		{  
			Set<String> code = new HashSet<String>();
			Collections.addAll(code, ids);
			codeWords.add(code);



			if(sinkedMessages.contains(ids[0]) && !sinkedMessages.contains(ids[1]))
			{
				Message m = this.removeFromIncomingBuffer(id, from);
				m.setID(ids[1]);
				this.putToIncomingBuffer(m, from);
				m = super.messageTransferred(ids[1], from);
				sinkedMessages.add(ids[1]);
				System.out.println("codewords = "+codeWords.size()+" success =  "+c++ +"  Total sinked =  "+sinkedMessages.size());
				checkCodeWords(m,from);
				return m;

			}
			else if(sinkedMessages.contains(ids[1]) && !sinkedMessages.contains(ids[0]))
			{
				Message m = this.removeFromIncomingBuffer(id, from);
				m.setID(ids[0]);
				this.putToIncomingBuffer(m, from);
				m = super.messageTransferred(ids[0], from);
				sinkedMessages.add(ids[0]);
				System.out.println("codewords = "+codeWords.size()+" success =  "+c++ +"  Total sinked =  "+sinkedMessages.size());
				checkCodeWords(m,from);
				return m;
			}


			Message m = this.removeFromIncomingBuffer(id, from);
			checkCodeWords(m,from);
			return m;
		}
		


	}

	private void checkCodeWords(Message m,DTNHost from) {

		int j=0;
		while(j<codeWords.size())
		{
			Set<String> s = codeWords.get(j);
			String id1 = (String) s.toArray()[1];
			String id2 =(String) s.toArray()[0];
			if(sinkedMessages.contains(id1) && sinkedMessages.contains(id2))
			{
				codeWords.remove(s);
				continue;
			}

			else if(sinkedMessages.contains(id1) && !sinkedMessages.contains(id2))
			{
				m.setID(id2);
				this.putToIncomingBuffer(m, from);
				m = super.messageTransferred(id2, from);
				sinkedMessages.add(id2);
				codeWords.remove(s);
				System.out.println("codewords = "+codeWords.size()+" success =  "+c++ +"  Total sinked =  "+sinkedMessages.size());
				j=0;
				continue;
			}
			else if(sinkedMessages.contains(id2) && !sinkedMessages.contains(id1))
			{
				m.setID(id1);
				this.putToIncomingBuffer(m, from);
				m = super.messageTransferred(id1, from);
				sinkedMessages.add(id1);
				codeWords.remove(s);
				System.out.println("codewords = "+codeWords.size()+" success =  "+c++ +"  Total sinked =  "+sinkedMessages.size());
				j=0;
				continue;
			}

			j++;
		}


	}


	protected int checkReceiving(Message m) {
		String[] ids = m.getId().split("&");

		/*two coded messages*/
		if(ids.length>1)
		{
			if(sinkedMessages.contains(ids[0]) && sinkedMessages.contains(ids[1]))
				return DENIED_OLD;


			else if(!sinkedMessages.contains(ids[0]) && !sinkedMessages.contains(ids[1]))
			{
				Set<String> code = new HashSet<String>();
				Collections.addAll(code, ids);
				for(Set<String> c:codeWords)
					if(code.equals(c))
						return DENIED_OLD;

				return super.checkReceiving(m);

			}

			else
				return super.checkReceiving(m);


		}

		/* not coded message*/
		else
		{

			if(sinkedMessages.contains(m.getId()))
				return DENIED_OLD;

			return super.checkReceiving(m);
		}

	}



	public List<String> getSinkedMessages() {
		return sinkedMessages;
	}



	public String hello(){
		return "SinkRouter";
	}

	@Override
	public MessageRouter replicate() {
		return new SinkRouter(this);
	}

}




package routing;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import routing.clusterbased.ClusterTable;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import core.Tuple;

/* Implemented by Khalil Massri as proposed in Clustering and Cluster-Based Routing Protocol for Delay
 * Tolerant Mobile Networks (Ha Dang, Hongyi Wu)
 */

public class ClusteredToSink extends DTNRouter {

	/** Router's setting namespace ({@value})*/
	public static final String CLUSTERED_NS = "ClusteredToSink";
	public static final int DELTA=120;
	public static final int GATEWAYOUTDATETIME=120;

	
	private Map<Integer,NodeInfo> clusters;
	private Map<String,GatewayInfo> gateways;
	private String clusterId=null;
	
	private int seq=0;
	
	/* for sink nodes
	 * 
	 */
	private String sinkClusterId = "0:0";
	private int sinkAddress = 0;
		
	
	/** The parameters string*/
	public static final String ALPHA_S = "alpha";
	public static final String GAMMA_S = "gamma";
	public static final String GAMMAHAT_S = "gammaHat";

	
	private double alpha;
	private double gamma;
	private double gammaHat;

	/** The default values for parameters */
	public static final double DEFAULT_GAMMA = 0.3;
	public static final double DEFAULT_GAMMAHAT = 0.1;
	public static final double DEFAULT_ALPHA = 0.15;


	/**
	 * Constructor. Creates a new prototype router based on the settings in
	 * the given Settings object.
	 * @param settings The settings object
	 */
	public ClusteredToSink(Settings settings) {
		super(settings);
		
		Settings s = new Settings(CLUSTERED_NS);		
		if (s.contains(GAMMA_S)) {
			gamma = s.getDouble(GAMMA_S);
		} 
		else {
			gamma = DEFAULT_GAMMA;
		}

		if (s.contains(GAMMAHAT_S)) {
			gammaHat = s.getDouble(GAMMAHAT_S);
		} 
		else {
			gammaHat = DEFAULT_GAMMAHAT;
		}
		
		if (s.contains(ALPHA_S)) {
			alpha = s.getDouble(ALPHA_S);
		} 
		else {
			alpha = DEFAULT_ALPHA;
		}

       
	}
	
	/**
	 * Copy constructor. Creates a new router based on the given prototype.
	 * @param r The router prototype where setting values are copied from
	 */
	protected ClusteredToSink(ClusteredToSink r) {
		super(r);
		this.gamma=r.gamma;
		this.gammaHat=r.gammaHat;
		this.alpha = r.alpha;
		this.clusters=new HashMap<Integer,NodeInfo>();
		this.gateways=new HashMap<String,GatewayInfo>();
		
		
	}	

	
	
	
	
	@Override
	public void changedConnection(Connection con) {
		if(clusterId==null){
			clusterId=getHost().getAddress()+":"+(seq++);
			getHost().color=new Color(clusterId.hashCode());
		}
		
		
		if(!clusters.containsKey(sinkAddress))
			clusters.put(sinkAddress, new NodeInfo(alpha,sinkClusterId,SimClock.getIntTime()));
		
		DTNHost other = con.getOtherNode(getHost());
		/* if the other node is a generator do nothing
		 * 
		 */
		if(other.getRouter().hello().equals("GeneratorRouter"))
			return;
		
		checkTimeoutEvent();
		
		if(con.isUp()){
			
			if(other.isSink())
			{
				int currentTime=SimClock.getIntTime();
				/* update the meeting probability
				 * 
				 */
				
				if(clusters.containsKey(sinkAddress))
					clusters.get(sinkAddress).update(currentTime, false);
				else
					clusters.put(sinkAddress, new NodeInfo(alpha,sinkClusterId,currentTime));
				
								
				
				
			}
			
			
			else if(con.isInitiator(getHost()))
			{
				int currentTime=SimClock.getIntTime();
				ClusteredToSink otherRouter = (ClusteredToSink)other.getRouter();
				
				
				/* update the meeting probability
				 * 
				 */
				
				if(clusters.containsKey(other.getAddress()))
					clusters.get(other.getAddress()).update(currentTime, false);
				else
					clusters.put(other.getAddress(), new NodeInfo(alpha,otherRouter.getClusterId(),currentTime));
				
				if(otherRouter.clusters.containsKey(getHost().getAddress()))
					otherRouter.clusters.get(getHost().getAddress()).update(currentTime, false);
				else
					otherRouter.clusters.put(getHost().getAddress(), new NodeInfo(alpha,this.getClusterId(),currentTime));
				
				
				if(otherRouter.getClusterId().equals(clusterId))
					/* verify if the are still qualified to stay 
					 * in the same cluster
					 */
					membershipCheck(otherRouter);
				else
					join(otherRouter);
				
			}
			
		}	
		
		}
	
	/* verify if the are still qualified to stay 
	 * in the same cluster, either to synchronize their information
	 * or to leave each other cluster if the are not qualified
	 */
	private void membershipCheck(ClusteredToSink otherRouter) {
		if(clusters.get(otherRouter.getHost().getAddress()).getProb()>=gamma)
			synch(otherRouter);
		else
			leave(otherRouter);
		
	}

	/*
	private void toSinkMembershipCheck(){
		
		if(clusters.get(sinkAddress).getProb()<gamma)
		{
			clusterId=getHost().getAddress()+":"+(seq++);
			getHost().color=new Color(clusterId.hashCode());
			gateways.clear();			
			
		}
			
		
	}
	
	
	private void toSinkJoin(){
		
		if(clusters.get(sinkAddress).getProb()>=gamma)
						
			clusterId = sinkClusterId;
			
			
	}
	*/
	
	
	private void synch(ClusteredToSink otherRouter) {
		
		// from i to j
		doSynchOf(otherRouter);
		// from j to i
		otherRouter.doSynchOf(this);

		// gateway synch
		// i to j
		doGatewaySynchOf(otherRouter);
		// j to i
		otherRouter.doGatewaySynchOf(this);
	}
	
	
	void doSynchOf(ClusteredToSink otherRouter){
		
		
		
		for(Entry<Integer,NodeInfo> e:clusters.entrySet()){
			Integer k= e.getKey();
			NodeInfo node = e.getValue();
			if(!node.getClusterId().equals(clusterId))
				continue;
			/*
			 * cluster members, see if the other node has
			 * different information for this node
			 */
			NodeInfo otherNode = otherRouter.clusters.get(k);
			if(otherNode == null) 
				continue;
			
			if(!otherNode.getClusterId().equals(clusterId)){
				if(node.getTime()>otherNode.getTime())
					otherNode.setClusterId(clusterId);
				else
					node.setClusterId(otherNode.getClusterId());
				}
			}
				
	}
	
	/*
	 * update the gateway for each cluster to the newer information
	 * between the two nodes
	 */
	void doGatewaySynchOf(ClusteredToSink otherRouter){
		
		for(Entry<String,GatewayInfo> e:gateways.entrySet())
		{
			GatewayInfo gateway =e.getValue();
			GatewayInfo otherGateway =otherRouter.gateways.get(e.getKey());
			if(otherGateway==null)
				continue;
			if(gateway.getProb()<otherGateway.getProb())
			{
				gateway.setGateway(otherGateway.getGateway());
				gateway.setProb(otherGateway.getProb());
				}
			}
		
		}


		
	/*
	 * one must leave the cluster(lower stability)
	 */
	private void leave(ClusteredToSink otherRouter) {
		if(stab() < otherRouter.stab())
		// i leave
			buy(otherRouter);
			
		
		else
		// j leave
			otherRouter.buy(this);
			
		
				
	}

	
	private void buy(ClusteredToSink otherRouter) {
		clusterId=getHost().getAddress()+":"+(seq++);
		getHost().color=new Color(clusterId.hashCode());
		gateways.clear();
		otherRouter.clusters.get(getHost().getAddress()).setClusterId(clusterId);
		
	}

	double stab()
	{
		double min = 1;
		for(Entry<Integer,NodeInfo> e:clusters.entrySet())
		{
			if(!(e.getValue().getClusterId().equals(clusterId)))
				continue;
			if(e.getValue().getProb()<min)
				min=e.getValue().getProb();
		}

		return min;
		
	}

private void join(ClusteredToSink otherRouter) {
		
	boolean pass,otherPass;
	pass=checkJoin(otherRouter);
	otherPass=otherRouter.checkJoin(this);
	
		if(!pass && !otherPass)
		return;
		
		if(pass && otherPass)
		{
			if(stab()<otherRouter.stab())
				goToJoin(otherRouter);
			else otherRouter.goToJoin(this);
			
		}
		else if(pass)
			goToJoin(otherRouter);
		else
			otherRouter.goToJoin(this);
		
	}
	
	
	
/*
 * check if i has a prob>gamma for all nodes with the same clusterid
 *  with respect to j
 * 
 */
	boolean checkJoin(ClusteredToSink otherRouter){
		
		for(Entry<Integer,NodeInfo> e:otherRouter.clusters.entrySet()){
				if(!(e.getValue().getClusterId().equals(otherRouter.getClusterId())))
					continue;
				if(!clusters.containsKey(e.getKey()))
					return false;
				if(clusters.get(e.getKey()).getProb()<gamma)
					return false;
			}
		return true;
	}
	
	private void goToJoin(ClusteredToSink otherRouter) {
		
		clusterId=otherRouter.getClusterId();
		
		for(Entry<Integer,NodeInfo> e:otherRouter.clusters.entrySet())
			if(e.getValue().getClusterId().equals(otherRouter.clusterId)&&clusters.containsKey(e.getKey()))
				clusters.get(e.getKey()).setClusterId(otherRouter.getClusterId());
		
		gateways.clear();
		gateways.putAll(otherRouter.gateways);
		
	}
	
	
	
	private String getClusterId() {
		
		return clusterId;
	}

	
		
	@Override
	protected void transferDone(Connection con) {
		Message m = con.getMessage();
		/* was the message delivered to the final recipient? */
		this.deleteMessage(m.getId(), false); // delete from buffer
		
	}
	
	
    @Override
	protected Message getOldestMessage(boolean excludeMsgBeingSent) {
		Collection<Message> messages = this.getMessageCollection();
		List<Message> validMessages = new ArrayList<Message>();

		for (Message m : messages) {	
			if (excludeMsgBeingSent && isSending(m.getId())) {
				continue; // skip the message(s) that router is sending
			}
			validMessages.add(m);
		}
		
		
		
		return validMessages.get(validMessages.size()-1); // return last message
	}
	
	@Override
	public void update() {
		super.update();
		if(clusterId==null){
			clusterId=getHost().getAddress()+":"+(seq++);
			getHost().color=new Color(clusterId.hashCode());
		}
		
		checkTimeoutEvent();
		
		if (!canStartTransfer() ||isTransferring()) {
			return; // nothing to transfer or is currently transferring 
		}
		
		// try messages that could be delivered to final recipient
		if (exchangeDeliverableMessages() != null) {
			return;
	}
		
		//try messages that could be delivered to final recipient
		if (tryMessagesForSinks() != null) 
			return;
	
		
		
		tryOtherMessages();	
	}
	
	
	private void checkTimeoutEvent() {
		
		int currentTime=SimClock.getIntTime();
		boolean timeout=true;
		for(Entry<Integer, NodeInfo> e:clusters.entrySet())
			if(currentTime - e.getValue().getTime()>=DELTA)
			{
				timeout=true;
				for(Connection con:getConnections())
					if(con.getOtherNode(getHost()).getAddress()==e.getKey())
						timeout=false;
				e.getValue().update(currentTime,timeout);
				}
		
		gatewayOutdate(currentTime);
		gatewayUpdate(currentTime);
		
		
	}

	private void gatewayUpdate(int currentTime) {
		
		/*case 3
		 * add any cluster not included in the gateway Table
		 */
		
		for(Entry<Integer, NodeInfo> e:clusters.entrySet()){
			
			if(gateways.containsKey(e.getValue().getClusterId()))
				continue;
			addClusterToGatewayTable(e.getValue().getClusterId());
		}
		
		
		for(Entry<String,GatewayInfo> e:gateways.entrySet()){
			/* identify node k to which it has the highest contact probability
			 * among all nodes in Cluster c
			 */
			Integer k = findKOf(e.getKey());
			
			if(k!=null){
			double maxProb=clusters.get(k).getProb();
				
			/* case 1
			 * if this node is not the gateway of the cluster find 
			 * the best gateway
			 */
				if(e.getValue().getGateway()!= getHost().getAddress()){
					if(maxProb > e.getValue().getProb())
						e.getValue().update(k,maxProb,currentTime);
					}
				/* case 2
				 * if this node is the gateway for the cluster
				 * try to find another qualified one or remove it 
				 */
				else{
					if(maxProb>=gammaHat)
						e.getValue().update(k, maxProb, currentTime);
					else
						gateways.remove(e);
					
					}
				}
			}
		}
			
		
	

	private void gatewayOutdate(int currentTime) {

	
		if(gateways.isEmpty())return;
		
	for(Entry<String,GatewayInfo> e:gateways.entrySet())
		if(currentTime - e.getValue().getTime()>GATEWAYOUTDATETIME)
			gateways.remove(e);
		
	}

	private void addClusterToGatewayTable(String c) {
		
		Integer k=findKOf(c);
		if(k!=null){
			double maxProb=clusters.get(k).getProb();
			int time=clusters.get(k).getTime();
				if(maxProb>=gammaHat)
					gateways.put(c,new GatewayInfo(k,maxProb,time));
				
			}
		}

	
	private Integer findKOf(String key) {
		double max=0;
		Integer k=null;
		
		
		for(Entry<Integer, NodeInfo> e:clusters.entrySet())
			if(e.getValue().getClusterId().equals(key))
				if(e.getValue().getProb() > max){
				max=e.getValue().getProb();
				k=e.getKey();
			}

	return k;
	}

	private Connection tryOtherMessages() {
		 
	
		Collection<Message> msgCollection = getMessageCollection();
		
		/* for all connected hosts that are not transferring at the moment,
		 * collect all the messages that could be sent */
		for (Connection con : getConnections()) {
			
			DTNHost other = con.getOtherNode(getHost());
			
			if(other.getRouter().hello().equals("GeneratorRouter"))
				continue;
			
			if(other.isSink())
				return tryMessagesForSinks();
			
			ClusteredToSink othRouter = (ClusteredToSink)other.getRouter();
			
			if (othRouter.isTransferring())//||othRouter.getClusterId().equals(clusterId))
				continue;
			
			for (Message m : msgCollection) {
				
				if (othRouter.hasMessage(m.getId())||m.getHops().contains(other))
					continue;
				
				
				if(!(clusterId.equals(othRouter.getClusterId())))
					if(othRouter.clusters.get(sinkAddress).getProb()>clusters.get(sinkAddress).getProb())
							if(startTransfer(m, con)==RCV_OK)
							return con;
				
			}
			
				
						
		}
		
		
			
		return null;	
	}
	
		
	@Override
	public RoutingInfo getRoutingInfo() {
		RoutingInfo top = super.getRoutingInfo();
		top.addMoreInfo(new RoutingInfo("my cluster is "+clusterId));
		top.addMoreInfo(new RoutingInfo("ToSinkProb "+clusters.get(sinkAddress).getProb()));
		
		String s="";
		
		for(Entry<String,GatewayInfo> e:gateways.entrySet()){
			s="";
			for(Entry<Integer,NodeInfo> ee:clusters.entrySet())
				if(ee.getValue().getClusterId().equals(e.getKey()))
					s+=", "+ee.getKey();
			
			top.addMoreInfo(new RoutingInfo("cluster "+e.getKey()+" mem "+s+" "+" Gate "+e.getValue().getGateway()));

			
		}		

		return top;
	}
	
	@Override
	public MessageRouter replicate() {
		ClusteredToSink r = new ClusteredToSink(this);
		return r;
	}
	

class NodeInfo{
	
	double prob;
	String clusterId;
	int time;
	
	public NodeInfo(double alpha, String clusterId, int currentTime) {

	this.prob=alpha;
	this.clusterId=clusterId;
	time=currentTime;
	
	}

	public void setClusterId(String clusterId) {
		this.clusterId=clusterId;
		getHost().color=new Color(this.clusterId.hashCode());
		
		
	}

	public void update(int currentTime,boolean timeOut) {
		if(timeOut)
			prob=(1-alpha)*prob;
		else
			prob=(1-alpha)*prob+alpha;
		
		time=currentTime;
	}
	
	String getClusterId(){return clusterId;}
	int getTime(){return time;}
	double getProb(){return prob;}
}

class GatewayInfo{
	
	double prob;
	Integer gateway;
	int time;
	
	public GatewayInfo(Integer k, double maxProb, int time) {

	this.gateway =k;
	this.prob=maxProb;
	this.time=time;
	
	}
	public void setProb(double prob) {
		this.prob=prob;
	}
	public void setGateway(Integer gateway) {
		this.gateway=gateway;
	}
	double getProb(){return prob;}
	public void update(Integer k, double max, int currentTime) {
		gateway=k;
		prob=max;
		time=currentTime;
		
	}
	Integer getGateway(){return gateway;}
	int getTime(){return time;}
}

	
}
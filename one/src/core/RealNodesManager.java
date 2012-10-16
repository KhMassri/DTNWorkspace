package core;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.EventObject;
import java.util.Random;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/*
 * This manager scans for other DTN devices on port 8000. and if so, it will exchange 
 * the messages between it (mounted by node 0 in the simulator) and the other 
 * DTN device. 
 * 
 * */

public class RealNodesManager implements ConnectionEventListener{
	public static final String MANAGER_NS = "RealNodesManager";
	//	public static final int HOST_ADDRESS = new Settings(MANAGER_NS).getInt("host_addres");

	public static final String Name = "One_1";
	public static final String DestName = "One_2";	

	public static World world;

	private DTNHost androidBridge;
	private DTNHost wsnBridge1;
	private DTNHost wsnBridge2;

	public static final String wsn1IP = new Settings(MANAGER_NS).getSetting("wsn1IP");
	public static final String wsn2IP = new Settings(MANAGER_NS).getSetting("wsn2IP");


	public static final int wsn1Port = new Settings(MANAGER_NS).getInt("wsn1Port");
	public static final int wsn2Port = new Settings(MANAGER_NS).getInt("wsn2Port");

	private ConnectionScanner connectionScanner;
	//private WSNServer wsnServer;
	private EventDispatcher wsnBridgeDispatcher;

	public RealNodesManager(World world) {
		RealNodesManager.world = world;

		/* Bridges Configuration
		 * 
		 */
		androidBridge  = RealNodesManager.world.getNodeByAddress(0);
		wsnBridge1 = RealNodesManager.world.getNodeByAddress(2);
		wsnBridge2 = RealNodesManager.world.getNodeByAddress(3);
		wsnBridge1.color=Color.RED;
		wsnBridge2.color = Color.RED;
		wsnBridgeDispatcher = new EventDispatcher();
		wsnBridgeDispatcher.bind(this);
		wsnBridge1.configureAsWSNBridge(wsnBridgeDispatcher,new Coord(50,100),wsn1IP,wsn1Port);
		wsnBridge2.configureAsWSNBridge(wsnBridgeDispatcher,new Coord(200,100),wsn2IP,wsn2Port);

		/*
		 * scanning for wifi devices around 
		 */
		try {
			this.connectionScanner  = new ConnectionScanner();
			this.connectionScanner.start();
			this.connectionScanner.bind(this);

			//			this.wsnServer = new WSNServer(wsnBridge);
			//			this.wsnServer.start();

		} catch(Exception e){
			System.out.print(e.getMessage());
		}

	}

	public void exit(){
		this.connectionScanner.exit = true;
	}


	/*
	 * upon a connection event this method is called for exchanging information between 
	 * ONE_1 and the other bridge
	 */
	public void exchange(String ip,int port,DTNHost bridge){
		try{
			world.inPause = true;

			/** Prepare Connection */


			Socket tcpsocket  = new Socket(ip,port);
			ObjectOutputStream  output = new  ObjectOutputStream(tcpsocket.getOutputStream());
			ObjectInputStream input = new ObjectInputStream(tcpsocket.getInputStream());




			/** Sending my name */
			output.writeObject(RealNodesManager.Name);


			/** Receiving other name */
			String otherId = (String) input.readObject();

			System.out.println("Node "+bridge+" is connected to "+otherId);


			/** Sending my id  (VALID ONLY BETWEEN ANDROID - HERE IS SIMULATED)*/
			ArrayList<String> myids = new ArrayList<String>();
			for (Message msg: bridge.getMessageCollection())
				myids.add(msg.getId());
					
//			output.writeObject(myids);


					/** Receiving other messages id (NOT USED - Only Between Android*/
//			ArrayList<String> ids = (ArrayList<String>) input.readObject();

					/** Sending messages */
			Collection<MessageProxy> msgs = new ArrayList<MessageProxy>();
			for (Message msg: bridge.getMessageCollection()){
				if (msg.getTo() != bridge)
					continue;
				msgs.add(new MessageProxy(msg));
			}

			Random generator = new Random(213981209);

			StringBuilder sb = new StringBuilder();
			sb.append("<messages>");
			for (MessageProxy msgPr : msgs){



				sb.append("<msg>");
				sb.append("<id>"+generator.nextInt(10000000)+"</id>");
				sb.append("<from>"+RealNodesManager.Name+"</from>");
				sb.append("<to>"+otherId+"</to>");	

				sb.append("<body>");
				sb.append(msgPr.getId());
				sb.append("#");
				sb.append(Integer.toString(msgPr.getFrom()));
				sb.append("#");
				sb.append(Integer.toString(msgPr.getTo()));
				sb.append("#");
				sb.append(Integer.toString(msgPr.getSize()));
				sb.append("#");
				sb.append(msgPr.getData());


				sb.append("</body>");


				sb.append("</msg>");
			}
			sb.append("</messages>");
			output.writeObject(sb.toString());

			System.out.print("Sending......."+sb.toString());


			/** Removing sent messages */
			System.out.println("Sended: "+msgs.size());
			for (MessageProxy mP : msgs)
				bridge.getRouter().deleteMessage(mP.getId(), false);


					/** Receive response */
					String xml = (String) input.readObject();
					System.out.print("Receiving......."+xml);

					ArrayList<MessageProxy> proxy_msgs = fromXml(xml);
					Collection<Message> simulator_msgs = new ArrayList<Message>();
					
					for (MessageProxy msg: proxy_msgs)
						simulator_msgs.add(msg.convert());


					/** Adding received messages */
					System.out.println("Received: "+simulator_msgs.size());
					for (Message m : simulator_msgs)
						bridge.getRouter().createNewMessage(m);
						


		} catch (Exception ex){
			ex.printStackTrace();
		}

		world.inPause = false;
		return;
	}


	public void onConnectionReceived(ConnectionEvent evnt){
		if(evnt.getSource() instanceof RealNodesManager)
			exchange(evnt.ip,evnt.port,androidBridge);
		else if (evnt.getSource() instanceof DTNHost)
			if(evnt.getSource().equals(wsnBridge1))
				exchange(evnt.ip,evnt.port,wsnBridge1);
			else if(evnt.getSource().equals(wsnBridge2))
				exchange(evnt.ip,evnt.port,wsnBridge2);
	}



	public static ArrayList<MessageProxy> fromXml(String xml){
		ArrayList<MessageProxy> parsedData = new ArrayList<MessageProxy>();

		Document doc;
		try {

			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
					new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));
			Element root=doc.getDocumentElement();


			NodeList notes= root.getChildNodes();;//.getElementsByTagName("messages"); 

			for(int i=0;i<notes.getLength();i++){
				Node c= notes.item(i);

				if(c.getNodeType()==Node.ELEMENT_NODE){      
					MessageProxy newNote= new MessageProxy(); 

					NodeList noteDetails=c.getChildNodes(); 
					for(int j=0;j<noteDetails.getLength();j++){
						Node c1=noteDetails.item(j);
						if(c1.getNodeType()==Node.ELEMENT_NODE){
							Element detail=(Element)c1;
							String nodeName  = detail.getNodeName(); 
							String nodeValue = detail.getFirstChild().getNodeValue();


							if(nodeName.equals("body")){
								String[] msgProxy = nodeValue.split("#");
								newNote.setId(msgProxy[0]);
								newNote.setFrom(msgProxy[1]);
								newNote.setTo(msgProxy[2]);
								newNote.setSize(msgProxy[3]);
								newNote.setData(msgProxy[4]);
							}
						}  
					}


					parsedData.add(newNote);
				}                                                                       
			}

			return parsedData;
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (FactoryConfigurationError e) {
			e.printStackTrace();
		} 

		return null;    
	}


}


class ConnectionScanner extends EventDispatcher {

	public boolean exit = false;

	public ConnectionScanner()  {
	}

	public void run(){
		try{
			while (!exit){
				//Set-up
				DatagramSocket socket = new DatagramSocket(8000);
				socket.setBroadcast(true);

				//Wait for
				byte[] buf = new byte[1024*100];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);

				//Close connection
				socket.close();

				// Get IP
				InetAddress address = packet.getAddress();
				String ip = address.toString().replace("/", "");

				System.out.println("Check Connection");
				this.dispatch(new ConnectionEvent(this, ip,3000));

				sleep(10000); //Wait 10 seconds
			}

		} catch(Exception e){
			System.out.println(e.getMessage());
		}
	}
}



class ConnectionEvent extends EventObject {

	private static final long serialVersionUID = 1L;
	public String ip;
	public int port;

	public ConnectionEvent(Object source, String ip,int port) {
		super(source);
		this.ip = ip;
		this.port = port;
	}
}

interface ConnectionEventListener extends EventListener {
	public void onConnectionReceived(ConnectionEvent evt);
}

class EventDispatcher extends Thread {
	protected javax.swing.event.EventListenerList listenerList =
			new javax.swing.event.EventListenerList();

	public void bind(ConnectionEventListener listener) {
		listenerList.add(ConnectionEventListener.class, listener);
	}

	public void unbind(ConnectionEventListener listener) {
		listenerList.remove(ConnectionEventListener.class, listener);
	}

	void dispatch(ConnectionEvent evt) {
		Object[] listeners = listenerList.getListenerList();
		// Each listener occupies two elements - the first is the listener class
		// and the second is the listener instance
		for (int i=0; i<listeners.length; i+=2) {
			if (listeners[i]==ConnectionEventListener.class) {
				((ConnectionEventListener)listeners[i+1]).onConnectionReceived(evt);
			}
		}
	}
}


class MessageProxy implements Serializable {

	private static final long serialVersionUID = -2758368066043283637L;


	private int from;
	private int to;
	private String id;
	private int size;
	private String data;

	public MessageProxy(){

	}
	public MessageProxy(Message msg){
		this.from = msg.getFrom().getAddress();
		this.to = msg.getTo().getAddress();
		this.id = msg.getId();
		this.size = msg.getSize();
		this.data = msg.getData();

	}

	public Message convert(){
		return new Message(RealNodesManager.world.getNodeByAddress(this.from),
				RealNodesManager.world.getNodeByAddress(this.to),
				this.id,
				this.size,this.data);
	}

	public int getFrom(){
		return from;
	}
	public void setFrom(String value){
		try{
			this.from = Integer.parseInt(value);
		} catch (NumberFormatException ex){
			ex.printStackTrace();
		}
	}

	public int getTo(){
		return to;
	}
	public void setTo(String value){
		try{
			this.to = Integer.parseInt(value);
		} catch (NumberFormatException ex){
			ex.printStackTrace();
		}
	}

	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
	public String getId(){
		return id;
	}
	public void setId(String value){
		this.id = value;
	}

	public int getSize(){
		return size;
	}
	public void setSize(String value){
		try{
			this.size = Integer.parseInt(value);
		} catch (NumberFormatException ex){
			ex.printStackTrace();
		}
	}


}




/*
 * 
 * Not used now, it would be used in the case we need the WSN to connect 
 * to ONE whenever it needs
 */

//class WSNServer extends Thread {
//
//	private ServerSocket dateServer;
//	private DTNHost wsnBridge;

//
//	public WSNServer(DTNHost wsnBridge) throws Exception {
//		this.wsnBridge=wsnBridge;  
//		dateServer = new ServerSocket(3000);
//		System.out.println("Server listening on port 3000.");
//		this.start();
//	} 
//
//	public void run() {
//		while(true) {
//			try {
//				System.out.println("Waiting for WSN connections.");
//				Socket client = dateServer.accept();
//				System.out.println("Accepted a connection from: "+ client.getInetAddress());
//				Connect c = new Connect(client);
//			} catch(Exception e) {}
//		}
//	}
//}
//
//
//class Connect extends Thread {
//	private Socket client = null;
//	private ObjectInputStream ois = null;
//	private ObjectOutputStream oos = null;
//
//	public Connect() {}
//
//	public Connect(Socket clientSocket) {
//		client = clientSocket;
//		try {
//			ois = new ObjectInputStream(client.getInputStream());
//			oos = new ObjectOutputStream(client.getOutputStream());
//		} catch(Exception e1) {
//			try {
//				client.close();
//			}catch(Exception e) {
//				System.out.println(e.getMessage());
//			}
//			return;
//		}
//		this.start();
//	}
//
//
//	public void run() {
//		try {
//
//			// exchange
//
//			/** Receive response */
//			String xml = (String) ois.readObject();
//			ArrayList<MessageProxy> proxy_msgs = RealNodesManager.fromXml(xml);
//			Collection<Message> simulator_msgs = new ArrayList<Message>();
//			for (MessageProxy msg: proxy_msgs)
//				simulator_msgs.add(msg.convert());
//
//					for(Message m:simulator_msgs)
//						System.out.println(m.getFrom());
//
//							/** Adding received messages */
//							//System.out.println("Received: "+simulator_msgs.size());
//							//for (Message m : simulator_msgs)
//							//AndroidBridge.getRouter().createNewMessage(m);
//							//System.out.println("Message in Router: "+AndroidBridge.getRouter().getMessageCollection().size());
//
//							// close streams and connections
//							ois.close();
//							oos.close();
//							client.close(); 
//		} catch(Exception e) {}       
//	}
//}
//













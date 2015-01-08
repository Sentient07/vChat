package ServerClient;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server implements Runnable{
    //List Having list of clients
    private final ArrayList<ClientConnection> clientList
            = new ArrayList<>();
    private HashMap<String, keyWordHandler> taskMap
            = new HashMap<>();
    //HasMap having the Mapping of String(ClientID) and ClientConnection class
    private final HashMap<String, ClientConnection> idMap
            = new HashMap<>();
    //HashMap having the map of String(ClientID) and String(Nick)
    private final HashMap<String, String> nameMap
            = new HashMap<>();
    //List having IP address
    private final CopyOnWriteArrayList<SocketAddress> banIpaddress = new CopyOnWriteArrayList<SocketAddress>();
    //Hashmap containing, string-> command, id->socket
    private final ConcurrentHashMap<String, Socket> idSocketMap = new ConcurrentHashMap();
    private static final ConcurrentHashMap<String, Commands> commandMap = new ConcurrentHashMap();

    
    //List of commands that could be executed.
    enum Commands {
    	KICK, KICK_IP, REGISTER, PM, ME, AVOID, FILE, MUTE
    }
    
    static {
    	commandMap.put("kick", Commands.KICK);
    	commandMap.put("kickip", Commands.KICK_IP);
    	commandMap.put("register", Commands.REGISTER);
    	commandMap.put("pm", Commands.PM);
    	commandMap.put("me", Commands.ME);
    	commandMap.put("avoid", Commands.AVOID);
    	commandMap.put("file", Commands.FILE);
    }
    
    //Returns the enum from the Map of Commands to the enum
    private Commands enumMap(String cmd){
    	Commands mcommand = commandMap.get(cmd);
    	System.out.println("returning the enum map");
    	return mcommand;
    }
    
    //Returns the client Socket Object
    private Socket sockMap(String id){
    	Socket sock = idSocketMap.get(id);
    	return sock;
    }
    
    //Gives the IpAddress of the client connected 
    private SocketAddress returnsIP(String cid){
    	Socket sock = sockMap(cid);
    	SocketAddress ip = sock.getRemoteSocketAddress();
    	return ip;
    }
    
    //Returns ClientConnection instance corresponding to the id
    private ClientConnection returnsidMap(String Clientid) {
        if (this.idMap.containsKey(Clientid)) {
            ClientConnection connectionInstance = this.idMap.get(Clientid);
            return connectionInstance;
        } else {
            return null;
        }
    }

    public static void main(String args[]) {
        new Thread(new Server()).start();
    }

    public void register(String s, keyWordHandler r) {
        this.taskMap.put(s, r);
    }

    @Override
    public void run() {
        try {
            ServerSocket server = new ServerSocket(8888);
            long currentId = 0;
            System.out.println("Server listening for connections!");
            acceptingClient: //A label for breaking from the loop when necessary
            while (true) {
                Socket s = server.accept();
                if(!(banIpaddress.contains(s.getRemoteSocketAddress()))){
	                System.out.println("New connection! ID: " + currentId);
	                ClientConnection instance = new 
	                    ClientConnection(currentId++, s);
	                String id = String.valueOf((currentId - 1));
	                //puts the id and ClientConnection class's intance to the hashmap
	                this.idMap.put(id, instance);
	                this.idSocketMap.put(id, s);
	                //adds the ClientConnection class's instance to the array list
	                this.clientList.add(instance);
	                new Thread(instance).start(); 
                }
                else {
                	OutputStreamWriter osw = new OutputStreamWriter(s.getOutputStream());
                	osw.write("You're banned here !");
                	osw.flush();
                	osw.close();
                	s.close();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }

    private void registerKeyWord(String keyword, keyWordHandler k1) {
        boolean isKeyPresent = false;
        String parse;
        for (Entry thisEntry : this.taskMap.entrySet()) {
            parse = (String) thisEntry.getKey();
            if (parse.equals(keyword)) {
                isKeyPresent = true;
                break;
            }
        }
        if (isKeyPresent == true) {
            this.taskMap.put(keyword, k1);
        }

    }

    //Broadcast message to all the members in the client list including the client
    protected void broadcastMessages(String message) throws IOException {
    	
    	System.out.println("Gonna send message to be broadcased");
        for (ClientConnection client : clientList) {
            client.writeOnSocket(message + "\r\n");
        }
    }

    //Sends message to specific Client
    void SendtoClient(ClientConnection receiveInstant, String Message) throws IOException {
        receiveInstant.writeOnSocket(Message);

    }

    void removeClient(ClientConnection inst) {
        clientList.remove(inst);
    }

	class ClientConnection implements Runnable {
		
		private LinkedBlockingQueue<String> messageQ;
		private final OutputStreamWriter outs;
	    private final long mId;
	    private volatile Socket mSocket;
	    private final long timelap = 120000;
	    private final long delay;
	    private Timer pingTimer = new Timer();
	    private TimerTask pingTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    writeOnSocket("PING");
                    pingTimer.schedule(timedoutAction, timelap);
                } catch (IOException e) {
                    try {
						quitClient();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
                }
            }
        };
	    private final TimerTask timedoutAction = new TimerTask() {
	        
	        @Override
	        public void run() {
	            try {
	                quitClient();
	            } catch (IOException ex) {
	                Logger.getLogger(ClientConnection.class.getName()).log(Level.SEVERE, null, ex);
	            }
	        }
	    };
	
	    public ClientConnection(long id, Socket s) throws Exception {
	        this.delay = 0;
	    	this.mId = id;
	        this.mSocket = s;
	        this.outs = new OutputStreamWriter(mSocket.getOutputStream());
	        this.messageQ = new LinkedBlockingQueue<String>();
	    }
	
	    @Override
	    public void run() {
	    	
	    	
	    	MessageHandling mhandle	= new MessageHandling();
	    	new Thread(mhandle).start();
	    	
	        try {
	            //System.out.println("entered the try block !");
	            String message, line;
	            //boolean toBeBroadcasted = true;
	            BufferedReader lines = new BufferedReader(
	                    new InputStreamReader(
	                            mSocket.getInputStream()));
	            System.out.println(lines.ready());
	            while ((line = lines.readLine()) != null) {
	                if (line.equals("PONG")) {
	                    timedoutAction.cancel();
	                    pingTimer.schedule(timedoutAction, timelap);
	                } else if (!(":q".equals(line))) {
	                    message = mId + ":-" + line;
	                    System.out.println(message);
	                    //Sending to the message Handling class
	                    mhandle.addToQueue(message);
	                    
	                } else {
	                    quitClient();
	                }
	            }
	            
	        } catch (IOException ex) {
	            Logger.getLogger(ClientConnection.class.getName())
	                    .log(Level.SEVERE, null, ex);
	        } finally {
	            try {
	                quitClient();
	            } catch (IOException ex) {
	                Logger.getLogger(ClientConnection.class.getName())
	                        .log(Level.SEVERE, null, ex);
	            }
	        }
	    }
	
	    private void quitClient() throws IOException {
	        String quitMessage = "Client " + mId + " terminated!";
	        System.out.println(quitMessage);
	        broadcastMessages(quitMessage);
	        removeClient(this);
	        mSocket.close();
	
	    }
	
	    public synchronized void writeOnSocket(String message) throws IOException {
        	System.out.println("Writing on Socket");
	    	outs.write(message);
	        outs.flush();
	    }
	    
		class MessageHandling implements Runnable {
			private HashMap<String, keyWordHandler> taskMap;
		    private final long timeout;
		    
		    public MessageHandling() {
		        //this.messageQueue = cInstance.messageQ;      
		        this.timeout = 1;
		    }
		    
		    @Override
		    public void run() {
		        while (true) {
	                String message;
					try {
						message = messageQ.take();
						System.out.println("It's taking");
						SendsTo(message) ;

					} catch (IOException | InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
		        }
		    }
		
		    //Has logical error ! Write cases for "!"
		    void SendsTo(String message) throws IOException {
		    	//Checks if the message starts with ":-"
		        if (message.contains(":-")) {
		        	//The StringArray that has the message and the one who has sent
		            String[] contentArray = message.split(":-", 2);
		            //Get's the client connection instance of the client <l:37>
		            ClientConnection clientInstance
		                    = returnsidMap(contentArray[0]);
		            String fMessage = contentArray[1];
		            String[] cmdMsgArray;		            
		            if(fMessage.startsWith("/")){
		            	System.out.println("It's a command !");
		            	cmdMsgArray = fMessage.substring(1).split(" ", 2);
		            	//mcontent[0];
		            	//mcontent[1];
		            	if (commandMap.containsKey(cmdMsgArray[0])){
		            		System.out.println("True");
		            		Commands cmd = enumMap(cmdMsgArray[0]);
		            		switch (cmd){
		            		case KICK:
		            			//Returns the client to be kicked
		            			ClientConnection cClient = returnsidMap(cmdMsgArray[1].split(" ", 2)[0]);
		            			cClient.writeOnSocket("You're being Kicked !");
		            			Socket cSock = sockMap(cmdMsgArray[1].split(" ", 2)[0]);
		            			cSock.close();
		            			break;
		            		case KICK_IP:
		            			Socket kSock = sockMap(cmdMsgArray[1].split(" ", 2)[0]);
		            			ClientConnection kClient = returnsidMap(cmdMsgArray[1].split(" ", 2)[0]);
		            			kClient.writeOnSocket("You're being banned ! ");
		            			banIpaddress.add(kSock.getRemoteSocketAddress());
		            			kSock.close();
		            			break;
		            		case PM :
		            			ClientConnection pClient = returnsidMap(cmdMsgArray[1].split(" ", 2)[0]);
		            			pClient.writeOnSocket(cmdMsgArray[1].split(" ", 2)[1]);
		            			break;
		            		case ME:
		            			String mMessage = "***" + contentArray[0]+ " " + cmdMsgArray[1];
		            			broadcastMessages(mMessage);
		            			break;
		            		case REGISTER :
		            			System.out.println("This feature will be made available soon !");
		            			break;
		            		case FILE:
		            			System.out.println("This feature will be made available soon !");
		            			break;
		            		case AVOID:
		            			System.out.println("This feature will be made available soon !");
		            		default :
		            			System.out.println("There is no support for the following command");
		            		}
		            	}
		            }	
		            else {
		            	//Broadcasting messages
		            	System.out.println("Sending message to be broadcased");
		            	broadcastMessages(message);
		            }
		        }
		    }
		    
		    void addToQueue(String message) {
		    	System.out.println("It's getting added");
		        messageQ.add(message);
		    }
		}
	}
}
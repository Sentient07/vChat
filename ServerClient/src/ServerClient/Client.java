package ServerClient;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author ramana
 */

public class Client implements Runnable {
    private final Socket mSock;
    private final OutputStreamWriter outs;
    private LinkedBlockingQueue<String> mQueue;
    
    public Client() throws IOException {
    	this.mSock = new Socket("localhost", 8888);
    	this.outs = new OutputStreamWriter(mSock.getOutputStream());
    	this.mQueue = new LinkedBlockingQueue<String>();
    	System.out.println("Constructor Loaded");
    }
       
    @Override
    public void run() {
        System.out.println("Enter the message to be sent ! ");            
        new Thread(new Runnable() {
            @Override
            public void run(){
            	System.out.println("Second Thread Entered");
            	try {
                    BufferedReader br =
                    new BufferedReader(
                            new InputStreamReader(mSock.getInputStream()));
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.equals("PING")) {
                            writeOnSocket("PONG");
                        }   
                        else {
                            System.out.println(line);
                        }
                    }
                } 
                catch(IOException e) {
                    e.printStackTrace(System.out);
                } 
            }

        }).start();
        
        Scanner s = new Scanner(System.in);
        String line;
        while(!((line = s.nextLine()).equals(":q"))){
        	System.out.println("Hit an enter");
            try {
                writeOnSocket(line + "\n");
            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).
                        log(Level.SEVERE, null, ex);
            }
        }
        s.close();
        try {    
            mSock.close();
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).
                    log(Level.SEVERE, null, ex);
        
        }
        
    }
    
    public synchronized void writeOnSocket(String message) throws IOException{
        outs.write(message);
        outs.flush();
        System.out.println("Message Sent");
    }
    
    
    public static void main(String[] args) throws IOException {
        new Thread(new Client()).start();
        //new Thread(new messageHandling(mSock)).start();

    }    
}

/*
    synchronized void writeOnSocket(String ) {
        Scanner s = new Scanner(System.in);
        try {
            OutputStreamWriter outs
                    = new OutputStreamWriter(
                            mSock.getOutputStream());
            String line;
            while (!((line = s.nextLine()).equals(":q"))) {
                outs.write(line + "\n");
                outs.flush();

            }
            outs.write(":q");
            outs.flush();
            mSock.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            s.close();
        }
    }
   */
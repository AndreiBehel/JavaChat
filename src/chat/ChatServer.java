/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package chat;

import java.lang.StringBuilder;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import javax.swing.JTextArea;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 *
 * @author Andrew
 */
public class ChatServer extends Thread {
    private static int PORT = 9001;
    private static int uniqId;
    private SimpleDateFormat sdf;
    private boolean isServRunning = true;
     
    private static HashSet<String> names = new HashSet<String>();   
    private static ArrayList<Handler> al;
    private static ArrayList<String> client_messages;

    private JTextArea ServerOutput;
    
    public ChatServer(JTextArea out) {
        ServerOutput = out;
        al = new ArrayList<Handler>();
        client_messages = new ArrayList<String>();
        sdf = new SimpleDateFormat("HH:mm:ss");
    }
    
    public static void setPort(String p) {
        PORT = Integer.parseInt(p);
    }
    
    public int getNumOfClients() {
        return names.size();
    }
    
    private void display(String msg) {
        String mess = sdf.format(new Date()) + " " + msg;
        ServerOutput.append(mess + "\n");
    }
    
    public void servStop() {
        isServRunning = false;
         try {
             new Socket("localhost" , PORT).close();
         } catch(IOException e) {};
    }
    public void run() {
        try {
            ServerSocket listener = new ServerSocket(PORT);
            display("The chat server is running.\nON PORT: " + PORT + "\n");
            while (isServRunning) {
                if(isServRunning) {
                    Handler h = new Handler(listener.accept());
                    al.add(h);
                    h.start();
                }
                else
                    break;
            }
            //We close all conections and stop all threat
            try {
                listener.close();
                Iterator <Handler> iterator = al.iterator();
                while (iterator.hasNext()) {
                    iterator.next().close();
                }
            } catch(IOException e) {
                display("Exception closing the server and clients: " + e);
            }      
        } catch(Exception e) {
            String msg = " Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }
    private synchronized void broadcast(String message) {//Rewrite function, all threat should work with it.
        String time = sdf.format(new Date());
        String messageLf = time + " " + message + "\n";
        client_messages.add(messageLf);
        
        for(int i = al.size(); --i >= 0;) {
            if(!al.get(i).writeMsg(new ChatMessage(ChatMessage.MESSAGE, messageLf))) {
                al.remove(i);
                display("Disconnected Client " + al.get(i).name + " removed from list.");
            }
        }
    }
    
    private synchronized void remove(int id) {
        Iterator <Handler> iter = al.iterator();
        while(iter.hasNext()) {
            if(iter.next().id == id) {
                iter.remove();
                return;
            }
        }
    }
    
    private synchronized boolean isNameUsed(String name) {
        Iterator <Handler> iter = al.iterator();
        while(iter.hasNext()) {
            if(iter.next().name.equals(name)) {
                display(name + " login is used.");
                return true;
            }
        }
        display(name + " just connected.");
        return false;
    }
    
    private class Handler extends Thread {
        private String name;
        private int id;
        private ChatMessage cm;
        private String date;
        
        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private boolean pass = false;
        
        public Handler(Socket socket) {
            id = ++uniqId;
            this.socket = socket;
            // Create character streams for the socket.
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in  = new ObjectInputStream(socket.getInputStream());
                name = ((ChatMessage) in.readObject()).getMessage();
                
                if(isNameUsed(name)){
                    writeMsg(new ChatMessage(ChatMessage.AuthenticationNotPass, ""));
                }
                else {
                    writeMsg(new ChatMessage(ChatMessage.AuthenticationPass, ""));
                }
            } catch(IOException e) {
                display("Exception creating new Input/output Streams: " + e);
                return;
            }  catch (ClassNotFoundException e) {}
            date = new Date().toString() + "\n";
        }
        
        public int getID() {
            return id;
        }
        
        private void close(){
            try {     
                if(in != null)
                    in.close();
                if(out != null)
                    out.close();
                if(socket != null) 
                   socket.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        
        public void run() {
            boolean IsRunning = true;
            while (IsRunning && isServRunning) {
               try {
                   cm = (ChatMessage) in.readObject();
               } catch(IOException e) {
                   display(name + " Exception reading Streams: " + e);
                   break;
               } catch(ClassNotFoundException e2) {
                   break;
               }
               String message = cm.getMessage();
               
               switch(cm.getType()) {
                   case ChatMessage.MESSAGE:
                       broadcast(name + ": " + message);
                       break;
                   case ChatMessage.LOGOUT:
                       display(name + " disconnected with a LOGOUT message.");
                       IsRunning = false;
                       break;
                   case ChatMessage.WHOISIN:
                       StringBuilder list = new StringBuilder();
                       for(int i = 0; i < al.size(); ++i) {
                            list.append((i+1) + ") " + al.get(i).name + " since " + al.get(i).date + '\n');                           
                       }
                       writeMsg(new ChatMessage(ChatMessage.WHOISIN, list.toString()));
                       break;
                   case ChatMessage.MESSAGEHISTORY:
                       StringBuilder hist = new StringBuilder();
                       Iterator <String> iter = client_messages.iterator();
                       while(iter.hasNext()) {
                           hist.append(iter.next() + '\n');
                       }
                       writeMsg(new ChatMessage(ChatMessage.MESSAGEHISTORY, hist.toString()));
                       break;
               }   
            }
            remove(id);
            close();        
        }
        private boolean writeMsg(ChatMessage cm) {
               if(!socket.isConnected()) {
                   close();
                   return false;
               }
               try {
                   out.writeObject(cm);
               } catch(IOException e) {
                   display("Error sending message to " + name);
                   display(e.toString());
               }
              return true;
        }    
    }
}
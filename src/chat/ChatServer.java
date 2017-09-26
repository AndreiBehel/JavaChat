/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
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
    private boolean IsRunning = true;
     
    private static HashSet<String> names = new HashSet<String>();
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();   
    private static ArrayList<Handler> al;

    private JTextArea ServerOutput;
    
    public ChatServer(JTextArea out) {
        ServerOutput = out;
        al = new ArrayList<Handler>();
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
    public void Serv_Stop() {
        IsRunning = false;
         try {
             new Socket("localhost" , PORT).close();
         } catch(IOException e) {};
        names.clear();
        writers.clear();
    }
    public void run() {
        try {
            ServerSocket listener = new ServerSocket(PORT);
            display("The chat server is running.\nport: " + PORT + "\n");
            while (IsRunning) {
                if(IsRunning) {
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
                    iterator.next().in.close();
                    iterator.next().out.close();
                    iterator.next().socket.close();
                }
            } catch(IOException e) {
                display("Exception closing the server and clients: " + e);
            }      
        } catch(Exception e) {
            String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
            display(msg);
        }
    }
    private synchronized void broadcast(String message) {//Rewrite function, all threat should work with it.
        String time = sdf.format(new Date());
        String messageLf = time + " " + message + "\n";
        ServerOutput.append(messageLf);
        
        for(int i = al.size(); --i >= 0;) {
            if(!al.get(i).writeMsg(messageLf)) {
                al.remove(i);
                 display("Disconnected Client " + al.get(i).name + " removed from list.");
            }
        }
    }
    
    synchronized void remove(int id) {
        Iterator <Handler> iter = al.iterator();
        while(iter.hasNext()) {
            if(iter.next().id == id) {
                iter.remove();
                return;
            }
        }
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
                name = (String) in.readObject();
                Iterator <Handler> iter = al.iterator();
                while(iter.hasNext()) {
                    if(iter.next().name.equals(name)) {
                        display(name + " login is used.");
                        out.writeObject(new ChatMessage(ChatMessage.AuthenticationNotPass, ""));//rewrite
                    }
                }
                out.writeObject(new ChatMessage(ChatMessage.AuthenticationPass, ""));
                display(name + " just connected.");
                //
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
            while (IsRunning) {
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
                       writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
                       for(int i = 0; i < al.size(); ++i) {
                            writeMsg((i+1) + ") " + al.get(i).name + " since " + al.get(i).date);//здесь нужно посылать в braodcast
                       }
                       break;
               }   
            }
            remove(id);
            close();        
        }
        private boolean writeMsg(String msg) {// переделать передачу списка юзеров
               if(!socket.isConnected()) {
                   close();
                   return false;
               }
               try {
                   out.writeObject(new ChatMessage(ChatMessage.MESSAGE, msg));
                   //out.writeObject(msg);
               } catch(IOException e) {
                   display("Error sending message to " + name);
                   display(e.toString());
               }
              return true;
        }    
    }
}
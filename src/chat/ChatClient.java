/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package chat;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.InetAddress;
import java.net.ConnectException;
import java.nio.channels.SocketChannel;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.awt.Color;
import javax.swing.text.StyleConstants;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledDocument;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;

/**
 *
 * @author Andrew
 */
public class ChatClient extends Thread {
    private String name = "ouoi";
    private InetAddress ia;
    private String IP;
    private int Port = 9001;
    private String Password;
    
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    
    private JTextArea input;
    private JTextPane output;
    
    private boolean isConnected = false;
    
    public void setClientInfo(String na, String Pass, String IpAddr, String port) {
        name = na;
        //ia = InetAddress.getByName(IpAddr);
        Password = Pass;
        IP = IpAddr;
        Port = Integer.parseInt(port);
    }
    public ChatClient(JTextArea inp, JTextPane outp) {
        input = inp;
        output = outp;
    }

    private synchronized void sendMessage(ChatMessage msg) {
        try {
            out.writeObject(msg);
        } catch(IOException e) {
            JOptionPane.showMessageDialog(null,
                                    "Exception writing to server:\n" + e,
                                    "Connection error!",
                                    JOptionPane.ERROR_MESSAGE);
        }
    }
    void sendMess(String message) {
        new ListenFromServer(message).start();
    }
     private void disconnect() {
         try {
             isConnected = false;
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
     public void dis() {
          sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
     }
    public void run() {
        try (Socket socket = new Socket(InetAddress.getByName(IP), Port)) 
        {
            isConnected = true;
            in  = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            
            //new ListenFromServer().start();
            try {
                out.writeObject(name);
                ChatMessage cm = (ChatMessage) in.readObject();
                if(cm.getType() == ChatMessage.AuthenticationNotPass) {
                    JOptionPane.showMessageDialog(null,
                                    "Connection is Refused",
                                    "Your Nickname is used.\n",
                                    JOptionPane.ERROR_MESSAGE);
                    disconnect();
                }
            } catch(IOException e) {
                JOptionPane.showMessageDialog(null,
                                    "Exception doing login :\n" + e,
                                    "Connection error!",
                                    JOptionPane.ERROR_MESSAGE);
                disconnect();
                //return false;
            } catch(ClassNotFoundException e2) {
                  
             }
           // Настройка стиля текста
            StyledDocument doc = output.getStyledDocument();
            SimpleAttributeSet keyWord = new SimpleAttributeSet();
            SimpleAttributeSet keyWord2 = new SimpleAttributeSet();
            StyleConstants.setForeground(keyWord, Color.red);
            StyleConstants.setBold(keyWord, true);
            while(isConnected) {
                try {
                    ChatMessage msg = (ChatMessage)in.readObject();
                    try {
                        //doc.insertString(0, msg.substring(msg.indexOf(":")+2)+ "\n", keyWord2 );
                       // doc.insertString(0, msg.substring(8, msg.indexOf(":")+1)+ "\n", keyWord );
                        doc.insertString(0, msg.getMessage()+ "\n", keyWord2 );
                    } catch (Exception e) {}
                } catch(IOException e) {
                    break;
                } catch(ClassNotFoundException e2) {
                } 
            }
            disconnect();
        } catch(IOException e ) {
            e.printStackTrace();
           JOptionPane.showMessageDialog(null,
                                    "Check connection propeties",
                                    "Connection error!",
                                    JOptionPane.ERROR_MESSAGE);
        }
    }
    class ListenFromServer extends Thread {
        private String message;
        public ListenFromServer(String msg) {
            message = msg;
        }
        public void run() {
           if(message.equalsIgnoreCase("LOGOUT")) {
                sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
            }
            else if(message.equalsIgnoreCase("WHOISIN")) {
              sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));    
            }
            else {
                sendMessage(new ChatMessage(ChatMessage.MESSAGE, message));
            }
        }
    }
}
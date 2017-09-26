/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package chat;

import java.io.IOException;
import java.net.Socket;
import java.net.InetAddress;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.StringTokenizer;

import java.awt.Color;
import java.awt.EventQueue;
import java.util.Iterator;
import javax.swing.text.StyleConstants;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledDocument;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JLabel;
import javax.swing.JToggleButton;

import java.util.concurrent.LinkedBlockingDeque;
/**
 *
 * @author Andrew
 */
public class ChatClient extends Thread {
    private String name = "ouoi";
    private String IP;
    private int Port = 9001;
    private String Password;
    
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    
    private ArrayList<String> users;
    private ArrayList<String> messages;
    
    private LinkedBlockingDeque<ChatMessage> lbd;
    
    private JTextArea input;
    private JTextPane output;
    private JLabel state;
    private JToggleButton st;
    private StyledDocument doc;
    private SimpleAttributeSet keyWord;
    private SimpleAttributeSet keyWord2;
     
    private boolean isConnected = false;
    
    public void setClientInfo(String na, String Pass, String IpAddr, String port) {
        name = na;
        Password = Pass;
        IP = IpAddr;
        Port = Integer.parseInt(port);
    }
    public ChatClient(JTextArea inp, JTextPane outp, JLabel stat, JToggleButton sta) {
        input = inp;
        output = outp;
        state = stat;
        st = sta;
        users = new ArrayList<String>();
        messages = new ArrayList<String>();
        lbd = new LinkedBlockingDeque<ChatMessage>();
        
        // customization of text style
        doc = output.getStyledDocument();
        keyWord = new SimpleAttributeSet();
        keyWord2 = new SimpleAttributeSet();
        StyleConstants.setForeground(keyWord, Color.red);
        StyleConstants.setBold(keyWord, true);
    }

    void sendMess(String message) {
        try{
            lbd.put(new ChatMessage(ChatMessage.MESSAGE, message));
         } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private boolean authentication() {
        try{
            lbd.put(new ChatMessage(ChatMessage.LOGIN, name));
            ChatMessage cm = (ChatMessage) in.readObject();
            if(cm.getType() == ChatMessage.AuthenticationPass) {
                return true;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch(ClassNotFoundException e2) {
            
        } catch(IOException e) {
             JOptionPane.showMessageDialog(null,
                                    "Exception doing authentication :\n" + e,
                                    "Connection error!",
                                    JOptionPane.ERROR_MESSAGE);
             disconnect();
        }
        return false;
    }
    private void getData() {
        try{
            lbd.put(new ChatMessage(ChatMessage.WHOISIN, ""));
            ChatMessage cm = (ChatMessage) in.readObject();
            StringTokenizer str = new StringTokenizer(cm.getMessage(), "\n");
            while(str.hasMoreTokens()) {
                users.add(str.nextToken().toString());
            }
            
            lbd.put(new ChatMessage(ChatMessage.MESSAGEHISTORY, ""));
            cm = (ChatMessage) in.readObject();
            StringTokenizer str2 = new StringTokenizer(cm.getMessage(), "\n");
            while(str2.hasMoreTokens()) {
                messages.add(str2.nextToken().toString());
            }
            Iterator <String> iter = messages.iterator();
            while(iter.hasNext()) {
                try {
                   doc.insertString(0, iter.next()+"\n", keyWord2 );
                } catch (Exception e) {}
            }
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch(ClassNotFoundException e2) {
            
        } catch(IOException e) {
             JOptionPane.showMessageDialog(null,
                                    "Exception doing login :\n" + e,
                                    "Connection error!",
                                    JOptionPane.ERROR_MESSAGE);
             disconnect();
        }
      
    }
     private void disconnect() {
         EventQueue.invokeLater(new Runnable() {
             public void run()
             {
                 state.setText("Offline");
                 st.setSelected(false);
             }
         });
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
         try{
            lbd.put(new ChatMessage(ChatMessage.LOGOUT, ""));
            isConnected = false;
            lbd.put(new ChatMessage(ChatMessage.LOGOUT, ""));
         } catch (InterruptedException e) {
            e.printStackTrace();
         }        
     }
     public void listOfUsers() {
         Iterator <String> iter = users.iterator();
         try {
            while(iter.hasNext()) {
                doc.insertString(0, iter.next()+"\n", keyWord );
            }
            doc.insertString(0, "\n", keyWord);
         } catch (Exception e) {}
     }
     
    public void run() {
        try (Socket socket = new Socket(InetAddress.getByName(IP), Port)) 
        {
            isConnected = true;
            EventQueue.invokeLater(new Runnable() {
                public void run()
                {
                    state.setText("Online");
                    st.setSelected(true);
                }
            });
            
            in  = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            new senderToServer().start();
            
            while(true) {
                if(!authentication()) {
                    dis();
                    JOptionPane.showMessageDialog(null,
                                    "Your Nickname is used.\n",
                                    "Connection is Refused",
                                    JOptionPane.ERROR_MESSAGE);
                    break;
                }
                getData();

                while(isConnected) {
                    try {
                        ChatMessage msg = (ChatMessage)in.readObject();
                        switch(msg.getType()) {
                            case ChatMessage.MESSAGE:
                                try {
                                    doc.insertString(0, msg.getMessage(), keyWord2 );
                                } catch (Exception e) {}
                                break;
                            case ChatMessage.WHOISIN:
                                try {
                                    doc.insertString(0, "List of the users"+ "\n", keyWord2 );
                                    doc.insertString(0, msg.getMessage()+ "\n", keyWord2 );
                                } catch (Exception e) {}
                                break;
                        }
                    } catch(IOException e) {
                        break;
                    } catch(ClassNotFoundException e2) {} 
                }
                break;
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
    class senderToServer extends Thread {
        public void run() {
            while(isConnected) {
                try {
                     out.writeObject(lbd.take());
                } catch(IOException e) {
                JOptionPane.showMessageDialog(null,
                                    "Exception writing to server:\n" + e,
                                    "Connection error!",
                                    JOptionPane.ERROR_MESSAGE);
                    break;
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            } 
        }
    }
}
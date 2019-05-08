/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clienttest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author 70136
 */
public class ClientTest {

    /**
     * @param args the command line arguments
     */
        public static final int ACTION_CODE_LOGIN=0,ACTION_CODE_VIEW=1,ACTION_CODE_DOWNLOAD=2,ACTION_CODE_UPLOAD=3,ACTION_CODE_INFO=4;
        public static int entry=5000;
        public static String serverIP="140.116.132.230";
    public static void main(String[] args) {
        // TODO code application logic here
        
        //login
        for(int i=0;i<1;i++){
        try {
            Socket socket = new Socket("140.116.132.230", entry);
            socket.setSoTimeout(5000);
              PrintWriter printer = new PrintWriter(socket.getOutputStream()); 
              BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
              printer.println("user:test,pas:1234,action:"+ACTION_CODE_LOGIN+",");
              printer.flush(); 
              System.out.println(reader.readLine());
               
              printer.close();
              reader.close();
              socket.close();
        } catch (IOException ex) {
            Logger.getLogger(ClientTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        }
        //view
        for(int i=0;i<1;i++){
        try {
              Socket sockets = new Socket(serverIP, entry);
              sockets.setSoTimeout(5000);
              BufferedReader reader = new BufferedReader(new InputStreamReader(sockets.getInputStream())); 
              PrintWriter printer = new PrintWriter(sockets.getOutputStream()); 
              printer.println("user:test,pas:1234,action:"+ACTION_CODE_VIEW+",");
              printer.flush(); 
              
              Thread view=new Thread(() -> {
                try {
                    Socket viewer=sockets;
                    ObjectInputStream objectInputStream=new ObjectInputStream(viewer.getInputStream());
                    File folder=(File) objectInputStream.readObject();
                    objectInputStream.close();
                    viewer.close();
                    printer.close();
                    reader.close();
                    for(String f:folder.list()){
                        System.out.println(f);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(ClientTest.class.getName()).log(Level.SEVERE, null, ex);
                }
                  
              });
              view.start();
              
        } catch (IOException ex) {
            Logger.getLogger(ClientTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        }
        //info
        try {
            Socket socket = new Socket("140.116.132.230", entry);
            socket.setSoTimeout(5000);
              PrintWriter printer = new PrintWriter(socket.getOutputStream()); 
              BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
              printer.println("user:test,pas:1234,action:"+ACTION_CODE_INFO+",");
              printer.flush(); 
              System.out.println(reader.readLine());
               
              printer.close();
              reader.close();
              socket.close();
        } catch (IOException ex) {
            Logger.getLogger(ClientTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        //download
        try {
              Socket socket = new Socket(serverIP, entry);
              socket.setSoTimeout(5000);
              BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
              PrintWriter printer = new PrintWriter(socket.getOutputStream()); 
              printer.println("user:test,pas:1234,action:"+ACTION_CODE_DOWNLOAD+",file:"+"D:/music/May'n - Kimi Shinitamou Koto Nakare [320kbps]/May'n - Kimi Shinitamou Koto Nakare [320kbps]/01. キミシニタモウコトナカレ.mp3"+",");
              printer.flush();
               System.out.println("download start");
              Thread dow=new Thread(() -> {
                try {
                     BufferedInputStream buf=new BufferedInputStream(socket.getInputStream());
                     FileOutputStream fout=new FileOutputStream("01. キミシニタモウコトナカレ.mp3");
                     int l=0;
                     byte[] bytes=new byte[4096];
                     int now=0;
                     while((l=buf.read(bytes))!=-1){
                     fout.write(bytes,0, l);
                     now+=4;
                     System.out.println("now:"+now/1024.0+"mb");
                     }
                     buf.close();
                     fout.flush();
                     fout.close();
                      
                     reader.close();
                     printer.close();
                     socket.close();
                     System.out.println("download finished");
                } catch (Exception ex) {
                    Logger.getLogger(ClientTest.class.getName()).log(Level.SEVERE, null, ex);
                    try {
                        reader.close();
                        printer.close();
                        socket.close();
                    } catch (IOException ex1) {
                        Logger.getLogger(ClientTest.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                    
                }
                  
              });
              dow.start();
              
              
        } catch (IOException ex) {
            Logger.getLogger(ClientTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        //upload
        try {
              Socket socket = new Socket(serverIP, entry);
              socket.setSoTimeout(5000);
              BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
              PrintWriter printer = new PrintWriter(socket.getOutputStream()); 
              printer.println("user:test,pas:1234,action:"+ACTION_CODE_UPLOAD+",file:"+"uptest.txt"+",");
              printer.flush();
               System.out.println("upload start");
              Thread upl=new Thread(() -> {
                try {
                     BufferedOutputStream buf=new BufferedOutputStream(socket.getOutputStream());;
                     FileInputStream fin=new FileInputStream("uptest.txt");
                     int l=0;
                     byte[] bytes=new byte[4096];
                     int now=0;
                     while((l=fin.read(bytes))!=-1){
                     buf.write(bytes,0, l);
                     now+=4;
                     System.out.println("now:"+now/1024.0+"mb");
                     }
                     buf.flush();
                     buf.close();
                     fin.close();
                      
                     reader.close();
                     printer.close();
                     socket.close();
                     System.out.println("upload finished");
                } catch (Exception ex) {
                    Logger.getLogger(ClientTest.class.getName()).log(Level.SEVERE, null, ex);
                    try {
                        reader.close();
                        printer.close();
                        socket.close();
                    } catch (IOException ex1) {
                        Logger.getLogger(ClientTest.class.getName()).log(Level.SEVERE, null, ex1);
                    }
                    
                }
                  
              });
              upl.start();
              
              
        } catch (IOException ex) {
            Logger.getLogger(ClientTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        //
    }
    
    public static void listAllFile(File folder){
          
          File[] files = folder.listFiles();
          if(files==null)return;
          for (File s : files)
          {
            if(s.isDirectory()){
                listAllFile(s);
            }
            else if(s.getName().endsWith(".mp3")||s.getName().endsWith(".wav")){
                System.out.println(s.getName()+" size:"+s.length()/1024/1024.0+"mb");
            }
            
          }
        
    }
}

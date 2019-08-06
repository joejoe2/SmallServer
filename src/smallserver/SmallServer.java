/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package smallserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author 70136
 */
public class SmallServer {

    /**
     * @param args the command line arguments
     */
    public static final int ACTION_CODE_LOGIN=0,ACTION_CODE_VIEW=1,ACTION_CODE_DOWNLOAD=2,ACTION_CODE_UPLOAD=3,ACTION_CODE_INFO=4;
    public static int entry=5000;
    public static String serverIP="219.91.49.21";
    public static boolean occupied[]=new boolean[11];
    public static String account="test";
    public static String pass="1234";
    public static String root="D:/music";
    public static File serverSpace=new File(root);
    
    public static void main(String[] args){
        // TODO code application logic here
         
        ServerSocket server=null;
        try {
            server = new ServerSocket(entry);
            //server.setSoTimeout(20000);
        } catch (IOException ex) {
            Logger.getLogger(SmallServer.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        occupied[0]=true;
        System.out.println("Server started at "+serverIP+" port:"+entry+" "+LocalDateTime.now()+" !\n");
        
        while(true){
            Socket client;
            PrintWriter printer;
            BufferedReader reader;
            String  msg;
            try {
            client = server.accept();
            printer = new PrintWriter(client.getOutputStream()); 
            reader = new BufferedReader(new InputStreamReader(client.getInputStream())); 
            msg = reader.readLine();
            } catch (Exception ex) {
                Logger.getLogger(SmallServer.class.getName()).log(Level.SEVERE, null, ex);
                continue;
            }
            
            String  user=msg.substring(msg.indexOf("user:")+5,msg.indexOf(",",msg.indexOf("user:")));
            String  passward=msg.substring(msg.indexOf("pas:")+4,msg.indexOf(",",msg.indexOf("pas:")));
            String  action=msg.substring(msg.indexOf("action:")+7,msg.indexOf(",",msg.indexOf("action:")));
            String  des=msg.substring(msg.indexOf("des:")+4,msg.indexOf(",",msg.indexOf("des:")));
            
            if(msg==null||user==null||passward==null||action==null){
                try {
                    client.close();
                } catch (Exception ex) {
                    Logger.getLogger(SmallServer.class.getName()).log(Level.SEVERE, null, ex);
                }
                continue;
            }
            
            if(Integer.parseInt(action)==ACTION_CODE_LOGIN){
                if(user.equalsIgnoreCase(account)&&passward.equals(pass)){
                     printer.println("login success");
                     System.out.println(user+" login success from "+client.getInetAddress().toString()+" port:"+client.getPort()+" at "+LocalDateTime.now());
                }else{
                     printer.println("login fail");
                     System.out.println(user+" login fail from "+client.getInetAddress().toString()+" port:"+client.getPort()+" at "+LocalDateTime.now());
                }
                printer.flush(); 
                printer.close();
                try{
                reader.close(); 
                client.close();
                }catch(Exception ex){
                     Logger.getLogger(SmallServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }else if(Integer.parseInt(action)==ACTION_CODE_VIEW){
                    if(des==null||des.equals("root")){
                        des=root;
                    }
                    File Space=new File(des);
                    
                    System.out.println(user+" request view from "+client.getInetAddress().toString()+" port:"+client.getPort()+" at "+LocalDateTime.now()+"  port:"+entry);
                     Thread thread=new Thread(() -> {
                        try {
                            Socket target=client;
                            ObjectOutputStream objectOutputStream=new ObjectOutputStream(target.getOutputStream());
                            File[] fol=Space.listFiles();
                            String[] result=new String[fol.length];
                            for (int i=0;i<result.length;++i) {
                                if(fol[i].isDirectory()){
                                result[i]="-d "+fol[i].getAbsolutePath();
                                }else{
                                result[i]=fol[i].getAbsolutePath();
                                }
                            }
                            objectOutputStream.writeObject(result);
                            objectOutputStream.flush();
                            objectOutputStream.close();
                            target.close();
                            printer.close(); 
                            reader.close();
                            System.out.println(user+"'s request view from "+client.getInetAddress().toString()+" port:"+client.getPort()+" has been finished"+" at "+LocalDateTime.now()+" port:"+entry);
                        } catch (Exception ex) {
                                Logger.getLogger(SmallServer.class.getName()).log(Level.SEVERE, null, ex);
                            try {
                                client.close();
                                printer.close(); 
                                reader.close();
                            } catch (IOException ex1) {
                                Logger.getLogger(SmallServer.class.getName()).log(Level.SEVERE, null, ex1);
                            }
                        }
                      });
                      thread.start();
            }else if(Integer.parseInt(action)==ACTION_CODE_INFO){
                System.out.println(user+" request info from "+client.getInetAddress().toString()+" port:"+client.getPort()+" at "+LocalDateTime.now()+" port:"+entry);
                printer.println("servertime:"+LocalDateTime.now()+" available processors:"+Runtime.getRuntime().availableProcessors()+" total memory:"+Runtime.getRuntime().totalMemory());
                
                printer.flush(); 
                printer.close(); 
                try { 
                    reader.close();
                    client.close();
                } catch (Exception ex) {
                    Logger.getLogger(SmallServer.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            }else if(Integer.parseInt(action)==ACTION_CODE_DOWNLOAD){
                String  f=msg.substring(msg.indexOf("file:")+5,msg.indexOf(",",msg.indexOf("file:")));
                System.out.println(user+" request download "+f+"from "+client.getInetAddress().toString()+" port:"+client.getPort()+" at "+LocalDateTime.now()+" port:"+entry);
                File file=new File(f);
                Thread dThread=new Thread(() -> {
                    try {
                        FileInputStream fileInputStream=new FileInputStream(file);
                        System.out.println("size="+file.length()/1024/1024.0);
                        printer.println(file.length()/1024/1024.0);
                        printer.flush();
                        BufferedInputStream bufferedInputStream=new BufferedInputStream(fileInputStream);
                        BufferedOutputStream fout=new BufferedOutputStream(client.getOutputStream());
                        int l = 0;
                        byte[] bytes = new byte[4096];
                        while ((l = bufferedInputStream.read(bytes)) != -1) {    
                        fout.write(bytes, 0, l);
                        }
                        
                        fout.flush();
                        fout.close();
                        if(!client.isClosed())
                        client.shutdownOutput();
                        
                        fileInputStream.close();
                        bufferedInputStream.close();
                        //printer.flush();
                        printer.close(); 
                        reader.close();
                        client.close();
                        System.out.println(user+" request download finished from "+client.getInetAddress().toString()+" port:"+client.getPort()+" at "+LocalDateTime.now()+" port:"+entry);
                    } catch (Exception ex) {
                        try {
                            Logger.getLogger(SmallServer.class.getName()).log(Level.SEVERE, null, ex);
                            printer.close(); 
                            reader.close();
                            client.close();
                        } catch (IOException ex1) {
                            Logger.getLogger(SmallServer.class.getName()).log(Level.SEVERE, null, ex1);
                        }
                    }
                });
                  dThread.start();
            }else if(Integer.parseInt(action)==ACTION_CODE_UPLOAD){
                String  f=msg.substring(msg.indexOf("file:")+5,msg.indexOf(",",msg.indexOf("file:")));
                System.out.println(user+" request upload "+f+"from "+client.getInetAddress().toString()+" port:"+client.getPort()+" at "+LocalDateTime.now()+" port:"+entry);
                Thread upThread=new Thread(() -> {
                    BufferedInputStream buf=null;
                    try {
                        buf = new BufferedInputStream(client.getInputStream());
                        FileOutputStream fout=new FileOutputStream(f);
                        int l=0;
                        byte[] bytes=new byte[4096];
                        while((l=buf.read(bytes))!=-1){
                            fout.write(bytes,0, l);
                        }
                        printer.close(); 
                        reader.close();
                        fout.close();
                        buf.close();
                        client.close();
                        System.out.println(user+" request upload "+f+" finisded from "+client.getInetAddress().toString()+" port:"+client.getPort()+" at "+LocalDateTime.now()+" port:"+entry);
                    } catch (Exception ex) {
                        Logger.getLogger(SmallServer.class.getName()).log(Level.SEVERE, null, ex);
                        try {
                            printer.close(); 
                            reader.close();
                            client.close();
                        } catch (IOException ex1) {
                            Logger.getLogger(SmallServer.class.getName()).log(Level.SEVERE, null, ex1);
                        }
                    }
                });
                upThread.start();
            }
            
            
            
        }
        
    }
    
}

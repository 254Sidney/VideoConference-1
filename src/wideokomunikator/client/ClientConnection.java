
package wideokomunikator.client;

import com.sun.net.ssl.internal.ssl.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import wideokomunikator.net.Frame;

public class ClientConnection extends Thread{
    private boolean active = true;
    private volatile Frame frame = null;
    private InputStream server_in;
    private OutputStream server_out;
    private SSLSocket server;
    private Hashtable responses,requests;
    private int frame_id = 0;
    public ClientConnection(String hostname,int port) throws IOException {
        Security.addProvider(new Provider());
        SSLSocketFactory  socketfactory = (SSLSocketFactory )SSLSocketFactory .getDefault();
        server = (SSLSocket)socketfactory.createSocket(hostname,port);
        final String[] enabledCipherSuites = { "SSL_DH_anon_WITH_RC4_128_MD5" }; //Default Anonymous Cipher Suite   
        server.setEnabledCipherSuites(enabledCipherSuites);  
        server_in = server.getInputStream();
        server_out = server.getOutputStream();
        
        responses = new Hashtable <Integer, Frame >();
        requests = new Hashtable <Integer, Frame >();

    }
    public void sendFrame(Frame frame_to_send){
        frame_id = getNextFrameId();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(frame!=null){
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
                frame = frame_to_send;
                
            }
        }).start();
    }
    
    public int getNextFrameId(){
        return frame_id+2;
    }
    
    @Override
    public void run() {     
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(active){
                    if(frame==null){
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {}
                    }else{
                        try{
                            requests.put(frame.getMESSAGE_ID(), frame);
                            frame.send(server_out);
                            server_out.flush();
                            frame = null;
                        }catch(IOException ex){
                            ex.printStackTrace();
                        }
                    }                        
                }
                close();
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(active){
                    try {
                        Frame f = new Frame(server_in);
                        if(responses.get(f.getMESSAGE_ID())!=null){
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    while(responses.get(f.getMESSAGE_ID())!=null){
                                        try {
                                            Thread.sleep(10);
                                        } catch (InterruptedException ex) {}
                                    };
                                    responses.put(f.getMESSAGE_ID(), f);                                
                                }
                            }).start();
                        }else{
                            responses.put(f.getMESSAGE_ID(), f);   
                            
                        }
                            try {
                                Thread.sleep(10);
                            }catch (InterruptedException ex) {}
                        
                    } catch (ClassNotFoundException | IOException ex) {
                        ex.printStackTrace();
                    }   
                    
                }
                close();
            }
        }).start();
    }
    
    public Frame getFrame(int id){
        Frame frame = (Frame) responses.get(id);
        if(frame!=null){
            responses.remove(id);
            requests.remove(id);
        }
        return frame;
    }
    
    private void close(){
        if(!server.isClosed()){
            try {
                server_in.close();
                server_out.close();
                server.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
    }
    
    
}

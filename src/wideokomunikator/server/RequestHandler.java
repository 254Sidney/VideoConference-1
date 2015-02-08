package wideokomunikator.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.net.ssl.SSLSocket;
import wideokomunikator.net.Frame;
import wideokomunikator.server.net.Response;

public class RequestHandler extends Thread{
    private SSLSocket client;
    private OutputStream client_out;
    private InputStream client_in;
    private boolean client_active=true;
    private Frame frame = null;
    public RequestHandler(SSLSocket client) throws IOException {
        this.client = client;     
        //open client streams
        client_in = client.getInputStream();
        client_out = client.getOutputStream();              
        //new Thread(this).run();
    }

    @Override
    public void run() { 
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(client_active){
                    try {
                        Frame f = new Frame(client_in);
                        Response r = new Response(f,client_out);
                        //System.out.println(f);
                    } catch (ClassNotFoundException | IOException ex) {
                        client_active=false;
                        ex.printStackTrace();
                    }                    
                }
                close();
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(client_active){
                    if(frame==null){
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {}
                    }else{
                        try {
                            frame.send(client_out);
                            client_out.flush();
                            frame = null;
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            client_active=false;
                        }
                    }
                }
                close();
            }
        }).start();
    }
    private void close(){
        if(!client.isClosed()){
            try{
                client_in.close();
                client_out.close();
                client.close();   
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }
    
    public void sendFrame(Frame frame){
        this.frame = frame;
    }
}

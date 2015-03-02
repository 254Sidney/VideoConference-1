
package wideokomunikator.server;

import wideokomunikator.server.db.Database;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;


public class Server extends Thread{
    private SSLServerSocket serverSocket;
    public static ArrayList<RequestHandler> clients; 
    public static ArrayList<wideokomunikator.server.conference.Server> conferences;
    private boolean serverActivity = true;
    public Server(InetAddress host,int port) {
        SSLServerSocketFactory serverSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();  
        try {
            serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(port,20,host);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        init();
    }    

    public Server(int port){
        SSLServerSocketFactory serverSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();  
        try {
            serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(port);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        init();
    }
    
    private void init(){
        clients = new ArrayList<RequestHandler>();
        conferences = new ArrayList<wideokomunikator.server.conference.Server>();
        final String[] enabledCipherSuites = { "SSL_DH_anon_WITH_RC4_128_MD5" };
        serverSocket.setEnabledCipherSuites(enabledCipherSuites);  
        Database.getInstance();        
        
    }
    

    @Override
    public void run() {
        while(serverActivity){
            SSLSocket socket;
            try {
                socket = (SSLSocket)serverSocket.accept();
                System.out.println(socket.getRemoteSocketAddress());
                RequestHandler req = new RequestHandler(socket);
                req.start();
                clients.add(req);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        try {
            serverSocket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
    }
    
}

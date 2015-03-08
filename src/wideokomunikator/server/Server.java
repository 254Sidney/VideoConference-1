
package wideokomunikator.server;

import wideokomunikator.server.db.Database;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;


public class Server extends Thread{
    private SSLServerSocket serverSocket;
    public static ArrayList<RequestHandler> clientsThreads; 
    public static ArrayList<wideokomunikator.server.conference.Server> conferences;
    public static java.util.concurrent.ConcurrentHashMap<Integer,Boolean> clientsActivity;
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
        clientsThreads = new ArrayList<RequestHandler>();
        conferences = new ArrayList<wideokomunikator.server.conference.Server>();
        clientsActivity = new ConcurrentHashMap<>();
        final String[] enabledCipherSuites = { "SSL_DH_anon_WITH_RC4_128_MD5" };
        serverSocket.setEnabledCipherSuites(enabledCipherSuites);  
        Database.getInstance();                
    }
    

    @Override
    public void run() {
        System.out.println("Server starts");
        while(serverActivity){
            SSLSocket socket;
            try {
                socket = (SSLSocket)serverSocket.accept();
                System.out.println(socket.getRemoteSocketAddress());
                RequestHandler req = new RequestHandler(socket);
                req.start();
                clientsThreads.add(req);
            } catch (IOException ex) {
                serverActivity = false;
                System.out.println("Server closed: "+ex.getMessage());
            }
        }
        try {
            serverSocket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
}

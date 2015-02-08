
package wideokomunikator.server;

import wideokomunikator.server.db.Database;
import java.io.IOException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;


public class Server extends Thread{
    private SSLServerSocket serverSocket;
    private boolean serverActivity = true;
    public Server(int port) throws IOException {
        SSLServerSocketFactory serverSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();  
        serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(port);
        final String[] enabledCipherSuites = { "SSL_DH_anon_WITH_RC4_128_MD5" };
        serverSocket.setEnabledCipherSuites(enabledCipherSuites);  
        Database.getInstance();
        //System.out.println(Database.getInstance().Sign("wilczynskip", "haslo".toCharArray()));
        
    }    

    @Override
    public void run() {
        while(serverActivity){
            SSLSocket socket;
            try {
                socket = (SSLSocket)serverSocket.accept();
                System.out.println(socket.getRemoteSocketAddress());
                new RequestHandler(socket).start();            
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

package wideokomunikator;

import com.sun.corba.se.spi.orbutil.threadpool.ThreadPool;
import java.awt.event.ComponentAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import wideokomunikator.client.Client;
import wideokomunikator.server.Server;

public class main {
    private static AudioVideo audio;
    private static wideokomunikator.server.conference.Server server;
    static TargetDataLine microphone = null;
    public static void main(String[] args) {
        try {
            server = new wideokomunikator.server.conference.Server();
            new Thread(server).start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        
        audio = new AudioVideo();
        audio.record2();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        server.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                audio.stop();
                server.stopServer();
            }
            
        });
        //audio.stop();
            /*
            try {
            new Server(5000).start();
            } catch (IOException ex) {
            ex.printStackTrace();
            }
            new wideokomunikator.client.Client().setVisible(true);
            //ConferenceView panel = new ConferenceView(720,480);
            //panel.setVisible(true);
            
            Client c = new Client();
            */           
    }
    
    

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        audio.stop();
    }
    

}

package wideokomunikator;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.ToolFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import javax.sound.sampled.TargetDataLine;
import wideokomunikator.server.Server;
import wideokomunikator.server.conference.Server2;

public class main {

    static TargetDataLine microphone = null;

    public static void main(String[] args) throws UnknownHostException {
        //new Server(InetAddress.getByName("178.62.207.64"), 5000).start();
        new Server(InetAddress.getByName("localhost"), 5000).start();
        //new Server2();
        new wideokomunikator.client.Client("localhost", 5000).setVisible(true);
        //new wideokomunikator.client.Client("178.62.207.64", 5000);
        //new wideokomunikator.client.Client("192.168.2.102", 5000);
        //new wideokomunikator.client.Client("192.168.2.103", 5000);
        //new wideokomunikator.client.Client();
        //ConferenceView panel = new ConferenceView(720,480);
        //panel.setVisible(true);

        //Client c = new Client();
    }

    //192.168.2.110:51870
}

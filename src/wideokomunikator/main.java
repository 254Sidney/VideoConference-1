package wideokomunikator;

import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.sound.sampled.TargetDataLine;
import wideokomunikator.client.NewJFrame;
import wideokomunikator.server.Server;

public class main {

    static TargetDataLine microphone = null;

    public static void main(String[] args) throws UnknownHostException {
        //new Server(InetAddress.getByName("178.62.207.64"), 5000).start();
        //new wideokomunikator.client.Client("178.62.207.64", 5000);
        new Server(InetAddress.getByName("192.168.2.102"), 5000).start();
        new wideokomunikator.client.Client("192.168.2.102", 5000).setVisible(true);
        //new NewJFrame();
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wideokomunikator.server.conference;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IError;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.io.XugglerIO;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import wideokomunikator.net.UDPInputStream;

/**
 *
 * @author Piotr
 */
public class Server2 {

    private Thread comunicationThread;
    private DatagramSocket serverSocket;
    private final int DATAGRAM_SIZE = 64000;
    private boolean active = true;
    IMediaReader mediareader;
    UDPInputStream input;
    private ServerSocket server;

    IContainer container = null;

    public Server2() {
        try {
            input = new UDPInputStream("localhost", 6000);
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        try {
            server = new ServerSocket(6001);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        comunicationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                /*
                 IContainerFormat format = IContainerFormat.make();
                 format.setInputFormat("mp4");
                 //format.setOutputFormat("mp4", null, "video/mp4");
                 mediareader.setQueryMetaData(true);
                    
                 mediareader.getContainer().isHeaderWritten();
                 System.out.println("otwieram \n" + mediareader.getContainer().toString());
                 //mediareader.open();
                 //int open = container.open(input, format);
                 //System.out.println("open ");
                 IPacket packet = IPacket.make();
                 int c = 0;
                 while (active) {
                 System.out.println("dsa");
                 IError error = mediareader.readPacket();
                 System.out.println(error.toString());
                 }
                 System.out.println("zamykam");
                 container.close();
                 */

                try {
                    Socket socket = server.accept();
                    mediareader = ToolFactory.makeReader(XugglerIO.map(socket.getInputStream()));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                mediareader.getContainer().getStream(0);
                System.out.println(mediareader.getContainer().getNumStreams());
                mediareader.addListener(ToolFactory.makeViewer());
                while (mediareader.readPacket() == null) {
                    System.out.println("lkh");
                };

            }
        });
        comunicationThread.start();
    }

}

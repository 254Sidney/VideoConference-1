package wideokomunikator.client;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import javax.swing.JPanel;
import wideokomunikator.audiovideo.AudioVideo;

public class ConferenceView extends JPanel {

    private boolean full_screan = false;
    private Dimension window_size;
    private Point location;
    private BufferedImage image = null, bufor = null;
    private boolean record = false;
    private InetSocketAddress serverAdress = null;
    private DatagramSocket serverSocket = null;
    private Thread comunicationThread = null;
    private final int DATAGRAM_SIZE = 64000;
    private boolean Active = true;
    private AudioVideo audiovideo = null;

    public ConferenceView(LayoutManager layout) {
        super(layout);
    }

    public ConferenceView() {
        initComponents();
        
    }

    @Override
    public void paint(Graphics g) {
        bufor = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g2 = bufor.getGraphics();
        Graphics2D g2d = (Graphics2D) g2;
        if (image != null) {
            g2d.drawImage(image, 0, 0, getWidth(), getHeight(), this);
        }
        g.drawImage(bufor, 0, 0, this);
    }

    private void initComponents() {
        location = new Point(100, 100);
        setLocation(location);
        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    setFullScrean(full_screan = !full_screan);
                }
            }

        });
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    setFullScrean(full_screan = !full_screan);
                }
            }

        });
        setVisible(true);
    }

    public void setImag(BufferedImage image) {
        this.image = image;
        repaint();
    }

    public void setFullScrean(boolean value) {
        full_screan = value;
        //dispose();
        //setUndecorated(value);
        if (value) {
            setSize(Toolkit.getDefaultToolkit().getScreenSize());
            setLocation(0, 0);
        } else {
            setSize(window_size);
            setLocation(location);
        }
        setVisible(true);
    }

    public void initConnection(String host, int port, int UserID) {
        serverAdress = new InetSocketAddress(host, port);
        try {
            serverSocket = new DatagramSocket(0);
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        try {
            String serverID = getMessage("init\n"+UserID);
            String[] prop = serverID.split("\n");            
            audiovideo = new AudioVideo(UserID,host,Integer.parseInt(prop[0]),Integer.parseInt(prop[1])){

                @Override
                public void setImage(BufferedImage image2, int ID) {
                    image = image2;
                    repaint();
                }
                
            };
            audiovideo.record();
        } catch (SocketTimeoutException ex) {
            ex.printStackTrace();
        }

    }

    byte[] buffer = new byte[DATAGRAM_SIZE];

    private synchronized String getMessage(String message) throws SocketTimeoutException {
        DatagramPacket packetToSend = new DatagramPacket(message.getBytes(), message.getBytes().length, serverAdress);
        DatagramPacket packetReceived = new DatagramPacket(buffer, buffer.length);
        try {
            serverSocket.setSoTimeout(1000);
            serverSocket.send(packetToSend);
            serverSocket.receive(packetReceived);
        } catch (SocketTimeoutException ex) {
            throw ex;
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return new String(packetReceived.getData(), 0, packetReceived.getLength());

    }


    
    public void close(){
        if(audiovideo!=null)
            audiovideo.stop();
        
    }




}

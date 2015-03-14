package wideokomunikator.client;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import javax.swing.*;
import wideokomunikator.audiovideo.AudioVideo;

public class ConferenceView extends JDesktopPane {

    private boolean full_screan = false;
    private Dimension window_size;
    private Point location;
    private boolean record = false;
    private InetSocketAddress serverAdress = null;
    private DatagramSocket serverSocket = null;
    private Thread comunicationThread = null;
    private final int DATAGRAM_SIZE = 64000;
    private AudioVideo audiovideo = null;
    private HashMap<Integer, View> userViewMap;
    private int UserID;
    private JPopupMenu popup;

    public ConferenceView() {
        initComponents();
    }

    private void initComponents() {
        userViewMap = new HashMap<Integer, View>();
        location = new Point(100, 100);
        setLocation(location);
        popup = new JPopupMenu("Menu");
        JMenuItem item;
        popup.add(item = new JMenuItem("Zamknij konferencje"));
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });        
        setComponentPopupMenu(popup);
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

    public void setFullScrean(boolean value) {
        full_screan = value;
        if (value) {
            setSize(Toolkit.getDefaultToolkit().getScreenSize());
            setLocation(0, 0);
        } else {
            setSize(window_size);
            setLocation(location);
        }
        setVisible(true);
    }

    public boolean isFullScrean() {
        return full_screan;
    }
    

    public void initConnection(String host, int port, int UserID) {
        this.UserID = UserID;
        serverAdress = new InetSocketAddress(host, port);
        try {
            serverSocket = new DatagramSocket(0);
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        try {
            String serverID = getMessage("init\n" + UserID);
            String[] prop = serverID.split("\n");
            audiovideo = new AudioVideo(UserID, host, Integer.parseInt(prop[0]), Integer.parseInt(prop[1])) {

                @Override
                public void setImage(BufferedImage image, int ID) {
                    View view = userViewMap.getOrDefault(ID, null);
                    if (view == null) {
                        view = new View("", true, false, true, true);
                        userViewMap.put(ID, view);
                        add(view);
                    }
                    view.setImage(image);
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
        DatagramPacket packetReceived = new DatagramPacket(buffer, buffer.length);
        try {
            DatagramPacket packetToSend = new DatagramPacket(message.getBytes(), message.getBytes().length, serverAdress);
            serverSocket.setSoTimeout(5000);
            serverSocket.send(packetToSend);
            serverSocket.receive(packetReceived);
        } catch (SocketTimeoutException ex) {
            throw ex;
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return new String(packetReceived.getData(), 0, packetReceived.getLength());

    }

    public void close() {
        if (serverAdress != null) {
            try {
                String response = getMessage("close\n" + UserID);
            } catch (SocketTimeoutException ex) {
            }
        }
        if (audiovideo != null) {
            audiovideo.stop();
        }
        removeAll();
        userViewMap.clear();
        repaint();

    }

    class View extends JInternalFrame {

        BufferedImage image;
        JLabel label = new JLabel();

        public View(String title, boolean resizable, boolean closable, boolean maximizable, boolean iconifiable) {
            super(title, resizable, closable, maximizable, iconifiable);
            setComponentPopupMenu(popup);
            setContentPane(label);
            setSize(320, 240);
            setVisible(true);
        }

        public void setImage(BufferedImage image) {
            label.setIcon(new ImageIcon(image.getScaledInstance(label.getWidth(), label.getHeight(), BufferedImage.SCALE_SMOOTH)));
        }
    }

}

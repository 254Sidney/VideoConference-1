package wideokomunikator.client;

import com.sun.net.ssl.internal.ssl.Provider;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.Security;
import java.util.Hashtable;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import wideokomunikator.net.Frame;
import static wideokomunikator.net.MESSAGE_TYPE.REQUEST;

public class ClientConnection extends Thread {

    private boolean active = true;
    private volatile Frame frame = null;
    private InputStream server_in;
    private OutputStream server_out;
    private SSLSocket server;
    private Hashtable<Integer, Frame> responses, requests;
    private int frame_id = 0;
    private PropertyChangeSupport chg = new PropertyChangeSupport(this);
    private Frame serverRequest = null;

    public ClientConnection(String hostname, int port) throws IOException {        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {}
        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setStringPainted(true);
        progress.setString("Łączenie z serwerem");
        JDialog dialog = new JDialog();
        dialog.setUndecorated(true);
        dialog.setContentPane(progress);
        dialog.pack();
        dialog.setVisible(true);
        dialog.setLocationRelativeTo(null);
        Security.addProvider(new Provider());
        SSLSocketFactory socketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        server = (SSLSocket) socketfactory.createSocket();
        server.connect(new InetSocketAddress(hostname, port));

        final String[] enabledCipherSuites = {"SSL_DH_anon_WITH_RC4_128_MD5"}; //Default Anonymous Cipher Suite   
        server.setEnabledCipherSuites(enabledCipherSuites);
        server_in = server.getInputStream();
        server_out = server.getOutputStream();
        dialog.setVisible(false);
        dialog = null;
        responses = new Hashtable<Integer, Frame>();
        requests = new Hashtable<Integer, Frame>();
    }

    public void sendFrame(final Frame frame_to_send) {
        frame_id = getNextFrameId();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (frame != null) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        JOptionPane.showMessageDialog(null, "Przerwano połaczenie z serwerem " + ex.getMessage(), "Bład", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                        active = false;
                    }
                }
                frame = frame_to_send;

            }
        }).start();
    }

    public int getNextFrameId() {
        return frame_id + 2;
    }

    @Override
    public void run() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (active) {
                    if (frame == null) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {
                        }
                    } else {
                        try {
                            requests.put(frame.getMESSAGE_ID(), frame);
                            frame.send(server_out);
                            frame = null;
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(null, "Przerwano połaczenie z serwerem " + ex.getMessage(), "Bład", JOptionPane.ERROR_MESSAGE);
                            ex.printStackTrace();
                            active = false;
                        }
                    }
                }
                close();
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (active) {
                    try {
                        final Frame f = new Frame(server_in);
                        if (f.getMESSAGE_TYPE() == REQUEST) {
                            Frame old = serverRequest;
                            serverRequest = f;
                            chg.firePropertyChange("frame", old, serverRequest);
                        } else {
                            if (responses.get(f.getMESSAGE_ID()) != null) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        while (responses.get(f.getMESSAGE_ID()) != null) {
                                            try {
                                                Thread.sleep(10);
                                            } catch (InterruptedException ex) {
                                            }
                                        };
                                        responses.put(f.getMESSAGE_ID(), f);
                                    }
                                }).start();
                            } else {
                                responses.put(f.getMESSAGE_ID(), f);

                            }
                        }
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {
                        }

                    } catch (javax.net.ssl.SSLException ex) {
                        JOptionPane.showMessageDialog(null, "Przerwano połaczenie z serwerem " + ex.getMessage(), "Bład", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                        active = false;
                    } catch (ClassNotFoundException | IOException ex) {
                        JOptionPane.showMessageDialog(null, "Przerwano połaczenie z serwerem " + ex.getMessage(), "Bład", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                        active = false;
                    }

                }
                close();
            }
        }).start();
    }

    public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
        chg.addPropertyChangeListener(l);
    }

    public Frame getFrame(int id) {
        Frame frame = (Frame) responses.get(id);
        if (frame != null) {
            responses.remove(id);
            requests.remove(id);
        }
        return frame;
    }

    private void close() {
        if (!server.isClosed()) {
            try {
                server_in.close();
                server_out.close();
                server.close();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Połaczenia zamkniete uruchom program ponownie " + ex.getMessage(), "Bład", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }

    }

}

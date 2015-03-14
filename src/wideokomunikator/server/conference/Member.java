package wideokomunikator.server.conference;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Member extends Thread {

    private int UserID;
    private DatagramSocket audioSocket;
    private DatagramSocket videoSocket;
    private ArrayList<Member> users;
    private boolean Active = true;
    private final int DATAGRAM_SIZE = 64000;
    private InetAddress host = null;
    private int audioPort = -1, videoPort = -1;
    private Thread audioThread, videoThread;
    private long lastActivity = 0;

    public Member(int UserID, ArrayList<Member> users) throws SocketException {
        this.UserID = UserID;
        this.users = users;
        audioSocket = new DatagramSocket(0);
        videoSocket = new DatagramSocket(0);
    }

    public int getAudioPort() {
        return audioSocket.getLocalPort();
    }

    public int getVideoPort() {
        return videoSocket.getLocalPort();
    }

    public int getUserID() {
        return UserID;
    }    

    @Override
    public void run() {
        StartAudioReceiveThread();
        StartVideoReceiveThread();
    }

    public boolean isActive() {
        long timeSecond = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastActivity);
        if (timeSecond < 60 || lastActivity == 0) {
            return true;
        } else {
            return false;
        }
    }

    private void StartAudioReceiveThread() {
        audioThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[DATAGRAM_SIZE];
                while (Active) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    try {
                        audioSocket.receive(packet);
                        lastActivity = System.currentTimeMillis();
                        if ((packet.getPort() != audioPort) || (!packet.getAddress().equals(host))) {
                            audioPort = packet.getPort();
                            host = packet.getAddress();
                        }

                    } catch (IOException ex) {
                        Active = false;
                    }
                    new Hendler(Arrays.copyOf(packet.getData(), packet.getLength()), true).start();
                }
            }
        });
        audioThread.start();
    }

    private void StartVideoReceiveThread() {
        videoThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[DATAGRAM_SIZE];
                while (Active) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    try {
                        videoSocket.receive(packet);
                        lastActivity = System.currentTimeMillis();
                        if ((packet.getPort() != videoPort) || (!packet.getAddress().equals(host))) {
                            videoPort = packet.getPort();
                            host = packet.getAddress();
                        }

                    } catch (IOException ex) {
                        Active = false;
                    }
                    new Hendler(Arrays.copyOf(packet.getData(), packet.getLength()), false).start();
                }
            }
        });
        videoThread.start();
    }

    public void close() {
        Active = false;
        audioSocket.close();
        videoSocket.close();
        while (audioThread.isAlive() || videoThread.isAlive()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(Member.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public DatagramSocket getAudioSocket() {
        return audioSocket;
    }

    public DatagramSocket getVideoSocket() {
        return videoSocket;
    }

    public InetAddress getHost() {
        return host;
    }

    class Hendler extends Thread {

        private final byte[] data;
        private final boolean isAudio;

        public Hendler(byte[] packet, boolean isAudio) {
            this.data = packet;
            this.isAudio = isAudio;
        }

        @Override
        public void run() {
            for (Member m : users) {
                //if (m.UserID != UserID) {
                    int port = isAudio ? m.audioPort : m.videoPort;
                    InetAddress host = m.host;
                    if (host != null && port != -1) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                DatagramSocket datagramsocket = (isAudio ? m.audioSocket : m.videoSocket);
                                DatagramPacket packet = new DatagramPacket(data, data.length, host, port);
                                try {
                                    datagramsocket.send(packet);
                                } catch (IOException ex) {
                                }
                            }
                        }).start();

                    }
                //}
            }
        }
    }
}

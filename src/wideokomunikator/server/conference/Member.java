package wideokomunikator.server.conference;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Member extends Thread {

    private int UserID;
    private DatagramSocket audioSocket;
    private DatagramSocket videoSocket;
    private ArrayList<Member> users;
    private boolean Active = true;
    private final int DATAGRAM_SIZE = 64000;
    private InetAddress host = null;
    private int audioPort = -1, videoPort = -1;

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

    @Override
    public void run() {
        StartAudioReceiveThread();
        StartVideoReceiveThread();
    }

    private void StartAudioReceiveThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[DATAGRAM_SIZE];
                while (Active) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    try {
                        audioSocket.receive(packet);
                        if ((packet.getPort() != audioPort) || (!packet.getAddress().equals(host))) {
                            audioPort = packet.getPort();
                            host = packet.getAddress();
                        }
                        new Hendler(Arrays.copyOf(packet.getData(), packet.getLength()), true).start();

                    } catch (IOException ex) {

                    }
                }
            }
        }).start();
    }

    private void StartVideoReceiveThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[DATAGRAM_SIZE];
                while (Active) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    try {
                        videoSocket.receive(packet);
                        if ((packet.getPort() != videoPort) || (!packet.getAddress().equals(host))) {
                            videoPort = packet.getPort();
                            host = packet.getAddress();
                        }
                        new Hendler(Arrays.copyOf(packet.getData(), packet.getLength()), false).start();

                    } catch (IOException ex) {
                    }
                }
            }
        }).start();
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

            int packetID = ByteBuffer.wrap(Arrays.copyOfRange(data, 4, 8)).order(ByteOrder.BIG_ENDIAN).getInt();
            for (Member m : users) {
                if (m.UserID == UserID) {
                    int port = isAudio ? m.audioPort : m.videoPort;
                    InetAddress host = m.host;
                    if (host != null && port != -1) {
                        DatagramSocket datagramsocket = (isAudio ? m.audioSocket : m.videoSocket);                        
                        DatagramPacket packet = new DatagramPacket(data, data.length, host, port);
                        try {
                            if (packetID == 0) {
                            }
                            datagramsocket.send(packet);
                        } catch (IOException ex) {
                        }

                    }
                }
            }
        }
    }
}

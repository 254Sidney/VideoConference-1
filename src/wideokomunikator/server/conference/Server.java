package wideokomunikator.server.conference;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server extends Thread {

    private DatagramSocket datagram_socket;
    private DatagramSocket datagram_socket2;
    private DatagramSocket serverSocket;
    private final int DATAGRAM_SIZE = 64000;
    private boolean active = true;
    private ArrayList<Member> members = new ArrayList<Member>();

    private Thread comunicationThread;

    public Server(int[] UsersIDs) throws SocketException {
        serverSocket = new DatagramSocket(0);
    }

    public int getAdress() {
        return serverSocket.getLocalPort();
    }

    private void addUser(Member m) {
        members.add(m);
    }

    public void comunicationThread() {
        comunicationThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[DATAGRAM_SIZE];
                while (active) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    try {
                        serverSocket.receive(packet);
                        new ComunicationHendler(packet).start();
                    } catch (IOException ex) {
                        active = false;
                    }
                }
            }
        });
        comunicationThread.start();
    }

    public void activityThread() {
        new Thread(new Runnable() {
            final int sleepTime = 1000;// 1 minute

            @Override
            public void run() {
                while (active) {
                    boolean isActive = false;
                    for (Member m : members) {
                        if (m.isActive()) {
                            isActive = true;
                        }
                    }
                    if (isActive == false && members.size() > 0) {
                        active = false;
                    }
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ex) {
                    }
                }
                close();
            }
        }).start();
    }

    public void close() {
        serverSocket.close();
        while (comunicationThread.isAlive()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
        for (Member m : members) {
            m.close();
        }
        wideokomunikator.server.Server.conferences.remove(this);
    }

    @Override
    public void run() {
        comunicationThread();
        activityThread();
    }

    public void stopServer() {
        active = false;
        datagram_socket.close();
        datagram_socket2.close();
    }

    class ComunicationHendler extends Thread {

        private DatagramPacket packet = null;

        public ComunicationHendler(DatagramPacket packet) {
            this.packet = packet;
        }

        @Override
        public void run() {
            String[] message = getMessage(packet).split("\n");
            String header = message[0];
            if (header.matches("init")) {
                try {
                    Member m = new Member(Integer.parseInt(message[1]), members);
                    m.start();
                    members.add(m);
                    String mesasge = m.getAudioPort() + "\n" + m.getVideoPort();
                    sendMessage(mesasge);

                } catch (SocketException ex) {
                    ex.printStackTrace();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        private String getMessage(DatagramPacket packet) {
            return new String(packet.getData(), 0, packet.getLength());
        }

        private void sendMessage(String message) throws IOException {
            if (message == null) {
                throw new NullPointerException(message);
            }
            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.getBytes().length, this.packet.getSocketAddress());
            serverSocket.send(packet);
        }

    }
}

package wideokomunikator.server.conference;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Server extends Thread {

    private DatagramSocket serverSocket;
    private final int DATAGRAM_SIZE = 64000;
    private boolean active = true;
    private ArrayList<ConferenceUser> members = new ArrayList<ConferenceUser>();

    private Thread comunicationThread;

    public Server(int[] UsersIDs) throws SocketException {
        serverSocket = new DatagramSocket(0);
    }

    public int getAdress() {
        return serverSocket.getLocalPort();
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

    public boolean started = false;
    public void activityThread() {
        new Thread(new Runnable() {
            final int sleepTime = 10000;// 10 sec

            @Override
            public void run() {
                while (active) {
                    boolean isActive = false;
                    for (ConferenceUser m : members) {
                        if (m.isActive()) {
                            started = true;
                            isActive = true;
                        }
                    }
                    if (isActive == false && members.size() > 0) {
                        active = false;
                    }
                    if(started == true && members.size() == 0){
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
        for (ConferenceUser m : members) {
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
                    ConferenceUser m = new ConferenceUser(Integer.parseInt(message[1]), members);
                    m.start();
                    members.add(m);
                    String mesasge = m.getAudioPort() + "\n" + m.getVideoPort();
                    sendMessage(mesasge);

                } catch (SocketException ex) {
                    ex.printStackTrace();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else if (header.matches("close")) {
                for (int i = 0; i < members.size(); i++) {
                    if (members.get(i).getUserID() == Integer.parseInt(message[1])) {
                        synchronized (members) {
                            members.get(i).close();
                            members.remove(i);
                        }
                        break;
                    }
                }
                try {
                    sendMessage("ok");
                } catch (IOException ex) {
                    
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

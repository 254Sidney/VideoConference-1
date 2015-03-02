package wideokomunikator.server.conference;

import com.xuggle.ferry.IBuffer;
import com.xuggle.xuggler.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import javax.sound.sampled.AudioFormat;
import wideokomunikator.client.ConferenceView;

public class Server extends Thread {

    private DatagramSocket datagram_socket;
    private DatagramSocket datagram_socket2;
    private DatagramSocket serverSocket;
    private final int DATAGRAM_SIZE = 64000;
    private boolean active = true;
    private ArrayList<Member> members = new ArrayList<Member>();
    private ConferenceView view;
    private AudioFormat audioFormat = null;
    private IStreamCoder audioCoder;
    private IStreamCoder videoCoder;

    private Thread comunicationThread;

    public Server(int[] UsersIDs) throws SocketException {
        serverSocket = new DatagramSocket(0);

        //serverSocket.bind(new InetSocketAddress(findFreePort()));
        //System.out.println(serverSocket.getInetAddress() + " " + serverSocket.getLocalAddress() + " " + serverSocket.toString());

        /*
         videoCoder = IStreamCoder.make(IStreamCoder.Direction.DECODING, ICodec.ID.CODEC_ID_H264);        
         videoCoder.open(null, null);

         audioCoder = IStreamCoder.make(IStreamCoder.Direction.DECODING, ICodec.ID.CODEC_ID_AAC);
         audioCoder.setChannels(2);
         audioCoder.open(null, null);

         audioFormat = new AudioFormat(48000, 16, 2, true, true);
         try {
         speakers = getSpeaker();
         speakers.open(audioFormat);
         speakers.start();
         } catch (LineUnavailableException ex) {
         ex.printStackTrace();
         }

         view = new ConferenceView(620, 480);
         //setContentPane(view);
         //setLocationRelativeTo(null);
         //setSize(620, 480);
         //setVisible(true);
         //setDefaultCloseOperation(EXIT_ON_CLOSE);
         */
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
                    }
                }
            }
        });
        comunicationThread.start();
    }

    @Override
    public void run() {
        comunicationThread();
        /*
         new Thread(new Runnable() {
         @Override
         public void run() {
         byte[] buffer = new byte[DATAGRAM_SIZE];
         while (active) {
         DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
         try {
         datagram_socket.receive(packet);
         } catch (IOException ex) {
         break;
         }
         new PacketHendler(packet, false).start();
         }

         }
         }).start();
         new Thread(new Runnable() {

         @Override
         public void run() {
         byte[] buffer = new byte[DATAGRAM_SIZE];
         while (active) {
         DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
         long now = System.nanoTime();
         try {
         datagram_socket2.receive(packet);
         } catch (IOException ex) {
         break;
         }
         IPacket ipacket = IPacket.make();
         if (packet.getLength() > 0) {
         IBuffer buff = IBuffer.make(ipacket, packet.getLength());
         buff.put(packet.getData(), 0, 0, packet.getLength());
         ipacket.setData(buff);
         decodeAudio(ipacket);
         ipacket.reset();
         }
         }
         }
         }).start();
         */
    }


    public void stopServer() {
        active = false;
        datagram_socket.close();
        datagram_socket2.close();
    }

    class PacketHendler extends Thread {

        private DatagramPacket packet = null;
        private boolean isAudio;

        public PacketHendler(DatagramPacket packet, boolean isAudio) {
            this.packet = packet;
            this.isAudio = isAudio;
        }

        @Override
        public void run() {
            IPacket ipacket = IPacket.make();
            if (packet.getLength() > 0) {
                IBuffer buff = IBuffer.make(ipacket, packet.getLength());
                buff.put(packet.getData(), 0, 0, packet.getLength());
                ipacket.setData(buff);
                if (isAudio) {
                    //decodeAudio(ipacket);
                } else {
                    //decodeVideo(ipacket);
                }
                ipacket.reset();
            }
        }

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
                    String mesasge = m.getAudioPort()+"\n"+m.getVideoPort();
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
        private void sendMessage(String message) throws IOException{
            if (message == null) throw new NullPointerException(message);
            DatagramPacket packet = new DatagramPacket(message.getBytes(),message.getBytes().length,this.packet.getSocketAddress());
            serverSocket.send(packet);
        }

    }
}

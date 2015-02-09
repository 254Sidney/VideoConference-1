package wideokomunikator.server.conference;

import com.xuggle.ferry.IBuffer;
import com.xuggle.xuggler.*;
import com.xuggle.xuggler.Utils;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFrame;
import wideokomunikator.client.ConferenceView;

public class Server extends JFrame implements Runnable {

    private DatagramSocket datagram_socket;
    private DatagramSocket datagram_socket2;
    private final int DATAGRAM_SIZE = 64000;
    private boolean active = true;
    private List<Member> members = new ArrayList<Member>();
    private ConferenceView view;
    private SourceDataLine speakers = null;
    private AudioFormat audioFormat = null;
    private IStreamCoder audioCoder;
    private IStreamCoder videoCoder;

    public Server() throws SocketException, IOException {
        datagram_socket = new DatagramSocket(30000);
        datagram_socket2 = new DatagramSocket(30001);

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
        setContentPane(view);
        setLocationRelativeTo(null);
        setSize(620, 480);
        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private SourceDataLine getSpeaker() throws LineUnavailableException {
        DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
        speakers = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        speakers.open(audioFormat);
        return speakers;
    }

    private void addUser(Member m) {
        members.add(m);
    }

    @Override
    public void run() {

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

    }

    private void decodeAudio(IPacket packet) {
        IAudioSamples audio = IAudioSamples.make(packet.getData(), audioCoder.getChannels(), audioCoder.getSampleFormat());
        int offset = 0;
        while (offset < packet.getSize()) {

            int bytesDecoded = audioCoder.decodeAudio(audio, packet, offset);
            offset += bytesDecoded;

            try {
                if (audio.isComplete()) {
                    byte[] bytes = audio.getData().getByteArray(0, audio.getSize());
                    speakers.write(bytes, 0, bytes.length);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }
    IConverter videoConverter = null;

    private void decodeVideo(IPacket packet) {
        IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(), videoCoder.getWidth(), videoCoder.getHeight());
        int offset = 0;
        while (offset < packet.getSize()) {
            try {
                int bytesDecoded = videoCoder.decodeVideo(picture, packet, offset);
                offset += bytesDecoded;
                if (picture.isComplete()) {
                    videoConverter = ConverterFactory.createConverter(ConverterFactory.XUGGLER_BGR_24, picture);
                    videoConverter.toImage(picture);
                    BufferedImage image = videoConverter.toImage(picture);
                    view.setImag(image);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void WritePicture(BufferedImage image, String name) throws IOException {
        File file = new File(name);
        file.createNewFile();
        ImageIO.write(image, "png", file);
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
                    decodeAudio(ipacket);
                } else {
                    decodeVideo(ipacket);
                }
                ipacket.reset();
            }
        }

    }
}

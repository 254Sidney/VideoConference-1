package wideokomunikator.server.conference;

import com.xuggle.ferry.IBuffer;
import com.xuggle.mediatool.*;
import com.xuggle.xuggler.*;
import com.xuggle.xuggler.Utils;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFrame;
import wideokomunikator.client.ConferenceView;

public class Server extends JFrame implements Runnable {

    private IMediaReader reader;
    private DatagramSocket datagram_socket;
    private final int DATAGRAM_SIZE = 65508;
    private boolean active = true;
    private List<Member> members = new ArrayList<Member>();
    private ByteArrayOutputStream output = new ByteArrayOutputStream();
    private ByteArrayInputStream input = null;
    private IMediaViewer viewer = ToolFactory.makeViewer();
    private ConferenceView view;
    private SourceDataLine speakers = null;
    private AudioFormat audioFormat = null;

    public Server() throws SocketException, IOException {
        datagram_socket = new DatagramSocket(new InetSocketAddress("localhost", 30000));

        videoCoder = IStreamCoder.make(IStreamCoder.Direction.DECODING, ICodec.ID.CODEC_ID_H264);
        //videoCoder.setTimeBase(IRational.make(30, 1));
        videoCoder.setWidth(620);
        videoCoder.setHeight(480);
        IRational frameRate = IRational.make(1000, 1);
        videoCoder.setPixelType(IPixelFormat.Type.YUV420P);
        videoCoder.setNumPicturesInGroupOfPictures(1);
        videoCoder.setBitRate(900000);
        videoCoder.setBitRateTolerance(90000);
        videoCoder.setFlag(IStreamCoder.Flags.FLAG_QSCALE, true);
        videoCoder.setFrameRate(frameRate);
        videoCoder.setTimeBase(IRational.make(1, 1000));
        videoCoder.setGlobalQuality(0);
        videoCoder.setAutomaticallyStampPacketsForStream(true);
        videoCoder.open();

        audioCoder = IStreamCoder.make(IStreamCoder.Direction.DECODING, ICodec.ID.CODEC_ID_AAC);
        audioCoder.setSampleRate((int) 48000);
        audioCoder.setBitRate(128000);
        audioCoder.setBitRateTolerance(90000);
        audioCoder.setChannels(2);
        audioCoder.setFrameRate(IRational.make(30, 1));
        audioCoder.setAutomaticallyStampPacketsForStream(true);
        audioCoder.setSampleFormat(IAudioSamples.Format.FMT_S16);
        audioCoder.open();
        
        audioFormat = new AudioFormat(48000, 16, 2, true, true);
        try {
            speakers = getSpeaker();
            speakers.open(audioFormat);
            speakers.start();
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        }
        try {
        File f = new File("D:/server.aac");
            f.createNewFile();
            fos = new FileOutputStream(f);
        } catch (IOException ex) {
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

    IStreamCoder audioCoder;
    IStreamCoder videoCoder;

    @Override
    public void run() {
        while (active) {
            byte[] buffer = new byte[DATAGRAM_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                datagram_socket.receive(packet);
                //System.out.println("Server - odebrałem:"+packet.getLength());
            } catch (IOException ex) {
                //ex.printStackTrace();            
            }
            //System.out.println(packet.getLength());
            IPacket ipacket = IPacket.make();
            //ipacket.
            if (packet.getLength() > 0) {
                IBuffer buff = IBuffer.make(ipacket, packet.getLength());
                buff.put(packet.getData(), 0, 0, packet.getLength());
                ipacket.setData(buff);
                //System.out.println();
                decodeAudio(ipacket);
            }
            //System.out.println(ipacket.getSize());
            //IPacket.make();
            //decodeVideo(ipacket);
            //System.out.println(ipacket.toString());
            //output.write(packet.getData(), 0, packet.getLength());
        }
        /*
         input = new ByteArrayInputStream(output.toByteArray());
         IContainer container = IContainer.make();
         IContainerFormat format = IContainerFormat.make();
         format.setOutputFormat("aac", null, "audio/aac");
         container.open(input, format);
         container.setFormat(format);
         audioCoder = IStreamCoder.make(IStreamCoder.Direction.DECODING, ICodec.ID.CODEC_ID_AAC);
         audioCoder.setSampleRate((int) 48000);
         audioCoder.setBitRate(128000);
         audioCoder.setBitRateTolerance(9000);
         audioCoder.setChannels(2);
         audioCoder.setFrameRate(IRational.make(30, 1));
         audioCoder.setGlobalQuality(0);
         audioCoder.open();
        
         videoCoder = IStreamCoder.make(IStreamCoder.Direction.DECODING, ICodec.ID.CODEC_ID_H264);
         videoCoder.setTimeBase(IRational.make(30, 1));
         videoCoder.setWidth(600);
         videoCoder.setHeight(400);
         /*
         container.open(input, format);
         container.addNewStream(audioCoder);
         System.out.println(container.isHeaderWritten());
         container.isHeaderWritten();
         System.out.println(container.toString());
         */
        //con.addNewStream(videoCoder);
        //System.out.println(con.getNumStreams());
        

        try {
            output.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        //System.out.println(container.getNumStreams());

    }
    FileOutputStream fos;
    private void decodeAudio(IPacket packet) {
        IAudioSamples audio = IAudioSamples.make(512, 2);
        int offset = 0;
        while (offset < packet.getSize()) {
            try {
                int bytesDecoded = audioCoder.decodeAudio(audio, packet, offset);
                offset += bytesDecoded;
                if (audio.isComplete()) {
                    //System.out.println("Super "+audio.getTimeStamp());
                    byte[] bytes = audio.getData().getByteArray(0, audio.getSize());
                    speakers.write(bytes, 0, bytes.length);
                    //fos.write(bytes);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    private void decodeVideo(IPacket packet) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(), videoCoder.getWidth(), videoCoder.getHeight());
                int offset = 0;
                while (offset < packet.getSize()) {
                    try {
                        int bytesDecoded = videoCoder.decodeVideo(picture, packet, offset);
                        offset += bytesDecoded;
                        if (picture.isComplete()) {
                            BufferedImage javaImage = Utils.videoPictureToImage(picture);
                            view.setImag(javaImage);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void WritePicture(BufferedImage image, String name) throws IOException {
        File file = new File(name);
        file.createNewFile();
        ImageIO.write(image, "png", file);
    }

    public void stopServer() {
        active = false;
        datagram_socket.close();
        File f = new File("D:server.mp4");
        try {
            f.createNewFile();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        try {
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(output.toByteArray());
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

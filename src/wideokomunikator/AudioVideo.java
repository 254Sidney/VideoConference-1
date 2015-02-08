package wideokomunikator;

import com.xuggle.mediatool.*;
import com.xuggle.xuggler.*;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import org.bytedeco.javacv.FrameGrabber;

public class AudioVideo {

    DataOutputStream os;
    FileOutputStream fos = null;
    long ct = System.currentTimeMillis();
    //private IContainer wr = IContainer.make();
    //Audio
    private SourceDataLine speakers = null;
    private TargetDataLine microphone = null;
    private float sampleRate = 48000.0f;
    private int sampleSize = 16;
    private int channels = 2;
    private boolean signed = true;
    private boolean bigEndian = true;
    private AudioFormat audioFormat = null;
    private Thread thread_audio, thread_video;
    //Video
    private FrameGrabber camera = null;

    private boolean record;

    public AudioVideo() {
        try {
            microphone = getMicrophone(getMics()[0]);
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        }
        setAudioFormat(sampleRate, sampleSize, channels, signed, bigEndian);

        try {
            camera = getCamera();
        } catch (FrameGrabber.Exception ex) {
            ex.printStackTrace();
        }

        try {
            datagram_socket = new DatagramSocket(new InetSocketAddress("localhost", 30001));
        } catch (SocketException ex) {
            ex.printStackTrace();
        }

    }

    IContainer container = IContainer.make();
    IStreamCoder audioCoder;
    IStreamCoder videoCoder;
    ObjectOutputStream oos;

    public void record2() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            oos = new ObjectOutputStream(baos);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        File f = new File("D:/file.aac");
        try {
            fos = new FileOutputStream(f);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        initCamera(camera);
        try {
            //speakers = getSpeaker();
            initMicrophone(microphone);
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        }
        IContainerFormat format = IContainerFormat.make();
        //format.setOutputFormat("aac", null, "audio/aac");
        format.setOutputFormat("mp4", null, "video/mp4");
        container.setFormat(format);
        audioCoder = IStreamCoder.make(IStreamCoder.Direction.ENCODING, ICodec.ID.CODEC_ID_AAC);
        audioCoder.setSampleRate((int) sampleRate);
        audioCoder.setBitRate(128000);
        audioCoder.setBitRateTolerance(90000);
        audioCoder.setChannels(channels);
        audioCoder.setFrameRate(IRational.make(30, 1));
        audioCoder.setAutomaticallyStampPacketsForStream(true);
        //audioCoder.setGlobalQuality(75);
        audioCoder.open();

        try {
            camera.start();
        } catch (FrameGrabber.Exception ex) {
            ex.printStackTrace();
        }
        IRational frameRate = IRational.make(1000, 1);
        videoCoder = IStreamCoder.make(IStreamCoder.Direction.ENCODING, ICodec.ID.CODEC_ID_H264);
        videoCoder.setWidth(camera.getImageWidth());
        videoCoder.setHeight(camera.getImageHeight());
        videoCoder.setPixelType(IPixelFormat.Type.YUV420P);
        videoCoder.setNumPicturesInGroupOfPictures(1);
        videoCoder.setBitRate(900000);
        videoCoder.setBitRateTolerance(90000);
        videoCoder.setFlag(IStreamCoder.Flags.FLAG_QSCALE, true);
        videoCoder.setFrameRate(frameRate);
        System.out.println(camera.getFrameRate());
        videoCoder.setTimeBase(IRational.make(1, (int) camera.getFrameRate()));
        videoCoder.setGlobalQuality(10);
        videoCoder.setAutomaticallyStampPacketsForStream(true);
        videoCoder.setFlag(IStreamCoder.Flags.FLAG_LOW_DELAY, true);
        videoCoder.setFlag(IStreamCoder.Flags.FLAG2_FAST, true);
        videoCoder.setProperty("me_method", "+zero");
        videoCoder.setProperty("x264-params", "fast-pskip=1");
        videoCoder.open();

        container.open(new output(), format);
        container.addNewStream(audioCoder);
        container.addNewStream(videoCoder);
        container.writeHeader();

        System.out.println(videoCoder.toString());
        thread_audio = new Thread(new Runnable() {
            @Override
            public void run() {
                int size = microphone.getBufferSize() / 5;
                byte[] data = new byte[size];
                short[] audioSamples = new short[size / (sampleSize / 8)];
                int numBytesRead = 0;
                int i = 0;
                long ct = System.currentTimeMillis();
                microphone.start();
                IPacket packet = IPacket.make(2048);
                IAudioSamples sample = IAudioSamples.make(audioSamples.length, channels);
                while (record) {
                    numBytesRead = microphone.read(data, 0, data.length);
                    long now = System.currentTimeMillis();
                    audioSamples = toShort(data);
                    sample.put(audioSamples, 0, 0, audioSamples.length);
                    sample.setComplete(true, audioSamples.length / channels, (int) sampleRate, channels, IAudioSamples.Format.FMT_S16, IAudioSamples.defaultPtsToSamples(now - ct, (int) sampleRate));
                    int offset = 0;
                    while (offset < audioSamples.length / channels) {
                        int bytesEncoded = audioCoder.encodeAudio(packet, sample, offset);
                        offset += bytesEncoded;
                        if (packet.getSize() > 0) {
                            sendPacket(packet);
                            packet.reset();
                        }
                    }
                }
                microphone.stop();
                audioCoder.close();
            }
        });
        thread_video = new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedImage image = null;
                IConverter converter = null;
                IVideoPicture pictureSample;
                IPacket packet = IPacket.make();
                while (record) {
                    try {
                        image = camera.grab().getBufferedImage();
                    } catch (FrameGrabber.Exception ex) {
                        ex.printStackTrace();
                    }
                    long now = System.currentTimeMillis();
                    long timeStamp = (now - ct) * 1000;
                    //image = convertToType(image, BufferedImage.TYPE_3BYTE_BGR);
                    converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);
                    pictureSample = converter.toPicture(image, timeStamp);
                    videoCoder.encodeVideo(packet, pictureSample, 0);
                    if (packet.isComplete()) {
                        sendPacket(packet);
                        packet.reset();
                    }
                }

                while (!packet.isComplete()) {
                    videoCoder.encodeVideo(packet, null, 0);
                    if (packet.isComplete()) {
                        sendPacket(packet);
                    }
                }
                videoCoder.close();
                try {
                    camera.stop();
                } catch (FrameGrabber.Exception ex) {
                    ex.printStackTrace();
                }
                
            }
        }
        );
        record = true;
        ct = System.currentTimeMillis();

        thread_audio.start();
        //thread_video.start();
    }

    private void WritePicture(BufferedImage image, String name) throws IOException {
        File file = new File(name);
        file.createNewFile();
        ImageIO.write(image, "png", file);
    }

    private void sendPacket(IPacket packet) {
        if (packet.getSize() > 0) {
            byte[] bytes = packet.getData().getByteArray(0, packet.getSize());
            DatagramPacket datagramPacket = new DatagramPacket(bytes, 0, bytes.length, new InetSocketAddress("localhost", 30000));

            try {
                datagram_socket.send(datagramPacket);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private SourceDataLine getSpeaker() throws LineUnavailableException {
        DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
        speakers = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        speakers.open(audioFormat);
        return speakers;
    }

    public void stop() {
        //System.out.println("pocz");
        record = false;
        while (thread_audio.isAlive() || thread_video.isAlive()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
            }
        }        
        System.out.println("Stop");
    }

    public void setAudioFormat(float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian) {
        audioFormat = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    private void initMicrophone(TargetDataLine microphone) throws LineUnavailableException {
        microphone.open(audioFormat);
    }

    private TargetDataLine getMicrophone(Mixer.Info mixerInfo) throws LineUnavailableException {
        Mixer m = AudioSystem.getMixer(mixerInfo);
        Line.Info[] lineInfos = m.getTargetLineInfo();
        return (TargetDataLine) AudioSystem.getLine(lineInfos[0]);
    }

    private Mixer.Info[] getMics() throws LineUnavailableException {
        ArrayList<Mixer.Info> lines = new ArrayList();
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixerInfos) {
            Mixer m = AudioSystem.getMixer(info);
            Line.Info[] lineInfos = m.getTargetLineInfo();
            for (int i = 0; i < lineInfos.length; i++) {
                if (lineInfos[i].getLineClass() == TargetDataLine.class) {
                    lines.add(info);
                }
            }
        }
        return lines.toArray(new Mixer.Info[lines.size()]);
    }

    public void initCamera(FrameGrabber frameGrabber) {
        camera.setImageWidth(640);
        camera.setImageHeight(480);
        camera.setFrameRate(30);
        camera.setPixelFormat(org.bytedeco.javacpp.avutil.AV_PIX_FMT_YUV420P);
    }

    public FrameGrabber getCamera() throws FrameGrabber.Exception {
        return FrameGrabber.createDefault(0);
    }

    public short[] toShort(byte[] source) {
        short[] destination = new short[source.length / 2];
        int index = -1;
        for (int i = 0; i < destination.length; i++) {
            destination[i] = (short) ((source[(index += 1)] << 8)
                    | (source[index += 1] & 0xFF));
            //System.out.println(destination);
        }
        return destination;
    }

    public byte[] toByte(short[] source) {
        byte[] destination = new byte[source.length * 2];
        int index = -1;
        for (int i = 0; i < source.length; i++) {
            destination[i + 0] = (byte) ((source[i] >> 8) & 0xFF);
            destination[i + 1] = (byte) ((source[i] & 0xFF));
            //System.out.println(destination);
        }
        return destination;
    }

    public static BufferedImage convertToType(BufferedImage sourceImage, int targetType) {
        BufferedImage image;
        // if the source image is already the target type, return the source image
        if (sourceImage.getType() == targetType) {
            image = sourceImage;
        } // otherwise create a new image of the target type and draw the new image
        else {
            image = new BufferedImage(sourceImage.getWidth(),
                    sourceImage.getHeight(), targetType);
            image.getGraphics().drawImage(sourceImage, 0, 0, null);
        }
        return image;
    }

    private DatagramSocket datagram_socket;

    class output extends OutputStream {

        private DatagramPacket packet;

        private void send(byte[] bytes, int off, int len) {
            packet = new DatagramPacket(bytes, off, len, new InetSocketAddress("localhost", 30000));
            //System.out.println("wysylam " + packet.getLength());
            /*try {
             datagram_socket.send(packet);
             } catch (IOException ex) {
             ex.printStackTrace();
             }*/
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            send(b, off, len);
        }

        @Override
        public void close() throws IOException {
            datagram_socket.close();
            super.close();
        }

        @Override
        public void write(int b) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }

}

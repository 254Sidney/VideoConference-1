package wideokomunikator.audiovideo;

import com.xuggle.ferry.IBuffer;
import com.xuggle.xuggler.*;
import com.xuggle.xuggler.video.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.JOptionPane;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import wideokomunikator.client.StreamBuffer;

public class AudioVideo {

    long ct = System.currentTimeMillis();
    //Audio
    private SourceDataLine speakers = null;
    private TargetDataLine microphone = null;
    private float sampleRate = 8000.0f;
    private int sampleSize = 16;
    private int channels = 1;
    private boolean signed = true;
    private boolean bigEndian = true;
    private AudioFormat audioFormat = null;
    private boolean readyMicrophone = false;
    ;
    private boolean readySpeakers = false;

    //Video
    private FrameGrabber camera = null;
    private IConverter videoConverter = null;
    private boolean readyCamera = false;
    private IVideoResampler videoResampler = null;

    private boolean record = true;

    private IStreamCoder audioCoder;
    private IStreamCoder videoCoder;

    private HashMap<Integer, IStreamCoder> audioDecoder;
    private HashMap<Integer, IStreamCoder> videoDecoder;
    private HashMap<Integer, StreamBuffer<Packet>> buffersAudioReceive;
    private HashMap<Integer, StreamBuffer<Packet>> buffersVideoReceive;
    private ArrayList<Packet> bufferVideo;
    private ArrayList<Packet> bufferAudio;

    private Thread thread_audio, thread_video;
    private DatagramSocket datagram_socket_audio;
    private DatagramSocket datagram_socket_video;
    private final String serverHost;
    private final int serverPortAudio, serverPortVideo;

    private final int DATAGRAM_SIZE = 64000;
    private int AudioPacketCounter = 0;
    private int VideoPacketCounter = 0;

    private final int UserID;

    private final ICodec.ID audioCodec = ICodec.ID.CODEC_ID_AAC, videoCodec = ICodec.ID.CODEC_ID_H264;

    public AudioVideo(int UserID, String serverHost, int serverPortAudio, int serverPortVideo) {
        this.UserID = UserID;
        System.out.println("Łącze z serwerem " + serverHost + " " + serverPortAudio + " " + serverPortVideo);
        this.serverHost = serverHost;
        this.serverPortAudio = serverPortAudio;
        this.serverPortVideo = serverPortVideo;
        try {
            datagram_socket_audio = new DatagramSocket();
            datagram_socket_video = new DatagramSocket();
            System.out.println("Otwietam wideo na porcie:" + datagram_socket_video.getLocalPort());
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        setAudioFormat(sampleRate, sampleSize, channels, signed, bigEndian);
        audioDecoder = new HashMap<>();
        videoDecoder = new HashMap<>();
        buffersAudioReceive = new HashMap<>();
        buffersVideoReceive = new HashMap<>();
        bufferVideo = new ArrayList<>();
        bufferAudio = new ArrayList<>();
    }

    private boolean initDevices() {
        try {
            speakers = getSpeaker();
            speakers.start();
            readySpeakers = true;
        } catch (LineUnavailableException ex) {
            showError("Brak urządzenia odtwarzającego");
            readySpeakers = false;
        }
        try {
            microphone = getMicrophone(getMics()[0]);
            initMicrophone(microphone);
            microphone.start();
            readyMicrophone = true;
        } catch (LineUnavailableException ex) {
            showError("Brak urządzenia nagrywającego audio");
            readyMicrophone = false;
        }
        try {
            camera = getCamera();
            initCamera(camera);
            camera.start();
            readyCamera = true;
        } catch (FrameGrabber.Exception ex) {
            showError("Brak urządzenia nagrywającego video");
            readyCamera = false;
        }
        return (readyCamera == true) && (readyMicrophone == true) && (readySpeakers == true);

    }

    private IStreamCoder initAudioCoder(IStreamCoder coder, boolean isEncoder) {
        if (isEncoder) {
            coder = IStreamCoder.make(IStreamCoder.Direction.ENCODING, audioCodec);
            coder.setSampleRate((int) sampleRate);
            coder.setChannels(channels);
            coder.setFlag(IStreamCoder.Flags.FLAG2_FAST, true);
            coder.setFlag(IStreamCoder.Flags.FLAG_LOW_DELAY, true);
        } else {
            coder = IStreamCoder.make(IStreamCoder.Direction.DECODING, audioCodec);
            coder.setSampleRate((int) sampleRate);
            coder.setChannels(channels);
        }
        return coder;
    }

    private IStreamCoder initVideoCoder(IStreamCoder coder, boolean isEncoder) {
        if (isEncoder) {
            IRational frameRate = IRational.make((int) camera.getFrameRate(), 1);
            coder = IStreamCoder.make(IStreamCoder.Direction.ENCODING, videoCodec);
            coder.setWidth(320);
            coder.setHeight(240);
            coder.setPixelType(IPixelFormat.Type.YUV420P);
            coder.setBitRate(64000);
            coder.setBitRateTolerance(32000);
            coder.setFrameRate(frameRate);
            coder.setNumPicturesInGroupOfPictures(3);
            coder.setTimeBase(IRational.make(1, (int) camera.getFrameRate()));
            coder.setFlag(IStreamCoder.Flags.FLAG_LOW_DELAY, true);
            coder.setFlag(IStreamCoder.Flags.FLAG2_FAST, true);
            coder.setProperty("me_method", "+zero");
            coder.setProperty("x264-params", "fast-pskip=1");
        } else {
            coder = IStreamCoder.make(IStreamCoder.Direction.DECODING, videoCodec);
            coder.setWidth(320);
            coder.setHeight(240);
        }
        return coder;
    }

    private boolean hasDecoders(int UserID) {
        return videoDecoder.getOrDefault(UserID, null) != null && audioDecoder.getOrDefault(UserID, null) != null;
    }

    private void getDecoders(int UserID) {
        if (videoDecoder.getOrDefault(UserID, null) == null) {
            IStreamCoder coder = null;
            coder = initVideoCoder(coder, false);
            coder.open(null, null);
            videoDecoder.put(UserID, coder);
        }
        if (audioDecoder.getOrDefault(UserID, null) == null) {
            IStreamCoder coder = null;
            coder = initAudioCoder(coder, false);
            coder.open(null, null);
            audioDecoder.put(UserID, coder);
        }
    }

    public void record() {

        boolean devicesOk = initDevices();
        if (!devicesOk) {
            System.out.println("Niesprawne urzadzenie " + readyCamera + " " + readyMicrophone + " " + readySpeakers);
            return;
        }
        audioCoder = initAudioCoder(audioCoder, true);
        videoCoder = initVideoCoder(videoCoder, true);
        IMetaData videocodecOptions = IMetaData.make();
        videocodecOptions.setValue("tune", "zerolatency");
        //videocodecOptions.setValue("preset", "ultrafast");
        IMetaData audiocodecOptions = IMetaData.make();
        audiocodecOptions.setValue("strict", "2");
        audiocodecOptions.setValue("tune", "zerolatency");
        audiocodecOptions.setValue("preset", "ultrafast");

        audioCoder.open(audiocodecOptions, null);
        videoCoder.open(videocodecOptions, null);
        thread_audio = new Thread(new Runnable() {
            @Override
            public void run() {
                int size = microphone.getBufferSize()/5;
                byte[] data = new byte[size];
                short[] audioSamples = new short[size / (sampleSize / 8)];
                long ct = System.currentTimeMillis();
                microphone.start();
                IPacket packet = IPacket.make(100);
                IAudioSamples sample = IAudioSamples.make(audioSamples.length, channels);
                while (record) {
                    microphone.read(data, 0, data.length);
                    long now = System.currentTimeMillis();
                    audioSamples = toShort(data);
                    sample.put(audioSamples, 0, 0, audioSamples.length);
                    sample.setComplete(true, audioSamples.length / channels, (int) sampleRate, channels, IAudioSamples.Format.FMT_S16, IAudioSamples.defaultPtsToSamples(now - ct, (int) sampleRate));
                    int offset = 0;
                    while (offset < audioSamples.length / channels) {
                        int bytesEncoded = audioCoder.encodeAudio(packet, sample, offset);
                        offset += bytesEncoded;
                        if (packet.getSize() > 0) {
                            bufferAudio.add(new Packet(packet, UserID));
                            packet = IPacket.make();
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
                    converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);
                    pictureSample = converter.toPicture(image, timeStamp);
                    videoResampler = IVideoResampler.make(videoCoder.getWidth(), videoCoder.getHeight(), pictureSample.getPixelType(), pictureSample.getWidth(), pictureSample.getHeight(), pictureSample.getPixelType());

                    IVideoPicture out = IVideoPicture.make(pictureSample.getPixelType(), videoCoder.getWidth(), videoCoder.getHeight());
                    out.setPts(timeStamp);
                    out.setTimeStamp(timeStamp);
                    videoResampler.resample(out, pictureSample);
                    videoCoder.encodeVideo(packet, out, 0);

                    if (packet.isComplete()) {
                        bufferVideo.add(new Packet(packet, UserID));
                        packet = IPacket.make();
                    }
                }

                while (!packet.isComplete() && record) {
                    videoCoder.encodeVideo(packet, null, 0);
                    if (packet.isComplete()) {
                        bufferVideo.add(new Packet(packet, UserID));
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
        ct = System.currentTimeMillis();
        StartAudioReceiveThread();
        StartVideoReceiveThread();
        startAudioSendingThread();
        //startVideoSendingThread();
        if (readyMicrophone) {
            thread_audio.start();
        }
        if (readyCamera) {
            //thread_video.start();
        }
    }
    int frames = 0;

    private void startVideoSendingThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (record) {
                    if (bufferVideo.size() > 0) {
                        Packet packet = bufferVideo.remove(0);
                        sendPacket(packet, false);
                    } else {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    private void startAudioSendingThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (record) {
                    if (bufferAudio.size() > 0) {
                        Packet packet = bufferAudio.remove(0);
                        sendPacket(packet, true);
                    } else {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    private void decodeAudio(Packet packet, int UserID) {
        IPacket ipacket = packet.getIPacket();
        IAudioSamples audio = IAudioSamples.make(ipacket.getData(), audioDecoder.get(UserID).getChannels(), audioDecoder.get(UserID).getSampleFormat());
        int offset = 0;
        while (offset < ipacket.getSize()) {
            int bytesDecoded = audioDecoder.get(UserID).decodeAudio(audio, ipacket, offset);
            offset += bytesDecoded;

            try {
                if (audio.isComplete()) {
                    byte[] bytes = audio.getData().getByteArray(0, audio.getSize());
                    speakers.write(bytes, 0, bytes.length);
                    audio.delete();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        ipacket.delete();

    }

    private void decodeVideo(Packet packet, int UserID) {
        IPacket ipacket = packet.getIPacket();
        IVideoPicture picture = IVideoPicture.make(IPixelFormat.Type.YUV420P, 320, 240);
        if (videoDecoder.getOrDefault(UserID, null) != null) {
            int offset = 0;
            while (offset < ipacket.getSize()) {
                try {
                    int bytesDecoded = videoDecoder.get(UserID).decodeVideo(picture, ipacket, offset);
                    offset += bytesDecoded;
                    if (picture.isComplete()) {
                        videoConverter = ConverterFactory.createConverter(ConverterFactory.XUGGLER_BGR_24, picture);
                        videoConverter.toImage(picture);
                        BufferedImage image = videoConverter.toImage(picture);
                        setImage(image, UserID);

                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            picture.delete();
        }
        ipacket.delete();
    }

    public void setImage(BufferedImage image, int ID) {

    }

    public void WritePicture(BufferedImage image, String name) throws IOException {
        File file = new File(name);
        file.createNewFile();
        ImageIO.write(image, "png", file);
    }

    private void sendPacket(Packet packet, boolean isAudio) {
        int packetNR = isAudio ? AudioPacketCounter : VideoPacketCounter;
        byte[] message = packet.getDataToSend(packetNR);
        DatagramPacket datagram;
        if (isAudio) {
            AudioPacketCounter = getNextAudioPacket();
            try {
                datagram = new DatagramPacket(message, message.length, new InetSocketAddress(serverHost, serverPortAudio));
                datagram_socket_audio.send(datagram);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            VideoPacketCounter = getNextVideoPacket();
            try {
                datagram = new DatagramPacket(message, message.length, new InetSocketAddress(serverHost, serverPortVideo));
                datagram_socket_video.send(datagram);
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
        record = false;
        while (thread_audio.isAlive() || thread_video.isAlive()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
            }
        }
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
        ArrayList<Mixer.Info> lines = new ArrayList<Mixer.Info>();
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
        camera.setFrameRate(24);
        camera.setPixelFormat(org.bytedeco.javacpp.avutil.AV_PIX_FMT_YUV420P);
    }

    public FrameGrabber getCamera() throws FrameGrabber.Exception {
        return OpenCVFrameGrabber.createDefault(0);
    }

    public short[] toShort(byte[] source) {
        short[] destination = new short[source.length / 2];
        int index = -1;
        for (int i = 0; i < destination.length; i++) {
            destination[i] = (short) ((source[(index += 1)] << 8)
                    | (source[index += 1] & 0xFF));
            if (destination[i] < 3 && destination[i] > -3) {
                destination[i] = 0;
            }
        }
        return destination;
    }

    public byte[] toByte(short[] source) {
        byte[] destination = new byte[source.length * 2];
        for (int i = 0; i < source.length; i++) {
            destination[i + 0] = (byte) ((source[i] >> 8) & 0xFF);
            destination[i + 1] = (byte) ((source[i] & 0xFF));
        }
        return destination;
    }

    private void StartAudioReceiveThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (record) {
                    byte[] buffer = new byte[DATAGRAM_SIZE];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    try {
                        datagram_socket_audio.receive(packet);
                        new Hendler(packet.getData(), packet.getLength(), true).start();
                        packet = null;
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
                while (record) {
                    byte[] buffer = new byte[DATAGRAM_SIZE];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    try {
                        datagram_socket_video.receive(packet);
                        new Hendler(packet.getData(), packet.getLength(), false).start();
                        packet = null;
                    } catch (IOException ex) {
                    }
                }
            }
        }).start();
    }

    private void StartVideoDecodeThread(int ID) {
        new Thread(new Runnable() {
            final int UserID = ID;
            final int timeout = 2000;
            boolean keypacket = false;

            @Override
            public void run() {
                int time = 0;
                while (record) {
                    synchronized (buffersVideoReceive.get(UserID)) {
                        StreamBuffer<Packet> streambuffer = buffersVideoReceive.get(UserID);
                        if (streambuffer.isNext()) {
                            Packet packet = streambuffer.getNext();
                            if (packet.isKeyPacket()) {
                                keypacket = true;
                            }
                            if (keypacket) {
                                decodeVideo(packet, UserID);
                            }
                            time = 0;
                        } else {
                            if (time >= timeout) {
                                streambuffer.skip();
                                keypacket = false;
                                time = 0;
                            } else {
                                try {
                                    Thread.sleep(10);
                                    time += 10;
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }

                    }
                }
            }
        }).start();
    }

    private void StartAudioDecodeThread(int ID) {
        new Thread(new Runnable() {
            final int timeout = 2000;
            boolean keypacket = false;

            @Override
            public void run() {
                int time = 0;
                while (record) {
                    StreamBuffer<Packet> streambuffer = buffersAudioReceive.get(ID);
                    synchronized (buffersAudioReceive.get(ID)) {
                        Packet packet;
                        if (streambuffer.isNext()) {
                            packet = streambuffer.getNext();
                            if (packet.isKeyPacket()) {
                                keypacket = true;
                            }
                            if (keypacket) {
                                decodeAudio(packet, ID);
                            }
                            time = 0;
                        } else {
                            if (time >= timeout) {
                                streambuffer.skip();
                                System.out.println("skipAudio");
                                keypacket = false;
                                time = 0;
                            } else {
                                try {
                                    Thread.sleep(10);
                                    time += 10;
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }).start();
    }

    private synchronized int getNextAudioPacket() {
        if (AudioPacketCounter >= Integer.MAX_VALUE) {
            return AudioPacketCounter = 0;
        }
        AudioPacketCounter++;
        return AudioPacketCounter;
    }

    private synchronized int getNextVideoPacket() {
        if (VideoPacketCounter >= Integer.MAX_VALUE) {
            return VideoPacketCounter = 0;
        }
        VideoPacketCounter++;
        return VideoPacketCounter;
    }

    class Hendler extends Thread {

        private final boolean isAudio;
        private final byte[] data;

        public Hendler(byte[] packet, int len, boolean isAudio) {
            this.isAudio = isAudio;
            this.data = Arrays.copyOf(packet, len);
        }

        @Override
        public void run() {            
            Packet packet = new Packet(data);
            if (hasDecoders(packet.getUserID()) == false) {
                getDecoders(packet.getUserID());
                buffersVideoReceive.put(packet.getUserID(), new StreamBuffer<>());
                buffersAudioReceive.put(packet.getUserID(), new StreamBuffer<>());
                StartVideoDecodeThread(packet.getUserID());
                StartAudioDecodeThread(packet.getUserID());
            }
            if (isAudio) {
                buffersAudioReceive.get(packet.getUserID()).setPacket(packet.getPacketId(), packet);
            } else {
                buffersVideoReceive.get(packet.getUserID()).setPacket(packet.getPacketId(), packet);;
            }

        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Błąd", JOptionPane.ERROR_MESSAGE);
    }

}

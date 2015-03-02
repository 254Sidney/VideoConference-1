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
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private float sampleRate = 48000.0f;
    private int sampleSize = 16;
    private int channels = 2;
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
    private HashMap<Integer, Integer> lastAudioIndex;
    private HashMap<Integer, Integer> lastVideoIndex;
    private HashMap<Integer, byte[]> lastAudioFrame;
    private HashMap<Integer, byte[]> lastVideoFrame;
    private HashMap<Integer, StreamBuffer<IPacket>> buffersAudio;
    private HashMap<Integer, StreamBuffer<IPacket>> buffersVideo;

    private Thread thread_audio, thread_video;
    private DatagramSocket datagram_socket_audio;
    private DatagramSocket datagram_socket_video;
    private final String serverHost;
    private final int serverPortAudio, serverPortVideo;

    private final int DATAGRAM_SIZE = 64000;
    private int AudioPacketCounter = 0;
    private int VideoPacketCounter = 0;

    private final int UserID;

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
        audioDecoder = new HashMap<Integer, IStreamCoder>();
        videoDecoder = new HashMap<Integer, IStreamCoder>();
        lastAudioIndex = new HashMap<Integer, Integer>();
        lastVideoIndex = new HashMap<Integer, Integer>();
        buffersAudio = new HashMap<Integer, StreamBuffer<IPacket>>();
        buffersVideo = new HashMap<Integer, StreamBuffer<IPacket>>();
        lastAudioFrame = new HashMap<Integer, byte[]>();
        lastVideoFrame = new HashMap<Integer, byte[]>();
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
            coder = IStreamCoder.make(IStreamCoder.Direction.ENCODING, ICodec.ID.CODEC_ID_AAC);
            coder.setBitRate(128000);
            coder.setSampleRate((int) sampleRate);
            coder.setBitRateTolerance(90000);
            coder.setChannels(channels);
            coder.setFrameRate(IRational.make((int) sampleRate, 1));
        } else {
            coder = IStreamCoder.make(IStreamCoder.Direction.DECODING, ICodec.ID.CODEC_ID_AAC);
            coder.setChannels(2);
            coder.setBitRate(128000);
            coder.setSampleRate((int) sampleRate);
            coder.setBitRateTolerance(90000);
            coder.setChannels(channels);
            coder.setFrameRate(IRational.make((int) sampleRate, 1));
        }
        return coder;
    }

    private IStreamCoder initVideoCoder(IStreamCoder coder, boolean isEncoder) {
        if (isEncoder) {
            IRational frameRate = IRational.make((int) camera.getFrameRate(), 1);
            coder = IStreamCoder.make(IStreamCoder.Direction.ENCODING, ICodec.ID.CODEC_ID_H264);
            coder.setWidth(320);
            coder.setHeight(240);
            coder.setPixelType(IPixelFormat.Type.YUV420P);
            coder.setBitRate(128000);
            coder.setBitRateTolerance(128000);
            coder.setFrameRate(frameRate);
            coder.setNumPicturesInGroupOfPictures(10);
            coder.setTimeBase(IRational.make(1, (int) camera.getFrameRate()));
            coder.setFlag(IStreamCoder.Flags.FLAG_LOW_DELAY, true);
            coder.setFlag(IStreamCoder.Flags.FLAG2_FAST, true);
            coder.setAutomaticallyStampPacketsForStream(true);
            coder.setProperty("me_method", "+zero");
            coder.setProperty("x264-params", "fast-pskip=1");
        } else {
            coder = IStreamCoder.make(IStreamCoder.Direction.DECODING, ICodec.ID.CODEC_ID_H264);
            coder.setNumPicturesInGroupOfPictures(10);
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

        audioCoder.open(null, null);
        videoCoder = initVideoCoder(videoCoder, true);
        //videoDecoder = initVideoCoder(videoDecoder, false);
        IMetaData videocodecOptions = IMetaData.make();
        videocodecOptions.setValue("tune", "zerolatency");
        IContainerFormat format = IContainerFormat.make();

        //container.open(output2, format);
        //System.out.println(container.addNewStream(videoCoder));
        videoCoder.open(videocodecOptions, null);
        //container.writeHeader();
        //container.flushPackets();
        //videoDecoder.open(null, null);
        thread_audio = new Thread(new Runnable() {
            @Override
            public void run() {
                int size = microphone.getBufferSize() / 5;
                byte[] data = new byte[size];
                short[] audioSamples = new short[size / (sampleSize / 8)];
                long ct = System.currentTimeMillis();
                microphone.start();
                IPacket packet = IPacket.make();
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
                            //sendPacket(packet.getData().getByteArray(0, packet.getSize()).clone(), audioCoder);
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
                    converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);
                    pictureSample = converter.toPicture(image, timeStamp);
                    videoResampler = IVideoResampler.make(videoCoder.getWidth(), videoCoder.getHeight(), pictureSample.getPixelType(), pictureSample.getWidth(), pictureSample.getHeight(), pictureSample.getPixelType());

                    IVideoPicture out = IVideoPicture.make(pictureSample.getPixelType(), videoCoder.getWidth(), videoCoder.getHeight());
                    out.setPts(timeStamp);
                    out.setTimeStamp(timeStamp);
                    videoResampler.resample(out, pictureSample);
                    videoCoder.encodeVideo(packet, out, 0);
                    //System.out.println(videoCoder.getNextPredictedPts()-packet.getPts());

                    if (packet.isComplete()) {
                        //System.out.println(packet.isKeyPacket()+" "+packet.isKey());
                        sendPacket(packet.getData().getByteArray(0, packet.getSize()), packet.isKeyPacket(), videoCoder);
                        //System.out.println(packet);
                        packet.reset();
                    }
                    //System.out.println(timeStamp);
                }

                while (!packet.isComplete() && record) {
                    videoCoder.encodeVideo(packet, null, 0);
                    if (packet.isComplete()) {
                        sendPacket(packet.getData().getByteArray(0, packet.getSize()), packet.isKeyPacket(), videoCoder);
                        packet.reset();
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
        if (readyMicrophone) {
            thread_audio.start();
        }
        if (readyCamera) {
            thread_video.start();
        }
    }
    int frames = 0;

    private void decodeAudio(IPacket packet, int UserID) {
        IAudioSamples audio = IAudioSamples.make(packet.getData(), audioDecoder.get(UserID).getChannels(), audioDecoder.get(UserID).getSampleFormat());
        int offset = 0;
        //System.out.println(audioDecoder.get(UserID).getNextPredictedPts());
        /*
         while (offset < packet.getSize()) {
         int bytesDecoded = audioDecoder.get(UserID).decodeAudio(audio, packet, offset);
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
         */
        packet.delete();

    }
    long time = System.currentTimeMillis();

    private void decodeVideo(IPacket packet, int UserID) {
        IVideoPicture picture = IVideoPicture.make(IPixelFormat.Type.YUV420P, 320, 240);
        //System.out.println(videoDecoder.get(UserID).getNextPredictedPts());
        //System.out.println(packet);
        if (videoDecoder.getOrDefault(UserID, null) != null) {
            int offset = 0;
            while (offset < packet.getSize()) {
                try {
                    int bytesDecoded = videoDecoder.get(UserID).decodeVideo(picture, packet, offset);
                    offset += bytesDecoded;
                    if (picture.isComplete()) {
                        if (picture.isKeyFrame()) {
                            //System.out.println("key");
                        }
                        videoConverter = ConverterFactory.createConverter(ConverterFactory.XUGGLER_BGR_24, picture);
                        videoConverter.toImage(picture);
                        BufferedImage image = videoConverter.toImage(picture);
                        setImage(image, UserID);
                        long now = System.currentTimeMillis();
                        //System.out.println((now -time)+" "+picture);
                        time = now;

                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            //videoDecoder.release();
            picture.delete();
        }
    }

    public void setImage(BufferedImage image, int ID) {

    }

    public void WritePicture(BufferedImage image, String name) throws IOException {
        File file = new File(name);
        file.createNewFile();
        ImageIO.write(image, "png", file);
    }

    //long time = System.currentTimeMillis();
    private synchronized void sendPacket(byte[] packet, boolean isKeyPacket, IStreamCoder coder) {
        boolean isAudio = (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO);
        int packetNR = isAudio ? AudioPacketCounter : VideoPacketCounter;
        byte[] counter = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(packetNR).array();
        byte[] userID = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(UserID).array();
        byte[] key = ByteBuffer.allocate(1).order(ByteOrder.BIG_ENDIAN).put(isKeyPacket == true ? (byte) 1 : (byte) 0).array();
        //byte isKey = (byte)1;//;
        //System.out.println(isKey);
        byte[] message = new byte[packet.length + counter.length + userID.length + key.length];
        System.arraycopy(userID, 0, message, 0, 4);
        System.arraycopy(counter, 0, message, 4, 4);
        System.arraycopy(key, 0, message, 8, key.length);
        System.arraycopy(packet, 0, message, 9, packet.length);
        //System.out.println(packetNR + " " + ByteBuffer.wrap(counter).order(ByteOrder.BIG_ENDIAN).getInt());
        DatagramPacket datagram;
        if (isAudio) {
            AudioPacketCounter = getNextAudioPacket();
            try {
                datagram = new DatagramPacket(message, message.length, new InetSocketAddress(serverHost, serverPortAudio));
                //System.out.println("Wysylam audio " + packetNR);
                datagram_socket_audio.send(datagram);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            VideoPacketCounter = getNextVideoPacket();
            try {
                datagram = new DatagramPacket(message, message.length, new InetSocketAddress(serverHost, serverPortVideo));
                //System.out.println("Wysylam wideo" + packetNR);
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
        camera.setFrameRate(30);
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
                        //decodeAudio(ipacket, id);
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

            @Override
            public void run() {
                while (record) {
                    StreamBuffer<IPacket> streambuffer = buffersVideo.get(UserID);
                    if (streambuffer != null) {
                        if (streambuffer.isNext()) {
                            decodeVideo(streambuffer.getNext(), UserID);
                        } else {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }
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

    boolean keypacket = false;

    class Hendler extends Thread {

        private final byte[] data;
        private final boolean isAudio;
        private final int len;
        private final int UserID;
        private final int packetID;
        private final boolean isKeyPacket;

        public Hendler(byte[] packet, int len, boolean isAudio) {
            this.data = packet;
            this.isAudio = isAudio;
            this.len = len;
            this.UserID = ByteBuffer.wrap(Arrays.copyOfRange(data, 0, 4)).order(ByteOrder.BIG_ENDIAN).getInt();
            this.packetID = ByteBuffer.wrap(Arrays.copyOfRange(data, 4, 8)).order(ByteOrder.BIG_ENDIAN).getInt();
            this.isKeyPacket = (ByteBuffer.wrap(Arrays.copyOfRange(data, 8, 9)).order(ByteOrder.BIG_ENDIAN).get() == (byte) 1) ? true : false;
        }

        @Override
        public void run() {
            if (hasDecoders(UserID) == false) {
                getDecoders(UserID);
            }
            if (!buffersVideo.containsKey(UserID)) {
                buffersVideo.put(UserID, new StreamBuffer<IPacket>());
                StartVideoDecodeThread(UserID);
            }
            if (!buffersAudio.containsKey(UserID)) {
                buffersAudio.put(UserID, new StreamBuffer<IPacket>());
            }
            IPacket ipacket = IPacket.make();
            IBuffer buff = IBuffer.make(ipacket, len - 9);
            buff.put(data, 9, 0, len - 9);
            ipacket.setData(buff);
            ipacket.setKeyPacket(isKeyPacket);

            {
                if (!isAudio) {
                    buffersVideo.get(UserID).setPacket(packetID, ipacket);
                    //System.out.println(packetID);
                    //if (isKeyPacket) {
                    //decodeVideo(ipacket, UserID);
                    //}
                    //}

                    //System.out.println("ID = " + packetID + " "+ ipacket.isKeyPacket()+" "+ipacket.isKey());
                    /*if (keypacket == false) {
                     if (isKeyPacket) {
                     keypacket = true;
                     ipacket.setKeyPacket(true);
                     decodeVideo(ipacket, UserID);
                     }
                     } else {
                     decodeVideo(ipacket, UserID);
                     }
                     //*/
                }
            }
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Błąd", JOptionPane.ERROR_MESSAGE);
    }

}

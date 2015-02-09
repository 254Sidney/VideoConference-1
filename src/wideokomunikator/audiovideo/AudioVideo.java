package wideokomunikator.audiovideo;

import com.xuggle.xuggler.*;
import com.xuggle.xuggler.video.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;

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
    private Thread thread_audio, thread_video;
    private DatagramSocket datagram_socket_audio;
    private DatagramSocket datagram_socket_video;

    //Video
    private FrameGrabber camera = null;

    private boolean record;

    private IStreamCoder audioCoder;
    private IStreamCoder videoCoder;

    public AudioVideo() {
        setAudioFormat(sampleRate, sampleSize, channels, signed, bigEndian);
        try {
            microphone = getMicrophone(getMics()[1]);
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        }

        try {
            //camera = FrameGrabber.createDefault(0);
            camera = getCamera();
        } catch (FrameGrabber.Exception ex) {
            ex.printStackTrace();
        }

        try {
            datagram_socket_audio = new DatagramSocket();
            datagram_socket_video = new DatagramSocket();
        } catch (SocketException ex) {
            ex.printStackTrace();
        }

    }

    public void record() {
        initCamera(camera);
        try {
            initMicrophone(microphone);
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        }
        audioCoder = IStreamCoder.make(IStreamCoder.Direction.ENCODING, ICodec.ID.CODEC_ID_AAC);
        audioCoder.setBitRate(128000);
        audioCoder.setSampleRate((int) sampleRate);
        audioCoder.setBitRateTolerance(90000);
        audioCoder.setChannels(channels);
        audioCoder.setFrameRate(IRational.make((int) sampleRate, 1));

        //audioCoder.setAutomaticallyStampPacketsForStream(true);
        //audioCoder.setProperty("vbr", "5");
        //IMetaData audiocodecOptions = IMetaData.make();
        //audiocodecOptions.setValue("tune", "zerolatency");
        audioCoder.open(null,null);

        try {
            camera.start();
        } catch (FrameGrabber.Exception ex) {
            ex.printStackTrace();
        }
        IRational frameRate = IRational.make((int) camera.getFrameRate(), 1);
        videoCoder = IStreamCoder.make(IStreamCoder.Direction.ENCODING, ICodec.ID.CODEC_ID_H264);
        videoCoder.setWidth(camera.getImageWidth());
        videoCoder.setHeight(camera.getImageHeight());
        videoCoder.setPixelType(IPixelFormat.Type.YUV420P);
        videoCoder.setBitRate(512000);
        videoCoder.setBitRateTolerance(128000);
        videoCoder.setFrameRate(frameRate);
        videoCoder.setNumPicturesInGroupOfPictures(10);
        videoCoder.setTimeBase(IRational.make(1, (int) camera.getFrameRate()));
        videoCoder.setFlag(IStreamCoder.Flags.FLAG_LOW_DELAY, true);
        videoCoder.setFlag(IStreamCoder.Flags.FLAG2_FAST, true);
        videoCoder.setProperty("me_method", "+zero");
        videoCoder.setProperty("x264-params", "fast-pskip=1");
        IMetaData videocodecOptions = IMetaData.make();
        videocodecOptions.setValue("tune", "zerolatency");
        videoCoder.open(videocodecOptions, null);
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
                            sendPacket(packet.getData().getByteArray(0, packet.getSize()).clone(), audioCoder);
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
                    videoCoder.encodeVideo(packet, pictureSample, 0);
                    if (packet.isComplete()) {
                        sendPacket(packet.getData().getByteArray(0, packet.getSize()), videoCoder);
                        packet.reset();
                    }
                }

                while (!packet.isComplete() && record) {
                    videoCoder.encodeVideo(packet, null, 0);
                    if (packet.isComplete()) {
                        sendPacket(packet.getData().getByteArray(0, packet.getSize()), videoCoder);
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
        record = true;
        ct = System.currentTimeMillis();

        thread_audio.start();
        thread_video.start();
    }
    int frames = 0;

    public void WritePicture(BufferedImage image, String name) throws IOException {
        File file = new File(name);
        file.createNewFile();
        ImageIO.write(image, "png", file);
    }

    private void sendPacket(byte[] packet, IStreamCoder coder) {
        long tim = System.nanoTime();
        boolean isAudio = (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO);
        DatagramPacket datagram;
        if (isAudio) {
            try {
                datagram = new DatagramPacket(packet, packet.length, new InetSocketAddress("localhost", 30001));
                datagram_socket_audio.send(datagram);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
            try {
                datagram = new DatagramPacket(packet, packet.length, new InetSocketAddress("localhost", 30000));
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

}

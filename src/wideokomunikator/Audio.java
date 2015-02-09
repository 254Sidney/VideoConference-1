package wideokomunikator;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.*;
import wideokomunikator.server.PasswordSecurity;

public class Audio {

    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private AudioFormat audio_format;
    private Thread thread;
    private ByteArrayOutputStream baos;
    private boolean recording;

    private float sampleRate = 48000.0f;
    private int sampleSize = 16;
    private int channels = 2;
    private boolean signed = true;
    private boolean bigEndian = true;
    private float samples[][];

    public Audio() throws LineUnavailableException {
        baos = new ByteArrayOutputStream();
        Mixer.Info[] lines = getDevices();
    }

    public static void printBytes(byte[] array, String name) {
        for (int k = 0; k < array.length; k++) {
            System.out.println(name + "[" + k + "] = " + "0x"
                    + PasswordSecurity.byteToHex(array[k]));
        }
    }

    private Mixer.Info[] getDevices() throws LineUnavailableException {
        ArrayList<Mixer.Info> lines = new ArrayList<Mixer.Info>();
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixerInfos) {
            Mixer m = AudioSystem.getMixer(info);
            Line.Info[] lineInfos = m.getTargetLineInfo();
            for (int i = 0; i < lineInfos.length; i++) {
                Line line = m.getLine(lineInfos[i]);
                if (lineInfos[i].getLineClass() == TargetDataLine.class) {
                    lines.add(info);
                }
            }
        }
        return lines.toArray(new Mixer.Info[lines.size()]);
    }

    private int numBytesRead = 0;


    public void record3() throws LineUnavailableException {

        audio_format = new AudioFormat(sampleRate, sampleSize, channels, signed, bigEndian);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audio_format);
        Mixer.Info[] lines = getDevices();
        //System.out.println(lines[1]);
        Mixer m = AudioSystem.getMixer(lines[1]);
        Line.Info[] lineInfos = m.getTargetLineInfo();
        Line line = m.getLine(lineInfos[0]);

        microphone = (TargetDataLine) AudioSystem.getLine(lineInfos[0]);
        microphone.open(audio_format);
        DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audio_format);
        speakers = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        speakers.open(audio_format);
        //System.out.println(samples.length);

        //writer.addVideoStream(videoStreamIndex, 0, videoCodec, width, height);
        new Thread(new Runnable() {
            @Override
            public void run() {
                File f = new File("D:/output.aac");
                f.setWritable(true);
                try {
                    f.createNewFile();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                IMediaWriter writer = ToolFactory.makeWriter(f.getPath());
                writer.setForceInterleave(false);
                writer.addAudioStream(1, 1, ICodec.ID.CODEC_ID_AAC, channels, (int) sampleRate);
                /*writer.addListener(new MediaToolAdapter(){
                    short prev = 0;
                    @Override
                    public void onAudioSamples(IAudioSamplesEvent event) {
                    ShortBuffer buffer = event.getAudioSamples().getByteBuffer().asShortBuffer();
                    for (int i = 0; i < buffer.limit(); ++i)
                        buffer.put(i, (short)((buffer.get(i) + prev)/2));
                        super.onAudioSamples(event);
                    }
                });
                        */
                int size = microphone.getBufferSize() / 10;

                byte[] data = new byte[size];
                recording = true;
                //short_samples = new short[size/sampleSize/8];
                short[] audioSamples = new short[size / (sampleSize / 8)];
                microphone.start();
                speakers.start();
                //test.
                while (recording) {
                    numBytesRead = microphone.read(data, 0, data.length);
                    long now = microphone.getMicrosecondPosition();
                    speakers.write(data, 0, numBytesRead);
                    audioSamples = toShort(data);
                    writer.encodeAudio(1, audioSamples, now, TimeUnit.MILLISECONDS);
                    writer.flush();
                }
                writer.close();
            }
        }) {
        }.start();
    }

    public short[] toShort(byte[] source) {
        short[] destination = new short[source.length / 2];
        int index = -1;
        for (int i = 0; i < destination.length; i++) {
            destination[i] = (short) ((source[(index+=1)] << 8)
                    | (source[index+=1] & 0xFF));
            //System.out.println(destination);
        }
        return destination;
    }

    public void stopRecording() {
        recording = false;
        speakers.drain();
        speakers.close();
        microphone.close();
        microphone.stop();
        microphone.close();
        System.out.println("lkjh");
    }

    private short lastInput = 0;

    private short twoPointMovingAverageFilter(short input) {
        short output = (short) ((input + lastInput) / 2);
        lastInput = input;
        return output;
    }

}

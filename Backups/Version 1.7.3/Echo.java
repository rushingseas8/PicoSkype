package pffft;

import java.util.*;
import javax.swing.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

/**
 * Just a tester that plays back what it hears.
 */
public class Echo {
    //static AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true);
    static AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, true);
    static TargetDataLine microphone;
    static AudioInputStream audioInputStream;
    static SourceDataLine sourceDataLine;

    static int numBytesRead;
    static int bytesRead = 0;

    //static int CHUNK_SIZE = 512;
    static int CHUNK_SIZE;

    static byte[] data; //The data written to and played
    
    static AudioVisualiser2 vis;

    public static void main(String[] args) {       
        JFrame frame = new JFrame();
        frame.setSize(640, 120);
        vis = new AudioVisualiser2();
        frame.add(vis);
        frame.setVisible(true);
        
        try {
            microphone = AudioSystem.getTargetDataLine(format);
            CHUNK_SIZE = microphone.getBufferSize() / 5;

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            data = new byte[CHUNK_SIZE];
            microphone.start();

            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            sourceDataLine.open(format);
            sourceDataLine.start();            

            //Write to data
            long time = System.currentTimeMillis();
            new Thread() {
                public void run() {
                    while(true) {   
                        write();
                        read();
                        try {
                            System.out.println("Estimated bandwidth (Kb/s): " + ((bytesRead / 128) / ((System.currentTimeMillis() - time) / 1000)));
                            System.out.println("Total bandwidth (Kb): " + (bytesRead/128));
                        } catch (Exception e) {}
                    }
                }
            }.start();

            // Block and wait for internal buffer of the
            // data line to empty.
            //sourceDataLine.drain();
            //sourceDataLine.close();
            //microphone.close();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private static void write() {
        numBytesRead = microphone.read(data, 0, CHUNK_SIZE);
        bytesRead = bytesRead + numBytesRead; 
        vis.setArray(data);
    }

    private static void read() {
        sourceDataLine.write(data, 0, data.length);        
        data = new byte[CHUNK_SIZE];
    }
}
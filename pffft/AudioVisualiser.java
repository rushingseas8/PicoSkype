package pffft;

import javax.swing.*;
import java.awt.*;

/**
 * Visualises an array of bytes; used by AudioGUI.
 * <p>
 * This is purely for fun.
 */
public class AudioVisualiser extends JPanel {
    private byte[] bytes;

    public AudioVisualiser() {
        super();
    }

    public void setArray(byte[] newArray) {
        this.bytes = newArray;
        repaint();
    }

    public byte[] getArray() {
        return bytes;
    }

    @Override
    public void paintComponent(Graphics g) {
        long time = System.currentTimeMillis();
        int width = getWidth();
        int height = getHeight();
        double xPixelsPerPoint = 4 * (double)width / bytes.length;
        double yPixelsPerPoint = 4 * (double)height / bytes.length;

        g.setColor(Color.BLACK);
        for(int i = 0; i < bytes.length - 1; i++) {
            g.drawLine((int)(i * xPixelsPerPoint), (int)(bytes[i] * yPixelsPerPoint) + height / 2,
                (int)((i+1) * xPixelsPerPoint), (int)(bytes[i+1] * yPixelsPerPoint) + height / 2);
        }
        System.out.println("Rendering took " + (System.currentTimeMillis() - time) + "ms.");
    }

    /**
     * Displays an audiovisualiser with sample data.
     */
    public static void main() {
        JFrame frame = new JFrame("Test");
        frame.setSize(640, 480);
        AudioVisualiser vis = new AudioVisualiser();

        byte[] test = new byte[440];
        for(int i = 0; i < test.length; i++) {
            test[i] = (byte)((Math.random() * 256) - 128);
        }
        vis.setArray(test);
        vis.setSize(480, 240);

        frame.add(vis);
        frame.setVisible(true);

        while(true) {
            test = new byte[440];
            for(int i = 0; i < test.length; i++) {
                test[i] = (byte)((Math.random() * 256) - 128);
            }
            vis.setArray(test);
            //try{Thread.sleep(50);}catch(Exception e){}
        }
    }
}
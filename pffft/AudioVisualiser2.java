package pffft;

import javax.swing.*;
import java.awt.*;

/**
 * Visualises an array of bytes; used by AudioGUI.
 * <p>
 * This is purely for fun.
 * <p>
 * Uses a sine wave-based drawing style.
 */
public class AudioVisualiser2 extends JPanel {
    private byte[] bytes;
    private double offset;
    private double intensity;
    private double[] averages;
    
    public double yScale;
    public double xScale;
    public double tScale;
    public int accuracy;
    public int boost;
    public boolean smooth;
    public boolean adapt;
    public Color color;

    /**
     * Creates a new visualiser with default parameters.
     * @param yScale is used by the program and sets the amplitude (default 1)
     * @param xScale defines the period as 2pi/xScale. (default 50)
     * @param tScale the speed at which we move across the screen (default 15)
     * @param accuracy defines the amount of samples to take as 1/accuracy (default 8)
     * @param boost how much we boost the signal by; multiplies yScale by boost (default 6)
     * @param smooth whether or not we smooth the result (default true)
     * @param adapt whether or not we adapt the value of boost dynamically (default false)
     */
    public AudioVisualiser2() {
        super();
        averages = new double[10];
        
        yScale = 1;
        xScale = 50;
        tScale = 15;
        accuracy = 1;
        boost = 6;
        smooth = true;
        adapt = true;
        color = Color.WHITE.darker();
        
        //Make the background transparent
        setOpaque(false);
        setBackground(new Color(0,0,0,0));
    }

    /**
     * Set the array of bytes to render.
     * This automatically updates the variables needed and draws the new array.
     */
    public void setArray(byte[] newArray) {
        this.bytes = newArray;

        //Intensity is the average intensity of the bytes.
        intensity = 0;
        for(int i = 0; i < newArray.length; i+=accuracy) 
            intensity+=Math.abs(newArray[i]) * boost;
        
        intensity/=newArray.length/accuracy;
        //System.out.println("Intensity:" + intensity + " Boost: " + boost);
        
        double avg = 0;
        for(int i = 0; i < averages.length - 1; i++) {
            avg += averages[i];
            averages[i] = averages[i+1];
        }
        avg+= averages[9] = intensity;
        avg/=10;
        
        double diff = yScale - (intensity / 256);
        if (smooth) 
            yScale = (yScale + intensity/256) / 2; //Set it equal to the average of the last intensity and old one
        else 
            yScale = intensity / 256; //No smoothing

        //Prevents the scale from going off the edges; adaptive boosting done here.
        if(yScale > .8) { if(adapt) boost /= 2; yScale = 1; }
        
        //If we're quiet, increase boost.
        if(adapt) if(yScale < .2 && boost > 6) boost *= 2; 

        repaint();
    }

    /**
     * Returns the array we're currently drawing.
     */
    public byte[] getArray() {
        return bytes;
    }

    @Override
    public void paint(Graphics g) {
        //Make the background transparent
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, getWidth(), getHeight());
        
        //Make sure we don't draw when there's nothing to draw.
        if(bytes == null) return;
        
        int width = getWidth();
        int period = (int)(2 * Math.PI / xScale);
        int height = getHeight();

        //Color c = new Color();
        g.setColor(color);
        for(int i = 0; i < getWidth() - 1; i++) {
            g.drawLine(i, (height / 2) + (int)(yScale * height * Math.sin(2 * (i+offset) * Math.PI / 180 ) / 2),
                i+1, (height / 2) + (int)(yScale * height * Math.sin(2 * (i+1+offset) * Math.PI / 180 ) / 2));
        }

        offset+=tScale;
    }

    /**
     * Displays an audiovisualiser with sample (random) data.
     */
    public static void main() {
        JFrame frame = new JFrame("Test");
        frame.setSize(640, 480);
        AudioVisualiser2 vis = new AudioVisualiser2();
        vis.setSize(new Dimension(640, 240));

        frame.add(vis);
        frame.setVisible(true);
        byte[] temp = new byte[440];
        while(true) {
            for(int i = 0; i < temp.length; i++) 
                temp[i] = (byte)(Math.random() * 256);
            vis.setArray(temp);
            //try{Thread.sleep(10);}catch(Exception e){}
        }
    }
}
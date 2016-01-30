import javafx.application.*;
import javafx.scene.web.*;
import javafx.scene.*;
import javafx.embed.swing.*;
import javafx.scene.media.*;
import javafx.scene.media.MediaPlayer.*;
import javafx.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

/**
 * Another cool inner class that plays audio in a small container.
 */
public class AudioPanel extends JFXPanel {
    public Media hit;
    public MediaPlayer player;

    private JSlider time, volume;

    public AudioPanel(File f) {
        super();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        String bip = f.toURI().toString();
        hit = new Media(bip);
        player = new MediaPlayer(hit);

        JButton pause = new JButton("►");
        pause.setPreferredSize(new Dimension(50, 50));
        pause.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent a) {
                    if (player.getStatus() == Status.PLAYING) {
                        player.pause();
                        pause.setText("►");
                    } else {
                        player.play();
                        pause.setText("||");
                    }
                }
            });
        add(pause);        

        //A slider for the time
        time = new JSlider();
        time.setMaximum((int)(hit.getDuration().toMillis()));

        //Constantly updates the slider based on time
        new Thread() {
            public void run() {
                while(true) {
                    int i = (int)player.getCurrentTime().toMillis();
                    SwingUtilities.invokeLater(new Runnable(){public void run(){time.setValue(i);}});
                    try {Thread.sleep(1000);}catch(Exception e){}
                }
            }
        }.start();

        //Listens for drag events to seek ahead
        time.addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent m) {
                    player.seek(new Duration(time.getValue()));
                }
            });
        time.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent m) {
                    player.seek(new Duration(time.getValue()));
                }
            });
        add(time);

        player.setVolume(.5); //Default volume level
        volume = new JSlider();
        volume.setValue(50);
        volume.setMaximum(100);
        volume.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent c) {
                    player.setVolume((volume.getValue() / 100.0));
                }
            });
        add(volume);

        setSize(400, 50);
        setPreferredSize(new Dimension(400, 50));
        setMaximumSize(new Dimension(400, 50));
        //player.play(); //Plays the song
    }
}
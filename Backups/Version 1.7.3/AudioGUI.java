package pffft;

import javax.swing.*;
import javax.swing.event.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

/**
 * The graphical interface used by the entire audio.
 * Contains buttons for muting, settings, hiding chat, and so on.
 */
public class AudioGUI extends JPanel {
    //For program interconnectivity.
    public Client client;

    //Needed for linking the two together.
    public AudioNetwork network;

    //GUI elements
    public JButton settings;
    public JButton hideChat;
    public JButton muteMic;

    public AudioVisualiser2 yourVis;
    public AudioVisualiser2 theirVis;

    //GUI images 
    private Image settingsImage;
    private Image hideChatImage;
    private Image microphoneImage;
    private Image microphoneMutedImage;
    private Image backgroundImage;

    private String debug;

    public int guiSize;

    public boolean muted;

    public AudioGUI() {
        //Call JPanel instantiation
        super();

        //Panel layout.
        setLayout(null);
        setSize(640, 480);
        addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent c) {
                    //Auto-position the gui elements.
                    settings.setLocation(getWidth() - settings.getWidth(), getHeight() - settings.getHeight());
                    hideChat.setLocation(0, getHeight() - hideChat.getHeight());
                    muteMic.setBounds((getWidth() / 2) - (muteMic.getWidth() / 2), getHeight() - muteMic.getHeight(), muteMic.getWidth(), muteMic.getHeight());
                    yourVis.setBounds(0, (getHeight() / 4) - (getHeight() / 8), getWidth(), Math.min(getHeight() / 4, 64));
                    theirVis.setBounds(0, (5 * getHeight() / 8) - (getHeight() / 8), getWidth(), Math.min(getHeight() / 4, 64));
                }
            });

        guiSize = 32;

        //Load those images in.

        ClassLoader loader = this.getClass().getClassLoader(); //Use a classloader to read in images from the local jar directory.

        String settingsImageURL = "pico" + File.separator + "images" + File.separator + "settingsGear.png";
        ImageIcon settingsImageIcon = new ImageIcon(loader.getResource(settingsImageURL));
        settingsImage = settingsImageIcon.getImage();
        settingsImageIcon = new ImageIcon(settingsImage.getScaledInstance(guiSize, guiSize, Image.SCALE_FAST));

        String hideChatImageURL = "pico" + File.separator + "images" + File.separator + "hideChatArrow.png";
        ImageIcon hideChatImageIcon = new ImageIcon(loader.getResource(hideChatImageURL));
        hideChatImage = hideChatImageIcon.getImage();
        hideChatImageIcon = new ImageIcon(hideChatImage.getScaledInstance(guiSize, guiSize, Image.SCALE_FAST));

        String microphoneImageURL = "pico" + File.separator + "images" + File.separator + "microphone.png";
        ImageIcon microphoneImageIcon = new ImageIcon(loader.getResource(microphoneImageURL));
        microphoneImage = microphoneImageIcon.getImage();
        microphoneImageIcon = new ImageIcon(microphoneImage.getScaledInstance(guiSize, guiSize, Image.SCALE_FAST));

        String microphoneMutedImageURL = "pico" + File.separator + "images" + File.separator + "microphoneMuted.png";
        ImageIcon microphoneMutedImageIcon = new ImageIcon(loader.getResource(microphoneMutedImageURL));
        microphoneMutedImage = microphoneMutedImageIcon.getImage();        
        microphoneMutedImageIcon = new ImageIcon(microphoneMutedImage.getScaledInstance(guiSize, guiSize, Image.SCALE_FAST));

        String backgroundImageURL = "pico" + File.separator + "images" + File.separator + "background.png";
        try {
            java.net.URL url = loader.getResource(backgroundImageURL);
            if(url != null)
                backgroundImage = javax.imageio.ImageIO.read(url); 
        } catch (java.io.IOException i) {}

        //Settings GUI button.
        settings = new JButton(settingsImageIcon);
        settings.setSize(guiSize, guiSize);
        settings.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent a) {

                }
            });
        settings.setBounds(getWidth() - settings.getWidth(), getHeight() - settings.getHeight(), settings.getWidth(), settings.getHeight());
        add(settings);

        //Chat toggle button.
        hideChat = new JButton(hideChatImageIcon);
        hideChat.setSize(guiSize, guiSize);
        hideChat.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent a) {

                }
            });
        hideChat.setBounds(0, getHeight() - hideChat.getHeight(), hideChat.getWidth(), hideChat.getHeight());
        add(hideChat);

        //Whether or not we're muted.
        muted = false;

        //Button for toggling microphone mute.
        muteMic = new JButton(microphoneImageIcon);
        muteMic.setSize(guiSize, guiSize);
        muteMic.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent a) {
                    //Toggle muted setting and update the gui icon.
                    muted = !muted;
                    network.setMute(muted);
                    ((ImageIcon)muteMic.getIcon()).setImage(
                        !muted ? microphoneImage.getScaledInstance(guiSize, guiSize, Image.SCALE_DEFAULT) :
                        microphoneMutedImage.getScaledInstance(guiSize, guiSize, Image.SCALE_DEFAULT));
                    client.logger.addText("Muted state is now: " + muted + "\n");
                }
            });
        muteMic.setBounds((getWidth() / 2) - (muteMic.getWidth() / 2), getHeight() - muteMic.getHeight(), muteMic.getWidth(), muteMic.getHeight());
        add(muteMic);        

        //Your visualiser!
        yourVis = new AudioVisualiser2();
        yourVis.setBounds(0, (getHeight() / 4) - (yourVis.getHeight() / 2), getWidth(), getHeight() / 4);
        add(yourVis);

        //Partner's visualiser!
        theirVis = new AudioVisualiser2();
        theirVis.setBounds(0, (5 * getHeight() / 8) - (theirVis.getHeight() / 2), getWidth(), getHeight() / 4);
        add(theirVis);

        setVisible(true);
    }

    /**
     * Initialise this AudioGUI object with a Client reference.
     */
    public void init(Client c) {
        this.client = c;
        this.network = c.audioNetwork;
    }

    @Override
    public void paintComponent(Graphics g) {
        if(backgroundImage == null) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
        } else {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), null);
        }	
    }

    /**
     * Changes the size of the GUI elements to the given size, in pixels.
     * The default value is 32.
     */
    public void setGuiSize(int to) {
        //Set the variable locally
        this.guiSize = to;

        //Update component size and position
        settings.setBounds(getWidth() - settings.getWidth(), getHeight() - settings.getHeight(), to, to);
        hideChat.setBounds(0, getHeight() - hideChat.getHeight(), to, to);
        muteMic.setBounds((getWidth() / 2) - (muteMic.getWidth() / 2), getHeight() - muteMic.getHeight(), to, to);

        //No need to do any image resizing, as our size is 0.
        if(to == 0) return; 

        //Resize the image textures.
        ((ImageIcon)settings.getIcon()).setImage(settingsImage.getScaledInstance(to, to, Image.SCALE_DEFAULT));
        ((ImageIcon)hideChat.getIcon()).setImage(hideChatImage.getScaledInstance(to, to, Image.SCALE_DEFAULT));
        if(!muted) 
            ((ImageIcon)muteMic.getIcon()).setImage(microphoneImage.getScaledInstance(to, to, Image.SCALE_DEFAULT));
        else 
            ((ImageIcon)muteMic.getIcon()).setImage(microphoneMutedImage.getScaledInstance(to, to, Image.SCALE_DEFAULT));
    }

    public static void main() {
        JFrame frame = new JFrame("Test");
        frame.setSize(640, 480);
        AudioGUI audio = new AudioGUI();
        frame.add(audio);
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 128, audio.guiSize);
        slider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent c) {
                    audio.setGuiSize(slider.getValue());
                }
            });
        frame.add(slider, "South");
        frame.setVisible(true);
    }
}
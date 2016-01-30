package pffft; 

import javafx.application.*;
import javafx.scene.web.*;
import javafx.scene.*;
import javafx.embed.swing.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A small player that plays youtube videos.
 */
public class YoutubePlayer extends JFXPanel {
    private String url;
    private int width, height;
    private int type;
    private WebView view;

    /**
     * A YoutubePlayer with a given URL and a type representing size.
     * @param type A parameter for the size of the viewer.
     * -1 = custom, 0 = audio only, 1 = 144p, 2 = 240p, 3 = 360p, 4 = 480p, 5 = 720p, 6 = 1080p
     */
    public YoutubePlayer(String url, int type) {
        this(url, 0, 0, type);
    }

    /**
     * A YoutubePlayer with a given URL and width and height values.
     */
    public YoutubePlayer(String url, int width, int height) {
        this(url, width, height, -1);
    }

    /**
     * A YoutubePlayer with all of the parameters.
     * Note that width and height are only used if type < 0 || type > 6.
     */
    public YoutubePlayer(String url, int width, int height, int type) {
        this.url = url;
        this.type = type;

        switch(type) {
            case 0: this.width = this.height = 0; break;              //audio
            case 1: this.width = 192; this.height = 144; break;       //144p
            case 2: this.width = 320; this.height = 240; break;       //240p
            case 3: this.width = 640; this.height = 360; break;       //360p
            case 4: this.width = 854; this.height = 480; break;       //480p
            case 5: this.width = 1280; this.height = 720; break;      //720p
            case 6: this.width = 1920; this.height = 1080; break;     //1080p
            default: this.width = width; this.height = height; break; //custom
        }

        Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    initFX();
                }
            });

        setSize(new Dimension(this.width + 18, this.height + 40));
        setPreferredSize(new Dimension(this.width + 18, this.height + 40));
        setMaximumSize(new Dimension(this.width + 18, this.height + 40)); 

        InputMap map = getInputMap(JComponent.WHEN_FOCUSED);
        map.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, 0), "newlink");
        map.put(KeyStroke.getKeyStroke("pressed ."), "right");
        map.put(KeyStroke.getKeyStroke("pressed ,"), "left");
        ActionMap amap = getActionMap();
        amap.put("newlink", new AbstractAction() 
            {
                public void actionPerformed(ActionEvent a) {
                    JFrame f = new JFrame("Enter new link");
                    f.setSize(240, 80);
                    f.setLocationRelativeTo(frame);
                    JTextField text = new JTextField();
                    text.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent a) {
                                Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            String id = (text.getText()).substring((text.getText()).indexOf("v=") + 2);
                                            System.out.println("Parsed id: " + id);

                                            WebView view2 = new WebView();
                                            view2.getEngine().loadContent(
                                                "<iframe width=\"" + width + "\" height=\"" + height + "\"" +
                                                " src=\"http://www.youtube.com/embed/" + id +"\"" + 
                                                " frameborder=\"0\" allowfullscreen></iframe>");

                                            Group root = new Group();
                                            Scene scene = new Scene(root, width, height);

                                            System.out.println(view2);
                                            if(view2 != null) 
                                                root.getChildren().add(view2);
                                            

                                            setScene(scene);
                                            
                                            f.dispose();
                                        }
                                    });
                            }
                        });
                    f.add(text);
                    f.setVisible(true);
                }
            });
    }

    private void setURL(String url) {
        this.url = url;
    }

    private void initFX() {
        Scene scene = createScene();
        setScene(scene);
    }

    private Scene createScene() {
        String id = url.substring(url.indexOf("v=") + 2);

        String content = "<iframe width=\"" + width + "\" height=\"" + height + "\"" +
            " src=\"http://www.youtube.com/embed/" + id +"\"" + 
            " frameborder=\"0\" allowfullscreen></iframe>";

        view = new WebView();
        view.getEngine().loadContent(content);

        Group root = new Group();
        Scene scene = new Scene(root, width, height);

        if(view != null)
            root.getChildren().add(view);

        return scene;
    }

    private static JFrame frame;
    public static void main() {
        frame = new javax.swing.JFrame("Test");
        frame.setSize(656, 400);
        frame.add(new YoutubePlayer("https://www.youtube.com/watch?v=gqELqRCnW6g", 3));
        frame.setVisible(true);
    }
}
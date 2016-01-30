import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;

/**
 * A cool little class that takes an image as a parameter, and lets you resize it at will.
 * @version 2.0 (added web support)
 */
public class DraggerPanel extends JPanel {
    private BufferedImage image; //The image we're drawing
    private Point startingPoint = null; //Starting point for resizing
    private boolean first = true; //Is this the first drag?
    private boolean lockedDrag; //If true, then lock x and y to be proportionally dragged.
    private JPopupMenu menu;
    private JMenuItem view, save;

    public DraggerPanel(BufferedImage image, boolean lockedDrag) {
        this(image, lockedDrag, 0, 0);
        //Set appropriate size based on image size.
        if(image != null) { 
            setSize(new Dimension(image.getWidth(), image.getHeight())); 
            setPreferredSize(new Dimension(image.getWidth(), image.getHeight())); 
            setMaximumSize(new Dimension(image.getWidth(), image.getHeight())); 
        }
        else {
            setSize(new Dimension(200,200));
            setPreferredSize(new Dimension(200,200));
            setMaximumSize(new Dimension(200,200));
        }
    }  

    /**
     * Used for getting images from websites.
     */
    public DraggerPanel(String url, boolean lockedDrag, int width, int height) {
        this(url, null, lockedDrag, width, height);
    }

    //Resizes to a max of 320x320
    public DraggerPanel(String url) {
        this(url, null, false, 0, 0);
        if(image != null) { 
            double ratio = (double)image.getWidth() / image.getHeight();
            if (image.getWidth() > image.getHeight()) {
                setSize(new Dimension(320, (int)(320 * ratio))); 
                setPreferredSize(new Dimension(320, (int)(320 * ratio))); 
                setMaximumSize(new Dimension(320, (int)(320 * ratio))); 
            } else {
                setSize(new Dimension((int)(320 / ratio), 320)); 
                setPreferredSize(new Dimension((int)(320 / ratio), 320)); 
                setMaximumSize(new Dimension((int)(320 / ratio), 320));                     
            }
        }
        else {
            setSize(new Dimension(200,200));
            setPreferredSize(new Dimension(200,200));
            setMaximumSize(new Dimension(200,200));
        }            
    }

    public DraggerPanel(BufferedImage image, boolean lockedDrag, int inX, int inY) {
        this(null, image, lockedDrag, inX, inY);
    }

    /**
     * @param url An optional string URL that determines where the image comes from. url and image can't both be not null.
     */
    private DraggerPanel(String url, BufferedImage image, boolean lockedDrag, int inX, int inY) {
        super();
        this.image = image;
        this.lockedDrag = lockedDrag;

        //Get image from a website! 
        if(url != null) {
            String sauce = "";
            if (url.contains("imgur.com")) //This is so that we can add more websites later.
                sauce = getImgurSourceURL(url);

            final String source = sauce;
            try {
                URL URL = new URL(source);
                this.image = javax.imageio.ImageIO.read(URL);
            } catch (IOException i) {
                i.printStackTrace();
            }
        }

        //The listener for dragging events.
        addMouseMotionListener(new MouseMotionListener() {
                private int inWidth = 0, inHeight = 0; //Initial height and width values
                private double ratio = 0; //Ratio of height to width for locked drag.

                public void mouseDragged(MouseEvent m) {
                    if (first) { //If we're first, record initial position.
                        startingPoint = m.getPoint();
                        first = false;
                        inWidth = getWidth();
                        inHeight = getHeight();
                        ratio = (double)inHeight / inWidth;
                    } else { //Otherwise, change the size of the window.
                        if (!lockedDrag) {
                            int w = (int)startingPoint.getX() - m.getX();
                            int h = (int)startingPoint.getY() - m.getY();
                            setSize(new Dimension(Math.abs(inWidth - w), Math.abs(inHeight - h)));
                            setPreferredSize(new Dimension(Math.abs(inWidth - w), Math.abs(inHeight - h)));
                            setMaximumSize(new Dimension(Math.abs(inWidth - w), Math.abs(inHeight - h)));
                        } else {
                            int w = (int)startingPoint.getX() - m.getX();
                            int h = (int)((double)ratio * w);
                            setSize(new Dimension(Math.abs(inWidth - w), Math.abs(inHeight - h)));
                            setPreferredSize(new Dimension(Math.abs(inWidth - w), Math.abs(inHeight - h)));
                            setMaximumSize(new Dimension(Math.abs(inWidth - w), Math.abs(inHeight - h)));
                        }
                    }
                    repaint();
                }

                public void mouseMoved(MouseEvent m){}
            });

        //Lets us know when you're not dragging anymore.
        addMouseListener(new MouseAdapter(){
                public void mousePressed(MouseEvent m) {
                    if (m.getButton() == MouseEvent.BUTTON3) {
                        createPopupMenu(m.getPoint());
                    }
                }

                public void mouseReleased(MouseEvent m){first = true;}
            });

        //Set appropriate size.
        setSize(new Dimension(inX, inY)); 
        setPreferredSize(new Dimension(inX, inY)); 
        setMaximumSize(new Dimension(inX, inY)); 

        //We're live, baby.
        setVisible(true);        

        //Create some variables now so they're not done dynamically later
        //Todo: Add a private inner class extending actionlistener to make this faster
        menu = new JPopupMenu();
        view = new JMenuItem("View full size");
        view.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent a) {
                    createViewer();
                }
            });

        //Make this more intuitive later, maybe save to a local downloads folder with "view" and "open enclosing folder" options, like in Skype.
        save = new JMenuItem("Save image as");
        save.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent a) {
                    promptSave();
                }
            });
        menu.add(view);
        menu.add(save);
    }

    public void paintComponent(Graphics g) {
        if (image == null) super.paintComponent(g);
        else g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
    }

    private void createPopupMenu(Point p) {
        menu.show(this, p.x, p.y);
    }

    private void createViewer() {
        JFrame frame = new JFrame("Image viewer");
        JPanel view = new JPanel() {
                public void paintComponent(Graphics g) {
                    g.drawImage(image, 0, 0, null);
                }

                public Dimension getPreferredSize() {
                    return new Dimension(image.getWidth(), image.getHeight());
                }
            };
        JScrollPane scroll = new JScrollPane(view);
        scroll.getHorizontalScrollBar().setUnitIncrement(10); //Slightly faster scrolling
        scroll.getVerticalScrollBar().setUnitIncrement(10); //Slightly faster scrolling
        frame.add(scroll);
        frame.setSize(480, 480);
        frame.setVisible(true);
    }

    private void promptSave() throws IllegalComponentStateException { //Sometimes JFileChooser throws a random exception, just ignore it.
        JFileChooser j = new JFileChooser();
        if (j.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            java.io.File f = j.getSelectedFile();

            //Make sure we're always .png
            if (!f.getName().endsWith(".png")) 
                f=new java.io.File(f.getAbsolutePath() + ".png");

            try {
                javax.imageio.ImageIO.write(image, "png", f);
            } catch (java.io.IOException i) {
                //who cares?
            }
        }
    }

    /**
     * For a given imgur link, extract the source image and return its address.
     */
    private String getImgurSourceURL(String imgurURL) {
        String source = "http:";
        try {
            URL url = new URL(imgurURL);
            URLConnection urlConnection = ((URLConnection)url.openConnection());

            InputStreamReader inputStream = new InputStreamReader(urlConnection.getInputStream());
            BufferedReader reader = new BufferedReader(inputStream);

            String htmlText = "";
            String wantedLine = "";
            while ((htmlText = reader.readLine()) != null) {
                if (htmlText.contains("src=\"") && htmlText.contains("i.imgur.com")) {
                    System.out.println(htmlText);
                    int index = htmlText.indexOf("\"") + 1;
                    source += htmlText.substring(index, htmlText.indexOf("\"", index));
                    break;
                }
            }
        } catch (Exception E) {
            E.printStackTrace();
        }
        System.out.println("Imgur source found: " + source);
        return source;
    }  
}
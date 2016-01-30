package pffft; 

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;

/**
 * An Object that represents the graphical interface of a Client.
 */
public class ClientGUI {
    //For program interconnectivity purposes.
    /**
     * A reference to the primary Client Object; used for its connections to the other primary program variables (network, logger, etc).
     */
    public Client client;

    //GUI instance variables
    /**
     * The main JFrame used by the program's GUI.
     */
    public JFrame frame;

    /**
     * A split pane that divides the audio and text panels.
     */
    public JSplitPane split;

    /**
     * The top panel that is used for audio information.
     */
    public AudioGUI audioPanel;

    /**
     * The Panel that holds everything related to the text client.
     */
    public JPanel textPanel;

    /**
     * The text client's primary text pane; logging and output is done on this pane.
     */
    public JTextPane text;

    /**
     * The text area used for entering text. 
     */
    public JTextArea field;   

    /**
     * Scroll panes used for scrolling through /text/ and /field/.
     */
    public JScrollPane textScroll, fieldScroll;

    /**
     * When resizing, the split pane will try to stay at this position.
     * Accepts values from 0 to 1.0.
     * The default value is 0.6, where the top gets 60% of the view.
     */
    public double defaultSplitPosition;

    //Used for autocompleting.
    private String[] commandsList;// = new String[]{"code","connect","define","help","new","user"}; 

    public ClientGUI() {
        long time = System.currentTimeMillis();

        //Main frame
        frame = new JFrame("PicoSkype BETA v1.8");
        frame.setSize(550, 480);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addComponentListener(new GUIResizeListener());

        //Split pane setup
        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);

        //Chat client 
        textPanel = new JPanel();
        textPanel.setLayout(new java.awt.BorderLayout());

        //Main text pane
        text = new JTextPane();
        text.setEditable(false); 
        
        //Text area for inputting text.
        field = new JTextArea();
        field.setRows(2);
        field.setLineWrap(true);

        //Set up key bindings.
        setupBindings();

        //Scroll panes for both text components.
        textScroll = new JScrollPane(text);
        fieldScroll = new JScrollPane(field);

        //Add all components to the chat panel
        textPanel.add(textScroll, "Center");
        textPanel.add(fieldScroll, "South");

        //Add all components to the split pane
        split.add(new JPanel()); //Temporary JPanel; will be replaced with audio
        split.add(textPanel); //Chat on bottom

        //Add the split pane and set visible
        frame.add(split);
        //frame.add(textPanel);
        frame.setVisible(true);

        //Variable for default split pane position; 60% space goes to top.
        defaultSplitPosition = .6;

        //Do any post-visible operations.
        field.requestFocus();
        split.setDividerLocation(defaultSplitPosition);
        split.resetToPreferredSizes();

        System.out.println("Did GUI setup in " + (System.currentTimeMillis() - time) + "ms.\n");
    }

    /**
     * Set up the key bindings for all of the components of the GUI.
     * A helper method used to reduce clutter in the instantiation method.
     */
    private void setupBindings() {
        InputMap input = null; ActionMap action = null;

        //Input text area.
        input = field.getInputMap(JComponent.WHEN_FOCUSED);
        action = field.getActionMap();

        //Special enter action
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "newline");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK), "newline");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "newline");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.META_DOWN_MASK), "newline");
        action.put("newline", new TextAreaSpecialEnterAction());

        //Regular enter action
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        action.put("enter", new TextAreaEnterAction());

        //Tab action
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "tab");
        action.put("tab", new TextAreaTabAction());

        //Global keybindings.
        input = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        action = frame.getRootPane().getActionMap();

        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.META_DOWN_MASK), "options");
        action.put("options", new FrameOptionsAction());
    }

    public void init(Client c) {
        this.client = c;
        if(c.audioGUI != null) {
            this.split.setTopComponent(c.audioGUI);
        }
        this.split.setDividerLocation(defaultSplitPosition);
    }

    public static void main() {
        new ClientGUI();
    }

    /**
     * Appends the text area's contents to the main text pane, and parses input as needed.
     */
    private class TextAreaEnterAction extends AbstractAction {
        public void actionPerformed(ActionEvent a) {
            //Clears the field and gets its data
            String s = field.getText();
            field.setText("");

            //If the length of the string is super long, delegate this to another thread to stop freezing.
            if(s.length() > 500) {
                new Thread() {
                    public void run() {
                        client.parser.parseLocal(s); //Checks for any commands & send over!             
                        field.setRows(2);
                        fieldScroll.setMinimumSize(field.getPreferredSize());
                        frame.getContentPane().validate();
                        return;
                    }
                }.start();
            } else {
                client.parser.parseLocal(s); //Checks for any commands & send over!             
                field.setRows(2);
                fieldScroll.setMinimumSize(field.getPreferredSize());
                frame.getContentPane().validate();
            }
        }
    }

    /**
     * Upon any enter with modifiers (ie, shift+enter, ctrl+enter), only add a newline and update
     * the size of the text area as needed. Do not submit the text.
     */
    private class TextAreaSpecialEnterAction extends AbstractAction {
        public void actionPerformed(ActionEvent a) {
            field.append("\n");
            fieldScroll.setMinimumSize(field.getPreferredSize());
            frame.getContentPane().validate();
        }
    }

    /**
     * On a tab press, autocomplete a command (if possible). Currently unfinished, and will only 
     * autocomplete one time. Planning to add a feature to cycle through possible options.
     */
    private class TextAreaTabAction extends AbstractAction {
        public void actionPerformed(ActionEvent a) {
            String fi = field.getText();
            if (fi.length() == 0 || fi.charAt(0) != '/') return;
            fi = fi.substring(1);

            for (int i = 0; i < commandsList.length; i++) {
                String st = commandsList[i];
                if (st.startsWith(fi))
                    field.setText("/" + st + " ");
            }
        }
    }

    /**
     * Opens up an options menu.
     */
    private class FrameOptionsAction extends AbstractAction {
        public void actionPerformed(ActionEvent a) {
            new OptionFrame(frame);
        }
    }

    /**
     * Listens and responds to any Component events.
     * Presently resets split position when GUI resizes.
     */
    private class GUIResizeListener extends ComponentAdapter {
        @Override
        public void componentResized(ComponentEvent c) {
            split.setDividerLocation(defaultSplitPosition);
        }
    }

    /**
     * Shuts the program down properly when it closes.
     */
    private class GUICloseListener extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent w) {
            if (client == null) return;
            java.io.File working = FileLoader.load("pico/working");
            working.delete();
        }
    }
}
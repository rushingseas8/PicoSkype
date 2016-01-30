import javax.swing.*;
import java.awt.event.*;

/**
 * An Object that represents the graphical interface of a Client.
 */
public class ClientGUI {
    //For program interconnectivity purposes.
    public ClientLogger logger;
    public ClientParser parser;

    //GUI instance variables
    public JFrame frame;
    private JTextPane text;
    public TextDNDListener textDnd;
    private JTextArea field;   
    private JScrollPane textScroll, fieldScroll;

    //Used for autocompleting.
    private String[] commandsList = new String[]{"code","connect","define","help","new","user"}; 

    public ClientGUI() {
        //Main frame
        frame = new JFrame("PicoSkype v1.6");
        frame.setSize(550, 480);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Main text pane
        text = new JTextPane();
        text.setEditable(false);  

        //Text area for inputting text.
        field = new JTextArea();
        field.setRows(2);
        field.setLineWrap(true);

        //Scroll panes for both text components.
        textScroll = new JScrollPane(text);
        fieldScroll = new JScrollPane(field);

        //Set up key bindings.
        setupBindings();

        //Add all components to the main frame, and set visible.
        frame.add(textScroll, "Center");
        frame.add(fieldScroll, "South");
        frame.setVisible(true);
        field.requestFocus();
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

    /**
     * Used by NetworkClient.
     */
    public JTextPane getTextPane() {
        return text;
    }

    public static void main() {
        new ClientGUI();
    }

    public void setClientLogger(ClientLogger c) {
        this.logger = c;
    }

    public void setClientParser(ClientParser c) {
        this.parser = c;
    }

    /**
     * Appends the text area's contents to the main text pane, and parses input as needed.
     */
    private class TextAreaEnterAction extends AbstractAction {
        public void actionPerformed(ActionEvent a) {
            //Clears the field and gets its data
            String s = field.getText();
            field.setText("");

            parser.parse(s, 0); //Checks for any commands & send over!             
            field.setRows(2);
            fieldScroll.setMinimumSize(field.getPreferredSize());
            frame.getContentPane().validate();
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
}
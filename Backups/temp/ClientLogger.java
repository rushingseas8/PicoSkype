import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class ClientLogger {
    private JTextPane logPane;

    public ClientLogger(JTextPane t) {
        logPane = t;
    }

    /**
     * Adds the String s (with a black color) to the end of the main text pane.
     */
    public void addText(String s) {
        addText(s, Color.BLACK);
    }

    /**
     * Adds the String s, using the Color c, to the end of the main text pane.
     */
    public void addText(String s, Color c) {
        SimpleAttributeSet set = new SimpleAttributeSet();
        set.addAttribute(StyleConstants.Foreground, c);

        addText(s, set);
    }

    /**
     * Add the String s to the end of the main text pane, using the attributes from the set a.
     */
    public void addText(String s, SimpleAttributeSet a) {
        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    try {
                        logPane.getStyledDocument().insertString(logPane.getStyledDocument().getLength(), s, a);
                    } catch (BadLocationException b) {
                        System.out.println("Error: In addText.");
                        b.printStackTrace();
                    }
                }
            }
        );
    }

    /**
     * Adds the JComponent 'c' to the main text pane.
     */
    public void addComponent(JComponent c) {
        logPane.insertComponent(c);
    }
}
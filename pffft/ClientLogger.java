package pffft; 

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

/**
 * An Object that allows for text to be added. This was moved away from the GUI's role because
 * logging text and adding Components is used by many classes in the program; it made sense to
 * delegate the task to a separate class instead of having differing implementations in every
 * class of the program.
 */
public class ClientLogger {
    private JTextPane logPane;

    /**
     * Creates a logger that uses the GUI's JTextPane to add text and components to.
     */
    public ClientLogger() {}
    
    /**
     * Passes the gui's JTextPane to this class.
     */
    public void init(Client c) {
        this.logPane = c.gui.text;
    }

    /**
     * Adds the a String (in the color Black) to the end of the main text pane. 
     * Please note that this does NOT append a newline to the end of the String.
     * @param s The String to be added
     */
    public void addText(String s) {
        addText(s, Color.BLACK);
    }

    /**
     * Adds the String s, using the Color c, to the end of the main text pane.
     * @param s The String to be added
     * @param c The Color that the user wants this String to be
     */
    public void addText(String s, Color c) {
        SimpleAttributeSet set = new SimpleAttributeSet();
        set.addAttribute(StyleConstants.Foreground, c);

        addText(s, set);
    }

    /**
     * Add the String s to the end of the main text pane, using the attributes from the set a.
     * @param s The String to be added
     * @param a The SimpleAttributeSet that the user wants the String to be added using.
     */
    public void addText(String s, SimpleAttributeSet a) {
        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    try {
                        logPane.getStyledDocument().insertString(logPane.getStyledDocument().getLength(), s, a);
                    } catch (BadLocationException b) {
                        System.out.println("Error: When trying to add text.");
                        b.printStackTrace();
                    }
                }
            }
        );
    }

    /**
     * Adds the JComponent 'c' to the main text pane. This component will be added to the end,
     * determined by setting the caret position of the main JTextPane to the length of the 
     * JTextPane's styled document.
     * @param c The JComponent to be added
     */
    public void addComponent(JComponent c) {
        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    logPane.setCaretPosition(logPane.getStyledDocument().getLength());
                    logPane.insertComponent(c);
                }
            }
        );
    }
}
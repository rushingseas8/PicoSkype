package pffft;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

/**
 * A class that handles errors in a user-friendly way. Only fatal errors should be used here.
 */
public class ErrorFrame extends JFrame {
    //A plain black text attribute.
    private static SimpleAttributeSet black;

    //An italicised red text attribute.
    private static SimpleAttributeSet red;

    //Setup the fonts
    static {
        black = new SimpleAttributeSet();
        black.addAttribute(StyleConstants.Foreground, Color.BLACK);

        red = new SimpleAttributeSet();
        red.addAttribute(StyleConstants.Foreground, Color.RED);
        red.addAttribute(StyleConstants.CharacterConstants.Italic, Boolean.TRUE);
    }

    public ErrorFrame(Throwable e, JFrame relative) {
        super("Error handler!");
        setSize(640, 400);
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        try {
            String additionalInfo = "(Sorry! No additional info.)";
            if(e instanceof ArrayIndexOutOfBoundsException) {
                additionalInfo = "This was most likely a programmer error. I'm sorry!";
            } else if (e instanceof NullPointerException) {
                additionalInfo = "Either this was a programmer error, or something didn't get properly set up. Try restarting and see if that fixes the issue.";
            } else if (e instanceof java.io.IOException) {
                additionalInfo = "There was a major crash when connecting to a friend, or trying to read a file. Restarting should fix this. Otherwise, please report the bug!";
            } else if (e instanceof javax.sound.sampled.LineUnavailableException) {
                additionalInfo = "Your microphone and/or speakers aren't yet supported, or are currently in use. You can't use Pico until this issue is fixed. :(";
            }
            //Add more later as errors arise

            String contents = 
                "Hey! Bad news is that PicoSkype experienced a serious error and needs to close. The good news is that we caught it for you, so that " +
                "the programmer will know how to fix the problem in another patch! If you can, send a message to the developer with the following information:\n" +
                "\n" +
                "*The error message and information (everything in red down below) inside of this window\n" + 
                "*As much information as possible about what you were doing right before this crash window appeared (the more info the better!)\n" + 
                "*Any ideas or guesses as to what went wrong (if at all possible)\n" +
                "\n" +
                "Once this window is closed, you can restart the program, and the problem should (hopefully) go away.\n" +
                "Sorry for the inconvenience. We'll try to fix this issue soon. Here is some extra info that may be of use:\n" +
                additionalInfo + "\n" + 
                "~~~~~~~~~~Auto-generated error dump below this line~~~~~~~~~~\n";
            pane.getStyledDocument().insertString(0, contents, black);

            //Get the stack trace
            StackTraceElement[] stackTrace = e.getStackTrace();

            //Print the exact error that caused this problem
            String errorMessage =
                e.toString() + "\n";

            //Print out where the error propogated to.
            for(int i = 0; i < stackTrace.length; i++) 
                errorMessage+="     at " + stackTrace[i] + "\n";

            pane.getStyledDocument().insertString(pane.getStyledDocument().getLength(), errorMessage, red);
        } catch (BadLocationException b) {}
        add(new JScrollPane(pane));

        addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent w) {
                    System.exit(0);
                }
            });

        setLocationRelativeTo(relative);
        setVisible(true);
    }

    public static void main() {
        try {
            //throw new java.io.IOException("Testing yo!");
            throw new Exception();
        } catch (Exception e) {
            new ErrorFrame(e, null);
        }
    }
}
import java.awt.*;
import java.awt.image.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.util.*;
import java.io.*;
import javax.swing.JTextPane;
import javax.imageio.*;

/**
 * A drag and drop listener used with a JTextPane, interfacing with a Client Object.
 * 
 */
public class TextDNDListener implements DropTargetListener {
    //For program interconnectivity
    private ClientNetwork network;
    
    //The textpane to add the listener to.
    private JTextPane text;

    public TextDNDListener(JTextPane text, ClientNetwork network) {
        this.text = text;
        this.network = network;
    }

    public void processDrag(DropTargetDragEvent d) {}

    public void drop(DropTargetDropEvent d) {
        text.setBackground(Color.WHITE);
        if(!d.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return; //If drag-and-drop isn't supported, return

        d.acceptDrop(d.getDropAction());

        //Gets a list of all files dropped in.
        AbstractList list = null;
        try {
            list = (AbstractList)d.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
        } catch (Exception e) {return;}
        if (list == null) {return;} if (list.size() == 0) {return;}

        //To-do: Try to read all of the files dragged in.
        File file = (File)list.get(0);
        BufferedImage image = null;
        try {
            image = ImageIO.read(file);
        } catch (IOException i) {return;}

        //Alright, let's try to send it over.
        if(image == null) {
            network.send(file, file.getName());
        } else {
            network.send(image, file.getName());
        }
        /*
        if (image == null) {
        try {
        //This wasn't an image- so try to send it as a file.
        network.sendFile(file);
        } catch (IOException i) {
        network.addText("Error: Failed to send file.\n", Color.RED);
        network.addText("ErInf: " + i.toString() + "\n", Color.RED); 
        }
        } else {
        try {
        network.sendImage(image, file.getName(), 0);
        } catch (IOException i) {
        network.addText("Error: Failed to send image.\n", Color.RED);
        network.addText("ErInf: " + i.toString() + "\n", Color.RED);                        
        }
        }*/
    }

    public void dragEnter(DropTargetDragEvent d) {}

    public void dragOver(DropTargetDragEvent d) { text.setBackground(new Color(220, 220, 240)); }

    public void dragExit(DropTargetEvent d) { text.setBackground(Color.WHITE); }

    public void dropActionChanged(DropTargetDragEvent d) {}
}
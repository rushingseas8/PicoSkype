import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.imageio.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import javafx.application.*;
import javafx.scene.web.*;
import javafx.scene.media.*;
import javafx.scene.media.MediaPlayer.*;
import javafx.scene.*;
import javafx.embed.swing.*;
import javafx.util.*;
import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * An Object that handles the network requirements of a Client.
 */
public class ClientNetwork {
    //For program interconnectivity.
    public ClientLogger logger;
    public ClientGUI gui;
    public ClientParser parser;

    //Networking instance vars
    private Socket client;
    private ServerSocket server;
    public String IP;
    public int port;

    //Input/output
    private DataInputStream input;
    private DataOutputStream output;
    private NetworkListenerThread thread; //listens for incoming data

    //Your name, and your friend's name
    public String nickname;
    public String friendName;

    //Are we server or client?
    private boolean isServer;

    public ClientNetwork(ClientGUI gui, String name, String ip, int port) {
        this.gui = gui;
        this.nickname = name;
        this.IP = ip;
        this.port = port;
    }

    public void begin() {
        tryCreateServer();
        startListening();
    }

    /**
     * Attempts to establish an initial connection with another person.
     * Note: Also used by the /connect command to establish a new connection.
     */
    public void tryCreateServer() {
        try {
            if (IP != "") { //If no IP, default to server.
                addText("Attemping to connect..\n");
                try {
                    client = new Socket(IP, port); //Tries connecting to a server.
                    setupIO();
                    tradeNames();       
                    isServer = false;
                    addText("Established connection (as client) to "  + IP + " at port " + port + ".\n");
                    addText("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");     
                    return;
                } catch (SecurityException s) {
                    addText("Error: Security error; maybe connection is impossible?\n", Color.RED);
                    addText("ErInf: " + s.toString() + "\n", Color.RED);
                } catch (IOException e) {} //Okay, clearly not- server isn't up or not connectable.
            }

            addText("Friend offline, waiting for join..\n");
            server = new ServerSocket(port); //Opens up local port
            client = server.accept(); //Listens for clients
            setupIO();
            tradeNames();
            isServer= true;
            addText("Established connection (as server) to "  + IP + " at port " + port + ".\n");
            addText("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");            
            return;
        } catch (SecurityException s) {
            addText("Error: Security error; maybe connection is impossible?\n", Color.RED);
            addText("ErInf: " + s.toString() + "\n", Color.RED);
        } catch (SocketException s) {
            addText("Warning: Socket closed.\n", Color.PINK);
        } catch (IOException e) {
            addText("Error: Couldn't establish connection.\n", Color.RED);
            addText("ErInf: " + e.toString() + "\n", Color.RED);
        }
    }

    /**
     * Sets up the input and output streams.
     * @precondition The socket 'client' is not null
     */
    private void setupIO() {
        try {
            input = new DataInputStream(new BufferedInputStream(client.getInputStream()));
            output = new DataOutputStream(client.getOutputStream());  
        } catch (IOException i) {
            addText("Error: Couldn't set up input and output streams.\n", Color.RED);
            addText("ErInf: " + i.toString() + "\n", Color.RED);
        }
    }

    /**
     * A handshake method. Sends our name over to the partner, and recieves partner name.
     */
    private void tradeNames() {
        try {
            sendString(nickname);
            friendName = input.readUTF();
            parser.setFriendName(friendName);
        } catch (IOException i) {
            friendName = "Friend";
            addText("Error: Couldn't accurately complete handshake.\n", Color.RED);
            addText("ErInf: " + i.toString() + "\n", Color.RED);
            i.printStackTrace();
        }        
    }

    /**
     * Begin listening to input from the input streams.
     * Note that this method will also restart listening if it currently is listening.k
     */
    public void startListening() {
        if(thread != null) { thread.interrupt(); thread.close(); }
        thread = new NetworkListenerThread();
        thread.start();
    }

    /**
     * Deals with unplanned losses of connection; ie, user disconnect, internet down, etc.
     */
    public void attemptReconnect() {
        //Close all open sockets, streams, etc.
        try {
            if(server != null) server.close();
            if(client != null) client.close();
            if(input != null) input.close();
            if(output != null) output.close();
        } catch (IOException i) {
            i.printStackTrace();
        }

        if (isServer) addText("Trying to reconnect.. (Waiting for client)\n", Color.BLUE);
        else addText("Trying to reconnect.. (Waiting for server)\n", Color.BLUE);

        String temp = null;
        for(int i = 0; i < 25; i++) {
            try {
                if (!isServer) { //Client tries to re-connect on the same IP and port we started with.
                    client = new Socket(IP, port);
                    isServer = false;
                } else { //Server creates a new socket on the old port, then listens for clients.
                    server = new ServerSocket(port);
                    client = server.accept();
                    isServer = true;
                }
            } catch (Exception e) {
                if (!e.toString().equals(temp)) { //Every time we get a different error message, update the user (prevents excess spam)
                    temp = e.toString();
                    addText("ErInf: " + temp + "\n", Color.RED);
                }

                try {Thread.sleep(1000);} catch(Exception meow) {} //Wait a bit between attempts
                continue; //We failed to connect; try again
            }

            //Hey, we did it! Set up the connections, and let the user know.
            addText("Found connection, setting up..\n", Color.BLUE);
            setupIO();
            tradeNames();
            startListening();
            addText("Re-established connection!\n", Color.BLUE);

            addText("Established connection (as " + (isServer?"server":"client") +") to "  + IP + " at port " + port + ".\n");
            addText("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");

            return;
        }
        addText("Failed to re-establish connection.\n", Color.RED);
    }

    /**
     * Sends a string over to the other person.
     * All errors (except null) are on you.
     */
    public void sendString(String s) throws IOException {
        if(output != null)
            output.writeUTF(s);   
    }    

    /**
     * Sends an Object over the network.
     * @precondition The Object must be of type: File, File (music), BufferedImage.
     */
    public void send(Object o, String name) {
        if(output == null || friendName == null) return;

        String header = null;
        if (o instanceof File) {
            if (((File)o).getName().endsWith(".mp3") || ((File)o).getName().endsWith(".wav")) {
                header = "" + friendName.hashCode() + "music";
            } else {
                header = "" + friendName.hashCode() + "files";
            }
        } else if (o instanceof BufferedImage) {
            header = "" + friendName.hashCode() + "image";
        } else {
            addText("Couldn't send object of class " + o.getClass() + ".\n");
            return;
        }

        byte[] nameBytes = name.getBytes();

        byte[] bytes = null;
        if (header.endsWith("image")) {
            bytes = toBytes((BufferedImage)o, 0);
            addText("\n" + nickname + " sent an image: \"" + name + "\" (" + bytes.length + " bytes)\n", Color.RED.darker());
        } else if (header.endsWith("files")) {
            try {bytes = Files.readAllBytes(((File)o).toPath());} catch (IOException i) {i.printStackTrace();}
            addText("\n" + nickname + " sent a file: \"" + name + "\" (" + bytes.length + " bytes)\n", Color.RED.darker());
        } else if (header.endsWith("music")) {
            try {bytes = Files.readAllBytes(((File)o).toPath());} catch (IOException i) {i.printStackTrace();}
            addText("\n" + nickname + " sent a song: \"" + name + "\" (" + bytes.length + " bytes)\n", Color.RED.darker());            
        }

        try {
            output.writeUTF(header);
            output.writeInt(nameBytes.length);
            output.write(nameBytes);
            output.writeInt(bytes.length);
            output.write(bytes);
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    /**
     * Reads in an Object and does stuff with it.
     */
    public void read() {
        try {
            String s = input.readUTF();

            if(!s.startsWith(""+friendName.hashCode())) { parser.parse(s, 1); }
            else {
                int l1 = 0, l2 = 0;
                byte[] b1 = null, b2 = null;
                
                l1 = input.readInt();
                b1 = new byte[l1];
                input.readFully(b1);
                
                l2 = input.readInt();
                b2 = new byte[l2];
                input.readFully(b2);
                
                String name = new String(b1);

                String type = s.substring(s.length() - 5);
                switch(type) {
                    case "image": 
                    {
                        BufferedImage image = toImage(b2); 
                        addText("\n" + friendName + " sent an image: \"" + name + "\" (" + l2 + " bytes)\n", Color.BLUE.darker());

                        double ratio = (double)image.getHeight() / image.getWidth();
                        DraggerPanel d = new DraggerPanel(image, true, 256, (int)(256 * ratio));
                        logger.addComponent(d);      
                        addText("\n");
                    }
                    break;
                    case "files": 
                    {
                        File file = new File(System.getProperty("user.dir") + File.separator + "working" + File.separator + name);
                        file.createNewFile();                        
                        addText("\n" + friendName + " sent a file: \"" + name + "\" (" + l2 + " bytes)\n", Color.BLUE.darker());
                        FileOutputStream fo = new FileOutputStream(file);
                        fo.write(b2);
                        fo.close();  

                        addFileInterface(name, b2);                        
                    }
                    break;
                    case "music": 
                    {
                        File file = new File(System.getProperty("user.dir") + File.separator + "working" + File.separator + name);
                        file.createNewFile();                        
                        addText("\n" + friendName + " sent a song: \"" + name + "\" (" + l2 + " bytes)\n", Color.BLUE.darker());
                        FileOutputStream fo = new FileOutputStream(file);
                        fo.write(b2);
                        fo.close(); 
                        logger.addComponent(new AudioPanel(file));
                        addText("\n");  

                        addFileInterface(name, b2);                        
                    }
                    break;

                    default: addText("Warning: Unknown header: " + s, Color.PINK); break;
                }
            }
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    /**
     * Turns a BufferedImage into a byte array.
     * @param cl the level of compression; 0 is lossless, 1 is lossy, 2 is very lossy
     */
    private byte[] toBytes(BufferedImage b, int cl) {
        //Sets up the compression names
        String comp = "";
        switch(cl) {
            case 0: comp = "png"; break;
            case 1: comp = "jpg"; break;
            case 2: comp = "gif"; break;
            default: comp = "gif"; break;
        }

        ByteArrayOutputStream by = new ByteArrayOutputStream();
        byte[] toReturn = null;
        try {
            ImageIO.write(b, comp, by); //png is lossless, jpg is lossy, gif is very lossy
            by.flush();
            toReturn = by.toByteArray();
            by.close();
        } catch (IOException i) {
            addText("Error: When turning image to bytes.\n", Color.RED);
            addText("ErInf: " + i.toString() + "\n", Color.RED);
        }
        return toReturn;
    }

    /**
     * Turns a byte array into a BufferedImage.
     */
    private BufferedImage toImage(byte[] b) {
        ByteArrayInputStream by = new ByteArrayInputStream(b);
        BufferedImage toReturn = null;
        try {
            toReturn = ImageIO.read(by);
        } catch (IOException i) {
            addText("Error: When turning bytes to image.\n", Color.RED);
            addText("ErInf: " + i.toString() + "\n", Color.RED);            
        }
        return toReturn;
    } 

    public void addText(String s) {
        logger.addText(s);
    }

    public void addText(String s, Color c) {
        logger.addText(s, c);
    }

    /**
     * A helper method used when working with files.
     */
    private void addFileInterface(String name, byte[] fileContents) {
        JButton save = new JButton("Save As"), view = new JButton("View");

        save.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent a) {
                    JFileChooser j = new JFileChooser();
                    j.setSelectedFile(new File(name));
                    if (j.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                        try {
                            FileOutputStream fo = new FileOutputStream(j.getSelectedFile());
                            fo.write(fileContents);
                            fo.close();
                        } catch (IOException i) {
                            addText("Error: Failed to save file.\n", Color.RED);
                            addText("ErInf: " + i.toString() + "\n", Color.RED);                        
                        }
                    }
                }
            });
        view.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent a) {
                    new Thread() {
                        public void run() {
                            JFrame frame = new JFrame("File viewer");
                            String st = new String(fileContents);
                            frame.add(new JScrollPane(new JTextArea(st)));
                            frame.setSize(480, 480);
                            frame.setLocationRelativeTo(frame);
                            frame.setVisible(true);
                        }
                    }.start();
                }            
            });
        logger.addComponent(view);
        logger.addComponent(save);
        addText("\n");            
    }

    public void setClientLogger(ClientLogger c) {
        this.logger = c;
    }

    public void setClientParser(ClientParser c) {
        this.parser = c;
    }

    private class NetworkListenerThread extends Thread {
        private boolean closed = false;
        public void run() {
            addText("Started inbound thread!\n");
            while(!closed) 
                read();
        }

        public void close() {
            closed = true;
        }
    }
}
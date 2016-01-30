package pffft; 

import javax.swing.*;
import javax.imageio.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.nio.file.*;

/**
 * An Object that handles the network requirements of a Client.
 */
public class ClientNetwork {
    //For program interconnectivity.
    public Client cli;

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

    //The IP of the true server (used if there are more than 2 users)
    private String serverIP;

    public ClientNetwork() {}

    public void init(Client c) {
        this.cli = c;
        this.nickname = c.nickname;
        this.IP = c.ip;
        this.port = c.port;
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
                    isServer = false;
                    addText("Established connection (as client) to " + friendName + " at IP " + IP + " at port " + port + ".\n");
                    addText("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");     
                    return;
                } catch (IOException e) {} //Okay, clearly not- server isn't up or not connectable.
            }

            addText("Friend is offline, waiting for them to connect..\n");
            server = new ServerSocket(port); //Opens up local port
            client = server.accept(); //Listens for clients
            setupIO();
            isServer= true;
            addText("Established connection (as server) to " + friendName + " at IP " + IP + " at port " + port + ".\n");
            addText("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");            
            return;
        } catch (SocketException s) {
            addText("Warning: Socket closed.\n", Color.PINK);
        } catch (IOException e) {
            addText("Error: Couldn't establish connection.\n", Color.RED);
            addText("ErInf: " + e.toString() + "\n", Color.RED);
        }
    }

    /**
     * Sets up the input and output streams, and does a handshake.
     * @precondition The socket 'client' is not null
     */
    private void setupIO() {
        try {
            input = new DataInputStream(new BufferedInputStream(client.getInputStream()));
            output = new DataOutputStream(client.getOutputStream());  
            send(nickname);
            friendName = input.readUTF();
            cli.parser.setFriendName(friendName);
        } catch (IOException i) {
            addText("Error: Couldn't set up input and output streams/do handshake.\n", Color.RED);
            addText("ErInf: " + i.toString() + "\n", Color.RED);
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
     * Attempts to re-establish a connection with a user after unforseen circumstances, such as 
     * an internet problem or your friend disconnecting. 
     * <p><p>
     * This method is NOT suitable for creating brand new connections; the "/connect" command 
     * attempted to use this method and had many problems with threading. The proper way is to 
     * change the network variables as needed and then create a new server using "tryCreateServer()",
     * and finally using "startListening()" to begin the network connection. 
     * <p><p>
     * This method is used when we reach an end-of-stream error (usually means friend disconnected),
     * and when we get an IOException (which is some strange IO error that reconnection fixes 
     * often enough to warrent trying it).
     * <p>
     * @see 
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
        for(int i = 0; i < 300; i++) { //Try to connect for roughly five minutes.
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
     * <p>
     * This used to be called 'sendString', but was renamed to 'send' for consistency.
     * @since beta 1.7.3 
     */
    public void send(String s) throws IOException {
        if(output != null)
            output.writeUTF(s);   
    }    

    /**
     * Sends an Object over the network.
     * @precondition The Object must be of type: File, File (music), BufferedImage.
     */
    public void send(Object o, String name) {
        if(output == null || nickname == null) return;

        String header = null;
        if (o instanceof File) {
            if (((File)o).getName().endsWith(".mp3") || ((File)o).getName().endsWith(".wav")) {
                header = "" + nickname.hashCode() + "music";
            } else {
                header = "" + nickname.hashCode() + "files";
            }
        } else if (o instanceof BufferedImage) {
            header = "" + nickname.hashCode() + "image";
        } else {
            addText("Couldn't send object of class " + o.getClass() + ".\n");
            return;
        }

        byte[] nameBytes = name.getBytes();

        byte[] bytes = null;
        if (header.endsWith("image")) {
            bytes = toBytes((BufferedImage)o, 0);
            addText("\n" + nickname + " sent an image: \"" + name + "\" (" + bytes.length + " bytes)\n", Color.RED.darker());
            cli.logger.addComponent(new DraggerPanel((BufferedImage)o, false));
        } else if (header.endsWith("files")) {
            try {bytes = Files.readAllBytes(((File)o).toPath());} catch (IOException i) {i.printStackTrace();}
            addText("\n" + nickname + " sent a file: \"" + name + "\" (" + bytes.length + " bytes)\n", Color.RED.darker());
        } else if (header.endsWith("music")) {
            try {bytes = Files.readAllBytes(((File)o).toPath());} catch (IOException i) {i.printStackTrace();}
            addText("\n" + nickname + " sent a song: \"" + name + "\" (" + bytes.length + " bytes)\n", Color.RED.darker());
            cli.logger.addComponent(new AudioPanel((File)o));
            cli.logger.addText("\n");
        }

        final String header2 = header;
        final byte[] nameBytes2 = nameBytes;
        final byte[] bytes2 = bytes;

        //Send stuff over on another thread to prevent freezing when sending.
        new Thread() {
            public void run() {
                try {
                    output.writeUTF(header2);
                    output.writeInt(nameBytes2.length);
                    output.write(nameBytes2);
                    output.writeInt(bytes2.length);
                    output.write(bytes2);
                } catch (IOException i) {
                    i.printStackTrace();
                }
                return;
            }
        }.start();
    }

    /**
     * Reads in an Object and does stuff with it.
     */
    public void read() throws IOException {
        System.out.println("Bam!");
        String s = input.readUTF();

        if(!s.startsWith(""+friendName.hashCode())) { cli.parser.parse(s, 1); }
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

                    double ratio = (double)image.getHeight() / image.getWidth();
                    DraggerPanel d = new DraggerPanel(image, true, 256, (int)(256 * ratio));
                    addText("\n" + friendName + " sent an image: \"" + name + "\" (" + l2 + " bytes)\n", Color.BLUE.darker());
                    cli.logger.addComponent(d);      
                    addText("\n");
                }
                break;
                case "files": 
                {
                    File file = FileLoader.makeFile("pico/working/" + name);                      
                    FileOutputStream fo = new FileOutputStream(file);
                    fo.write(b2);
                    fo.close();  

                    addText("\n" + friendName + " sent a file: \"" + name + "\" (" + l2 + " bytes)\n", Color.BLUE.darker());
                    addFileInterface(name, b2);                        
                }
                break;
                case "music": 
                {
                    File file = FileLoader.makeFile("pico/working/" + name);    
                    FileOutputStream fo = new FileOutputStream(file);
                    fo.write(b2);
                    fo.close(); 

                    addText("\n" + friendName + " sent a song: \"" + name + "\" (" + l2 + " bytes)\n", Color.BLUE.darker());
                    addMusicInterface(file, b2);         
                }
                break;

                default: addText("Warning: Unknown header: " + s + "\n", Color.PINK); break;
            }
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
        cli.logger.addText(s);
    }

    public void addText(String s, Color c) {
        cli.logger.addText(s, c);
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
        cli.logger.addComponent(view);
        cli.logger.addComponent(save);
        addText("\n");            
    }

    /**
     * A helper method for working with music files- adds a save button and a music player
     */
    private void addMusicInterface(File file, byte[] musicContents) {
        JButton save = new JButton("Save As");

        save.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent a) {
                    JFileChooser j = new JFileChooser();
                    j.setSelectedFile(file);
                    if (j.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                        try {
                            FileOutputStream fo = new FileOutputStream(j.getSelectedFile());
                            fo.write(musicContents);
                            fo.close();
                        } catch (IOException i) {
                            addText("Error: Failed to save song.\n", Color.RED);
                            addText("ErInf: " + i.toString() + "\n", Color.RED);                        
                        }
                    }
                }
            });
        cli.logger.addComponent(save);
        cli.logger.addComponent(new AudioPanel(file));
        addText("\n");
    }

    private class NetworkListenerThread extends Thread {
        private boolean closed = false;
        public void run() {
            addText("Started inbound thread!\n");
            while(!closed) {
                try {
                    read();
                } catch (EOFException e) { //Indicates disconnection of some kind, usually friend leaving.
                    addText(friendName + " disconnected.\n");
                    if(closed) return;
                    attemptReconnect();
                } catch (NullPointerException n) { //Usually would be thrown on "input" being null.
                    addText("Error: Input data stream is null - listening thread crashed.\n", Color.RED);
                    return;
                } catch (SocketException se) { //Probably means a reconnection was attempted. Just ignore.     
                    if(closed) return;
                } catch (IOException e) { //General catch-all for any other problems we could have had.
                    if (e.toString().equalsIgnoreCase("Stream closed")) { //Usually when we reconnect & friend leaves.
                        addText("Error: Underlying stream closed- " + friendName + " potentially disconnected.\n", Color.PINK);
                    } else {
                        addText("Error: General IO failure.\n", Color.RED);
                        addText("ErInf: " + e.toString() + "\n", Color.RED);
                        new ErrorFrame(e, cli.gui.frame);
                    }
                    if(closed) return;
                    attemptReconnect();
                } catch (Throwable e) { //I really don't know what happened. I hope this isn't ever thrown.
                    addText("Error: General error; I don't know what happened.\n", Color.RED); 
                    new ErrorFrame(e, cli.gui.frame);
                    e.printStackTrace();
                    if(closed) return;
                }
            }
        }

        public void close() {closed = true;}
    }
}
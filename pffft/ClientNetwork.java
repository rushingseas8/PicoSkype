package pffft; 

import javax.swing.*;
import javax.imageio.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.util.*;
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
    //private NetworkListenerThread thread; //listens for incoming data

    //Your name, and your friend's name
    public String nickname;
    public String friendName;

    //Are we server or client?
    private boolean isServer;

    //The IP of the true server (used if there are more than 2 users)
    private String serverIP;

    //The port of the true server
    private int serverPort;

    //Connections
    public ArrayList<Person> people;

    //Used for separating messages from system references.
    private static final int HEADER = -1221270899;

    public ClientNetwork() {
        //clients = new ArrayList<>();
        //people  = new ArrayList<>();
        //inputs  = new ArrayList<>();
        //outputs = new ArrayList<>();
        people = new ArrayList<>();
    }

    public void init(Client c) {
        this.cli = c;
        this.nickname = c.nickname;
        this.IP = c.ip;
        this.port = c.port;
    }

    public void begin() {
        //tryCreateServer();
        //startListening();
        if(!tryConnectAsClient())
            tryConnectAsServer();
    }

    /**
     * Try to connect as a client.
     * Used for multiuser connections.
     * @since 1.8
     */
    public boolean tryConnectAsClient() {
        try { //Tries connecting to a server.
            addText("Attemping to connect..\n");
            client = new Socket(IP, port); 
            //If the above code passed without error, then we're connected; start to open streams.

            isServer = false;

            Person p = new Person(people.size());
            p.open(client).start();

            serverIP = IP;
            serverPort = port;

            addText("Established connection (as client) to " + friendName + " at IP " + IP + " at port " + port + ".\n");
            addText("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");     
        } catch (IOException e) { //Okay, clearly not- server isn't up or not connectable.
            addText("Failed to connect as client.\n");
            return false;
        } 
        return true;
    }

    /**
     * Begin the server listener thread. This will accept any new clients and enable them to
     * communicate with anyone else. This is used for multiuser connections.
     * @since 1.8
     */
    public void tryConnectAsServer() {
        //Set up the server
        try {
            addText("Attempting to bind server..\n");
            server = new ServerSocket(port);
            isServer = true;
            addText("Success!\n");
        } catch (IOException i) {
            addText("Failed to bind server.\n");
            i.printStackTrace();
        }

        //Begin listening for clients
        new Thread() {
            public void run() {
                addText("Starting to listen for clients!\n");
                while(true) {
                    try {
                        Socket newSocket = server.accept();
                        Person p = new Person(people.size());
                        p.open(newSocket).start();
                        //System.out.println(p.toString());
                        addText("Connected to " + p.name + "! Size is now " + people.size() + "\n");
                    } catch (IOException i) {
                        i.printStackTrace();
                    }
                }
            }
        }.start();
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
     * @deprecated
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
            //setupIO();
            //startListening();
            addText("Re-established connection!\n", Color.BLUE);

            addText("Established connection (as " + (isServer?"server":"client") +") to "  + IP + " at port " + port + ".\n");
            addText("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");

            return;
        }
        addText("Failed to re-establish connection.\n", Color.RED);
    }

    /**
     * Sends a string to all connected users.
     * (Note: Updated in beta 1.8 to reflect multi-user support)
     * @since beta 1.7.3
     */
    public void send(String s) throws IOException {
        System.out.println(people.size());
        for(Person p : people) {
            System.out.println(p);
            if(p.output != null) {
                System.out.println("Not null, sending");
                p.output.writeUTF(s); 
            }
        }
    }

    /**
     * Sends an Object over the network.
     * (Note: Updated in beta 1.8 to reflect multi-user support)
     * @precondition The Object must be of type: File, File (music), BufferedImage
     */
    public void send(Object o, String name) {
        if(output == null || nickname == null) return;

        String header = null;
        if (o instanceof File) {
            if (((File)o).getName().endsWith(".mp3") || ((File)o).getName().endsWith(".wav")) {
                header = "" + HEADER + "music";
            } else {
                header = "" + HEADER + "files";
            }
        } else if (o instanceof BufferedImage) {
            header = "" + HEADER + "image";
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
                for (Person p : people) {
                    try {
                        p.output.writeUTF(header2);
                        p.output.writeInt(nameBytes2.length);
                        p.output.write(nameBytes2);
                        p.output.writeInt(bytes2.length);
                        p.output.write(bytes2);
                    } catch (IOException i) {
                        i.printStackTrace();
                    }
                }
                return;
            }
        }.start();
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

    private void addText(String s) {
        cli.logger.addText(s);
    }

    private void addText(String s, Color c) {
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

    /**
     * A structure representing a person we're connected to. Used for communicating with multiple users.
     * @since 1.8
     */
    private class Person {
        public int ID;
        public String name;
        public Socket socket;
        public DataInputStream input;
        public DataOutputStream output;
        public PersonListenerThread thread;
        public boolean transfer;

        /**
         * Create a Person object around a given Socket and with a given ID.
         * @param s The Socket that this connection represents
         * @param ID The unique ID to identify which Person this is
         */
        public Person(int ID) {
            this.ID = ID;
        }

        /**
         * Creates a Person object from the toString() representation of another Person. Used for sending over networks.
         */
        public Person (String s, int ID) {
            String[] parts = s.split("*");
            addText("Trying to parse Person " + parts[0] + " " + parts[1] + " " + parts[2] + "\n");
            this.ID = ID;
            this.name = parts[0];
            try {
                this.socket = new Socket(parts[1], Integer.parseInt(parts[2]));
            } catch (IOException i) {
                addText(i.toString());
                addText("Error: Couldn't connect to Person w/ ID " + ID + " (name = " + parts[0] + " ip = " + parts[1] + ")\n", Color.RED);
            } catch (Exception n) {
                addText(n.toString());
                addText("Error: Couldn't connect to Person w/ ID " + ID + " because the port was not a number.\n", Color.RED);
            }
        }

        public Person open(Socket socket) {
            try {
                this.socket = socket;

                //Set up the input and output streams
                input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                output = new DataOutputStream(socket.getOutputStream());  

                //Below is the handshake procedure; we trade names, let the person know if we're a server,
                //then clients get a list of people to connect to from the server (if applicable).

                //Send our name over
                output.writeUTF(nickname);
                //Read in and save friend's name
                this.name = input.readUTF();

                //Let the user know if we're a server or not
                output.writeBoolean(isServer);
                //Find out if the user is a server or not
                boolean areTheyServer = input.readBoolean();

                if(transfer) {
                    people.add(this);
                    return this;
                }
                
                if(isServer) {
                    for(Person p : people) {
                        addText("*Sending: " + p.toString());
                        output.writeUTF(p.toString());
                    }

                    addText("*Sending: END TRANSMISSION");
                    output.writeUTF("END TRANSMISSION");
                } else {
                    if(areTheyServer) { //Client connected to a server; request list
                        String s = null;
                        while(!(s = input.readUTF()).equals("END TRANSMISSION")) {
                            addText("*Reading: " + s);
                            Person p = new Person(s, people.size());
                            p.transfer = true;
                            p.open(p.socket).start();
                            people.add(p);
                        }
                    } else { //Client connected to a client (opening a normal connection); just add them

                    }
                }

                people.add(this);
            } catch (IOException i) {
                i.printStackTrace();
            }
            return this;
        }

        public Person start() {
            thread = new PersonListenerThread(this);
            thread.start();
            return this;
        }

        private void close() {
            try {
                //Close all of the connections that this person has opened.
                socket.close();
                input.close();
                output.close();

                //Clear references.
                socket = null;
                input = null;
                output = null;
                thread = null;

                //Remove this reference from the overall pool.
                people.remove(this);
            } catch (IOException i) {
                i.printStackTrace();
            }
        }

        public String toString() {
            String s = "";
            s+= name + "*" + "localhost" + "*" + socket.getLocalPort();
            return s;
        }

        /**
         * A class that handles all user input.
         */
        private class PersonListenerThread extends Thread {
            private Person person;
            private boolean closed;

            public PersonListenerThread(Person p) {
                this.person = p;
                this.closed = false;
            }

            public void run() {
                while(true) {
                    try {
                        System.out.println("Reading");
                        this.read();
                    } catch (EOFException e) { //Indicates user disconnection, intentional or otherwise. Attempt to reconnect.
                        System.out.println("EOF exception");
                        addText(person.name + " disconnected.\n"); break;
                    } catch (NullPointerException | SocketException e) { //Either we had a problem during setup, or thread is being told to close.
                        System.out.println("Nullpointer or socket");
                        e.printStackTrace();
                        break;
                    } catch (IOException e) { //A catch-all for any other problems that may have occured.
                        System.out.println("IO");
                        e.printStackTrace(); break;
                    }
                }

                //If we ever end up here, then the thread must be shut down.
                System.out.println("Closing");
                person.close();
                return;
            }

            private void read() throws IOException {
                String s = person.input.readUTF();

                if(!s.startsWith(""+HEADER)) { cli.parser.parseNonLocal(s, name); }
                else {
                    int l1 = 0, l2 = 0;
                    byte[] b1 = null, b2 = null;

                    l1 = person.input.readInt();
                    b1 = new byte[l1];
                    person.input.readFully(b1);

                    l2 = person.input.readInt();
                    b2 = new byte[l2];
                    person.input.readFully(b2);

                    String name = new String(b1);

                    String type = s.substring(s.length() - 5);
                    switch(type) {
                        case "image": 
                        {
                            BufferedImage image = toImage(b2); 

                            double ratio = (double)image.getHeight() / image.getWidth();
                            DraggerPanel d = new DraggerPanel(image, true, 256, (int)(256 * ratio));
                            addText("\n" + name + " sent an image: \"" + name + "\" (" + l2 + " bytes)\n", Color.BLUE.darker());
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

                            addText("\n" + name + " sent a file: \"" + name + "\" (" + l2 + " bytes)\n", Color.BLUE.darker());
                            addFileInterface(name, b2);                        
                        }
                        break;
                        case "music": 
                        {
                            File file = FileLoader.makeFile("pico/working/" + name);    
                            FileOutputStream fo = new FileOutputStream(file);
                            fo.write(b2);
                            fo.close(); 

                            addText("\n" + name + " sent a song: \"" + name + "\" (" + l2 + " bytes)\n", Color.BLUE.darker());
                            addMusicInterface(file, b2);         
                        }
                        break;

                        default: addText("Warning: Unknown header: " + s + "\n", Color.PINK); break;
                    }
                }
            }
        }
    }
}
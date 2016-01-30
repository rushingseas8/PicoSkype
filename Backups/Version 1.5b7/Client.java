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

//For history, see the readme file
/**
 * A direct peer-to-peer text/audio/video program.
 * @author George Aleksandrovich
 * @version beta 1.5b7a
 */
public class Client {
    //GUI instance variables
    private JFrame frame;
    private JTextPane text;
    private JTextArea field;

    //Networking instance vars
    private Socket client;
    private ServerSocket server;
    private String IP;
    private int port;

    //Input/output
    private DataInputStream input;
    private DataOutputStream output;
    private ListenerThread thread; //listens for incoming data

    //Your name, and your friend's name
    private String nickname;
    private String friendName;

    //Are we server or client?
    private boolean isServer;

    //Used for autocompleting.
    private String[] commandsList = new String[]{"code","connect","define","help","new","user"};
    
    //Chat log info.
    private File log;

    public Client() {
        this("George", "localhost", 12000); //Change this as needed
    }

    public Client(String user, String ip, int port) {
        this.nickname = user;
        this.IP = ip;
        this.port = port;

        //Sets up GUI
        frame = new JFrame("PicoSkype v1.5b7a");
        frame.setSize(550, 480);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        text = new JTextPane();
        text.setEditable(false);

        //Support for drag and drop (drag images in!)
        new DropTarget(text, DnDConstants.ACTION_COPY_OR_MOVE, new DropTargetListener() {
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
                    if (image == null) {
                        try {
                            //This wasn't an image- so try to send it as a file.
                            sendFile(file);
                        } catch (IOException i) {
                            addText("Error: Failed to send file.\n", Color.RED);
                            addText("ErInf: " + i.toString() + "\n", Color.RED); 
                        }
                    } else {
                        try {
                            sendImage(image, file.getName(), 0);
                        } catch (IOException i) {
                            addText("Error: Failed to send image.\n", Color.RED);
                            addText("ErInf: " + i.toString() + "\n", Color.RED);                        
                        }
                    }
                }

                public void dragEnter(DropTargetDragEvent d) {}

                public void dragOver(DropTargetDragEvent d) { text.setBackground(new Color(220, 220, 240)); }

                public void dragExit(DropTargetEvent d) { text.setBackground(Color.WHITE); }

                public void dropActionChanged(DropTargetDragEvent d) {}
            });          

        field = new JTextArea();
        field.setRows(2);
        field.setLineWrap(true);
        JScrollPane scroll = new JScrollPane(field);

        //On normal enter
        Action textAreaEnterAction = new AbstractAction() {
                public void actionPerformed(ActionEvent a) {
                    //Clears the field and gets its data
                    String s = field.getText();
                    field.setText("");

                    parse(s, 0); //Checks for any commands & send over!             
                    field.setRows(2);
                    scroll.setMinimumSize(field.getPreferredSize());
                    frame.getContentPane().validate();
                }
            };

        //Shift+enter, alt+enter, etc    
        Action textAreaSpecialEnterAction = new AbstractAction() {
                public void actionPerformed(ActionEvent a) {
                    field.append("\n");
                    scroll.setMinimumSize(field.getPreferredSize());
                    frame.getContentPane().validate();
                }
            };  

        //Autocomplete on tab - currently doesn't cycle through.
        Action textAreaTabAction = new AbstractAction() {
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
            };

        //Makes modifier+enter create a newline, enter submit text, tab for autocomplete
        field.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "newline");
        field.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.ALT_DOWN_MASK), "newline");
        field.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "newline");
        field.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.META_DOWN_MASK), "newline");
        field.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "tab"); //Tab, no modifiers
        field.getActionMap().put("newline", textAreaSpecialEnterAction);
        field.getActionMap().put("tab", textAreaTabAction);
        field.getActionMap().put(field.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke("ENTER")), textAreaEnterAction);

        //Global keybindings
        Action openOptions = new AbstractAction() {
                public void actionPerformed(ActionEvent a) {
                    new OptionFrame(frame);
                }
            };
        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.META_DOWN_MASK), "options");
        frame.getRootPane().getActionMap().put("options", openOptions);

        frame.add(new JScrollPane(text), "Center");
        frame.add(scroll, "South");
        frame.setVisible(true);    
        field.requestFocus();
        
        //Prepares the chat log file.
        log = new File("log.txt");

        //Sets up the server, input/output, etc.
        trySetupServer();

        //Begin listening for inputs
        startListening();
    }

    /**
     * Does its best to establish an initial connection with another person.
     * Also used by the /connect command to establish a new connection.
     */
    private void trySetupServer() {
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
     * Deals with unplanned losses of connection; ie, user disconnect, internet down, etc.
     */
    private void attemptReconnect() {
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
        else addText("Trying to reconnect.. (Attempt 00/25)\n", Color.BLUE);

        //Used for a cool trick- to make the attempt counter update in-place.
        int offset = text.getStyledDocument().getLength() - 7;

        //Try to reconnect 25 times - roughly 25 seconds.
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

                if (!isServer) { //Only do the cool text trick if we're a client trying to reconnect. Not applicable to servers.
                    try {
                        SimpleAttributeSet set = new SimpleAttributeSet();
                        set.addAttribute(StyleConstants.Foreground, Color.BLUE);

                        text.getStyledDocument().remove(offset, 2);
                        if(i+1 < 10) {
                            text.getStyledDocument().insertString(offset, "0", set);
                            text.getStyledDocument().insertString(offset+1, ""+(i+1), set);
                        } else {
                            text.getStyledDocument().insertString(offset, ""+(i+1), set);
                        }
                    } catch (BadLocationException b) {} //lolno
                }

                continue; //We failed to connect; try again
            }

            //Hey, we did it! Set up the connections, and let the user know.
            addText("Found connection, setting up..\n", Color.BLUE);
            setupIO();
            tradeNames();
            startListening();
            addText("Re-established connection!\n", Color.BLUE);

            if(isServer) {
                addText("Established connection (as server) to "  + IP + " at port " + port + ".\n");
                addText("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n");
            } else {
                addText("Established connection (as client) to "  + IP + " at port " + port + ".\n");
                addText("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"); 
            }

            return;
        }
        addText("Failed to re-establish connection.\n", Color.RED);
    }

    /**
     * Creates a new Thread to listen for inbound data.
     */
    private void startListening() {
        //Sets up a thread to print out whatever is recieved.
        thread = new ListenerThread();     
        thread.start();
    }

    /**
     * Set up the input and output streams.
     * @precondition client is not null
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
     * Sends over this client's name, and recieves the partner's name.
     * If the output stream will throw an error, but the input stream won't,
     * and for some reason the issue fixes itself, then partner will always
     * be called "Friend". I don't see how it could happen, though.
     */
    private void tradeNames() {
        try {
            sendString(nickname);
            friendName = input.readUTF();
        } catch (IOException i) {
            friendName = "Friend";
            addText("Error: Couldn't accurately complete handshake.\n", Color.RED);
            addText("ErInf: " + i.toString() + "\n", Color.RED);
            i.printStackTrace();
        }        
    }

    /**
     * Parses the input string for commands and does actions based on the result.
     * This method will print out the parsed text.
     * @param person The ID of the person: 0 is this client, 1 is friend.
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */
    private void parse(String s, int person) {
        if (s.length() == 0) return; //So you can't spam enter
        boolean isCommand = false;

        //Insert name
        switch(person) {
            case 0: addText(nickname + ":", Color.RED.darker());    break;
            case 1: addText(friendName + ":", Color.BLUE.darker()); break;
            default: addText("System: Couldn't identify person number " + person + "\n"); break;
        } 

        //This is some kind of command!
        if (s.charAt(0) == '/') {
            String command = s.substring(1, s.indexOf(" ") == -1 ? s.length() : s.indexOf(" "));
            isCommand = true;
            addText(s + "\n"); //Print out the command immediately afterwards.

            switch(command) {
                case "help": 
                addText(
                    "Command list\n" +
                    "/help = shows this help menu\n" + 
                    "/define [word] = gives the definition in chat (beta)\n" +
                        //"/code [code sample] = highlights the given code (not implemented yet)\n" +
                    "/user [name] = changes your username to 'name' (currently local)\n" +
                    "/connect [ip] [port] = reconnects you to port 'port' at ip 'ip' (beta)\n" +
                    "/new [website] = opens a browser window with 'website' loaded\n"
                ); break;
                case "define": 
                {
                    addText("\"" + s.substring(command.length()+1).trim() + "\":" + define(s.substring(command.length()+1).trim()) + "\n"); 
                    break;
                }
                case "code": break; //Not yet implemented
                case "user": nickname = s.substring(command.length()+1).trim(); break;
                case "connect":
                {
                    String ip = ""; int pt = 0;
                    try {
                        //Parse for IP and number
                        ip = s.substring(s.indexOf(" ", command.length() + 1), s.lastIndexOf(" ")).trim();
                        pt = Integer.parseInt(s.substring(s.lastIndexOf(" ")).trim());
                        if (ip == null || pt < 0 || pt > 65535) throw new Exception("Input out of range"); //Filter out bad inputs
                        if (this.IP == ip && this.port == port) throw new Exception("Trying to connect to open connection"); //Don't let us try to reconnect to current.
                    } catch (NumberFormatException n) {
                        addText("System: Connecting using port 12000.");
                        pt = 12000;
                    } catch (Exception e) {
                        addText("System: Failed to parse (" + e.getMessage() + "). Proper usage is '/connect [IP] [port]'\n");
                        break; //If we failed, then return
                    }

                    //Reset thread, and update variables
                    if(thread != null) { thread.interrupt(); thread.close(); }
                    thread = null;
                    IP = ip;
                    port = pt;

                    //Invoke later so GUI doesn't hang.
                    new Thread() {
                        public void run() {
                            //attemptReconnect(); //Don't do this because it closes everything & throws errors
                            trySetupServer();
                            startListening();
                        }
                    }.start();
                }
                break;

                case "new": 
                {
                    if(s.trim().equals("/new")) {
                        try{Desktop.getDesktop().browse(new URI("http://www.google.com"));}catch(Exception e){e.printStackTrace();} //By default, go to google
                    } else {
                        //Go to the given website & update the command text
                        String uri = s;
                        if(!s.substring(5).startsWith("http://www.")) uri = "http://www." + s.substring(s.indexOf(" ")).trim();
                        try{Desktop.getDesktop().browse(new URI(uri));}catch(Exception e){e.printStackTrace();} 
                        addText("(Parsed url:/new " + uri + ")");
                    }
                }
                break;

                default: //do nothing
            }
        }

        if (!isCommand) {
            //Adds the text without coloring. Modify text itself (swallow commands, etc) before this line.
            addText(s + "\n");

            //Insert media players as appropriate
            if (s.contains("youtube.com")) {
                text.insertComponent(new YoutubePlayer(s, 3));
                addText("\n");
            }

            if (s.contains("imgur.com")) {
                if (!s.startsWith("http://")) s = "http://" + s;
                text.insertComponent(new DraggerPanel(s));
                addText("\n");
            }

            //Make sure this is a local string- if it is, then send it over. Else, don't send to avoid duplicates!
            if (person == 0) {
                try {
                    sendString(s);
                } catch (IOException e) {
                    addText("Error: failed to send message.\n", Color.RED);
                    addText("ErInf: " + e.toString() + "\n", Color.RED);
                    if(s.length() > 10000)
                        addText("Can you really say you're surprised?", Color.RED);
                }  
            }
        }
    }

    /**
     * Looks up the word in a dictionary.
     * @author Adam
     * @version 0.1
     */
    private String define(String s) {
        return WordReference.getDefinition(s);
    }

    /**
     * Sends a string over to the other person.
     * All errors (except null) are on you.
     */
    private void sendString(String s) throws IOException {
        if(output != null)
            output.writeUTF(s);   
    }

    /**
     * Sends a file over to the other person.
     * All errors (except null) are on you.
     * To-do: Make this send 1-8kb chunks of the file to make this easier on
     * both parties.
     */
    private void sendFile(File f) throws IOException {
        if(output == null) return;
        byte[] toSend = Files.readAllBytes(f.toPath());
        sendString(nickname.hashCode() + "files"); //File indicator string
        output.writeInt(f.getName().getBytes().length); //File name length
        output.write(f.getName().getBytes()); //File name
        output.writeInt(toSend.length); //Length code
        output.write(toSend); //File itself

        float linmb = (float)(toSend.length / 1048576.0); //Size of file sent
        addText("\n" + nickname + " sent a file: \"" + f.getName() + "\" (" + linmb + " MB)\n", Color.RED.darker());
    }    

    /**
     * Reads in a file from the input stream, as long as it was sent via sendFile().
     * Delegates the task of drawing components to other methods based on the file type detected.
     * Adds in two buttons to confirm or view the send request.
     */
    private void readFiles() {
        if(input == null) return;

        boolean isSong = false;

        int length = -1;
        String name = null;
        try {
            int l = input.readInt(); //File name length
            byte[] nameArr = new byte[l];
            input.readFully(nameArr); //Read file name as bytes
            name = new String(nameArr); //Set file name

            length = input.readInt(); //Read the length of the input
        } catch (IOException i) {
            addText("Error: Failed to read file in.\n", Color.RED);
            addText("ErInf: " + i.toString() + "\n", Color.RED);
        }        

        //For music files, mark out for music player adding.
        if(name.endsWith(".mp3") || name.endsWith(".wav")) {
            //Report music file info.
            float linmb = (float)(length / 1048576.0); //Used to report size
            addText("\n" + friendName + " sent a song: \"" + name + "\" (" + linmb + " MB)\n", Color.BLUE.darker());
            isSong = true;
        } else {
            //Report file information
            float linmb = (float)(length / 1048576.0); //Used to report size
            addText("\n" + friendName + " sent a file: \"" + name + "\" (" + linmb + " MB)\n", Color.BLUE.darker());
        }

        //Read the actual contents of the file after getting the header info, to make the program seem more responsive.
        byte[] bytes = null;
        try {
            bytes = new byte[length];
            input.readFully(bytes); //Read the file
        } catch (IOException i) {
            addText("Error: Failed to read file in.\n", Color.RED);
            addText("ErInf: " + i.toString() + "\n", Color.RED);            
        }

        //Add music player.
        if(isSong) 
            readMusic(bytes, name);

        //Make all these a private inner class to prevent memory leaks.
        JButton save = new JButton("Save As"), view = new JButton("View");
        final byte[] array = bytes; final String nm = name; //So we can do this anonymously
        save.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent a) {
                    JFileChooser j = new JFileChooser();
                    j.setSelectedFile(new File(nm));
                    if (j.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                        try {
                            FileOutputStream fo = new FileOutputStream(j.getSelectedFile());
                            fo.write(array);
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
                            String st = new String(array);
                            JFrame frame = new JFrame("File viewer");
                            frame.add(new JScrollPane(new JTextArea(st)));
                            frame.setSize(480, 480);
                            frame.setLocationRelativeTo(frame);
                            frame.setVisible(true);
                        }
                    }.start();
                }            
            });
        text.insertComponent(view);
        text.insertComponent(save);
        addText("\n");
    }

    /**
     * A private helper method that deals with reading and playing music files.
     */
    private void readMusic(byte[] b, String n) {
        try {
            byte[] bytes = b; String name = n;
            File f = new File(name);
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes);
            fo.close();
            text.insertComponent(new AudioPanel(f));
            addText("\n");
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    /**
     * Sends an image over to the other person.
     * All errors (except null) are on you.
     * To-do: Make this send 1-8kb chunks of the image to make this easier on
     * both parties.
     */
    private void sendImage(BufferedImage im, String name, int cl) throws IOException {
        if(output == null) return;

        //Send the image itself over
        byte[] toSend = toBytes(im, cl);
        sendString(nickname.hashCode() + "image"); //Header for image
        output.writeInt(name.getBytes().length); //Length of filename
        output.write(name.getBytes()); //Name bytes
        output.writeInt(toSend.length); //Tells how many image bytes to read.
        output.write(toSend); //Image bytes

        //Let the user know that the image was sent
        float linmb = (float)(toSend.length / 1048576.0); //Size of file sent
        addText("\n" + nickname + " sent an image: \"" + name + "\" (" + linmb + " MB)\n", Color.RED.darker());

        //Show the image itself- Not needed anymore because we solved the inserting issue!
        //ImageIcon ii = new ImageIcon(im);
        //text.insertIcon(ii);

        //Embed the image in a special wrapper panel
        double ratio = (double)im.getHeight() / im.getWidth();
        DraggerPanel d = new DraggerPanel(im, true, 256, (int)(256 * ratio));
        text.insertComponent(d);

        //Spacer
        addText("\n");
    }

    /**
     * Reads in an image from the input stream, and inserts it into the text.
     * Most errors are on you, so only call this when you know you have an image.
     */
    private BufferedImage readImage() {
        if(input == null) return null;

        int length = -1;
        byte[] bytes = null;
        String name = null;
        try {
            //Read the file name from the stream
            int l = input.readInt();
            byte[] arr = new byte[l];
            input.readFully(arr);
            name = new String(arr); 
            
            length = input.readInt(); //Read the length of the input
            bytes = new byte[length];
            input.readFully(bytes); //Read the image in
        } catch (IOException i) {
            addText("Error: Failed to read image in.\n", Color.RED);
            addText("ErInf: " + i.toString() + "\n", Color.RED);
        }

        BufferedImage image = toImage(bytes); //Create the image
        ImageIcon icon = new ImageIcon(image); //Turn the image into an icon for drawing

        float linmb = (float)(length / 1048576.0); //Used to report size
        addText("\n" + friendName + " sent an image: \"" + name + "\" (" + linmb + " MB)\n", Color.BLUE.darker());
        //text.insertIcon(icon);
        //text.insertComponent(new DraggerFrame(image, false)); //this doesn't resize?
        double ratio = (double)image.getHeight() / image.getWidth();
        DraggerPanel d = new DraggerPanel(image, true, 256, (int)(256 * ratio));
        text.insertComponent(d);
        addText("\n");        

        return image; //Return the image, in case you want to further use it
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

    /**
     * Helper method that inserts text with black color.
     */
    private void addText(String s) {
        addText(s, Color.BLACK);
    }

    /**
     * Add text to the main text pane with the given color.
     */
    private void addText(String s, Color c) {
        SimpleAttributeSet set = new SimpleAttributeSet();
        set.addAttribute(StyleConstants.Foreground, c);
        SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    try {
                        text.getStyledDocument().insertString(text.getStyledDocument().getLength(), s, set);
                        text.setCaretPosition(text.getStyledDocument().getLength());
                    } catch (BadLocationException e) {
                        addText("Error: Bad location when adding text!\n", Color.RED);
                        addText("ErInf: " + e.toString(), Color.RED);
                    }
                }
            }
        );
    }

    public static void main() {
        new Client();
    }

    private class ListenerThread extends Thread {
        String imageCode;
        String filesCode; 
        String audioCode;
        String videoCode;

        private boolean closed = false;
        public void run() {
            //while(friendName == null) {try{wait();}catch(InterruptedException i){i.printStackTrace();}}
            imageCode = "" + friendName.hashCode() + "image"; //Image header
            filesCode = "" + friendName.hashCode() + "files"; //File  header
            audioCode = "" + friendName.hashCode() + "audio"; //Audio header
            videoCode = "" + friendName.hashCode() + "video"; //Video header

            String s = "";
            addText("Started inbound thread!\n");

            try {
                while(!closed) {
                    s = input.readUTF();
                    if (s != null) {
                        if (s.equals(imageCode)) {
                            readImage(); s = ""; continue;
                        }
                        if (s.equals(filesCode)) {
                            readFiles(); s = ""; continue;
                        }

                        //addText(friendName + ":", Color.BLUE.darker());
                        //addText(s + "\n");
                        parse(s, 1); //Checks for commands
                    }
                }
            } catch (EOFException e) {
                //Indicates disconnection of some kind, usually friend leaving.
                addText(friendName + " disconnected.\n");
                if(closed) return;
                try{Thread.sleep(1000);}catch(Exception mew){}
                attemptReconnect();
            } catch (NullPointerException n) {
                //Usually would be thrown on "input" being null.
                addText("Error: Input data stream is null - inbound thread crash.\n", Color.RED);
                return;
            } catch (SocketException se) {
                //Probably means a reconnection was attempted. Just ignore.
                addText("Error: Socket exception.\n", Color.PINK);
                //se.printStackTrace();
                if(closed) return;
            } catch (IOException e) {
                //General catch-all for any other problems we could have had.
                if (e.toString().equalsIgnoreCase("Stream closed")) { //Usually when we reconnect & friend leaves.
                    addText("Error: Underlying stream closed- " + friendName + " potentially disconnected.\n", Color.PINK);
                    //addText("ErInf: " + e.toString() + "\n", Color.PINK);
                } else {
                    addText("Error: General IO failure.\n", Color.RED);
                    addText("ErInf: " + e.toString() + "\n", Color.RED);
                }
                //e.printStackTrace();
                if(closed) return;
                try{Thread.sleep(1000);}catch(Exception mew){}
                attemptReconnect();
            } catch (Throwable e) {
                addText("Error: General error.\n", Color.RED); //I don't know what happened.
                e.printStackTrace();
                if(closed) return;
            }
        }        

        public void close() {
            closed = true;
        }
    }
}
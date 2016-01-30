package pffft; 

import java.io.*;
import java.util.HashMap;

/**
 * A class that does the initial setup for the program, creates the initial variables, and 
 * holds references to all of them. The rest of the program will then refer to this class's
 * variables instead of directly calling on those variables.
 * <p>
 * The reference variables here have a specific system for their usage- They are initially
 * created using their respective constructors (generally without parameters), which sets
 * up their initial conditions. From there, every module has its init() method called, which
 * passes it references to every other class.
 * <p>
 * If you are modifying the source code or creating commands, please be aware that most of the
 * classes will NOT work properly until their init() method is called.
 * 
 * @author George Aleksandrovich
 * @version 1.7.3
 */
public class Client {
    /**
     * The version number of this Client. 
     */
    public String version = "1.7.3";

    /**
     * A reference to an ClientGUI Object that deals with all of the graphics of the program.
     * @see pffft.ClientGUI
     */
    public ClientGUI gui;

    /**
     * A reference to a ClientLogger Object, which contains helper methods that add JComponents
     * and text to the main JTextPane of the graphics.
     * @see pffft.ClientLogger
     */
    public ClientLogger logger;

    /**
     * A reference to the ClientNetwork Object, which holds methods related to sending and
     * receiving Objects from partners.
     * @see pffft.ClientNetwork
     */
    public ClientNetwork network;

    /**
     * A reference to a ClientParser, which has everything to do with commands.
     * @see pffft.ClientParser
     */
    public ClientParser parser;

    /**
     * A reference to the AudioGUI, which handles the GUI related to the audio portion of the program.
     * @see pffft.AudioGUI
     */
    public AudioGUI audioGUI;

    /**
     * A reference to the audio networking object.
     * @see pffft.AudioNetwork
     */
    public AudioNetwork audioNetwork;

    /**
     * A hashmap that stores global program options. 
     * <p><p>
     * The first value is a String key used to identify the specific option; "embed youtube player" is one
     * example of a key value that may be used.
     * <p><p>
     * The second value is the option value, saved as a String. Typical examples include "true", "100", or
     * "George". Be sure to cast the String into the appropriate value.
     * <p><p>
     * There are NO guarantees that the Strings will be the exected value; if you expect a key to return
     * a Boolean value, it may well be saved as an Integer. Be sure to handle exceptions when casting.
     */
    public HashMap<String, String> options;

    /**
     * The name you're signing in with.
     */
    public String nickname;

    /**
     * The ip that you're currently connected to.
     */
    public String ip;

    /**
     * The port on which the connection is established.
     */
    public int port;

    /**
     * Creates a Client with default parameters for name, IP, and port; 
     * The default option parameters are used. This is used for developer testing and debugging.
     */
    public Client() {
        this("George", "localhost", 12000, null);
    }

    /**
     * Creates a new Client.
     * @param nickname The local name you wish to be called
     * @param ip The initial IP to connect to
     * @param port The port that the program will use
     * @param parameters Boolean parameters used by Runner for initial options.
     */
    public Client(String nickname, String ip, int port, boolean[] parameters) {
        long time = System.currentTimeMillis();

        this.nickname = nickname;
        this.ip = ip;
        this.port = port;

        //Sets up the program files folder.
        FileLoader.makeDir("~/pico");      

        //Sets up a working directory
        FileLoader.makeDir("~/pico/working");

        //Sets up a custom command directory
        FileLoader.makeDir("~/pico/commands");  
        FileLoader.makeDir("~/pico/commands/src");    

        //Startup options setup
        boolean commandUpdatesForced = true;

        if(parameters != null) 
            commandUpdatesForced = parameters[0];

        //Set up the options hashmap with default values.
        options = new HashMap<String, String>();
        options.put("embed youtube player", "true");
        options.put("embed images", "true");
        options.put("embed audio", "true");
        
        //Try to load in the options file; if there is none, then create an empty one.
        loadOptions();

        //Initial setup
        gui = new ClientGUI();
        logger = new ClientLogger();
        network = new ClientNetwork();
        parser = new ClientParser();

        audioGUI = new AudioGUI();
        audioNetwork = new AudioNetwork(ip, port + 1);

        //Tie the program together
        gui.init(this);
        logger.init(this);
        network.init(this);
        parser.init(this);

        audioGUI.init(this);
        audioNetwork.init(this);

        //Drag and drop support
        new java.awt.dnd.DropTarget(gui.text, java.awt.dnd.DnDConstants.ACTION_COPY_OR_MOVE,
            new TextDNDListener(gui.text, network)); 

        //Load in the default commands
        parser.createDefaultCommands(); 
        
        //Load in custom commands on another thread, if the user selected this option.
        if(commandUpdatesForced) 
            new Thread() { public void run() {parser.loadCustomCommands();}}.start();

        logger.addText("Did Client setup in " + (System.currentTimeMillis() - time) + "ms.\n", java.awt.Color.PINK);

        //Starts trying to connect chat. Do this on another thread so that nothing will hang.
        new Thread() { public void run() { network.begin(); } }.start();

        //Do the same for audio connection on another thread so that they can load asynchronously.
        new Thread() { public void run() { audioNetwork.begin(); } }.start();
    }

    /**
     * Creates a new Client with default parameters.
     */
    public static void main() {
        new Client();
    }
    
    //Loads in the options file, or creates one if one does not exist.
    private void loadOptions() {
        File file = FileLoader.load("~/pico/options.txt");
        if(file == null) {
            file = FileLoader.makeFile("~/pico/options.txt");
            try(FileOutputStream fo = new FileOutputStream(file)) {
                //This writes the default values to the file.
                java.util.Iterator<java.util.Map.Entry<String, String>> it = options.entrySet().iterator();
                while(it.hasNext()) {
                    java.util.Map.Entry entry = it.next();
                    fo.write((entry.getKey() + "\n").getBytes());
                    fo.write((entry.getValue() + "\n").getBytes());
                }
            } catch(IOException i) {
                i.printStackTrace();
            }
        } else {
            try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String k = null, v = null;
                while((k = reader.readLine()) != null && (v = reader.readLine()) != null) {
                    options.put(k, v);
                    System.out.println("\"" + k + "\", " + v);
                }
            } catch (IOException i) {
                i.printStackTrace();
            }
        }
    }
}
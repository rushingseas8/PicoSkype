/**
 * Sets up the Client.
 * @author George Aleksandrovich
 * @version 1.6
 */
public class Client {
    public ClientGUI gui;
    public ClientLogger logger;
    public ClientNetwork network;
    public ClientParser parser;

    public Client() {
        this("George", "localhost", 12000);
    }

    public Client(String nickname, String ip, int port) {
        //Sets up a working directory
        java.io.File dir = new java.io.File(System.getProperty("user.dir") + java.io.File.separator + "working");
        if(!dir.exists() || !dir.isDirectory()) {dir.mkdirs();}

        //Initial setup
        gui = new ClientGUI();
        logger = new ClientLogger(gui.getTextPane());
        network = new ClientNetwork(gui, nickname, ip, port);
        parser = new ClientParser(gui, network);

        //Tie the program together
        gui.setClientLogger(logger);
        gui.setClientParser(parser);
        new java.awt.dnd.DropTarget(
            gui.getTextPane(),
            java.awt.dnd.DnDConstants.ACTION_COPY_OR_MOVE,
            new TextDNDListener(
            gui.getTextPane(), network));

        network.setClientLogger(logger);
        network.setClientParser(parser);

        parser.setClientLogger(logger);

        //Run
        network.begin();
    }

    public static void main() {
        new Client();
    }
}
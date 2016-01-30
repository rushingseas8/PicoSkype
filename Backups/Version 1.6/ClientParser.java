import java.awt.*;
import java.net.*;
import java.io.*;

/**
 * Parses a given string for commands, and executes them accordingly.
 */
public class ClientParser {
    private ClientGUI gui;
    private ClientLogger logger;
    private ClientNetwork network;
    private String nickname;
    private String friendName;

    public ClientParser(ClientGUI gui, ClientNetwork network) {
        this.gui = gui;
        this.network = network;
        this.nickname = network.nickname;
        this.friendName = network.friendName;
    }
    
    public void setName(String to) {
        nickname = to;
    }
    
    public void setFriendName(String to) {
        friendName = to;
    }

    public void parse(String s, int person) {
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
                    addText("\"" + s.substring(command.length()+1).trim() + "\":" + 
                        WordReference.getDefinition(s.substring(command.length()+1).trim()) + "\n"); 
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
                        if (network.IP == ip && network.port == pt) throw new Exception("Trying to connect to open connection"); //Don't let us try to reconnect to current.
                    } catch (NumberFormatException n) {
                        addText("System: Connecting using port 12000.");
                        pt = 12000;
                    } catch (Exception e) {
                        addText("System: Failed to parse (" + e.getMessage() + "). Proper usage is '/connect [IP] [port]'\n");
                        break; //If we failed, then return
                    }

                    //Reset thread, and update variables
                    network.IP = ip;
                    network.port = pt;

                    //Invoke later so GUI doesn't hang.
                    new Thread() {
                        public void run() {
                            //attemptReconnect(); //Don't do this because it closes everything & throws errors
                            network.tryCreateServer();
                            network.startListening();
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
                final String url = s;
                new Thread() {
                    public void run() {
                        logger.addComponent(new YoutubePlayer(url, 3));
                        addText("\n");
                    }
                }.start();
            }

            if (s.contains("imgur.com")) {
                if (!s.startsWith("http://")) s = "http://" + s;
                logger.addComponent(new DraggerPanel(s));
                addText("\n");
            }

            //Make sure this is a local string- if it is, then send it over. Else, don't send to avoid duplicates!
            if (person == 0) {
                try {
                    network.sendString(s);
                } catch (IOException e) {
                    addText("Error: failed to send message.\n", Color.RED);
                    addText("ErInf: " + e.toString() + "\n", Color.RED);
                    if(s.length() > 10000)
                        addText("Can you really say you're surprised?", Color.RED);
                }  
            }
        }        
    }

    public void setClientLogger(ClientLogger l) {
        logger = l;
    }
    
    private void addText(String s) {
        logger.addText(s);
    }

    private void addText(String s, Color c) {
        logger.addText(s, c);
    }
}
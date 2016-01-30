//TO-DO: make the parser work properly with multiple users
package pffft; 

import java.awt.*;
import java.net.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import javax.tools.*;
import java.util.*;

/**
 * Parses a given string for commands, and executes them accordingly.
 */
public class ClientParser {
    public Client client;

    public String nickname;
    public String friendName;

    private JavaCompiler compiler; //Used to compile plugins
    private ArrayList<Command> commands;
    private Calendar cal; //Used for timestamps

    public ClientParser() {
        this.compiler = ToolProvider.getSystemJavaCompiler();
        this.commands = new ArrayList<>();        
        this.cal = Calendar.getInstance();
    }

    public void init(Client cli) {
        this.client = cli;

        this.nickname = client.network.nickname;
        this.friendName = client.network.friendName;

        if(compiler == null) {
            addText("System: Hey, are you running from a JAR file? Because you can't create commands using that.\n", Color.PINK);
            //addText("Executing Java path: " + System.getProperty("java.home") + "\n", Color.PINK);
            //addText("JDK path: " + System.getenv("JAVA_HOME") + "\n", Color.PINK);
        }
    }

    public void setName(String to) {
        nickname = to;
    }

    public void setFriendName(String to) {
        friendName = to;
    }

    /**
     * Deprecated, do not use except for legacy code.
     * Will be removed in a future release.
     * //@deprecated
     */
    public void parse(String s, int person) {
        if (s.length() == 0) return; //So you can't spam enter
        boolean isCommand = false;

        addText(getTimeStamp());

        //Insert name
        switch(person) {
            case 0: addText(nickname + ": ", Color.RED.darker());    break;
            case 1: addText(friendName + ": ", Color.BLUE.darker()); break;
            default: addText("System: Couldn't identify person number " + person + "\n"); break;
        } 

        //This is some kind of command!
        if (s.charAt(0) == '/') {
            isCommand = true;
            String command = s.substring(1, s.indexOf(" ") == -1 ? s.length() : s.indexOf(" "));
            String[] parameters = s.indexOf(" ") == -1 ? null : s.substring(s.indexOf(" ") + 1).split(" "); //Make this split by quotes, too
            int numParameters = parameters == null ? 0 : parameters.length;
            if (parameters != null) for(String st : parameters) System.out.println(st);
            addText(s + "\n"); //Print out the command immediately afterwards.

            for(Command c : commands) {
                if (c.identifier.equals(command)) {
                    if (c.numParameters == numParameters) {
                        c.execute(parameters);
                    } else {
                        //addText("Found command " + c.identifier + " that needs " + c.numParameters + " parameters, but found " + numParameters + ".\n", Color.PINK);
                        continue;
                    }
                    return;
                }
            }
            addText("System: Couldn't find the command \"" + command + "\" with " + numParameters + " parameters.\n");
        }

        if (!isCommand) {
            //Adds the text without coloring. Modify text itself (swallow commands, etc) before this line.
            addText(s + "\n");

            //Insert media players as appropriate
            if (s.contains("youtube.com")) {
                final String url = s;
                new Thread() {
                    public void run() {
                        client.logger.addComponent(new YoutubePlayer(url, 3));
                        addText("\n");
                    }
                }.start();
            }

            if (s.contains("imgur.com")) {
                if (!s.startsWith("http://")) s = "http://" + s;
                client.logger.addComponent(new DraggerPanel(s));
                addText("\n");
            }

            //Make sure this is a local string- if it is, then send it over. Else, don't send to avoid duplicates!
            if (person == 0) {
                try {
                    client.network.send(s);
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
     * Parses text that the local user sends (this is you!)
     * @since 1.8
     */
    public void parseLocal(String s) {
        addText(getTimeStamp() + nickname + ": ", Color.RED.darker());

        //Command parsing
        if(s.charAt(0) == '/') {
            String command = s.substring(1, s.indexOf(" ") == -1 ? s.length() : s.indexOf(" "));
            String[] parameters = s.indexOf(" ") == -1 ? null : s.substring(s.indexOf(" ") + 1).split(" "); //Make this split by quotes, too
            int numParameters = parameters == null ? 0 : parameters.length;
            //if (parameters != null) for(String st : parameters) System.out.println(st);
            addText(s + "\n"); //Print out the command immediately afterwards.

            for(Command c : commands) {
                if (c.identifier.equals(command)) {
                    if (c.numParameters == numParameters) {
                        c.execute(parameters);
                    } else {
                        continue;
                    }
                    return;
                }
            }
            addText("System: Couldn't find the command \"" + command + "\" with " + numParameters + " parameters.\n");
        } else { //No commands
            //Add the text
            addText(s + "\n");
            System.out.println("Not a command, sending over.");

            //Send the text over
            try { client.network.send(s); }
            catch (IOException e) {
                addText("Error: failed to send message.\n", Color.RED);
                addText("ErInf: " + e.toString() + "\n", Color.RED);
                if(s.length() > 10000)
                    addText("Can you really say you're surprised?", Color.RED);
            }  

            //Insert media as needed
            insertMediaForString(s);
        }
    }

    /**
     * Parses text from other users.
     * @since 1.8
     */
    public void parseNonLocal(String s, String friendName) {
        addText(getTimeStamp() + friendName + ": ", Color.BLUE.darker());
        addText(s + "\n");
        insertMediaForString(s);
    }

    /**
     * Returns the current timestamp in the form '[HR:MN[AM/PM]]\n'
     * @since 1.8
     */
    public String getTimeStamp() {
        cal = Calendar.getInstance();
        String timestamp = "[";
        int hr = cal.get(Calendar.HOUR); //HOUR is 12hr time; HOUR_OF_DAY is 24hr
        int mn = cal.get(Calendar.MINUTE);
        int am = cal.get(Calendar.AM_PM);
        timestamp+=(hr < 10 ? hr == 0 ? "12" : ("0" + hr) : hr) + ":";
        timestamp+=(mn < 10 ? ("0" + mn) : mn);
        timestamp+=(am < 01 ? "AM" : "PM") + "] ";

        return timestamp;
    }

    /**
     * Insert media players as appropriate.
     * If the String contains "youtube.com", then add a youtube player
     * If the String contains "imgur.com", then add a dragger panel
     * @since 1.8
     */
    public void insertMediaForString(String s) {
        if (s.contains("youtube.com")) {
            client.logger.addComponent(new YoutubePlayer(s, 3));
            addText("\n");
        }

        if (s.contains("imgur.com")) {
            if (!s.startsWith("http://")) s = "http://" + s;
            client.logger.addComponent(new DraggerPanel(s));
            addText("\n");
        }        
    }

    /**
     * Used internally to load in the system default commands, which use the command infrastructure.
     */
    public void createDefaultCommands() {
        commands.add(new Command("help", "[]; shows this help menu", null, new DefaultCommandRunnable() {
                    public void run() {
                        for(Command c : commands) {addText(c.identifier + ": " + c.description + "\n");}
                    }
                }));

        commands.add(new Command("inspect", "[command]; view more info on a command", new Object[1], new DefaultCommandRunnable() {
                    public void run() {
                        for(Command c : commands) if (c.identifier.equals((String)args[0])) addText(c.toString() + "\n");
                    }
                }));

        commands.add(new Command("command", "[name] [desc] [Object[] params] [Runnable]", new Object[4], new DefaultCommandRunnable() {
                    public void run() {
                        String n = (String)args[0];
                        String d = (String)args[1];
                        String p = (String)args[2];
                        String r = (String)args[3];
                        Command c = createCommand(n,d,p,r,commands.size());
                        if(c != null) {
                            c.init(client);
                            commands.add(c);
                        }
                        else addText("Failed to create requested command.\n");
                    }
                }));

        commands.add(new Command("define", "[word]; defines 'word'", new Object[]{new String()}, new DefaultCommandRunnable() {
                    public void run() {
                        System.out.println(args[0]);
                        addText("The definition of " + (String)args[0] + " is " + WordReference.getDefinition((String)args[0]));
                    }
                }));

        commands.add(new Command("connect", "[ip] [port]; attempts to connect w/ given info", new Object[]{new String(), new Integer(0)}, new DefaultCommandRunnable()
                {
                    public void run() {
                        String ip = ""; int pt = 0;
                        try {
                            //Parse for IP and number
                            ip = (String)args[0];
                            pt = Integer.parseInt((String)args[1]);
                            if (ip == null || pt < 0 || pt > 65535) throw new Exception("Input out of range"); //Filter out bad inputs
                            if (client.network.IP == ip && client.network.port == pt) throw new Exception("Trying to connect to currently open connection"); //Don't let us try to reconnect to current.
                        } catch (NumberFormatException n) {
                            addText("System: Connecting using port 12000.");
                            pt = 12000;
                        } catch (Exception e) {
                            addText("System: Failed to parse (" + e.getMessage() + "). Proper usage is '/connect [IP] [port]'\n");
                        }

                        client.network.IP = ip;
                        client.network.port = pt;

                        //client.network.tryCreateServer();
                        //client.network.startListening();
                    }
                }));

        commands.add(new Command("new", "[]; opens google.com", null, new DefaultCommandRunnable()
                {
                    public void run() {
                        try{Desktop.getDesktop().browse(new URI("https://www.google.com"));} catch (Exception e) {}
                    }
                }));                

        commands.add(new Command("new", "[url]; opens url in a browser", new Object[]{new String()}, new DefaultCommandRunnable()
                {
                    public void run() {
                        String uri = (String)args[0];

                        if (uri != null && !uri.equals("")) {}else{ try{Desktop.getDesktop().browse(new URI("https://www.google.com")); return; }catch(Exception e){e.printStackTrace();} }

                        if (!uri.startsWith("https://www.")) uri = "https://www." + uri;
                        try{Desktop.getDesktop().browse(new URI(uri));}catch(Exception e){e.printStackTrace();} 
                    }
                }));     

        for(Command c : commands) {
            c.init(client);
        }   

        //Creates a commands.txt file if needed.
        File file = FileLoader.load("~/pico/commands/commands.txt");
        if(file == null) {
            file = FileLoader.makeFile("~/pico/commands/commands.txt");
            try (FileOutputStream fo = new FileOutputStream(file)) {
                fo.write(
                    ("//Write custom commands here. Syntax: 'name of command', 'description', 'parameters', 'action code'\n" +
                        "//Everything outside of quotes is a comment\n" +
                        "//Available variables: 'gui', 'network', 'parser', 'logger', 'audioGUI', 'audioNetwork', 'FileLoader' (static)\n")
                    .getBytes());
                addText("Auto-generated commands.txt under 'commands' folder.\n", Color.PINK); 
                return;
            } catch (IOException i) {
                addText("Failed to create commands.txt.\n", Color.PINK);
                i.printStackTrace();
            }
        }
    }

    /**
     * Loads custom user commands from commands.txt.
     * Please note that any classes you add in will NOT be used; only the ones defined in commands.txt are.
     */
    public void loadCustomCommands() {
        addText("Attempting to load custom commands.\n", Color.PINK);

        //File file = new File(System.getProperty("user.dir") + File.separator + "pico" + File.separator + "commands" + File.separator + "commands.txt");
        File file = FileLoader.load("~/pico/commands/commands.txt");

        String contents = null;
        try (FileReader reader = new FileReader(file)) {
            char[] temp = new char[(int)file.length()];
            reader.read(temp);
            contents = new String(temp);
        } 
        catch (FileNotFoundException f) { f.printStackTrace(); }
        catch (IOException i) { i.printStackTrace(); }
        if(contents == null) { addText("Failed to read in commands file.\n", Color.PINK); return; }

        addText("Searching for custom commands..\n", Color.PINK);
        int count = 0;
        for (int i = 0; i < contents.length();) {
            int q1, q2, q3, q4, q5, q6, q7, q8;
            q1 = contents.indexOf("*", i);
            if(i == 0 && q1 == -1) { addText("No custom commands found.\n", Color.PINK); return; }
            if (q1 == -1) {break;}
            q2 = contents.indexOf("*", q1+1);
            q3 = contents.indexOf("*", q2+1);
            q4 = contents.indexOf("*", q3+1);
            q5 = contents.indexOf("*", q4+1);
            q6 = contents.indexOf("*", q5+1);
            q7 = contents.indexOf("*", q6+1);
            q8 = contents.indexOf("*", q7+1);
            if(q1==-1||q2==-1||q3==-1||q4==-1||q5==-1||q6==-1||q7==-1||q8==-1) { addText("Improper number of quotation marks. Some commands may not be recognised.\n", Color.PINK); return; }

            String nm = contents.substring(q1 + 1, q2);
            String dc = contents.substring(q3 + 1, q4);
            String pm = contents.substring(q5 + 1, q6);
            String ac = contents.substring(q7 + 1, q8);
            //System.out.println("Name:"+nm+"\nDesc:"+dc+"\nParams:"+pm+"\nAction:"+ac);
            addText("Found command: " + nm + ".\n", Color.PINK);

            Command c = createCommand(nm, dc, pm, ac, count);
            addText("Command c=" + c.toString() + "\n", Color.PINK);
            if (c != null) {
                addText("Loading in command: " + nm + ".\n", Color.PINK);
                //c.init(client.gui, client.network, this, client.logger);
                commands.add(c);
                //c.execute();
            } else {
                addText("Couldn't properly load command: " + nm + ".\n", Color.RED);
            }

            i = q8 + 1;
            count++;
        }

        addText("Done loading in custom plugins.\n", Color.PINK);
        for(Command c : commands) {
            c.init(client);
        }   
    }

    /**
     * Creates a java file from the given pseudo-command, compiles, returns an instance.
     */
    private Command createCommand(String name, String desc, String params, String action, int num) {
        if(name == null || desc == null || params == null || action == null) { /*addText("Command " + name + " had a null input.\n",Color.PINK);*/ return null; }
        if(name == "" || desc == "" || params == "" || action == "") { /*addText("Command " + name + " has a bad description in commands.txt.\n", Color.PINK);*/ return null; }

        String commandName = name.substring(0, 1).toUpperCase() + name.substring(1);
        //File f = new File(System.getProperty("user.dir") + File.separator + "pico" + File.separator + "commands"+ File.separator + "Command" + commandName + ".java"); //~/pico/commands/src/Command#.java
        File f = FileLoader.makeFile("~/pico/commands/Command" + commandName + ".java");
        System.out.println(f);

        String source = new String(
                "package pico.commands;\n" +
                "import javax.swing.*;\n" +
                "import java.awt.*;\n" +
                "//Java file autogenerated by ClientParser. Do NOT modify.\n" +
                "public class Command" + commandName + " extends pffft.Command {\n" +
                "   public Command" + commandName + "() {\n" +
                "       super(\"" + name + "\",\n" +
                "           \"" + desc + "\",\n" +
                "           " + params + ");\n" +
                "   }\n" +
                "   \n" +
                "   @Override\n" +
                "   protected void action() {\n" +
                "       " + action + "\n" +
                "   }\n" +
                "}\n"
            );

        try (FileOutputStream fo = new FileOutputStream(f)) { /*f.createNewFile();*/ fo.write(source.getBytes());}
        catch(Exception e) {e.printStackTrace();}         

        int result = compiler.run(null, null, null, "-cp", new File(System.getProperty("user.dir")).getAbsolutePath(), f.getAbsolutePath());
        if (result != 0) { addText("Failed to compile Command" + commandName + ".java.\n", Color.RED); return null; }

        f.delete(); //Delete source after we're done

        try {
            Class com = Class.forName("pico.commands.Command" + commandName);
            return (Command)com.newInstance(); //Oh man is this unsafe
        } catch (ClassNotFoundException c) {
            addText("Failed to get class Command" + commandName + ".class.\n", Color.RED);
            c.printStackTrace();
            return null;
        } catch (InstantiationException i) {
            addText("Failed to get run Command" + num + ".class.\n", Color.RED);
            i.printStackTrace();
            return null;
        } catch (IllegalAccessException i) {
            addText("Failed to get run Command" + num + ".class.\n", Color.RED);
            i.printStackTrace();
            return null;
        }
    }

    private void addText(String s) {
        client.logger.addText(s);
    }

    private void addText(String s, Color c) {
        client.logger.addText(s, c);
    }
}
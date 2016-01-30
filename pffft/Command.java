package pffft; 

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;

/**
 * Allows the user to extend the program by providing their own commands.
 */
public class Command {
    //Variables plugin makers can user.
    /**
     * A reference to the graphics object.
     */
    protected ClientGUI gui;

    /**
     * A reference to the network object.
     */
    protected ClientNetwork network;

    /**
     * A reference to the parser object.
     */
    protected ClientParser parser;

    /**
     * A reference to the logger object.
     */
    protected ClientLogger logger;
    
    /**
     * A reference to the audio GUI object.
     */
    protected AudioGUI audioGUI;
    
    /**
     * A reference to the audio network object.
     */
    protected AudioNetwork audioNetwork;

    //General variables used by Parser.
    /**
     * The name used by the command. Typing '/' plus the identifier executes this command.
     */
    public String identifier;

    /**
     * The decription used by the command. The /help command uses this for the description.
     * <p>
     * While the description can be anything, the standard usage involves some notification of
     * the number and type of parameters (ie, [String ip] or [int port]), and a short explanation
     * of the command's intended effects.
     */
    public String description;

    /**
     * A general Object array used to determine the arguments this command needs to be passed.
     * <p><p>
     * This array is initialised on creation of the command. While it's preferred to make this
     * array contain examples of the parameters to be passed (ie, new Object[]{new String()}),
     * this isn't essential.
     * <p><p>
     * The length of this array must be equal to the length of the array of arguments passed
     * to this command for this command to execute.
     */
    public Object[] parameters;

    /**
     * A helper integer that is equal to (parameters == null ? 0 : parameters.length).
     */
    public int numParameters;

    /**
     * An array representing parameters passed to this command. When execute() is called, assuming the
     * basic checks have passed, this array will hold a copy of the parameters, where they
     * can be accessed. Guaranteed to be of same length as the initial parameter array, parameters[].
     * <p><p>
     * You'll have to cast from Object to the proper type you'd like. There's no guarantee
     * of what exact Object each element will be, but it most likely will be a String.
     * <p><p>
     * Example usage: Say you have a command called 'define'. 'define' takes one String parameter.
     * To access this parameter for your own use, call args[0] and cast it as a String. Easy!
     */
    protected Object[] args;

    //Only used by default commands.
    /**
     * This is only used internally by default commands. Don't worry about this variable otherwise.
     */
    private DefaultCommandRunnable runnable;

    /**
     * Creates a new Command that will be recognized by the ClientParser.
     * This command will be parameterless.
     * 
     * @param name The name of the command. If 'name' is "test", for example, then
     *  doing the command '/test' inside of the input text area will execute action().
     */
    public Command(String name) {
        this(name, null, null);
    }

    /**
     * Creates a new Command that will be recognized by the ClientParser.
     * This command will be parameterless.
     * 
     * @param name The name of the command. If 'name' is "test", for example, then
     *  doing the command '/test' inside of the input text area will execute action().
     * @param description The formal description of the command, called on '/help' method.
     *  The recommended format includes information on usage, parameters, and any help the 
     *  user needs.
     */
    public Command(String name, String description) {
        this(name, description, null);
    }

    /**
     * Creates a new Command that will be recognized by the ClientParser.
     * This command will have parameters.
     * 
     * @param name The name of the command. If 'name' is "test", for example, then
     *  doing the command '/test' inside of the input text area will execute action().
     * @param description The formal description of the command, called on '/help' method.
     *  The recommended format includes information on usage, parameters, and any help the 
     *  user needs.
     * @param parameters A list of all of the parameters this command takes. This command
     *  will then expect the parameters to follow in this order when called.
     */
    public Command(String name, String description, Object[] parameters) {
        this(name, description, parameters, null);
    }

    /**
     * This method is only used for default commands. 
     */
    public Command(String name, String description, Object[] para, DefaultCommandRunnable runnable) {
        this.identifier = name;
        this.description = description;
        this.parameters = para;
        this.numParameters = para == null ? 0 : para.length;
        this.runnable = runnable;
    }

    /**
     * Called by the ClientParser class to initialize this Command with any variables it needs to
     * properly modify the program. Don't worry about this part; just know you can use the variables
     * passed inside of your own Action.
     */
    public final void init(Client c) {
        this.gui = c.gui;
        this.network = c.network;
        this.parser = c.parser;
        this.logger = c.logger;
        this.audioGUI = c.audioGUI;
        this.audioNetwork = c.audioNetwork;
    }

    /**
     * Execute the code inside of action, or run runnable. This code is run on another
     * Thread to try to prevent the user from hanging the client with plugins.
     * As a user, don't worry about this method.
     * 
     * <p><p>
     * Please try to be civil with your use of code. There are no checks in place on code passed,
     * so this can cause instability on both your client and friends' clients. 
     */
    public final void execute(Object[] para) { 
        if (runnable != null) {//If this is a system default command, run its executable.
            //Pass the Runnable the parameters we have.
            new Thread(runnable.pass(para)).start();
        } else { //Else, pass the parameters and execute action().
            args = para;
            
            new Thread() {
                public void run() {
                    try { //Try to do the custon command's text..
                        action();
                    } catch (Exception e) { //Catch any thrown exceptions and notify the user.
                        logger.addText("SYSTEM: Command " + identifier + " threw an exception: " + e.toString() + ".\n");
                    }
                }
            }.start();
        }
    }

    /**
     * This method is overridden by the code you provide in commands.txt.
     * <p>
     * By default, this method returns without executing any code.
     */
    protected void action() throws Exception {
        return;
    }

    /**
     * Returns a String representation of this Command.
     */
    public String toString() {
        String s = "";
        s+="Class= " + getClass() + " Name= " + identifier;
        return s;
    }
}
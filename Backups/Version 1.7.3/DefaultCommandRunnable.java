package pffft;

/**
 * A bridge between default commands and the parameter interface.
 * How this works is we have an empty Object array, args[]. This allows
 * subclasses to access the args[] array properly. When the command is called,
 * it is passed the relevant arguments, which sets up the args[] array to have
 * the same conditions as the one in Command for external plugins. This then allows
 * standard, default commands to use parameters.
 */
public class DefaultCommandRunnable implements Runnable {
    Object[] args;

    public DefaultCommandRunnable() {}

    public DefaultCommandRunnable pass(Object[] args) {
        this.args = args;
        return this;
    }
    
    public void run() {
        return;
    }
}
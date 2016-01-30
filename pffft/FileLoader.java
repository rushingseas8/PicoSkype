package pffft;

import java.io.*;

/**
 * A class that makes importing files from inside the jar file, and putting them back into the jar file, much
 * more easy and much less verbose. Provides static helper methods for the whole program.
 * <p>
 * This class goes against the heirarchy that Client* imposes, where it needs a constructor and then an init()
 * method to be called; this is used in Runner, and so this does not make sense. Instead, it is a static class
 * that is available from the start.
 */
public class FileLoader {
    //Don't call this, man!
    private FileLoader() {}

    /**
     * Loads a file from the jar or local directory and returns it.
     * <p><p>
     * This method will NOT create a new file if it does not exist, it only tries to load it in.
     * Please be sure to check file.exists(), file.isDirectory(), and so on before using the returned file.
     * <p><p>
     * The format of the uri string uses forward slashes for convenience; On Microsoft Windows, this
     * is automatically converted to a backslash when trying to load in the file.
     * <p><p>
     * This method can find files both inside and outside the JAR file; to access external files,
     * use "~/" at the start of your String, and the search will start from the directory the 
     * JAR file is located in. This is to simplify finding out every time.
     * An example of this usage is "~/pico/commands/commands.txt".
     * <p><p>
     * To find Files within the JAR, do NOT use "~/" at the start, use "/" or start writing the
     * URI directly. 
     * An example of this usage is "pico/images/background.png".
     * <p><p>
     * Internally, this method uses System.getProperty("user.dir") to find the local directory, and
     * searches within the jar file by using local paths. The forward slashes are replaced with
     * the given platform's default file separator.
     * <p><p>
     * 
     * @param uri A String representing a URI. Searches in the JAR by default; Start with
     *  "~/" to look externally in the same directory as the JAR.
     *  
     * @return The file that was loaded in, or null if it does not exist.
     */
    public static File load(String uri) {
        String newURI = parse(uri);
        
        System.out.println("Loading " + newURI);

        File toReturn = new File(newURI);
        if(toReturn.exists()) return toReturn;
        else return null;
    }

    /**
     * If the directory at the given uri does not exist, create a directory there.
     * 
     * @return The newly made directory at the location of URI, null if the directory was not made.
     */
    public static File makeDir(String uri) {
        String newURI = parse(uri);
        
        File fi = new File(newURI);
        if(fi != null && (!fi.exists() || !fi.isDirectory())) 
            fi.mkdirs();
        return fi;
    }

    /**
     * Try to create a file at the given uri if it does not already exist.
     * 
     * @return The file at the given URI if it either exists or was created; null otherwise.
     */
    public static File makeFile(String uri) {
        String newURI = parse(uri);
        File fi = new File(newURI);
        if(!fi.exists()) {
            try {
                fi.createNewFile();
                return fi;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return fi;
    }

    private static String parse(String uri) {
        String toReturn = uri;
        if (toReturn == null) return null;

        //Handle special cases
        if (toReturn.startsWith("~/")) toReturn = System.getProperty("user.dir") + toReturn.substring(toReturn.indexOf("~/") + 1);
        else if (toReturn.startsWith("/")) toReturn = toReturn.substring(toReturn.indexOf("/"));

        //Make the file name platform-compatible
        toReturn.replaceAll("/", File.separator);
        return toReturn;
    }
}
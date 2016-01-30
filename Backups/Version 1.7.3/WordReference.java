package pffft; 

import java.io.*;
import java.net.*;
/**
 * WordReference class provides a series of static methods which allow a user to get
 * various types of information about a particular word.  Some examples include
 * getting the most common definition of a word, all of the definitions, and thesaurus reference. 
 * 
 * @author Adam Rzadkowski of PFFFT Productions LLC
 * @version 0.1
 */
public class WordReference
{
    public static void main(String[] args) {
        System.out.println("red: " + getDefinition("red") + "\n\n");
        System.out.println("yellow: " + getDefinition("yellow") + "\n\n");
        System.out.println("rhombus: " + getDefinition("rhombs") + "\n\n");
    }

    public static String getDefinition(String word) {
        String definition = "";

        try {
            URL url = new URL("http://dictionary.reference.com/browse/" + word);
            URLConnection urlConnection = ((URLConnection)url.openConnection());

            InputStreamReader inputStream = new InputStreamReader(urlConnection.getInputStream());
            BufferedReader reader = new BufferedReader(inputStream);

            String htmlText = "";
            String wantedLine = "";
            while ((htmlText = reader.readLine()) != null) {
                if (htmlText.contains("<div class=\"def-content\">")) {
                    //System.out.println(wantedLine = (reader.readLine()));
                    wantedLine = reader.readLine();
                    definition = wantedLine.substring(0,wantedLine.indexOf("</div>")).trim();
                    break;
                }
            }
        } catch (Exception E) {
            System.err.println("Well fuck.  The shit broke so hard I can't even fix it.");
            E.printStackTrace();
        }
        return definition;
    }
}

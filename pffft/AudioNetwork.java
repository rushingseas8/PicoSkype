package pffft;

import javax.sound.sampled.*;
import java.net.*;
import java.io.*;
import java.awt.*;
import java.util.*;

/**
 * Handles the networking and guts related to audio, as an analogue to ClientNetwork.
 */
public class AudioNetwork {
    //For program interconnectivity
    public Client cli;

    //Network variables.
    public String ip;
    public int port;

    public ServerSocket server;
    public Socket client;

    private InputStream input;
    private OutputStream output;

    private boolean isServer;

    //Audio variables.
    private AudioFormat format;
    private TargetDataLine microphone;
    private SourceDataLine speakers;

    private int CHUNK_SIZE;
    private byte[] recievingData;
    private byte[] sendingData;

    private int numBytesSent;
    private int numBytesRead;
    private int bytesSent;
    private int bytesRead;

    private boolean muted;

    //Reference to the visualisers we have to pass our data to.
    private AudioVisualiser2 yourVis;
    private AudioVisualiser2 theirVis;

    /**
     * Establish an audio connection on the given IP and port.
     * Default port is the text port plus one.
     */
    public AudioNetwork(String ip, int port) {
        //Instance variables
        this.ip = ip;
        this.port = port;
    }

    public void init(Client c) {
        this.cli = c;
        this.yourVis = c.audioGUI.yourVis;
        this.theirVis = c.audioGUI.theirVis;
    }

    public void begin() {
        //Set up the network first; no need to worry about audio setup if the network isn't up, right?
        try {
            tryConnect();
        } catch (IOException i) {
            cli.logger.addText(i.toString() + " at " + i.getStackTrace()[0] + "\n");
        }

        //Set up the input and output streams, if the network setup was good.
        if(client != null) {
            try {
                input = client.getInputStream();
                output = client.getOutputStream();
            } catch (IOException i) {
                cli.logger.addText("Failed to setup audio network streams.");
                return;
            }
        } else {return;} //Return if tryConnect returned null client/server variables.

        //Okay, network is all set up! Now for the audio.

        //The format to use for audio. 44.1k is HD, 16k is SD, 8k is LD.
        format = new AudioFormat(16000.0f, 16, 1, true, true);

        //Try to set up the microphone listener.
        try {
            /**TEST- Commented below line. Should fix some issues.*/
            //microphone = AudioSystem.getTargetDataLine(format); 
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format); //Added chunk_size parameter here, should speed things up.
        } catch (LineUnavailableException l) {
            cli.logger.addText("Microphone format unsupported: " + format);
            return;
        }

        microphone.start();

        //Try to set up the speakers with the same format as the microphone.
        try {
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
            speakers = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            speakers.open(format); 
        } catch (LineUnavailableException l) {
            cli.logger.addText("Speaker format unsupported: " + format);
            return;
        }

        //Start the input and outputs!
        speakers.start();  

        /**
         * 32 is very, very CPU intensive, but as close to realtime as possible.
         * microphone.getBufferSize() / 50 is fast, but intensive.
         * microphone.getBufferSize() / 5; is a good balance.
         * Anything much higher will have the potential to overflow the buffer.
         */
        CHUNK_SIZE = microphone.getBufferSize() / 5;

        //Buffers for reading in and sending out data.
        recievingData = new byte[CHUNK_SIZE];
        sendingData = new byte[CHUNK_SIZE];

        //We're good, so start the network thread.
        cli.logger.addText("Audio- all good!\n");
        new AudioSendThread().start();
        //new AudioReadThread().start();
    }

    /**
     * Attempts to establish a connection with the given IP and port.
     * Analogous to tryConnectServer() in ClientNetwork.
     */
    private void tryConnect() throws IOException {
        try {
            if (!ip.equals("")) {
                try {
                    client = new Socket(ip, port);
                    //cli.logger.addText("Connected audio as client.\n");
                    isServer = false;
                    return;
                } catch (IOException e) {
                    //cli.logger.addText("Failed to connect audio as client.\n");
                    //cli.logger.addText(e.toString() + " at " + e.getStackTrace()[0] +"\n");
                }
            }

            server = new ServerSocket(port);
            client = server.accept();
            //cli.logger.addText("Connected audio as server.\n");
            isServer = true;
            return;
        } catch (IOException e) {
            cli.logger.addText("Audio failed to connect!\n");
            throw new IOException(e);
        }            
    }

    /**
     * Reconnects us to the partner if at all possible.
     * Analogous to attemptReconnect() in ClientNetwork.
     */
    public void tryReconnect() {
        //Close any active connections; both audio and network.
        try {
            safelyDestroy();
            if (input != null) input.close();
            if (output != null) output.close();
            if (server != null) server.close();
            if (client != null) client.close();
        } catch (IOException e) { 
            //cli.logger.addText(e.toString() + " at " + e.getStackTrace()[0] +"\n", Color.RED);
        }

        //Let the user know we're trying to reconnect audio.
        //cli.logger.addText("Trying to reconnect audio..\n", Color.BLUE);

        String temp = "";
        for(int i = 0; i < 300; i++) { //This will try to connect for roughly five minutes.
            try {
                if(!isServer) {
                    client = new Socket(ip, port);
                    cli.logger.addText("Reconnected audio as client!\n", Color.BLUE);
                } else {
                    server = new ServerSocket(port);
                    client = server.accept();
                    cli.logger.addText("Reconnected audio as server!\n", Color.BLUE);
                }
            } catch (IOException e) {
                if (!e.toString().equals(temp)) { //Every time we get a different error message, update the user (prevents excess spam)
                    temp = e.toString();
                    //cli.logger.addText("Audio ErInf: " + temp + "\n", Color.RED);
                }

                //Wait a second between attempts
                try{Thread.sleep(1000);}catch(Exception meow) {}

                continue;
            }

            //Set up data streams
            try {
                input = client.getInputStream();
                output = client.getOutputStream();
            } catch (IOException e) {
                cli.logger.addText("Audio failed to setup network streams.\n", Color.RED);
            }

            //Start up mic/speakers again
            microphone.start();
            speakers.start();

            //Set up threads again
            new AudioSendThread().start();

            //Break out of the reconnect loop
            return;
        }
    }

    /**
     * Toggles the state of the mute variable.
     */
    public void setMute(boolean to) {
        this.muted = to;
        
        //This makes the muting take effect faster.
        if(muted) {
            if(microphone != null) microphone.flush();
            if(speakers != null) speakers.flush();
        }
    }

    private void send() throws IOException {
        numBytesSent = microphone.read(sendingData, 0, CHUNK_SIZE); //Read from mic
        output.write(sendingData); //Send over the internet

        //Display sound
        yourVis.setArray(sendingData);

        bytesSent+=numBytesSent;
    }

    //Make the reading buffer bigger; constantly read into the reading buffer; write to speakers on another thread.
    private void read() throws IOException {
        numBytesRead = input.read(recievingData, 0, CHUNK_SIZE); //Read from the internet
        speakers.write(recievingData, 0, CHUNK_SIZE); //Write to speakers 

        //Display sound
        theirVis.setArray(recievingData);

        recievingData = new byte[CHUNK_SIZE]; //Clear buffer
        bytesRead+=numBytesRead;
    }   

    //Stops the microphone and speaker lines to prevent memory leaks and allow the program to recover.
    private void safelyDestroy() {
        microphone.stop();
        speakers.stop();
    }

    public static void main() {
        new AudioNetwork("", 12001);
    }

    /**
     * A helper thread that constantly sends over our microphone's data (if not muted!) and reads in 
     * the partner's data.
     */
    private class AudioSendThread extends Thread {
        public void run() {
            while(true) {
                try {
                    if(!muted) {
                        send();
                        read();
                        continue;
                    }
                    try {Thread.sleep(500);}catch(Exception e){} //Idle
                } catch (IOException e) {
                    //On error, exit the thread and shutdown the audio.
                    //cli.logger.addText("Audio stream broke or was disconnected!\n", Color.RED);
                    //cli.logger.addText("IOException in send/read! Attempting reconnect.\n", Color.RED);
                    //cli.logger.addText(e.toString() + " at " + e.getStackTrace()[0] +"\n", Color.RED);
                    break;
                }
            }

            //If we ever crash, try to reconnect. Once we either fail or succeed, destroy this thread.
            tryReconnect();
            return;
        }
    }
}
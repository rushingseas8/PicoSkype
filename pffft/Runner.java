package pffft; 

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

/**
 * Authenticates a user prior to running the main program.
 * @author George Aleksandrovich
 * @version 1.2
 */
public class Runner {
    static String version = "1.2";
    
    static JFrame frame;
    static JPanel panel;
    static JLabel port, ip, username;
    static JComboBox portField, ipField, userField;

    static boolean optionsVisible;
    
    static boolean[] options;
    static boolean commandUpdatesForced;

    public static void main(String[] args) {
        frame = new JFrame("Authenticator " + version);
        frame.setSize(300, 130);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        panel = new JPanel();
        panel.setSize(300, 130);
        panel.setLayout(null);
        
        //Load in the remembered contact information
        File contacts = FileLoader.makeFile("~/pico/contacts.txt");

        port = new JLabel("Port num:");
        portField = new JComboBox();
        portField.setEditable(true);    
        portField.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent a) {
                    doSignIn();
                }
            });        

        ip = new JLabel("IP Address:");
        ipField = new JComboBox();
        ipField.setEditable(true);    
        ipField.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent a) {
                    portField.requestFocus();
                }
            });        

        username = new JLabel("Username:");
        userField = new JComboBox();
        userField.setEditable(true);    
        userField.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent a) {
                    ipField.requestFocus();
                }
            });

        username.setBounds(10, 5, 70, 20);
        panel.add(username);
        userField.setBounds(90, 5, 195, 20);
        panel.add(userField);
        ip.setBounds(10, 30, 70, 20);
        panel.add(ip);
        ipField.setBounds(90, 30, 195, 20);
        panel.add(ipField);
        port.setBounds(10, 55, 70, 20);
        panel.add(port);
        portField.setBounds(90, 55, 195, 20);
        panel.add(portField);

        JButton optionsButton = new JButton("Options");
        optionsVisible = false;
        optionsButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent a) {
                    if(optionsVisible = !optionsVisible) {
                        frame.setSize(300, 160);
                        panel.setSize(300, 160);
                        frame.revalidate();
                    } else {
                        frame.setSize(300, 130);
                        panel.setSize(300, 130);
                        frame.revalidate();                        
                    }
                }
            });
        JButton enterButton = new JButton("Submit");
        enterButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent a) {
                    doSignIn();
                }
            });

        //optionsButton.setBounds(10, 81, 175, 20);
        optionsButton.setBounds(10, 81, 121, 20);
        panel.add(optionsButton);
        //enterButton.setBounds(180, 81, 101, 20);
        enterButton.setBounds(160, 81, 121, 20);
        panel.add(enterButton);

        options = new boolean[5];
        
        JCheckBox forceUpdateCommands = new JCheckBox("Force load plugins");
        commandUpdatesForced = false;
        options[0] = commandUpdatesForced;
        forceUpdateCommands.setSelected(commandUpdatesForced);
        forceUpdateCommands.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent i) {
                    commandUpdatesForced = !commandUpdatesForced;
                    options[0] = commandUpdatesForced;
                    forceUpdateCommands.setSelected(commandUpdatesForced);
                }
            });
        forceUpdateCommands.setBounds(10, 110, 380, 20);
        panel.add(forceUpdateCommands);

        frame.add(panel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        return;
    }

    private static void doSignIn() {
        //Get the information, and parse as needed.
        String user = (String)userField.getSelectedItem();
        String ip = (String)ipField.getSelectedItem();
        int port = 12000;

        //Default port is 12000.
        try { port = Integer.parseInt((String)portField.getSelectedItem()); }
        catch (NumberFormatException n) {}

        //Default username is "Friend". 
        if(user == null || user.trim().equals("")) user = "Friend"; 
        
        //**FOR TESTING ONLY, REMOVE FOR FINAL PRODUCT
        if(ip == null || ip.trim().equals("")) ip = "localhost";

        //Kill this GUI.
        if (frame != null)
            frame.dispose();

        //Run the actual client.
        final String user2 = user; final String ip2 = ip; final int port2 = port;
        
        new Thread(){public void run() {new Client(user2, ip2, port2, options);}}.start();
        return;
    }
}
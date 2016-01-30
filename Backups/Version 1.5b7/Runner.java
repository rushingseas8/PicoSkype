import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Authenticates a user prior to running the main program.
 * @author George Aleksandrovich
 * @version 1.1
 */
public class Runner {
    static JFrame frame;
    static JPanel panel;
    static JLabel port, ip, username;
    static JTextField portField, ipField, userField;
    
    public static void main(String[] args) {
        frame = new JFrame("Authenticator 1.1");
        frame.setSize(300, 130);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        panel = new JPanel();
        panel.setSize(300, 130);
        panel.setLayout(null);

        port = new JLabel("Port num:");
        portField = new JTextField();
        portField.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent a) {
                    doSignIn();
                }
            });        

        ip = new JLabel("IP Address:");
        ipField = new JTextField();
        ipField.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent a) {
                    portField.requestFocus();
                }
            });        

        username = new JLabel("Username:");
        userField = new JTextField();
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

        JLabel info = new JLabel("");
        JButton enterButton = new JButton("Submit");
        enterButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent a) {
                    doSignIn();
                }
            });

        info.setBounds(10, 81, 175, 20);
        panel.add(info);
        enterButton.setBounds(180, 81, 101, 20);
        panel.add(enterButton);

        frame.add(panel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void doSignIn() {
        //Get the information, and parse as needed.
        String user = userField.getText();
        String ip = ipField.getText();
        int port = 12000;
        
        //Default port is 12000.
        try { port = Integer.parseInt(portField.getText()); }
        catch (NumberFormatException n) {}

        //Default username is "Friend". 
        if(user.equals("")) user = "Friend"; 

        //Kill this GUI.
        if (frame != null)
            frame.dispose();

        //Run the actual client.
        final String user2 = user; final int port2 = port;
        new Thread(){public void run() {new Client(user2, ip, port2);}}.start();
    }
}
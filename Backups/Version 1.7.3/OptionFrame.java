package pffft; 

import javax.swing.*;
import java.awt.*;
/**
 * An options menu! By popular request.
 */
public class OptionFrame extends JFrame {
    public OptionFrame(JFrame parent) {
        super("Options menu");
        setSize(320, 240);
        setLocationRelativeTo(parent);

        JPanel panel = new JPanel();
        panel.setSize(320, 240);
        panel.setLayout(new GridLayout(5, 2));

        /*
        panel.add(new JLabel("Enable greentext?"));
        String s1 = greentextEnabled ? "Enabled" : "Disabled";
        JCheckBox greentext = new JCheckBox(s1, greentextEnabled);
        greentext.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent i) {
                    greentextEnabled = greentext.isSelected();
                    greentext.setText(greentextEnabled ? "Enabled" : "Disabled");
                }
            });
        panel.add(greentext);*/

        add(panel);
        setVisible(true);
    }
}
package stb.image.sample.utility;

import javax.swing.*;
import java.awt.*;

public class Swing {
    public static void centreWindow(Window frame) {
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (int) ((dimension.getWidth() - frame.getWidth()) / 2);
        int y = (int) ((dimension.getHeight() - frame.getHeight()) / 2);
        frame.setLocation(x, y);
    }

    public static void showErrorMessageBox(Component parent, String message) {
        JOptionPane.showMessageDialog(parent,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
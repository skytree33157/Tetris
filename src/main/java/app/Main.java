package app;

import javax.swing.SwingUtilities;

import menu.screens.StartMenuScreen;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StartMenuScreen menu = new StartMenuScreen();
            menu.setVisible(true);
        });
    }
}

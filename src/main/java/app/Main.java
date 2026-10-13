package app;

import javax.swing.SwingUtilities;

import menu.StartMenu;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StartMenu menu = new StartMenu();
            menu.setVisible(true);
        });
    }
}

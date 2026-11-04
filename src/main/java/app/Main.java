package app;

import javax.swing.SwingUtilities;

import game.GameMode;
import menu.P2PGameRunner;
import menu.StartMenu;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0) {
            startMultiplayer(args);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            StartMenu menu = new StartMenu();
            menu.setVisible(true);
        });
    }

    private static void startMultiplayer(String[] args) {
        if (args.length != 2) {
            printMultiplayerUsage();
            return;
        }
        switch (args[0]) {
            case "--p2p-server" -> {
                try {
                    P2PGameRunner.startServer(GameMode.valueOf(args[1].toUpperCase()));
                } catch (IllegalArgumentException exception) {
                    printMultiplayerUsage();
                }
            }
            case "--p2p-client" -> {
                if (args[1].isBlank()) {
                    printMultiplayerUsage();
                } else {
                    P2PGameRunner.joinServer(args[1]);
                }
            }
            default -> printMultiplayerUsage();
        }
    }

    private static void printMultiplayerUsage() {
        System.err.println("Usage: --p2p-server NORMAL|ITEM | --p2p-client <server-address>");
    }
}

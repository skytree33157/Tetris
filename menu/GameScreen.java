package menu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import blocks.core.Block;
import blocks.core.BlockFactory;
import board.Board;
import game.GameLoop;
import game.GamePanel;

// 게임 화면(보드 + 사이드 정보 패널)을 담는 창
// GameLoop.java / Board.java는 기존 public API만 사용하고 수정하지 않음
public class GameScreen extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final int RENDER_INTERVAL_MS = 50;

    private final GameLoop gameLoop;
    private final Thread gameThread;
    private final GamePanel gamePanel;

    public GameScreen() {
        super("SeoulTech SE Tetris");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        Board board = new Board();
        Block firstBlock = BlockFactory.createRandomBlock();
        gameLoop = new GameLoop(board, firstBlock);

        gamePanel = new GamePanel(board);
        // GameLoop가 블록을 고정시키고 나면 내부적으로 새 Block 인스턴스로 교체하는데,
        // 현재 GameLoop에는 그 새 블록을 밖에서 조회할 getter가 없어서
        // 여기서는 "최초 스폰된 블록"만 실시간으로 추적 가능함.
        // TODO(game 담당자): GameLoop에 현재 블록 조회용 getter/콜백이 추가되면 매 블록마다 갱신하도록 교체
        gamePanel.setCurrentBlock(firstBlock);

        setLayout(new BorderLayout());
        add(gamePanel, BorderLayout.CENTER);
        add(createSidePanel(), BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopGame();
                dispose();
                System.exit(0);
            }
        });

        gameThread = new Thread(gameLoop, "game-loop");
        gameThread.setDaemon(true);
        gameThread.start();

        // GameLoop를 직접 호출하지 않고, 백그라운드에서 mutate되는 Board를 주기적으로 다시 그리기만 함
        Timer renderTimer = new Timer(RENDER_INTERVAL_MS, e -> gamePanel.repaint());
        renderTimer.start();
    }

    private JPanel createSidePanel() {
        JPanel side = new JPanel();
        side.setBackground(Color.BLACK);
        side.setPreferredSize(new Dimension(160, 0));
        side.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
        side.setLayout(new GridLayout(3, 1, 0, 10));

        JLabel title = new JLabel("TETRIS");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Courier", Font.BOLD, 20));

        // TODO(game 담당자): GameLoop가 레벨/줄수 조회용 getter를 제공하면 실시간 값으로 교체
        JLabel levelLabel = new JLabel("LEVEL: -");
        levelLabel.setForeground(Color.WHITE);
        levelLabel.setFont(new Font("Courier", Font.PLAIN, 16));

        JLabel linesLabel = new JLabel("LINES: -");
        linesLabel.setForeground(Color.WHITE);
        linesLabel.setFont(new Font("Courier", Font.PLAIN, 16));

        side.add(title);
        side.add(levelLabel);
        side.add(linesLabel);

        return side;
    }

    private void stopGame() {
        gameThread.interrupt();
    }
}

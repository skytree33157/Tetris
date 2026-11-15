package game;

import java.util.List;

// 네트워크 다인전 경기와 ActionController를 연결하는 어댑터
public interface MatchLink {
    boolean isRunning();

    // 줄 삭제로 만든 공격을 경기 관리자에게 전달
    void publishAttack(List<int[]> rows);

    // 개인 게임 오버를 경기 관리자에게 알림
    void reportGameOver();
}
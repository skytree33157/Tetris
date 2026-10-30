package game;

import org.junit.jupiter.api.Test;
import blocks.tetromino.OBlock;
import blocks.core.Block;
import board.Board;
import difficulty.Difficulty;
import java.util.List;
import score.ScoreManager;
import static org.junit.jupiter.api.Assertions.*;

class BattleControllerTest {
    @Test
    void eitherPlayersLossStopsBothAndResultIsFinal() {
        for (BattleController.Player loser:BattleController.Player.values()) {
            try (BattleController battle=new BattleController()) {
                ActionController one=player(new Board()), two=player(new Board());
                battle.registerPlayer(BattleController.Player.ONE,one);
                battle.registerPlayer(BattleController.Player.TWO,two);
                assertFalse(one.softDropAction()); // 준비 상태에서는 진행하지 않는다.
                battle.start();
                assertTrue(battle.reportGameOver(loser));
                assertFalse(battle.reportGameOver(loser.opponent()));
                assertFalse(battle.finishByTime(999,1));
                assertEquals(loser==BattleController.Player.ONE ? BattleController.Outcome.TWO_WINS
                        : BattleController.Outcome.ONE_WINS,battle.getOutcome());
                assertEquals(BattleController.EndReason.TOP_OUT,battle.getEndReason());
                for(ActionController action:List.of(one,two)) {
                    Block block=action.getCurrentBlock();
                    int x=block.getX(), y=block.getY(), score=action.getScore();
                    int[][] cells=action.getBoardSnapshot();
                    action.moveLeftAction(); action.moveRightAction(); action.rotateAction();
                    action.softDropAction(); action.automaticDropAction(); action.hardDropWithScoreAction();
                    assertTrue(action.isStopped());
                    assertEquals(x,block.getX()); assertEquals(y,block.getY());
                    assertEquals(score,action.getScore());
                    assertArrayEquals(cells,action.getBoardSnapshot());
                }
            }
        }
    }

    @Test
    void timeLimitSupportsEitherWinnerAndDraw() {
        int[][] scores={{20,10},{10,20},{10,10}};
        BattleController.Outcome[] outcomes={BattleController.Outcome.ONE_WINS,
                BattleController.Outcome.TWO_WINS,BattleController.Outcome.DRAW};
        for(int i=0;i<scores.length;i++) {
            try(BattleController battle=new BattleController()) {
                ActionController action=player(new Board());
                battle.registerPlayer(BattleController.Player.ONE,action);
                assertFalse(battle.finishByTime(20,10));
                battle.start();
                assertTrue(battle.finishByTime(scores[i][0],scores[i][1]));
                assertEquals(outcomes[i],battle.getOutcome());
                assertEquals(BattleController.EndReason.TIME_LIMIT,battle.getEndReason());
                assertTrue(action.isStopped());
                assertThrows(IllegalStateException.class,battle::start);
            }
        }
    }

    @Test
    void initialSpawnCollisionIsReportedWhenMatchStarts() {
        Board board=new Board(); board.addBlock(3,0,new int[][]{{1}});
        try(BattleController battle=new BattleController()) {
            battle.registerPlayer(BattleController.Player.ONE,player(board));
            battle.start();
            assertEquals(BattleController.Outcome.TWO_WINS,battle.getOutcome());
        }
    }

    @Test
    void bothInitialCollisionsAreADraw() {
        try(BattleController battle=new BattleController()) {
            for(BattleController.Player id:BattleController.Player.values()) {
                Board board=new Board(); board.addBlock(3,0,new int[][]{{1}});
                battle.registerPlayer(id,player(board));
            }
            battle.start();
            assertEquals(BattleController.Outcome.DRAW,battle.getOutcome());
        }
    }

    @Test
    void closingBeforeStartCancelsAndRejectsFuturePlay() {
        BattleController battle=new BattleController();
        ActionController action=player(new Board());
        battle.registerPlayer(BattleController.Player.ONE,action);
        battle.close(); battle.close();
        assertEquals(BattleController.Outcome.CANCELLED,battle.getOutcome());
        assertEquals(BattleController.EndReason.CANCELLED,battle.getEndReason());
        assertEquals(0,battle.receiveAttack(BattleController.Player.ONE,List.of()));
        assertThrows(IllegalStateException.class,battle::start);
        assertTrue(action.isStopped());
    }

    @Test
    void remoteAttackCopiesInputAndEveryRead() {
        int[] row={11,0,11,11,11,11,11,11,11,11};
        BattleController.Attack attack=new BattleController.Attack(BattleController.Player.ONE,List.of(row));
        row[0]=0;
        attack.rows().get(0)[0]=0;
        assertEquals(11,attack.rows().get(0)[0]);
        assertThrows(IllegalArgumentException.class,
                ()->new BattleController.Attack(BattleController.Player.ONE,List.of(new int[9])));
    }

    @Test
    void p2pReplicaReportsLocalLossButWaitsForServersFinalResult() {
        Board board=new Board(); board.addBlock(3,0,new int[][]{{1}});
        try(BattleController replica=new BattleController(false)) {
            ActionController action=player(board);
            replica.registerPlayer(BattleController.Player.TWO,action);
            replica.start();
            assertTrue(action.isStopped());
            assertEquals(BattleController.Outcome.IN_PROGRESS,replica.getOutcome());
            assertEquals(List.of(BattleController.Player.TWO),replica.drainOutgoingGameOvers());
            assertFalse(replica.reportGameOver(BattleController.Player.TWO));
            assertTrue(replica.drainOutgoingGameOvers().isEmpty());
            assertFalse(replica.finishByTime(10,1));
            // 양쪽 동시 탈락 등도 서버가 확정한 결과 하나로 수렴한다.
            assertTrue(replica.acceptRemoteResult(BattleController.Outcome.DRAW,BattleController.EndReason.TOP_OUT));
            assertFalse(replica.acceptRemoteResult(BattleController.Outcome.ONE_WINS,BattleController.EndReason.TOP_OUT));
            assertEquals(BattleController.Outcome.DRAW,replica.getOutcome());
        }
    }

    @Test
    void authorityCannotBeOverwrittenByRemoteResult() {
        try(BattleController authority=new BattleController()) {
            authority.registerPlayer(BattleController.Player.ONE,player(new Board()));
            authority.start();
            assertThrows(IllegalStateException.class,()->authority.acceptRemoteResult(
                    BattleController.Outcome.TWO_WINS,BattleController.EndReason.TOP_OUT));
        }
    }

    private static ActionController player(Board board) {
        return new ActionController(board,new OBlock(),new GameStateManager(),new ScoreManager(),
                Difficulty.NORMAL,GameMode.NORMAL,0);
    }
}

package game;

import blocks.core.Block;
import blocks.tetromino.OBlock;
import blocks.tetromino.TBlock;
import board.Board;
import difficulty.Difficulty;
import item.types.BombItem;
import item.types.LineClearItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import score.ScoreManager;
import static org.junit.jupiter.api.Assertions.*;

class BattleAttackTest {
    private final List<BattleController> battles = new ArrayList<>();

    @AfterEach
    void closeBattles() { battles.forEach(BattleController::close); }

    private BattleController battle() {
        BattleController battle = new BattleController();
        battles.add(battle);
        return battle;
    }

    private ActionController player(BattleController battle, BattleController.Player id,
                                    Board board, Block block, int animationMs) {
        ActionController action = new ActionController(board, block, new GameStateManager(),
                new ScoreManager(), Difficulty.NORMAL, GameMode.ITEM, animationMs);
        battle.registerPlayer(id, action);
        return action;
    }

    @Test
    void doubleClearProducesRowsWithOnlyTheLockingBlocksCellsRemoved() {
        BattleController battle = battle();
        Board board = new Board();
        fillExcept(board,21,3,4);
        fillExcept(board,22,3,4);
        ActionController action = player(battle,BattleController.Player.ONE,board,new OBlock(),0);
        battle.start();
        action.hardDropAction();
        var attacks = battle.drainOutgoingAttacks();
        assertEquals(1, attacks.size());
        assertEquals(BattleController.Player.ONE,attacks.get(0).sender());
        assertEquals(2,attacks.get(0).rows().size());
        for (int[] row:attacks.get(0).rows()) assertArrayEquals(attackRow(3,4),row);
        assertTrue(battle.drainOutgoingAttacks().isEmpty());
    }

    @Test
    void singleClearsFromDifferentLocksDoNotAccumulateIntoAnAttack() {
        BattleController battle = battle();
        Board board = new Board();
        fillExcept(board,22,3,4);
        ActionController action = player(battle,BattleController.Player.ONE,board,new OBlock(),0);
        battle.start();
        action.hardDropAction();
        assertTrue(battle.drainOutgoingAttacks().isEmpty());
        int[][] shape = action.getCurrentBlock().getShape();
        int lowest = -1;
        for(int r=0;r<shape.length;r++) for(int c:shape[r]) if(c!=0) lowest=r;
        int[] floor = new int[10];
        Arrays.fill(floor,1);
        for(int c=0;c<shape[lowest].length;c++) if(shape[lowest][c]!=0) floor[3+c]=0;
        board.getBoard()[22]=floor;
        // Remove the previous block's remainder so the second block can reach this row.
        for(int r=0;r<22;r++) Arrays.fill(board.getBoard()[r],0);
        action.hardDropAction();
        assertTrue(battle.drainOutgoingAttacks().isEmpty());
    }

    @Test
    void tShapeDoesNotEraseTheEmptyCornersOfItsBoundingBox() {
        BattleController battle = battle();
        Board board = new Board();
        fillExcept(board,21,3,4,5);
        fillExcept(board,22,4);
        TBlock t = new TBlock();
        t.rotate(); t.rotate(); // 넓은 행이 위에 있어야 기존 칸에 막히지 않고 내려갈 수 있다.
        ActionController action = player(battle,BattleController.Player.ONE,board,t,0);
        battle.start();
        action.hardDropAction();
        List<int[]> rows=battle.drainOutgoingAttacks().get(0).rows();
        assertArrayEquals(attackRow(3,4,5),rows.get(0));
        assertArrayEquals(attackRow(4),rows.get(1));
    }

    @Test
    void itemForcedRowAndCompletedRowCountTogetherWithoutDoubleCounting() {
        BattleController battle = battle();
        Board board = new Board();
        fillExcept(board,21,3,4);
        board.addBlock(0,22,new int[][]{{2}});
        ActionController action = player(battle,BattleController.Player.ONE,board,
                new LineClearItem(new OBlock(),1,0),0);
        battle.start();
        action.hardDropAction();
        List<int[]> rows=battle.drainOutgoingAttacks().get(0).rows();
        assertEquals(2,rows.size());
        assertArrayEquals(attackRow(3,4),rows.get(0));
        assertArrayEquals(new int[]{11,0,0,0,0,0,0,0,0,0},rows.get(1));
    }

    @Test
    void bombSendsActuallyRemovedRowsButNotPartialDamage() {
        BattleController battle = battle();
        Board board = new Board();
        board.addBlock(3,21,new int[][]{{1},{1}});
        ActionController action=player(battle,BattleController.Player.ONE,board,new BombItem(),0);
        battle.start();
        action.hardDropAction(); // rests at row 20; explosion empties row 21 only
        assertTrue(battle.drainOutgoingAttacks().isEmpty());

        BattleController second=battle();
        Board twoRows=new Board();
        twoRows.addBlock(2,20,new int[][]{{1},{1}});
        ActionController bomb=player(second,BattleController.Player.ONE,twoRows,new BombItem(),0);
        // x=3 can pass the blocks at x=2; support at row 22 puts explosion center at 21.
        twoRows.addBlock(3,22,new int[][]{{1}});
        second.start();
        bomb.hardDropAction();
        assertEquals(3,second.drainOutgoingAttacks().get(0).rows().size());
    }

    @Test
    void bombIncludesFullRowsRemovedAfterExplosionInOriginalOrder() {
        BattleController battle=battle(); Board board=new Board();
        board.addBlock(3,21,new int[][]{{1}});
        fillExcept(board,22);
        ActionController action=player(battle,BattleController.Player.ONE,board,new BombItem(),0);
        battle.start(); action.hardDropAction();
        List<int[]> rows=battle.drainOutgoingAttacks().get(0).rows();
        assertEquals(2,rows.size());
        assertArrayEquals(new int[]{0,0,0,11,0,0,0,0,0,0},rows.get(0));
        assertArrayEquals(attackRow(),rows.get(1));
    }

    @Test
    void clearingMoreThanFourReceivedRowsDoesNotBreakTheLock() {
        BattleController battle=battle(); Board board=new Board();
        ActionController action=player(battle,BattleController.Player.ONE,board,new OBlock(),0);
        battle.start();
        battle.receiveAttack(BattleController.Player.ONE,
                List.of(attackRow(),attackRow(),attackRow(),attackRow(),attackRow()));
        action.hardDropAction();
        Block next=action.getNextBlock();
        assertDoesNotThrow(action::hardDropAction);
        assertTrue(board.findFullRows().isEmpty());
        assertSame(next,action.getCurrentBlock());
        assertEquals(5,battle.drainOutgoingAttacks().get(0).rows().size());
        assertTrue(action.getScore()>0);
    }

    @Test
    void animationPublishesOnlyAfterCompletionAndShutdownCancelsIt() {
        BattleController battle=battle();
        Board board=new Board();
        fillExcept(board,21,3,4); fillExcept(board,22,3,4);
        ActionController action=player(battle,BattleController.Player.ONE,board,new OBlock(),60_000);
        battle.start(); action.hardDropAction();
        assertTrue(battle.drainOutgoingAttacks().isEmpty());
        action.completeLineClear();
        assertEquals(1,battle.drainOutgoingAttacks().size());
    }

    @Test
    void opponentsLossCancelsPendingClearWithoutChangingBoardOrScore() {
        BattleController battle=battle();
        Board board=new Board();
        fillExcept(board,21,3,4); fillExcept(board,22,3,4);
        ActionController one=player(battle,BattleController.Player.ONE,board,new OBlock(),60_000);
        player(battle,BattleController.Player.TWO,new Board(),new OBlock(),0);
        battle.start(); one.hardDropWithScoreAction();
        assertTrue(one.isClearing());
        int[][] before=one.getBoardSnapshot(); int score=one.getScore();
        battle.reportGameOver(BattleController.Player.TWO);
        one.completeLineClear();
        assertArrayEquals(before,one.getBoardSnapshot());
        assertEquals(score,one.getScore());
        assertFalse(one.isClearing());
        assertTrue(battle.drainOutgoingAttacks().isEmpty());
        assertEquals(0,battle.receiveAttack(BattleController.Player.ONE,List.of(attackRow(0))));
    }

    @Test
    void pausedAnimationDoesNotPublishOrApplyQueuedRowsUntilResumed() {
        BattleController battle=battle();
        Board board=new Board();
        fillExcept(board,21,3,4); fillExcept(board,22,3,4);
        ActionController action=player(battle,BattleController.Player.ONE,board,new OBlock(),60_000);
        battle.start(); action.hardDropAction(); action.setPaused(true);
        battle.receiveAttack(BattleController.Player.ONE,List.of(attackRow(1)));
        action.completeLineClear();
        assertTrue(action.isClearing());
        assertEquals(1,action.getPendingAttackCount());
        assertTrue(battle.drainOutgoingAttacks().isEmpty());
        action.setPaused(false); action.completeLineClear();
        assertEquals(1,battle.drainOutgoingAttacks().size());
        assertEquals(0,action.getPendingAttackCount());
        assertArrayEquals(attackRow(1),board.getBoard()[22]);
    }

    @Test
    void fourLineClearSendsFourRows() {
        BattleController battle=battle(); Board board=new Board();
        for(int r=19;r<=22;r++) fillExcept(board,r,5);
        blocks.tetromino.IBlock block=new blocks.tetromino.IBlock(); block.rotate();
        ActionController action=player(battle,BattleController.Player.ONE,board,block,0);
        battle.start(); action.hardDropAction();
        List<int[]> rows=battle.drainOutgoingAttacks().get(0).rows();
        assertEquals(4,rows.size());
        for(int[] row:rows) assertArrayEquals(attackRow(5),row);
    }

    @Test
    void invalidAttackBatchDoesNotPartiallyFillTheQueue() {
        BattleController battle=battle();
        ActionController action=player(battle,BattleController.Player.ONE,new Board(),new OBlock(),0);
        battle.start();
        assertThrows(IllegalArgumentException.class,()->battle.receiveAttack(BattleController.Player.ONE,
                List.of(attackRow(0),new int[9])));
        assertEquals(0,action.getPendingAttackCount());
        assertThrows(IllegalArgumentException.class,()->battle.receiveAttack(BattleController.Player.ONE,
                List.of(new int[]{1,0,0,0,0,0,0,0,0,0})));
        assertEquals(0,action.getPendingAttackCount());
    }

    @Test
    void queueKeepsFirstTenRowsAndCopiesBothInputAndPreview() {
        BattleController battle=battle();
        ActionController action=player(battle,BattleController.Player.ONE,new Board(),new OBlock(),0);
        battle.start();
        List<int[]> first=new ArrayList<>();
        for(int i=0;i<8;i++) first.add(attackRow(i));
        assertEquals(8,battle.receiveAttack(BattleController.Player.ONE,first));
        assertEquals(2,battle.receiveAttack(BattleController.Player.ONE,
                List.of(attackRow(8),attackRow(9),attackRow(0),attackRow(1))));
        assertEquals(0,battle.receiveAttack(BattleController.Player.ONE,List.of(attackRow(0))));
        first.get(0)[1]=0;
        List<int[]> preview=action.getPendingAttackRows();
        for(int i=0;i<10;i++) assertArrayEquals(attackRow(i),preview.get(i));
        preview.get(0)[1]=0;
        assertEquals(11,action.getPendingAttackRows().get(0)[1]);
        assertEquals(10,action.getPendingAttackCount());
    }

    @Test
    void incomingRowsWaitForLockAndAreInsertedBeforeNextSpawn() {
        BattleController battle=battle();
        Board board=new Board();
        ActionController action=player(battle,BattleController.Player.ONE,board,new OBlock(),0);
        battle.start();
        Block next=action.getNextBlock();
        battle.receiveAttack(BattleController.Player.ONE,List.of(attackRow(0),attackRow(1)));
        assertEquals(0,board.getBoard()[22][9]);
        action.hardDropAction();
        assertEquals(2,board.getBoard()[19][3]);
        assertArrayEquals(attackRow(0),board.getBoard()[21]);
        assertArrayEquals(attackRow(1),board.getBoard()[22]);
        assertSame(next,action.getCurrentBlock());
        assertEquals(0,action.getPendingAttackCount());
    }

    @Test
    void incomingRowsCanTopOutThePlayerBeforeNextBlockIsSpawned() {
        BattleController battle=battle();
        Board board=new Board();
        board.addBlock(0,3,new int[][]{{1}});
        ActionController action=player(battle,BattleController.Player.ONE,board,new OBlock(),0);
        battle.start();
        Block falling=action.getCurrentBlock();
        battle.receiveAttack(BattleController.Player.ONE,List.of(attackRow(0)));
        action.hardDropAction();
        assertEquals(BattleController.Outcome.TWO_WINS,battle.getOutcome());
        assertSame(falling,action.getCurrentBlock());
        assertTrue(action.isStopped());
    }

    @Test
    void localRoutingWorksWithoutSocketsOrAnOutgoingDuplicate() {
        BattleController battle=battle();
        Board first=new Board();
        fillExcept(first,21,3,4); fillExcept(first,22,3,4);
        ActionController one=player(battle,BattleController.Player.ONE,first,new OBlock(),0);
        ActionController two=player(battle,BattleController.Player.TWO,new Board(),new OBlock(),0);
        battle.start(); one.hardDropAction();
        assertEquals(2,two.getPendingAttackCount());
        assertTrue(battle.drainOutgoingAttacks().isEmpty());
    }

    @Test
    void simultaneousAttacksAndTerminationDoNotDeadlock() throws Exception {
        BattleController battle=battle();
        Board a=new Board(), b=new Board();
        for(Board board:List.of(a,b)){fillExcept(board,21,3,4);fillExcept(board,22,3,4);}
        ActionController one=player(battle,BattleController.Player.ONE,a,new OBlock(),0);
        ActionController two=player(battle,BattleController.Player.TWO,b,new OBlock(),0);
        battle.start();
        ExecutorService executor=Executors.newFixedThreadPool(3);
        CountDownLatch go=new CountDownLatch(1);
        try {
            Future<?> f1=executor.submit(()->{await(go);one.hardDropWithScoreAction();});
            Future<?> f2=executor.submit(()->{await(go);two.hardDropWithScoreAction();});
            Future<?> f3=executor.submit(()->{await(go);battle.reportGameOver(BattleController.Player.ONE);});
            go.countDown();
            f1.get(2,TimeUnit.SECONDS); f2.get(2,TimeUnit.SECONDS); f3.get(2,TimeUnit.SECONDS);
            assertEquals(BattleController.Outcome.TWO_WINS,battle.getOutcome());
            int score=two.getScore();
            two.hardDropWithScoreAction(); two.softDropAction(); two.automaticDropAction();
            assertEquals(score,two.getScore());
        } finally {executor.shutdownNow();}
    }

    private static void await(CountDownLatch latch) {
        try {latch.await();} catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException(e);}
    }

    private static void fillExcept(Board board,int row,int...holes){
        int[] cells=new int[10]; Arrays.fill(cells,1);
        for(int c:holes)cells[c]=0;
        board.addBlock(0,row,new int[][]{cells});
    }

    private static int[] attackRow(int...holes){
        int[] cells=new int[10]; Arrays.fill(cells,11);
        for(int c:holes) cells[c]=0;
        return cells;
    }
}

package ui;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

import blocks.core.Block;
import blocks.core.BlockType;
import blocks.style.ColorMode;
import blocks.tetromino.IBlock;
import blocks.tetromino.JBlock;
import blocks.tetromino.LBlock;
import blocks.tetromino.OBlock;
import blocks.tetromino.SBlock;
import blocks.tetromino.TBlock;
import blocks.tetromino.ZBlock;
import item.types.BombItem;
import item.types.BonusItem;
import item.types.LineClearItem;
import item.types.SlowItem;
import item.types.WeightItem;
// FR-06·16·33·36: 화면 없는 이미지로 렌더러 검증. NEXT 패널 자체 검증 X.
class BlockRendererTest {
    @Test
    void copiedBlocksRenderExactlyLikeLiveBlocksIncludingItemsInEveryColorMode() {
        for (ColorMode mode : ColorMode.values()) {
            for (Block block : new Block[]{new OBlock(), new IBlock(), new BombItem(), new WeightItem(),
                    new LineClearItem(new OBlock(), 0, 0), new BonusItem(new OBlock(), 0, 0),
                    new SlowItem(new OBlock(), 0, 0)}) {
                block.setX(0); block.setY(0);
                var copy = game.ActionController.snapshotOf(block);
                BufferedImage live = image(), detached = image();
                Graphics2D g1 = live.createGraphics(), g2 = detached.createGraphics();
                try {
                    BlockRenderer.drawBlock(g1, block, 5, 5, 20, mode);
                    BlockRenderer.drawCells(g2, copy.cells(), 5, 5, 20, mode);
                } finally { g1.dispose(); g2.dispose(); }
                assertArrayEquals(pixels(live), pixels(detached), block.getClass().getSimpleName() + ": " + mode);
            }
        }
    }

    private BufferedImage image() { return new BufferedImage(90,90,BufferedImage.TYPE_INT_ARGB); }
    private int[] pixels(BufferedImage image) { return image.getRGB(0,0,90,90,null,0,90); }
    private BufferedImage cell(Block block, int row, int col, ColorMode mode, int size) {
        BufferedImage image = image(); Graphics2D g = image.createGraphics();
        try { BlockRenderer.drawBlockCell(g,block,row,col,15,15,size,mode); } finally { g.dispose(); }
        return image;
    }
    @Test
    void ordinaryCellsKeepLegacyRenderingInEveryModeAndSize() {
        for (ColorMode mode : ColorMode.values()) for (int size : new int[]{1,7,8,20,40})
            for (Block block : new Block[]{new IBlock(),new OBlock(),new TBlock(),new SBlock(),new ZBlock(),new JBlock(),new LBlock()}) {
                int row = 0, col = 0;
                search: for (int r=0;r<block.getHeight();r++) for (int c=0;c<block.getWidth();c++)
                    if (block.getShape()[r][c]!=0) { row=r; col=c; break search; }
                BufferedImage legacy=image(); Graphics2D g=legacy.createGraphics();
                try { BlockRenderer.drawCell(g,15,15,size,block.getType(),mode); } finally { g.dispose(); }
                assertArrayEquals(pixels(legacy),pixels(cell(block,row,col,mode,size)));
            }
    }
    @Test
    void attachedMarkersChangeOnlyMarkedCellAndKeepUnmarkedBackground() {
        for (ColorMode mode : ColorMode.values()) {
            OBlock source=new OBlock();
            for (Block item : new Block[]{new BonusItem(source,0,0),new LineClearItem(source,0,0),new SlowItem(source,0,0)}) {
                assertArrayEquals(pixels(cell(source,0,1,mode,40)),pixels(cell(item,0,1,mode,40)));
                BufferedImage markedCell = cell(item,0,0,mode,40);
                assertFalse(Arrays.equals(pixels(cell(source,0,0,mode,40)),pixels(markedCell)));
                assertEquals(Color.BLACK.getRGB(), markedCell.getRGB(17,17));
                assertEquals(Color.WHITE.getRGB(), markedCell.getRGB(19,19));
            }
        }
    }
    @Test
    void objectAndStoredCellMatchAndPreserveLegacyBorder() {
        for (ColorMode mode : ColorMode.values()) for (int size : new int[]{7,8,20,40})
            for (Block block : new Block[]{new BonusItem(new OBlock(),0,0),new LineClearItem(new OBlock(),0,0),new WeightItem(),new BombItem(),new SlowItem(new OBlock(),0,0)}) {
                int row=0,col=0;
                search: for(int r=0;r<block.getHeight();r++) for(int c=0;c<block.getWidth();c++)
                    if(block.getShape()[r][c]!=0) {row=r;col=c;break search;}
                BufferedImage actual=cell(block,row,col,mode,size), stored=image(); Graphics2D g=stored.createGraphics();
                try { BlockRenderer.drawBoardCell(g,15,15,size,ItemAppearanceResolver.toBoardCell(block,row,col),mode); }
                finally { g.dispose(); }
                assertArrayEquals(pixels(actual),pixels(stored));
                // 기존 2픽셀 테두리 영역 유지. 문자·폭탄 외형이 추가로 밖에 그려지는지 검증.
                BufferedImage background=image(); Graphics2D bg=background.createGraphics();
                try { BlockRenderer.drawCell(bg,15,15,size,ItemAppearanceResolver.resolve(block,row,col).type(),mode); }
                finally { bg.dispose(); }
                for(int y=0;y<90;y++) for(int x=0;x<90;x++)
                    if(x<15 || y<15 || x>=15+size || y>=15+size) assertEquals(background.getRGB(x,y),actual.getRGB(x,y), block.getClass().getSimpleName()+" size="+size+" x="+x+" y="+y);
            }
    }
    @Test
    void invalidArgumentsEmptyAndOutOfBoundsCellsDrawNothing() {
        BufferedImage image=image(); Graphics2D g=image.createGraphics(); OBlock block=new OBlock();
        try {
            BlockRenderer.drawCell(g,0,0,0,BlockType.O,ColorMode.NORMAL);
            BlockRenderer.drawCell(g,0,0,20,null,ColorMode.NORMAL);
            BlockRenderer.drawCell(g,0,0,20,BlockType.O,null);
            BlockRenderer.drawCell(null,0,0,20,BlockType.O,ColorMode.NORMAL);
            BlockRenderer.drawBlockCell(g,null,0,0,0,0,20,ColorMode.NORMAL);
            BlockRenderer.drawBlockCell(null,block,0,0,0,0,20,ColorMode.NORMAL);
            BlockRenderer.drawBlockCell(g,block,0,0,0,0,20,null);
            BlockRenderer.drawBlockCell(g,block,0,0,0,0,0,ColorMode.NORMAL);
            for(int[] cell:new int[][]{{-1,0},{2,0},{0,-1},{0,2}})
                BlockRenderer.drawBlockCell(g,block,cell[0],cell[1],0,0,20,ColorMode.NORMAL);
            BlockRenderer.drawBlockCell(g,new TBlock(),0,0,0,0,20,ColorMode.NORMAL);
            BlockRenderer.drawBoardCell(g,0,0,20,0,ColorMode.NORMAL);
            BlockRenderer.drawBoardCell(g,0,0,20,1,null);
            BlockRenderer.drawBoardCell(null,0,0,20,1,ColorMode.NORMAL);
            BlockRenderer.drawBoardCell(g,0,0,0,1,ColorMode.NORMAL);
            BlockRenderer.drawBlock(g,null,0,0,20,ColorMode.NORMAL);
            BlockRenderer.drawBlock(null,block,0,0,20,ColorMode.NORMAL);
            BlockRenderer.drawBlock(g,block,0,0,20,null);
            BlockRenderer.drawBlock(g,block,0,0,0,ColorMode.NORMAL);
        } finally { g.dispose(); }
        assertArrayEquals(new int[90*90],pixels(image));
    }
    @Test
    void wholeBlockUsesBoardCoordinatesAndSkipsCellsAboveBoard() {
        OBlock block=new OBlock(); block.setX(1); block.setY(-1);
        BufferedImage actual=image(),expected=image(); Graphics2D a=actual.createGraphics(),e=expected.createGraphics();
        try {
            BlockRenderer.drawBlock(a,block,10,10,20,ColorMode.NORMAL);
            BlockRenderer.drawBlockCell(e,block,1,0,30,10,20,ColorMode.NORMAL);
            BlockRenderer.drawBlockCell(e,block,1,1,50,10,20,ColorMode.NORMAL);
        } finally {a.dispose();e.dispose();}
        assertArrayEquals(pixels(expected),pixels(actual));
    }
    @Test
    void renderingPreservesCallerGraphicsState() {
        Graphics2D g=image().createGraphics();
        try {
            g.setColor(Color.MAGENTA); g.setBackground(Color.CYAN); g.setStroke(new BasicStroke(3));
            g.setFont(new Font(Font.MONOSPACED,Font.ITALIC,13));
            g.translate(2,3); g.setClip(0,0,80,80); g.setComposite(AlphaComposite.SrcOver.derive(0.6f));
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
            Paint paint=g.getPaint(); Stroke stroke=g.getStroke(); Font font=g.getFont();
            AffineTransform transform=g.getTransform(); Rectangle clip=g.getClipBounds();
            Composite composite=g.getComposite(); RenderingHints hints=g.getRenderingHints();
            for(Block item:new Block[]{new BombItem(),new WeightItem(),new BonusItem(new OBlock(),0,0),new SlowItem(new OBlock(),0,0)}) {
                int row=item instanceof WeightItem ? 1:0;
                BlockRenderer.drawBlockCell(g,item,row,0,10,10,40,ColorMode.NORMAL);
                assertEquals(paint,g.getPaint()); assertEquals(stroke,g.getStroke()); assertEquals(font,g.getFont());
                assertEquals(transform,g.getTransform()); assertEquals(clip,g.getClipBounds());
                assertEquals(composite,g.getComposite()); assertEquals(hints,g.getRenderingHints());
                assertEquals(Color.CYAN,g.getBackground());
            }
        } finally {g.dispose();}
    }
}

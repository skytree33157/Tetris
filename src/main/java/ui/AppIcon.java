package ui;

import java.awt.Image;
import java.awt.Window;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

// 창 왼쪽 위/작업표시줄 아이콘 (src/main/resources/icon/TFSE_*.png, packaging/TFSE.ico에서 추출)
public final class AppIcon {

    // 크기별로 넘기면 창 제목줄(작은 크기), 작업표시줄(큰 크기)에 맞는 것을 OS가 고름
    private static final int[] SIZES = {16, 32, 48, 256};
    private static List<Image> icons;

    private AppIcon() {
    }

    // 아이콘 파일이 없으면 기본 아이콘 유지
    public static void apply(Window window) {
        List<Image> images = load();
        if (!images.isEmpty()) {
            window.setIconImages(images);
        }
    }

    private static synchronized List<Image> load() {
        if (icons == null) {
            List<Image> images = new ArrayList<>();
            for (int size : SIZES) {
                try (InputStream in = AppIcon.class.getResourceAsStream("/icon/TFSE_" + size + ".png")) {
                    if (in != null) {
                        images.add(ImageIO.read(in));
                    }
                } catch (IOException e) {
                    // 읽지 못한 크기는 건너뜀
                }
            }
            icons = List.copyOf(images);
        }
        return icons;
    }
}

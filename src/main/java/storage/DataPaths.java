package storage;

import java.io.File;

// 설정/점수 파일 저장 위치
public final class DataPaths {

    private DataPaths() {
    }

    public static File dataDir() {
        // 테스트 등에서 경로를 바꿀 때 사용
        String override = System.getProperty("tetris.dataDir");
        // jpackage로 만든 exe에서 실행하면 exe 경로가 들어 있음
        String appPath = System.getProperty("jpackage.app-path");
        File dir;
        if (override != null && !override.isBlank()) {
            dir = new File(override);
        } else if (appPath != null) {
            dir = new File(new File(appPath).getParentFile(), "data");
        } else {
            // IDE/gradle로 실행할 때 (개발용)
            dir = new File(System.getProperty("user.home"), ".tfse/data");
        }
        dir.mkdirs();
        return dir;
    }

    public static File settingsFile() {
        return new File(dataDir(), "settings.properties");
    }

    public static File scoresFile() {
        return new File(dataDir(), "scores.txt");
    }
}

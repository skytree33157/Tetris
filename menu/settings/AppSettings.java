package menu.settings;

import blocks.style.ColorMode;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

// 설정값 보관 + 파일 저장/불러오기
public class AppSettings {
    
    private static final File SETTINGS_FILE =
        new File("menu/settings/settings.properties");

    private static final AppSettings INSTANCE = new AppSettings();

    private ScreenSize screenSize;
    private ColorMode colorMode;
    private final Map<KeyAction, Integer> keyBindings = new EnumMap<>(KeyAction.class);

    private AppSettings() {
        resetToDefaults();
        load();
    }

    public static AppSettings getInstance(){
        return INSTANCE;
    }

    public ScreenSize getScreenSize() {
        return screenSize;
    }

    public void setScreenSize(ScreenSize screenSize) {
        this.screenSize = screenSize;
    }
    
    public ColorMode getColorMode() {
        return colorMode;
    }

    public void setColorMode(ColorMode colorMode) {
        this.colorMode = colorMode;

    }

    public int getKey(KeyAction action) {
        return keyBindings.get(action);
    }

    public void setKey(KeyAction action, int keyCode) {
        keyBindings.put(action, keyCode);
    }

    
    // 기본값으로 복원 
    public void resetToDefaults() {
        screenSize = ScreenSize.MEDIUM;
        colorMode = ColorMode.NORMAL;
        keyBindings.put(KeyAction.MOVE_LEFT, KeyEvent.VK_LEFT);
        keyBindings.put(KeyAction.MOVE_RIGHT, KeyEvent.VK_RIGHT);
        keyBindings.put(KeyAction.MOVE_DOWN, KeyEvent.VK_DOWN);
        keyBindings.put(KeyAction.ROTATE, KeyEvent.VK_UP);
    }

    // 설정을 파일에 저장
    public void save() {
        SETTINGS_FILE.getParentFile().mkdirs();

        Properties props = new Properties();
        props.setProperty("screenSize", screenSize.name());
        props.setProperty("colorMode", colorMode.name());
        for (KeyAction action : KeyAction.values()) {
            props.setProperty("key." + action.name(), String.valueOf(keyBindings.get(action)));
        }

        try (FileOutputStream out = new FileOutputStream(SETTINGS_FILE)) {
            props.store(out, "SeoulTech SE Tetris settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 저장된 설정 불러오기 (파일 없으면 기본값 유지)
    public void load() {
        Properties props = new Properties();

        try (FileInputStream in = new FileInputStream(SETTINGS_FILE)) {
            props.load(in);
        } catch (IOException e) {
            return;
        }

        try {
            screenSize = ScreenSize.valueOf(props.getProperty("screenSize", screenSize.name()));
            colorMode = ColorMode.valueOf(props.getProperty("colorMode", colorMode.name()));
            for (KeyAction action : KeyAction.values()) {
                String value = props.getProperty("key." + action.name());
                if (value != null) {
                    keyBindings.put(action, Integer.parseInt(value));
                }
            }
        } catch (IllegalArgumentException e) {
            resetToDefaults(); // 저장된 값이 깨져 있으면 기본값으로 복구
        }
    }
}



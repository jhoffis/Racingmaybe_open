package settings_and_logging.hotkeys;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class HotkeyStorage {
    private static volatile HotkeyStorage instance;
    private final Properties properties;
    private static final String PROPERTIES_FILE = "keys.properties";

    private HotkeyStorage() {
        properties = new Properties();
        try (FileInputStream in = new FileInputStream(PROPERTIES_FILE)) {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HotkeyStorage getInstance() {
        if (instance == null) {
            synchronized (HotkeyStorage.class) {
                if (instance == null) {
                    instance = new HotkeyStorage();
                }
            }
        }
        return instance;
    }

    public synchronized void setHotkey(String key, int keycode) {
        properties.setProperty(key, String.valueOf(keycode));
        saveProperties();
    }

    public synchronized int getKeycodeFromFile(String key) {
        return Integer.parseInt(properties.getProperty(key, "-1"));
    }

    public synchronized int getKeycodeFromFile(String key, int defaultKeycode) {
        int keycode = Integer.parseInt(properties.getProperty(key, "-1"));
        if (keycode == -1) {
            setHotkey(key, defaultKeycode);
        }
        return keycode;
    }

    private synchronized void saveProperties() {
        try (FileOutputStream out = new FileOutputStream(PROPERTIES_FILE)) {
            properties.store(out, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
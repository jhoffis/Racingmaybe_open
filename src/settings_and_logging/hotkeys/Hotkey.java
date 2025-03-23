package settings_and_logging.hotkeys;

import org.lwjgl.glfw.GLFW;

public class Hotkey {

    private volatile int keycode = -1;
    public final int defaultKey;
    public final String name;
    public String prefix;
    private final String propertyKey;

    public Hotkey(String label, int defaultKeycode) {
        this.name = label;
        this.defaultKey = defaultKeycode;
        // replace all non-alphanumeric characters with empty string for storing in the properties file
        this.propertyKey = this.name.replaceAll("[^a-zA-Z0-9]", "");
        this.loadKeycode();
    }

    public Hotkey(String prefixed, int defaultKeycode, String prefix) {
        this(prefixed, defaultKeycode);
        this.prefix = prefix;
    }

    public String getPrefixedName() {
        if (prefix == null) {
            return name;
        } else {
            return "(" + prefix + ") " + name;
        }
    }

    public int getKeycode() {
        if (keycode == -1) {
            loadKeycode();
        }
        // might be worth logging here, for possible non recognized keycodes in config
        if (keycode == -1) {
            keycode = defaultKey;
        }
        return keycode;
    }


    public String getKeyName() {
        if (this.keycode == -1 || this.keycode == 0) {
            resetToDefault();
        }
        return keycodeToKeyname(this.keycode);
    }

    public void setKeycode(int keycode) {
        this.keycode = keycode;
        saveKeycode();
    }

    private void saveKeycode() {
        HotkeyStorage storage = HotkeyStorage.getInstance();
        storage.setHotkey(this.propertyKey, this.keycode);
    }

    private void loadKeycode() {
        HotkeyStorage storage = HotkeyStorage.getInstance();
        this.keycode = storage.getKeycodeFromFile(this.propertyKey);
        if (keycode == -1) {
            resetToDefault();
        }
    }

    public void resetToDefault() {
        this.setKeycode(defaultKey);
    }

    public static String keycodeToKeyname(int keycode) {

        int scancode = GLFW.glfwGetKeyScancode(keycode);
        String keyName = GLFW.glfwGetKeyName(keycode, scancode);

        if (keyName == null) {
            keyName = getFallbackKeyName(keycode);
        }

        if (keyName.length() == 1) {
            keyName = keyName.toUpperCase();
        } else {
            keyName = keyName.substring(0, 1).toUpperCase() + keyName.substring(1).toLowerCase();
        }

        return keyName;
    }

    private static String getFallbackKeyName(int keycode) {
        return switch (keycode) {
            case GLFW.GLFW_KEY_ESCAPE -> "Escape";
            case GLFW.GLFW_KEY_F1 -> "F1";
            case GLFW.GLFW_KEY_F2 -> "F2";
            case GLFW.GLFW_KEY_F3 -> "F3";
            case GLFW.GLFW_KEY_F4 -> "F4";
            case GLFW.GLFW_KEY_F5 -> "F5";
            case GLFW.GLFW_KEY_F6 -> "F6";
            case GLFW.GLFW_KEY_F7 -> "F7";
            case GLFW.GLFW_KEY_F8 -> "F8";
            case GLFW.GLFW_KEY_F9 -> "F9";
            case GLFW.GLFW_KEY_F10 -> "F10";
            case GLFW.GLFW_KEY_F11 -> "F11";
            case GLFW.GLFW_KEY_F12 -> "F12";
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_APOSTROPHE -> "'";
            case GLFW.GLFW_KEY_COMMA -> ",";
            case GLFW.GLFW_KEY_MINUS -> "-";
            case GLFW.GLFW_KEY_PERIOD -> ".";
            case GLFW.GLFW_KEY_SLASH -> "/";
            case GLFW.GLFW_KEY_0 -> "0";
            case GLFW.GLFW_KEY_1 -> "1";
            case GLFW.GLFW_KEY_2 -> "2";
            case GLFW.GLFW_KEY_3 -> "3";
            case GLFW.GLFW_KEY_4 -> "4";
            case GLFW.GLFW_KEY_5 -> "5";
            case GLFW.GLFW_KEY_6 -> "6";
            case GLFW.GLFW_KEY_7 -> "7";
            case GLFW.GLFW_KEY_8 -> "8";
            case GLFW.GLFW_KEY_9 -> "9";
            case GLFW.GLFW_KEY_SEMICOLON -> ";";
            case GLFW.GLFW_KEY_ENTER -> "Enter";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_BACKSPACE -> "Backspace";
            case GLFW.GLFW_KEY_INSERT -> "Insert";
            case GLFW.GLFW_KEY_DELETE -> "Delete";
            case GLFW.GLFW_KEY_RIGHT -> "Right";
            case GLFW.GLFW_KEY_LEFT -> "Left";
            case GLFW.GLFW_KEY_DOWN -> "Down";
            case GLFW.GLFW_KEY_UP -> "Up";
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> "Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> "Control";
            case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> "Alt";
            default -> "Unknown";
        };
    }

    public boolean equals(int keycode) {
        return this.keycode == keycode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Integer) {
            return this.getKeycode() == (Integer) obj;
        }
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Hotkey hotkey = (Hotkey) obj;
        return this.getKeycode() == hotkey.keycode;
    }
}
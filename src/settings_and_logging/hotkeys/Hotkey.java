package settings_and_logging.hotkeys;

import engine.graphics.ui.UITextField;
import org.lwjgl.glfw.GLFW;

public class Hotkey {
    private final String label;
    private int keycode;
    private final UITextField textField;
    private final String configLabel;
    private int defaultKey;

    public Hotkey(String displayLabel, String configLabel, UITextField textField) {
        this.label = displayLabel;
        this.configLabel = configLabel;
        this.textField = textField;
        loadKeycode();
    }

    public Hotkey(String displayLabel, String configLabel, int defaultKey) {
        this(displayLabel, configLabel, null);
        this.defaultKey = defaultKey;
    }

    public String getLabel() {
        return label;
    }

    public int getKeycode() {
        if (keycode == -1) {
            loadKeycode();
        }
        if (keycode == -1) {
            keycode = defaultKey;
        }
        return keycode;
    }

    public void setKeycode(int keycode) {
        this.keycode = keycode;
        saveKeycode();
    }

    public UITextField getTextField() {
        return textField;
    }

    public void loadKeycode() {
        HotkeyStorage storage = HotkeyStorage.getInstance();
        this.keycode = storage.getKeycodeFromFile(this.configLabel);
        if (keycode == -1) {
            resetToDefault();
        }
    }

    private void saveKeycode() {
        HotkeyStorage storage = HotkeyStorage.getInstance();
        storage.setHotkey(label, keycode);
    }

    public String getKeyName() {
        if (this.keycode == -1 || this.keycode == 0) {
            resetToDefault();
        }
        int scancode = GLFW.glfwGetKeyScancode(this.keycode);
        return GLFW.glfwGetKeyName(this.keycode, scancode);
    }

    public static String getKeyNameFromKeyCode(int keycode) {
        int scancode = GLFW.glfwGetKeyScancode(keycode);
        return GLFW.glfwGetKeyName(keycode, scancode);
    }

    public void resetToDefault() {
        keycode = defaultKey;
        saveKeycode();
    }
}
// src/settings_and_logging/hotkeys/Controls.java
package settings_and_logging.hotkeys;

import org.lwjgl.glfw.GLFW;

public enum Controls {
    INSTANCE;

    public static final Hotkey throttle;
    public static final Hotkey brake;
    public static final Hotkey clutch;
    public static final Hotkey shiftUp;
    public static final Hotkey shiftDown;
    public static final Hotkey nos;
    public static final Hotkey turbo;
    public static final Hotkey engineOn;
    public static final Hotkey left;
    public static final Hotkey right;
    public static final Hotkey lookBehind;
    public static final Hotkey ready;
    public static final Hotkey undo;
    public static final Hotkey sell;
    public static final Hotkey improve;
    public static final Hotkey quitRace;

    static {
        throttle = new Hotkey("Throttle", "Throttle", GLFW.GLFW_KEY_W);
        brake = new Hotkey("Brake", "Brake", GLFW.GLFW_KEY_S);
        nos = new Hotkey("NOS", "NOS", GLFW.GLFW_KEY_E);
        turbo = new Hotkey("Blow Turbo", "BlowTurbo", GLFW.GLFW_KEY_Q);
        lookBehind = new Hotkey("Look Behind", "LookBehind", GLFW.GLFW_KEY_R);
        clutch = new Hotkey("Clutch", "Clutch", GLFW.GLFW_KEY_SPACE);
        shiftUp = new Hotkey("Shift Up", "ShiftUp", GLFW.GLFW_KEY_UP);
        shiftDown = new Hotkey("Shift Down", "ShiftDown", GLFW.GLFW_KEY_DOWN);
        engineOn = new Hotkey("Engine On", "EngineOn", GLFW.GLFW_KEY_ENTER);
        left = new Hotkey("Left", "Left", GLFW.GLFW_KEY_A);
        right = new Hotkey("Right", "Right", GLFW.GLFW_KEY_D);
        ready = new Hotkey("Ready", "Ready", GLFW.GLFW_KEY_R, "CTRL: ready while chatting");
        undo = new Hotkey("Undo", "Undo", GLFW.GLFW_KEY_U, "CTRL: redo");
        sell = new Hotkey("Sell", "Sell", GLFW.GLFW_KEY_S, "CTRL: no prompt");
        improve = new Hotkey("Improve", "Improve", GLFW.GLFW_KEY_SPACE);
        quitRace = new Hotkey("Quit Race", "QuitRace", GLFW.GLFW_KEY_ESCAPE);
    }

    public static Hotkey[] getHotkeys() {
        return new Hotkey[] {throttle, brake, clutch, shiftUp, shiftDown, nos, turbo, engineOn, left, right, lookBehind, ready, undo, sell, improve, quitRace};
    }

    public static Hotkey[] getConfigurableHotkeys() {
        return new Hotkey[] {throttle, brake, nos, turbo, lookBehind, shiftUp, shiftDown, quitRace, ready, undo, sell, improve};
    }
}
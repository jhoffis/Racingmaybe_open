package settings_and_logging.hotkeys;

import org.lwjgl.glfw.GLFW;

public enum Controls {
    INSTANCE;

    public static final Hotkey throttle = new Hotkey("Throttle", GLFW.GLFW_KEY_W);
    public static final Hotkey brake = new Hotkey("Brake", GLFW.GLFW_KEY_S);
    public static final Hotkey nos = new Hotkey("NOS", GLFW.GLFW_KEY_E);
    public static final Hotkey turbo = new Hotkey("Blow Turbo", GLFW.GLFW_KEY_Q);
    public static final Hotkey lookBehind = new Hotkey("Look Behind", GLFW.GLFW_KEY_R);
    public static final Hotkey clutch = new Hotkey("Clutch", GLFW.GLFW_KEY_SPACE);
    public static final Hotkey shiftUp = new Hotkey("Shift Up", GLFW.GLFW_KEY_UP);
    public static final Hotkey shiftDown = new Hotkey("Shift Down", GLFW.GLFW_KEY_DOWN);
    public static final Hotkey engineOn = new Hotkey("Engine On", GLFW.GLFW_KEY_ENTER);
    public static final Hotkey left = new Hotkey("Left", GLFW.GLFW_KEY_A);
    public static final Hotkey right = new Hotkey("Right", GLFW.GLFW_KEY_D);
    public static final Hotkey ready = new Hotkey("Ready", GLFW.GLFW_KEY_R, "CTRL: ready while chatting");
    public static final Hotkey undo = new Hotkey("Undo", GLFW.GLFW_KEY_U, "CTRL: redo");
    public static final Hotkey sell = new Hotkey("Sell", GLFW.GLFW_KEY_S, "CTRL: no prompt");
    public static final Hotkey improve = new Hotkey("Improve", GLFW.GLFW_KEY_SPACE);
    public static final Hotkey quitRace = new Hotkey("Quit Race", GLFW.GLFW_KEY_ESCAPE);
    public static final Hotkey beep = new Hotkey("Beep", GLFW.GLFW_KEY_H);

    public static Hotkey[] getHotkeys() {
        return new Hotkey[] {throttle, brake, clutch, shiftUp, shiftDown, nos, turbo, engineOn, left, right, lookBehind, ready, undo, sell, improve, quitRace};
    }

    public static Hotkey[] getConfigurableHotkeys() {
        return new Hotkey[] {throttle, brake, nos, turbo, lookBehind, shiftUp, shiftDown, quitRace, ready, undo, sell, improve};
    }

}
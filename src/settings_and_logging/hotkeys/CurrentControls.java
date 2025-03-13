package settings_and_logging.hotkeys;

import org.lwjgl.glfw.GLFW;

public class CurrentControls {

    private final Hotkey throttle, brake, clutch, shiftUp, shiftDown, nos,
            turbo, engineOn, left, right, lookBehind, ready, undo, sell, improve, quitRace;
    private final Hotkey[] gears = new Hotkey[7];

    private static CurrentControls instance = null;

    protected CurrentControls() {


//      If properties file is not found, default keycodes are used,
//      otherwise the saved keycodes from keys.properties are loaded.

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
        gears[0] = new Hotkey("Gear 1", "Gear1", GLFW.GLFW_KEY_N);
        gears[1] = new Hotkey("Gear 2", "Gear2", GLFW.GLFW_KEY_U);
        gears[2] = new Hotkey("Gear 3", "Gear3", GLFW.GLFW_KEY_J);
        gears[3] = new Hotkey("Gear 4", "Gear4", GLFW.GLFW_KEY_I);
        gears[4] = new Hotkey("Gear 5", "Gear5", GLFW.GLFW_KEY_K);
        gears[5] = new Hotkey("Gear 6", "Gear6", GLFW.GLFW_KEY_O);
        gears[6] = new Hotkey("Gear 7", "Gear7", GLFW.GLFW_KEY_L);
    }

    public static CurrentControls getInstance() {
        if (instance == null) {
            instance = new CurrentControls();
        }
        return instance;
    }

    public Hotkey[] getHotkeys() {
        Hotkey[] allHotkeys = new Hotkey[] {throttle, brake, clutch, shiftUp, shiftDown, nos, turbo, engineOn, left, right, lookBehind, ready, undo, sell, improve, quitRace};
        Hotkey[] allControls = new Hotkey[allHotkeys.length + gears.length];
        System.arraycopy(allHotkeys, 0, allControls, 0, allHotkeys.length);
        System.arraycopy(gears, 0, allControls, allHotkeys.length, gears.length);
        return allControls;
    }

    public Hotkey[] getConfigurableHotkeys() {
        return new Hotkey[] {throttle,brake,nos,turbo,lookBehind,shiftUp,shiftDown,quitRace,ready,undo,sell,improve};
    }

    public Hotkey getThrottle() {
        return throttle;
    }

    public void setThrottle(int keycode) {
        throttle.setKeycode(keycode);
    }

    public Hotkey getBrake() {
        return brake;
    }

    public void setBrake(int keycode) {
        brake.setKeycode(keycode);
    }

    public Hotkey getClutch() {
        return clutch;
    }

    public void setClutch(int keycode) {
        clutch.setKeycode(keycode);
    }

    public Hotkey getShiftUp() {
        return shiftUp;
    }

    public void setShiftUp(int keycode) {
        shiftUp.setKeycode(keycode);
    }

    public Hotkey getShiftDown() {
        return shiftDown;
    }

    public void setShiftDown(int keycode) {
        shiftDown.setKeycode(keycode);
    }

    public Hotkey getNos() {
        return nos;
    }

    public void setNos(int keycode) {
        nos.setKeycode(keycode);
    }

    public Hotkey getBlowTurbo() {
        return turbo;
    }

    public void setBlowTurbo(int keycode) {
        turbo.setKeycode(keycode);
    }

    public Hotkey getEngineOn() {
        return engineOn;
    }

    public void setEngineOn(int keycode) {
        engineOn.setKeycode(keycode);
    }

    public Hotkey[] getGears() {
        return gears;
    }

    public void setGear(int index, int keycode) {
        if (index >= 0 && index < gears.length) {
            gears[index].setKeycode(keycode);
        }
    }

    public Hotkey getLeft() {
        return left;
    }

    public void setLeft(int keycode) {
        left.setKeycode(keycode);
    }

    public Hotkey getRight() {
        return right;
    }

    public void setRight(int keycode) {
        right.setKeycode(keycode);
    }

    public Hotkey getLookBehind() {
        return lookBehind;
    }

    public void setLookBehind(int keycode) {
        lookBehind.setKeycode(keycode);
    }

    public Hotkey getReady() {
        return ready;
    }

    public void setReady(int keycode) {
        ready.setKeycode(keycode);
    }

    public Hotkey getUndo() {
        return undo;
    }

    public void setUndo(int keycode) {
        undo.setKeycode(keycode);
    }

    public Hotkey getSell() {
        return sell;
    }

    public void setSell(int keycode) {
        sell.setKeycode(keycode);
    }

    public Hotkey getImprove() {
        return improve;
    }

    public void setImprove(int keycode) {
        improve.setKeycode(keycode);
    }

    public Hotkey getQuitRace() {
        return quitRace;
    }

    public void setQuitRace(int keycode) {
        quitRace.setKeycode(keycode);
    }

    public void refresh() {
        for (Hotkey hotkey : getHotkeys()) {
            hotkey.loadKeycode();
        }
    }
}
package settings_and_logging;

import static org.lwjgl.glfw.GLFW.*;

public class ControlsSettings extends Settings {

	private static final int GEARS = 7;

	public ControlsSettings() {
		super.init("keys.properties");
		init();

	}

	public void init() {
		int[] gearList = {GLFW_KEY_N, GLFW_KEY_U, GLFW_KEY_J, GLFW_KEY_I, GLFW_KEY_K, GLFW_KEY_O, GLFW_KEY_L};
		setThrottle(GLFW_KEY_W);
		setBrake(GLFW_KEY_S);
		setClutch(GLFW_KEY_SPACE);
		setNOS(GLFW_KEY_E);
		setTurboBlow(GLFW_KEY_Q);
		setLookBehind(GLFW_KEY_R);
		setGearUp(GLFW_KEY_DOWN);
		setGearDown(GLFW_KEY_UP);
		for (int i = 0; i < GEARS; i++) {
			setGear(gearList[i], i);
		}
		setReady(GLFW_KEY_R);
		setUndo(GLFW_KEY_U);
		setSell(GLFW_KEY_S);
		setImprove(GLFW_KEY_SPACE);
		setQuitRace(GLFW_KEY_ESCAPE);
	}

	public void setReady(int v) {
		writeToLine("Ready=" + v, 9 + GEARS);
	}

	public int getReady() {
		return getInt(9 + GEARS);
	}

	public void setUndo(int v) {
		writeToLine("Undo=" + v, 10 + GEARS);
	}

	public int getUndo() {
		return getInt(10 + GEARS);
	}

	public void setSell(int v) {
		writeToLine("Sell=" + v, 11 + GEARS);
	}

	public int getSell() {
		return getInt(11 + GEARS);
	}

	public void setImprove(int v) {
		writeToLine("Improve=" + v, 12 + GEARS);
	}

	public int getImprove() {
		return getInt(12 + GEARS);
	}

	public void setQuitRace(int v) {
		writeToLine("QuitRace=" + v, 13 + GEARS);
	}

	public int getQuitRace() {
		return getInt(13 + GEARS);
	}

	public void setThrottle(int v) {
		writeToLine("Throttle=" + v, 0);
	}

	public int getThrottle() {
		return getInt(0);
	}

	public void setBrake(int v) {
		writeToLine("Brake=" + v, 1);
	}

	public int getBrake() {
		return getInt(1);
	}

	public void setClutch(int v) {
		writeToLine("Clutch=" + v, 2);
	}

	public int getClutch() {
		return getInt(2);
	}

	public void setNOS(int v) {
		writeToLine("NOS=" + v, 3);
	}

	public int getNOS() {
		return getInt(3);
	}

	public void setGearUp(int v) {
		writeToLine("GearUp=" + v, 4);
	}

	public int getGearUp() {
		return getInt(4);
	}

	public void setTurboBlow(int v) {
		writeToLine("StrutsAle=" + v, 5);
	}

	public int getTurboBlow() {
		return getInt(5);
	}

	public void setLookBehind(int v) {
		writeToLine("BlowTurbo=" + v, 6);
	}

	public int getLookBehind() {
		return getInt(6);
	}

	public void setGearDown(int v) {
		writeToLine("GearDown=" + v, 7);
	}

	public int getGearDown() {
		return getInt(7);
	}

	public void setGear(int v, int gear) {
		writeToLine("Gear" + gear + "=" + v, 8 + gear);
	}

	public int getGear(int gear) {
		return getInt(8 + gear);
	}
}
package settings_and_logging;

import static org.lwjgl.glfw.GLFW.*;

public class ControlsSettings extends SettingsOld {

	final int gears = 7;
	
	public ControlsSettings() {
		if (super.init("keys.properties", 14 + gears)) {
			init();
		}
	}
	
	public void init() {
		// Set the standard keys to file ( WASD / arrows Space Shift Ctrl Enter )
		int[] gearList = { GLFW_KEY_N, GLFW_KEY_U, GLFW_KEY_J, GLFW_KEY_I, GLFW_KEY_K, GLFW_KEY_O, GLFW_KEY_L };
		setThrottle(GLFW_KEY_W);
		setBrake(GLFW_KEY_S);
		setClutch(GLFW_KEY_SPACE);
		setNOS(GLFW_KEY_E);
		setTurboBlow(GLFW_KEY_Q);
		setLookBehind(GLFW_KEY_R);
		setGearUp(GLFW_KEY_DOWN);
		setGearDown(GLFW_KEY_UP);
		for (int i = 0; i < 7; i++) {
			setGear(gearList[i], i);
		}
		setReady(GLFW_KEY_R);
		setUndo(GLFW_KEY_U);
		setSell(GLFW_KEY_S);
		setImprove(GLFW_KEY_SPACE);
		setQuitRace(GLFW_KEY_ESCAPE);
	}

	public void setReady(int v) {
		writeToLine("Ready=" + v, 9 + gears);
	}

	public int getReady() {
		return getSettingInteger(9 + gears);
	}
	
	public void setUndo(int v) {
		writeToLine("Undo=" + v, 10  + gears);
	}
	
	public int getUndo() {
		return getSettingInteger(10  + gears);
	}
	
	public void setSell(int v) {
		writeToLine("Sell=" + v, 11 + gears);
	}
	
	public int getSell() {
		return getSettingInteger(11 + gears);
	}
	
	public void setImprove(int v) {
		writeToLine("Improve=" + v, 12 + gears);
	}
	
	public int getImprove() {
		return getSettingInteger(12 + gears);
	}
	
	public void setQuitRace(int v) {
		writeToLine("QuitRace=" + v, 13 + gears);
	}
	
	public int getQuitRace() {
		return getSettingInteger(13 + gears);
	}
	
	public void setThrottle(int v) {
		writeToLine("Throttle=" + v, 0);
	}
	
	public int getThrottle() {
		return getSettingInteger(0);
	}

	public void setBrake(int v) {
		writeToLine("Brake=" + v, 1);
	}

	public int getBrake() {
		return getSettingInteger(1);
	}

	public void setClutch(int v) {
		writeToLine("Clutch=" + v, 2);
	}

	public int getClutch() {
		return getSettingInteger(2);
	}

	public void setNOS(int v) {
		writeToLine("NOS=" + v, 3);
	}

	public int getNOS() {
		return getSettingInteger(3);
	}

	public void setGearUp(int v) {
		writeToLine("GearUp=" + v, 4);
	}

	public int getGearUp() {
		return getSettingInteger(4);
	}

	public void setTurboBlow(int v) {
		writeToLine("StrutsAle=" + v, 5);
	}

	public int getTurboBlow() {
		return getSettingInteger(5);
	}
	
	public void setLookBehind(int v) {
		writeToLine("BlowTurbo=" + v, 6);
	}

	public int getLookBehind() {
		return getSettingInteger(6);
	}
	
	public void setGearDown(int v) {
		writeToLine("GearDown=" + v, 7);
	}

	public int getGearDown() {
		return getSettingInteger(7);
	}

	public void setGear(int v, int gear) {
		writeToLine("Gear" + gear + "=" + v, 8 + gear);
	}

	public int getGear(int gear) {
		return getSettingInteger(8 + gear);
	}
}

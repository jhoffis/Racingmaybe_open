package settings_and_logging;

public class ControlsSettings extends Settings {

	private static final int GEARS = 7;
	private static final int THROTTLE_LINE = 0;
	private static final int BRAKE_LINE = 1;
	private static final int CLUTCH_LINE = 2;
	private static final int NOS_LINE = 3;
	private static final int GEAR_UP_LINE = 4;
	private static final int TURBO_BLOW_LINE = 5;
	private static final int LOOK_BEHIND_LINE = 6;
	private static final int GEAR_DOWN_LINE = 7;
	private static final int GEAR_BASE_LINE = 8;
	private static final int READY_LINE = 9;
	private static final int UNDO_LINE = 10;
	private static final int SELL_LINE = 11;
	private static final int IMPROVE_LINE = 12;
	private static final int QUIT_RACE_LINE = 13;

	public void setReady(int v) {
		writeToLine("Ready=" + v, READY_LINE + GEARS);
	}

	public int getReady() {
		return getInt(READY_LINE + GEARS);
	}

	public void setUndo(int v) {
		writeToLine("Undo=" + v, UNDO_LINE + GEARS);
	}

	public int getUndo() {
		return getInt(UNDO_LINE + GEARS);
	}

	public void setSell(int v) {
		writeToLine("Sell=" + v, SELL_LINE + GEARS);
	}

	public int getSell() {
		return getInt(SELL_LINE + GEARS);
	}

	public void setImprove(int v) {
		writeToLine("Improve=" + v, IMPROVE_LINE + GEARS);
	}

	public int getImprove() {
		return getInt(IMPROVE_LINE + GEARS);
	}

	public void setQuitRace(int v) {
		writeToLine("QuitRace=" + v, QUIT_RACE_LINE + GEARS);
	}

	public int getQuitRace() {
		return getInt(QUIT_RACE_LINE + GEARS);
	}

	public void setThrottle(int v) {
		writeToLine("Throttle=" + v, THROTTLE_LINE);
	}

	public int getThrottle() {
		return getInt(THROTTLE_LINE);
	}

	public void setBrake(int v) {
		writeToLine("Brake=" + v, BRAKE_LINE);
	}

	public int getBrake() {
		return getInt(BRAKE_LINE);
	}

	public void setClutch(int v) {
		writeToLine("Clutch=" + v, CLUTCH_LINE);
	}

	public int getClutch() {
		return getInt(CLUTCH_LINE);
	}

	public void setNOS(int v) {
		writeToLine("NOS=" + v, NOS_LINE);
	}

	public int getNOS() {
		return getInt(NOS_LINE);
	}

	public void setGearUp(int v) {
		writeToLine("GearUp=" + v, GEAR_UP_LINE);
	}

	public int getGearUp() {
		return getInt(GEAR_UP_LINE);
	}

	public void setTurboBlow(int v) {
		writeToLine("StrutsAle=" + v, TURBO_BLOW_LINE);
	}

	public int getTurboBlow() {
		return getInt(TURBO_BLOW_LINE);
	}

	public void setLookBehind(int v) {
		writeToLine("BlowTurbo=" + v, LOOK_BEHIND_LINE);
	}

	public int getLookBehind() {
		return getInt(LOOK_BEHIND_LINE);
	}

	public void setGearDown(int v) {
		writeToLine("GearDown=" + v, GEAR_DOWN_LINE);
	}

	public int getGearDown() {
		return getInt(GEAR_DOWN_LINE);
	}

	public void setGear(int v, int gear) {
		writeToLine("Gear" + gear + "=" + v, GEAR_BASE_LINE + gear);
	}

	public int getGear(int gear) {
		return getInt(GEAR_BASE_LINE + gear);
	}
}
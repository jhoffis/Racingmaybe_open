package settings_and_logging.hotkeys;

public class RaceKeys {

	public static int throttle, stompThrottle, brake, clutch, shiftUp, shiftDown, nos,
			blowTurbo, engineOn, left, right, lookBehind, ready, undo, sell, improve, quitRace;
	private int[] gears = new int[7];

	public RaceKeys() {
		throttle = 87;
		stompThrottle = 16;
		brake = 83;
		clutch = 32;
		gears[0] = 78;
		gears[1] = 85;
		gears[2] = 74;
		gears[3] = 73;
		gears[4] = 75;
		gears[5] = 79;
		gears[6] = 76;
		shiftUp = 265;
		shiftDown = 264;
		engineOn = 84;
		nos = 69;
		blowTurbo = 81;
		left = 65;
		right = 68;
	}

	public boolean isThrottle(int key) {
		return key == throttle;
	}

	public boolean isStompThrottle(int key) {
		return key == stompThrottle;
	}

	public boolean isBrake(int key) {
		return key == brake;
	}

	public boolean isClutch(int key) {
		return key == clutch;
	}

	public boolean isGear(int key, int i) {
		return key == gears[i];
	}

	public boolean isShiftUp(int key) {
		return key == shiftUp;
	}

	public boolean isShiftDown(int key) {
		return key == shiftDown;
	}

	public boolean isNos(int key) {
		return key == nos;
	}

	public boolean isBlowTurbo(int key) {
		return key == blowTurbo;
	}

	public boolean isEngineON(int key) {
		return key == engineOn;
	}

	public int getThrottle() {
		return throttle;
	}

	public void setThrottle(int throttle) {
		this.throttle = throttle;
	}

	public int getStompThrottle() {
		return stompThrottle;
	}

	public void setStompThrottle(int stompThrottle) {
		this.stompThrottle = stompThrottle;
	}

	public int getBrake() {
		return brake;
	}

	public void setBrake(int brake) {
		this.brake = brake;
	}

	public int getClutch() {
		return clutch;
	}

	public void setClutch(int clutch) {
		this.clutch = clutch;
	}

	public int getShiftUp() {
		return shiftUp;
	}

	public void setShiftUp(int shiftUp) {
		this.shiftUp = shiftUp;
	}

	public int getShiftDown() {
		return shiftDown;
	}

	public void setShiftDown(int shiftDown) {
		this.shiftDown = shiftDown;
	}

	public int getNos() {
		return nos;
	}

	public void setNos(int nos) {
		this.nos = nos;
	}

	public int getBlowTurbo() {
		return blowTurbo;
	}

	public void setBlowTurbo(int blowTurbo) {
		this.blowTurbo = blowTurbo;
	}

	public int getEngineOn() {
		return engineOn;
	}

	public void setEngineOn(int engineOn) {
		this.engineOn = engineOn;
	}

	public int[] getGears() {
		return gears;
	}

	public void setGears(int[] gears) {
		this.gears = gears;
	}

}

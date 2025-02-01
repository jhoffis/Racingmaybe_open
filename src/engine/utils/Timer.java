package engine.utils;

public class Timer {

	private long lastLoopTime;
	public static float lastDelta;
	
	public void init() {
		lastLoopTime = getTime();
	}
	
	public long getTime() {
		return System.nanoTime();
	}
	
	public double getTimeInSeconds() {
		return getTime() / 1_000_000_000d;
	}
	
	public float getDelta() {
		long time = getTime();
		float delta = time - lastLoopTime;
		lastLoopTime = time;
		lastDelta = delta / 40000000f;
		 // Base a tick around 40 000 000 ns (40 ms) == 25 ticks per sec
        return lastDelta;
	}
	
	public double getLastLoopTime() {
		return lastLoopTime;
	}
	
}

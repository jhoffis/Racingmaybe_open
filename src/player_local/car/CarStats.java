package player_local.car;

import java.util.ArrayList;

public class CarStats {
	public boolean throttle, clutch, NOSON, tireboostON, NOSDownPressed,
			sequentialShift, soundBarrierBroken, hasDoneStartBoost, turboBlowON, redlinedThisGear, grinding, visualBrake;
	public int nosBottleAmountLeft, gear, finishSpeed;
	public long tireboostTimeLeft, lastTimeReleaseThrottle;
	public double speed, distance, spool, spdinc, brake, throttlePercent,
	clutchPercent // If < 1 then clutch engaged
	, rpm, rpmGoal, grindingCurrent;
	private final ArrayList<Long> nosTimesLeft = new ArrayList<>(), nosTimesFrom = new ArrayList<>();

	public double[] stats;
    public long grindingTime;
	public float percentTurboSuper;

    /**
	 * Resets temporary stats
	 */
	public void reset(Rep rep, float numSuperchargers, float numTurbos) {
		stats = new double[Rep.size()];
		for (int i = 0; i < stats.length; i++) {
			stats[i] = rep.get(i);
		}
		if (rep.getNameID() == 0)
			numTurbos++;
		
		if (numSuperchargers == 0)
			percentTurboSuper = 1;
		else if (numSuperchargers > numTurbos)
			percentTurboSuper = numTurbos / numSuperchargers;
		else
			percentTurboSuper = numSuperchargers / numTurbos;
		
		turboBlowON = false;
		NOSDownPressed = false;
		visualBrake = false;
		//Med space
//		clutch = false;
//		resistance = 0f;
		// Uten space
		grindingTime = 0;
		grindingCurrent = 1;
		throttlePercent = 0;
		clutch = true;
		clutchPercent = 1f;
		lastTimeReleaseThrottle = 0;
		redlinedThisGear = false;
		throttle = false;
		NOSON = false;
		soundBarrierBroken = false;
		hasDoneStartBoost = false;
		nosBottleAmountLeft = (int) stats[Rep.nosBottles];
		nosTimesLeft.clear();
		nosTimesFrom.clear();
		gear = 1;
		speed = 0;
		distance = 0;
		spool = stats[Rep.spoolStart];
		spdinc = 0;
		rpm = stats[Rep.rpmIdle];
		rpmGoal = rpm;
		tireboostTimeLeft = 0;
		sequentialShift = rep.is(Rep.sequential);
		
		brake = 0;
	}

	public float getNosPercentageLeft(int i) {
		
		if(i >= nosTimesLeft.size())
			return 1f;
		
		float res = 0.0f;
		
		long timespan = nosTimesLeft.get(i) - System.currentTimeMillis();
		if(timespan > 0) {
			res = (float)timespan / (float)(nosTimesLeft.get(i) - nosTimesFrom.get(i)) ;
		}
		
		return res;
	}
	
	public float getTBPercentageLeft() {
		float tbMaxTime = (float) stats[Rep.tbMs];
//		System.out.println("tbMaxTime " + tbMaxTime);
		float tbTime = tireboostTimeLeft - System.currentTimeMillis();
//		System.out.println("tbTime " + tbTime);
		var percent = tbTime / tbMaxTime;
//		System.out.println("Percent: " + percent);

		return Math.max(percent, 0);
	}

	public void popNosBottle(long fromTime, long tillTime) {
		nosTimesFrom.add(fromTime);
		nosTimesLeft.add(tillTime);
		nosBottleAmountLeft--;
	}
	
	public long getNosTimeLeft(int i) {
		
		var timesLeft = nosTimesLeft.toArray();
		
		return i < timesLeft.length ? (long) timesLeft[i] : -1;
	}

	public double getGrindLimit() {
		return stats[Rep.rpmTop] * 0.92;
	}
	
	public void changeLastNosTimeLeft(long val) {
		if (nosTimesLeft.size() > 0)
			nosTimesLeft.set(nosTimesLeft.size() - 1, nosTimesLeft.get(nosTimesLeft.size() - 1) + val);
	}
	
}
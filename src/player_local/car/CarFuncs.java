package player_local.car;

import main.Features;
import main.Main;

public class CarFuncs {

	private final double soundBarrierSpeed = 1234;

//	double ticktemp;
//	double tim;
//	long lastTime;
//	long timN;

	/*
	 * returns true if sound barrier is broken
	 */
	public boolean updateSpeed(CarStats stats, Rep rep, double tickFactor, long time) {

		double speedChange;

		// MOVEMENT
		if (stats.throttle && !stats.grinding && stats.clutchPercent < 1d && isGearCorrect(stats, rep)) {
			speedChange = speedInc(stats, rep, tickFactor, time)* stats.throttlePercent 
					+ 1.0
//					+ .2
					* decelerateCar(stats, rep, 1);
		} else {
			stats.NOSON = false;
			speedChange = decelerateCar(stats, rep, 1);

			if (stats.grinding)
				speedChange += decelerateCar(stats, rep, 1);

//			System.out.println("not throttle: " + stats.speed + stats.grinding);
			stats.tireboostON = false;
		}
		
		var grindPerc = (time - stats.grindingTime) / 1500f;
		if (grindPerc < 1f && grindPerc > 0f) {
			if (grindPerc < 0.5)
				stats.grindingCurrent = .3;
			else
				stats.grindingCurrent = 1f - (0.7 * (1f - grindPerc));
			if (speedChange > 0f)
				speedChange *= stats.grindingCurrent;
		} else {
			stats.grindingCurrent = 1;
		}


		// RPM

		stats.spdinc = speedChange * tickFactor;

		double topspd = stats.gear != 0 ? gearMax(stats, rep) : stats.stats[Rep.spdTop];
//		System.out.println("speed inc before: " + stats.spdinc);
		if (stats.speed + stats.spdinc > topspd) { // Wanted to go too fast
			stats.redlinedThisGear = true;
			stats.spdinc = topspd - stats.speed;
		} else {
			stats.redlinedThisGear = false;
		}
		updateRPM(stats, rep, tickFactor, time);

		var res = stats.speed < soundBarrierSpeed && stats.speed + stats.spdinc >= soundBarrierSpeed;
		
//		System.out.println(stats.spdinc);
		stats.speed += stats.spdinc;
		if (stats.speed < 0 || Double.isNaN(stats.speed))
			stats.speed = 0;
//		System.out.println("speed inc after: " + stats.spdinc);
//		System.out.println("speed: " + stats.speed);

		updateHighestSpeed(stats, rep);
		calculateDistance(stats, tickFactor);

		return res;
	}

	private void updateHighestSpeed(CarStats stats, Rep rep) {
		if (stats.speed > rep.get(Rep.highestSpdAchived))
			rep.set(Rep.highestSpdAchived, (long) stats.speed);
	}

	private double rpmToPercent(double rpm, CarStats stats) {
		var bareMin = stats.stats[Rep.rpmBaseIdle] * 0.5;
		if (rpm < bareMin)
			rpm = bareMin;

		return rpm / stats.stats[Rep.rpmBaseTop];
	}

	private double calcGearDrag(double tkw, double speed, double speedTop, double gearCmpTop, double aero) {
		double drag = 1d - Math.pow(speed / speedTop, 5d) - (.08 * gearCmpTop * aero);
		if (drag < .1)
			drag = .1;
//		if (tkw < 10000 && tkw > 0) {
//			double tooLittlePower = 150d * Math.pow(2.718, tkw);
//			double speedTopDiff = (tooLittlePower - speedTop) * .1d * gear;
//			drag += (1d - drag) *speedTopDiff;
//		}
		return drag;
	}
	
	public double turboFactor(CarStats stats) {
		var factor = (stats.stats[Rep.turboblow] - 1) / 100d;
		if (factor < 0)
			factor = 0;
		factor = Math.floor(factor) + 1;
//				System.out.println("factor: " + factor);
		return factor;
	}
	
	public double turboHorsepower(CarStats stats, Rep rep, double tickFactor, boolean change) {
		var tkw = rep.getTurboKW() * stats.spool;

		if (stats.turboBlowON && stats.throttle) {
			if (stats.stats[Rep.turboblow] >= 0) {
				var factor = turboFactor(stats);
				tkw *= factor + stats.stats[Rep.turboblowStrength];
				if (change)
					stats.stats[Rep.turboblow] -= tickFactor * 2d*factor;
			}
		}
		return tkw;
	}
	
	public double tbHeat(double heat, double timePerc) {
		return 0.04 * (heat + Math.max(0.5, timePerc) * 0.05*Math.pow(heat, 2));
	}
	
	public double maxTb(Rep rep) {
		return (tbHeat(rep.get(Rep.tbHeat), 1) + 1) * rep.get(Rep.tb);
	}

	private double speedInc(CarStats stats, Rep rep, double tickFactor, long comparedTimeLeft) {
		var w = stats.stats[Rep.kg];
		var rpmCalc = 1d;
		var gear = stats.gear;
		if (!rep.is(Rep.stickyclutch)) {
			rpmCalc = rpmToPercent(stats.rpm, stats);
			if (gear == 1) // Litt ekstra push til starten
				rpmCalc += 0.2 * (1.0 - rpmCalc);
		} else {
			rpmCalc = rpmToPercent(stats.stats[Rep.rpmTop], stats);
		}

		var kw = stats.stats[Rep.kW];

		if (rep.hasTurbo()) {
			kw += turboHorsepower(stats, rep, tickFactor, true);
		} else {
			stats.turboBlowON = false;
		}

		var tkw = kw * rpmCalc / w;

		var speedTop = rep.get(Rep.spdTop);
		var gearTop = rep.get(Rep.gearTop);

		var bobatea = (rep.get(Rep.spdTopBase) / speedTop);
		if (bobatea < .01)
			bobatea = .01;
		var spdInc = tkw * calcGearDrag(tkw, stats.speed, speedTop, (gear - 1d) / gearTop, stats.stats[Rep.aero])
				* (gear > 0 ? bobatea * (gearTop / gear) : 1);

		/*
		 * Boost
		 */
		final double minRpmCalc = 0.2;
		rpmCalc = rpmCalc * (1d - minRpmCalc) + minRpmCalc; // ikke nerf boost for mye basert p� rpm. Dytter tallene
															// n�rmere 1.0

		if (rep.hasNOS()) {
			int nosAmount = 0;

			for (int i = 0; i < rep.get(Rep.nosBottles); i++) {
				if (stats.getNosTimeLeft(i) > comparedTimeLeft) {
					nosAmount++;
					if (!rep.is(Rep.snos))
						spdInc += rep.get(Rep.nos) / nosAmount * rpmCalc;
				}
			}
			stats.NOSON = nosAmount > 0;
		}

		if (stats.tireboostTimeLeft > comparedTimeLeft) {
			double tb;
			if (stats.stats[Rep.tbHeat] > 0) {
				var timePerc = (stats.tireboostTimeLeft - comparedTimeLeft) / rep.get(Rep.tbMs);
				var alteredHeat = tbHeat(stats.stats[Rep.tbHeat], timePerc);
				tb = rep.get(Rep.tb) * (alteredHeat + 1);
				if (rep.get(Rep.tbHeat) > 0)
					rep.add(Rep.tbHeat, -.02f*tickFactor);
				else 
					rep.set(Rep.tbHeat, 0);
				System.out.println("TbHeat: " + rep.get(Rep.tbHeat)+ ", alteredHeat: " + alteredHeat + ", stats heat: " + stats.stats[Rep.tbHeat] + ", time: " + timePerc + ", change: " + (-.1f*tickFactor) + ", OG tb: " + rep.get(Rep.tb) + ", act tb: " + tb);	
			} else {
				tb = rep.get(Rep.tb);	
			}
			
			spdInc += tb * rpmCalc;
			stats.tireboostON = true;
			
			
//			if (stats.lastTimeReleaseThrottle > 0) {
//				stats.tireboostTimeLeft += 75;
//				stats.lastTimeReleaseThrottle = 0;
//			}
			
		} else {
			stats.tireboostON = false;
		}

		if (Double.isInfinite(spdInc) || Double.isNaN(spdInc)) {
			spdInc = Double.MAX_VALUE;
		}

		return spdInc;
	}

	public boolean isTopGear(CarStats stats, Rep rep) {
		return stats.gear == rep.getInt(Rep.gearTop);
	}

	public boolean isGearCorrect(CarStats stats, Rep rep) {
		var gearmax = gearMax(stats, rep);
		var res = stats.speed < gearmax;
		return res;
	}

	/**
	 * Gir farten til gitte gir
	 */
	public double gearMax(int gearSuggestion, CarStats stats, Rep rep) {
		return gearSuggestion * (stats.stats[Rep.spdTop] / rep.get(Rep.gearTop));
	}

	public double gearMax(CarStats stats, Rep rep) {
		return stats.gear * (stats.stats[Rep.spdTop] / rep.get(Rep.gearTop));
	}

	public double weightWindless(double kg) {
		var weight = kg/ 1200d;
		return Math.max(weight, 0.5);
	}

	public double decelerateCar(CarStats stats, Rep rep, double tickFactor) {
		double dec = 0;

//			dec = (rep.getTotalKW() / stats.stats[Rep.kg]);
//			if (dec > 5.0)
//				dec = 5.0;

		var rpmPerc = rpmToPercent(stats.rpm, stats);
		if (rpmPerc > 1)
			rpmPerc = 1;
		dec = -1.0d * (stats.clutch || stats.gear == 0 ? 0.5 : rpmPerc);
		var extraDec = 0.5 * Math.pow(3, 0.0025 * stats.speed);
		if (Math.abs(extraDec) > 250)
			extraDec = 250;
		dec *= extraDec;

		var windless = 100d / stats.speed + (10 * 100d / stats.speed);

		// lose more with lower weight
		windless *= weightWindless(stats.stats[Rep.kg]);

//		var power = Math.pow(stats.stats[Rep.kW] / 300d, 2) * (rpmPerc > 1 ? 1 : rpmPerc);
//		System.out.println("Power: " + power);
//		if (power < 1)
//			power = 1;
//		else if (power > 5)
//			power = 5;
//		windless /= power;
//		System.out.println("Windless: " + windless);

		if (windless < 1d)
			windless = 1d;
//		else if (windless > 1.5d)
//			windless = 1.5d;
		dec /= windless;
		dec *= stats.stats[Rep.aero];

		if (stats.speed > 10000) {
			dec -= stats.stats[Rep.aero] * Math.pow((stats.speed - 10000d) / 100000d, 3);
		}

//		System.out.println("dec: " + dec);
		if (rep.is(Rep.snos) && stats.NOSON) {
			var snosDec = stats.stats[Rep.nos];
			dec = (1d + (-6d*dec)) * snosDec;
		}
		dec *= 1 + 5 * stats.brake / (0.000001 + stats.stats[Rep.aero]);
		dec -= 2.5 * stats.brake;

		return dec * tickFactor;
	}

	private int idleRpmRandomizer(double rpm, double hp) {
		double percentageShake = hp / 10000.0;
		if (percentageShake > 0.15)
			percentageShake = 0.15;
		double res = rpm * percentageShake * Features.ran.nextFloat();
		res -= res / 2d;
		return (int) (rpm + res);
	}

	/**
	 * Updates RPM value based on engaged clutch and throttle and current speed
	 */
	private void updateRPM(CarStats stats, Rep rep, double tickFactor, long timeNow) {
		double rpmChange = 0;
		double rpm = stats.rpmGoal;
//		if (rpm < 0)
//			System.out.println("rpm: " + rpm);

		if (stats.clutchPercent < 1) {
//			System.out.println("clutch engaged");
			// If clutch engaged
			double change = rpm; // push prev rpm
			double gearFactor = stats.speed / (gearMax(stats, rep) + 1);
			rpm = stats.stats[Rep.rpmTop] * gearFactor;

			// Turbo spooling
			change = rpm - change; // pop and calc diff of (next - prev) rpm
			if (rep.hasTurbo()) {
				double increase = 0.0000045f * stats.stats[Rep.spool];
				if (stats.spool > 1)
					increase *= .04f;
				stats.spool += rpm * increase * tickFactor;
			}
//			else {
//				stats.spool = 0;
//			}

			double minimum;

			if (stats.redlinedThisGear) {
				rpm = idleRpmRandomizer(rpm, rep.getTotalKW());
			} else {
				if (stats.throttle)
					minimum = idleRpmRandomizer(stats.stats[Rep.rpmIdle] * 2 / 3, rep.getTotalKW());
				else
					minimum = idleRpmRandomizer(stats.stats[Rep.rpmIdle], rep.getTotalKW());

				if (rpm < minimum)
					rpm = minimum;
			}

			if (stats.redlinedThisGear && !isTopGear(stats, rep)) {
				stats.grindingTime = timeNow;
			}

		} else if (stats.throttle) { 
//			System.out.println("not clutch engaged " + stats.clutchPercent);
			// Not engaged clutch
			double maxRpm, redRpm;
			if (stats.stats[Rep.twoStep] != 0) {
				maxRpm = .75 * stats.stats[Rep.rpmTop]; 
				redRpm = .75 * stats.stats[Rep.rpmTop] - .2*stats.stats[Rep.rpmTop];
			} else {
				maxRpm = stats.stats[Rep.rpmTop] - 60; 
				redRpm = stats.stats[Rep.rpmTop] - 100; 
			}
			
			if (stats.rpm < maxRpm) {

				double rpmFactor = (stats.stats[Rep.rpmTop] * 0.8f) + (rpm * 0.2f);
				rpmChange = stats.stats[Rep.kW] * (rpmFactor / stats.stats[Rep.rpmTop]) * stats.throttlePercent;
				stats.spool = stats.stats[Rep.spoolStart];
			} else {
				// Redlining
				rpm = redRpm;
			}
		} else {
//			System.out.println("not clutch engaged and not throttle");

			// Not engaged and throttle not down
			if (rpm > stats.stats[Rep.rpmIdle]) {
				rpmChange = -(stats.stats[Rep.kW] * 0.25f);
				
				var maxLoss = stats.stats[Rep.rpmTop] / 32f;
				if (rpmChange < -maxLoss)
					rpmChange = -maxLoss;
			} else
				// Sets RPM to for instance 1000 rpm as standard.
				rpm = idleRpmRandomizer(stats.stats[Rep.rpmIdle], rep.getTotalKW());
			stats.spool = stats.stats[Rep.spoolStart];
		}

		stats.rpmGoal = rpm + rpmChange * tickFactor;

		var min = stats.stats[Rep.rpmIdle] / 2f;
		if (stats.rpmGoal < min)
			stats.rpmGoal = min;

		double diff = stats.rpmGoal - stats.rpm;
		if (Math.abs(diff) > 100d)
			stats.rpm = stats.rpm + (diff * tickFactor);
		else
			stats.rpm = stats.rpmGoal;
	}

	// private void CalculateDistance(CarStats stats, double tickFactor) {
	// // 25 ticks per sec. kmh, distance in meters. So, x / 3.6 / 25.
	// stats.AddDistance((stats.GetSpeed() / 90) * tickFactor);
	// }

	public void tireboost(CarStats stats, Rep rep, long now, int divideTime, int timeDiff) {
		stats.tireboostTimeLeft = now + (long) (stats.stats[Rep.tbMs] * tireboostLoss(rep, timeDiff)) - timeDiff;
	}

	public double tireboostLoss(Rep rep, double timeDiff) {
		if (rep.is(Rep.tbArea))
			return 1;

		double timeloss = 1d - (timeDiff * 0.001d);
		if (timeloss < 0d)
			timeloss = 0d;
		return timeloss;
	}

	public boolean nos(CarStats stats, long comparedTimeValue, long tillTime, int divideTime) {
		if (stats.nosBottleAmountLeft > 0) {
			stats.popNosBottle(comparedTimeValue, tillTime);
			return true;
		}
		return false;
	}
	
	public boolean nos(CarStats stats, long comparedTimeValue, int divideTime) {
		return nos(stats, comparedTimeValue, comparedTimeValue + (int) stats.stats[Rep.nosMs] / divideTime, divideTime);
	}

	private void calculateDistance(CarStats stats, double tickFactor) {
		// 25 ticks per sec. kmh, distance in meters. So, x / 3.6 / 25.
		// if tickFactor == 1 then that is 1 tick which is 1/25sec.
		stats.distance += (stats.speed / 3.6) / Main.TICK_STD * tickFactor;
	}

}

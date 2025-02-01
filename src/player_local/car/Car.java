package player_local.car;

import audio.CarAudio;
import engine.graphics.interactions.GearboxShift;
import engine.graphics.objects.Camera;
import engine.graphics.Renderer;
import main.Main;
import player_local.Layer;
import player_local.upgrades.TileNames;

public class Car {

	private final CarModel model;
	private final CarStats stats;
	public static final CarFuncs funcs = new CarFuncs();
	private Rep rep;
	private CarAudio audio;

	public Car() {
		stats = new CarStats();
		model = new CarModel();
		rep = new Rep();
		switchTo(0, false);
		reset(null);
	}
	
	public void switchTo(int index, boolean random) {
		if(rep != null && rep.getNameID() == index) {
			rep.setRandom(random);
			return;
		}
		model.setModel(index);
		
		int nosTimeStandard = 0;
		int nosBottleAmountStandard= 0;
		double nosStrengthStandard= 0;
		double hp = 0;
		double weight= 0;
		double speedTop= 0;
		int rpmIdle= 0;
		int rpmTop= 0;
		int gearTop= 0;
		int tbTimeStandard = 0;
		double tbStrengthStandard= 0;
		double tbArea= 0;
		double bar = 0;
		double turboblow = 1;
		boolean clutchShift = false, throttleShift = false, sequential = false;

		switch (index) {
			case 0 -> {
				rpmTop = 5500;
				rpmIdle = (int) (rpmTop / 4.5f);
				hp = Main.DEBUG ? Double.MAX_VALUE : 135;
				weight = Main.DEBUG ? -1 : 1650;
				gearTop = 4;
				speedTop = Main.DEBUG ? 10500 : 155;
				bar = 1.2;
				tbTimeStandard = 900;
				tbStrengthStandard = Main.DEBUG ? 900 : 0;
				nosTimeStandard = 500;
				turboblow = 2;
//				tbStrengthStandard = 0.4;
//				throttleShift = true;
//				sequential = true;
//				rpmIdle = 1200;
//				hp = 160;
//				weight = 1750;
//				gearTop = 5;
//				rpmTop = 10000;
//				speedTop = 160;
//				bar = 0.5;
			}
			case 1 -> {
				rpmTop = 6500;
				rpmIdle = rpmTop / 8;
				hp = 284;
				weight = 3050;
				gearTop = 3;
				speedTop = 150;
				tbTimeStandard = 1350;
				tbStrengthStandard = 1.0;
				tbArea = 1;
				nosTimeStandard = 500;
			}
			case 2 -> {
				rpmTop = 10000;
				rpmIdle = rpmTop / 8;
				hp = 90;
				weight = 1215;
				gearTop = 5;
				speedTop = 150;
				tbTimeStandard = 900;
				nosBottleAmountStandard = 1;
				nosStrengthStandard = 1.4;
				nosTimeStandard = 600;
			}
			case 3 -> {
				rpmTop = 5000;
				rpmIdle = rpmTop / 4;
				hp = 112;
				weight = 1450;
				gearTop = 6;
				speedTop = 150;
				tbTimeStandard = 700;
				nosTimeStandard = 400;
//				clutchShift = true;
			}
		}

		rep = new Rep(index, nosTimeStandard, nosBottleAmountStandard,
				nosStrengthStandard, hp, weight, speedTop, rpmIdle, rpmTop,
				gearTop, tbTimeStandard, tbStrengthStandard,
				tbArea, bar, clutchShift, throttleShift, sequential, random, turboblow);
		reset(null);
		
//		switch (index) {
//		case 0 -> {
////			rpmTop = 5500;
////			rpmIdle = (int) (rpmTop / 4.5f);
//////			hp = Main.DEBUG ? Double.MAX_VALUE : 130;
//////			weight = Main.DEBUG ? -1 : 1650;
////			hp = 50000;
////			weight = 1650;
////			gearTop = 4;
////			speedTop = Main.DEBUG ? 10500 : 155;
////			bar = 1.2;
////			tbTimeStandard = 900;
////			tbStrengthStandard = Main.DEBUG ? 900 : 0;
////			nosTimeStandard = 500;'
//			rpmTop = 65000;	
//			rpmIdle = 17000;
//			hp = 3000;
////			bar = 5;
//			weight = 1450;
//			gearTop = 6;
//			speedTop = 150;
//			tbTimeStandard = 700;
//			nosTimeStandard = 400;
////			turboblow = 3;
////			tbStrengthStandard = 0.4;
////			throttleShift = true;
////			sequential = true;
////			rpmIdle = 1200;
////			hp = 160;
////			weight = 1750;
////			gearTop = 5;
////			rpmTop = 10000;
////			speedTop = 160;
////			bar = 0.5;
//		}
//		case 1 -> {
//			rpmTop = 6500;
//			rpmIdle = rpmTop / 8;
//			hp = 284;
//			weight = 3050;
//			gearTop = 3;
//			speedTop = 150;
//			tbTimeStandard = 1350;
//			tbStrengthStandard = 1.0;
//			tbArea = 1;
//			nosTimeStandard = 500;
//		}
//		case 2 -> {
//			rpmTop = 10000;
//			rpmIdle = rpmTop / 8;
//			hp = 90;
//			weight = 1215;
//			gearTop = 5;
//			speedTop = 150;
//			tbTimeStandard = 900;
//			nosBottleAmountStandard = 1;
//			nosStrengthStandard = 1.4;
//			nosTimeStandard = 600;
//		}
//		case 3 -> {
//			rpmTop = 65000;	
//			rpmIdle = 17000;
//			hp = 3000;
////			bar = 5;
//			weight = 1450;
//			gearTop = 6;
//			speedTop = 150;
//			tbTimeStandard = 700;
//			nosTimeStandard = 400;
////			clutchShift = true;
//		}
//	}
//
//	rep = new Rep(index, nosTimeStandard, nosBottleAmountStandard,
//			nosStrengthStandard, hp, weight, speedTop, rpmIdle, rpmTop,
//			gearTop, tbTimeStandard, tbStrengthStandard,
//			tbArea, bar, clutchShift, throttleShift, sequential, random, turboblow);
//	rep.set(Rep.rpmBaseIdle, 0);
//	rep.set(Rep.rpmBaseTop, rpmTop *.7);
//	rep.set(Rep.spoolStart, 1);
//	reset(null);
	}

	public void completeReset() {
		switchTo(rep.getNameID(), rep.isRandom());
		reset(null);
	}
	
	public void reset(Layer layer) {
		float numSuperchargers = 0;
		float numTurbos = 0;
		if (layer != null) {
			numSuperchargers = layer.getAmountType(TileNames.Supercharger);
			numTurbos = layer.getAmountType(TileNames.Turbo);
		}
		stats.reset(rep, numSuperchargers, numTurbos);


		if (rep.get(Rep.kg) <= 0.0) {
			rep.set(Rep.kg, 0.001);
		}
		
		if (rep.get(Rep.aero) < 0) {
			rep.set(Rep.aero, 0);
		}

		if (!rep.is(Rep.sequential) && rep.get(Rep.gearTop) > 10) {
			rep.set(Rep.gearTop, 10);
		}
		
		if (Double.isNaN(rep.get(Rep.kW))) {
			rep.set(Rep.kW, Double.MAX_VALUE);
		}

		if (rep.get(Rep.rpmIdle) > rep.get(Rep.rpmTop) - 100) {
			rep.set(Rep.rpmIdle, rep.get(Rep.rpmTop) - 100);
		}

		for (int i = 0; i < Rep.tbArea; i++) {
			if (rep.get(i) < 0d)
				rep.set(i, 0d);
		}

		if (rep.get(Rep.spdTop) >= Math.pow(10.07925285, 9)) {
			rep.set(Rep.spdTop, Math.pow(10.07925285, 9));
		}
		
		if (audio != null) {
			audio.reset();
			audio.updateVolume();			
		}
	}

	public void updateSpeed(float tickFactor, long time) {
		if (funcs.updateSpeed(stats, rep, tickFactor, time)) {
			if (audio != null)
				audio.soundbarrier();
		}
		if (audio != null) {
			var throttlePerc = Math.max((float) stats.throttlePercent, 0.8f); 
			audio.motorPitch(stats.rpm, rep.get(Rep.rpmBaseTop), 
					switch (rep.getNameID()) {
						case 3:
							yield 1.2;
						default :
							yield 1.5;
					},
					throttlePerc*((stats.NOSON || stats.tireboostON) ? 3 : (stats.NOSDownPressed ? 0.5f : 1)));
			audio.airPitch(stats.speed, stats.stats[Rep.aero]);
			audio.turbospoolPitch((float) stats.spool, (float) rep.getTurboKW(),
					(stats.turboBlowON ? 3f : 1f) / (stats.clutch ? 5f : 1f), stats.percentTurboSuper, (float) (stats.rpm / rep.get(Rep.rpmTop)));
			if (stats.sequentialShift)
				audio.straightcutgearsPitch(stats.speed, rep.get(Rep.spdTop));
	
			if (stats.redlinedThisGear)
				audio.redline();
			else if (stats.throttle) {
				audio.redlineStop();
			}
		}
	}
	
	public boolean startBoost(int timeDiff, long time) {
		boolean res = false;
		
		if(stats.throttlePercent > 0.75 && !stats.hasDoneStartBoost) {
			stats.hasDoneStartBoost = true;
			
			res = tryTireboost(timeDiff, time);
	
			if (rep.is(Rep.nosAuto))
				nos(false);
		}
		
		return res;
	}

	private boolean tryTireboost(int timeDiff, long time) {
		if (hasTireboost()) {
			funcs.tireboost(stats, rep, time, 1, timeDiff);
			if (audio != null)
				audio.tireboost();
			return true;
		}
		return false;
	}

	public int tireboostLoss(int timeDiff) {
		return 100 - (int) (100d * funcs.tireboostLoss(rep, timeDiff));
	}

	public boolean isTireboostRight() {
		return stats.tireboostON && stats.throttle;
	}

	public boolean isTireboostRunning() {
		return stats.tireboostTimeLeft >= System.currentTimeMillis();
	}

	public boolean throttle(boolean down, boolean safe) {
		return throttle(down ? 1f : 0f, safe);
	}
	
	public boolean throttle(float perc, boolean safe) {
		var down = perc > 0f;
		stats.throttlePercent = perc;
		if(down != stats.throttle && (safe || stats.gear == 0)) {
			stats.throttle = down;
			if (audio != null) {
				if (down) {
					audio.motorAcc(hasTurbo());
				} else {
					tryTurboBlowoff();
					audio.motorDcc();
					
				}
			}
			return true;
		}
		return false;
	}

	public void brake(boolean down) {
		stats.brake = down ? 1 : 0;
	}

	public void clutch(boolean down) {
		clutch(down, down ? 1.0f : 0.0f);
	}
	
	public void clutch(boolean down, float percent) {
		if (down) {
			if (!stats.clutch) {
				stats.clutch = true;
				stats.clutchPercent = percent;
				if (audio != null)
					audio.clutch(true);
			}
		} else {
			if (stats.clutch) {
				stats.grinding = false;
				stats.clutch = false;
				if (audio != null)
					audio.clutch(false);
				if (stats.gear > 0)
					stats.clutchPercent = percent;
				
				stats.lastTimeReleaseThrottle = System.currentTimeMillis();
			}
		}
	}

	/**
	 * @return shifted into a new gear
	 */
	public GearboxShift shift(int gear, long timeNow) {
		var res = GearboxShift.nothing;
		
		if (gear != stats.gear && (gear <= rep.get(Rep.gearTop) && gear >= 0)) {
			if (canShift()) {
				stats.gear = gear;
				stats.grinding = false;
				stats.spool = stats.stats[Rep.spoolStart];

				if (!stats.clutch) {
					if (gear == 0)
						stats.clutchPercent = 1.0f;
					else
						stats.clutchPercent = 0.0f;
				}

				if (gear != 0) {
					if (audio != null)
						audio.gear();
//					if (rep.is(Rep.powerShift) && calcPowerloss(false) < 5 && !stats.redlinedThisGear) {
//						stats.speed += rep.get(Rep.spdTop) / 10f;
//					}
					res = GearboxShift.shifted;
				} else {
					if (audio != null)
						audio.gearNeutral();
					res = GearboxShift.neutral;
				}
				
			} else {
				if (audio != null)
					audio.grind();
				res = GearboxShift.grind;
				stats.grinding = true;
				stats.grindingTime = timeNow;
			}
		} else {
			stats.grinding = false;
		}
		return res;
	}

	public void shiftUp(long timeNow) {
		if (shift(stats.gear + 1, timeNow).didShift() && audio != null && stats.rpm > rep.get(Rep.rpmTop) * 0.7f)
			audio.backfire();
	}

	public void shiftDown(long timeNow) {
		shift(stats.gear - 1, timeNow);
	}

	public void nos(boolean down) {
		if (stats.nosBottleAmountLeft > 0) {
			if (!down) {
				if (rep.is(Rep.snos)) {
					funcs.nos(stats, System.currentTimeMillis(), 1);
					if (!funcs.nos(stats, 0, 0, 1)) {
						stats.changeLastNosTimeLeft(-(long) (stats.stats[Rep.nosMs] / 2));
					}
				} else {
					funcs.nos(stats, System.currentTimeMillis(), 1);
				}
			}
			if (audio != null) {
				stats.NOSDownPressed = down;				
				audio.nos(down, stats.stats[Rep.nos]);
			}
		}
	}

	public float getTurbometer() {
		return (float) (stats.spool * rep.get(Rep.bar) * 45.0) - 180f;
	}
	
	/**
	 * @return radian that represents rpm from -180 to ca. 35 - 40 idk yet
	 */
	public float getTachometer() {
		return 237f * (float) ((stats.rpm + 1.0) / rep.get(Rep.rpmTop))
				- 203f;
	}

	public void blowTurbo(boolean down) {
		// dont gain, lose.
		stats.turboBlowON = down && stats.stats[Rep.turboblow] > 0;
	}

	private void tryTurboBlowoff() {
		if (hasTurbo() && audio != null && stats.percentTurboSuper != 0)
			audio.turboBlowoff(stats.spool * rep.get(Rep.bar));
	}

	public void renderCar(Renderer renderer, Camera camera) {
		model.getModel().shader.setUniform("brake", (float) stats.brake);
		model.getModel().render(renderer, camera);
	}
	
	public void regenTurboBlow() {
		double currentTurboBlowAmount = stats.stats[Rep.turboblow];
		if (hasTurbo()
//				&& currentTurboBlowAmount < 100.0
		) {
//			This is in case I want an upper limit to be 100
//			double restAmount = 100.0 - currentTurboBlowAmount,
//					incAmount = rep.get(Rep.turboblowRegen);
//			rep.set(Rep.turboblow, currentTurboBlowAmount + Math.min(restAmount, incAmount));
			rep.set(Rep.turboblow, currentTurboBlowAmount + rep.get(Rep.turboblowRegen));
		}
	}

	public double calcPowerloss(boolean actualRpmTop) {
		double top = actualRpmTop ? rep.get(Rep.rpmTop) : rep.get(Rep.rpmBaseTop);
		double loss = 1d - stats.rpm / top;
//		System.out.println("powerloss: " + loss + ", rpm: " + stats.rpm + ", top: " + top);
		return Math.round(loss * 1000d) / 10d;
	}
	
	/*
	 * GETTERS AND SETTERS
	 */

	public CarStats getStats() {
		return stats;
	}

	public Rep getRep() {
		return rep;
	}

	public void setRep(Rep rep) {
		this.rep = rep;
	}

	public CarAudio getAudio() {
		return audio;
	}

	public void setAudio(CarAudio audio) {
		this.audio = audio;
	}

	public int getSpeed() {
		return (int) stats.speed;
	}

	public String getDistanceOnline() {
		return model.isFinished() ? "finished" : (int) -model.getPositionDistance() + "m";
	}

	public boolean hasTurbo() {
		return rep.hasTurbo();
	}

	public boolean hasNOS() {
		return rep.hasNOS();
	}

	public boolean hasTireboost() {
		return rep.hasTireboost();
	}

	public CarModel getModel() {
		return model;
	}

	public boolean canShift() {
		return stats.clutchPercent >= 1d || rep.is(Rep.throttleShift);
	}

}
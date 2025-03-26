package audio;

import java.util.Random;

import engine.math.Vec3;
import main.Features;
import player_local.car.CarStats;

/**
 * For each race load up the sounds for the cars and whatever is needed with
 * them here. Maybe have the buffers ready somewhere else, but you create the
 * carsounds here at the start of each race, and you delete them here after a
 * race. The same carsound is used when someone crosses the line. So that means,
 * actually, that you need sounds for every player. And then base the sound on
 * carstats and how powerful the engine is. But this can be implemented later. I
 * was the car to sound CRAAAAZY if you have tons of power. Maybe have like a
 * buzzing sound from the tireboost and a pssssst sound from the nos bottles.
 * For atmosphere, ya know. And then wind, rocks, shaking, quiet down everything
 * before hitting soundbarrier, etc.
 * 
 * I think that there should be nothing but atmosphere when actually racing.
 * Finish, win, lobby, whatever, there it is good to have music. Actually, it
 * would fit good to have a beat come in after you finish a race.. huh. Nothing
 * crazy there.
 * 
 * @author Jens Benz
 *
 */

public class CarAudio {

	public int level = 0;
	private Source motor, turboBlowoff, turbospool, supercharger, straightcutgears, redline, tireboost, grind, nos, nosMid, nosBig, nosDown,
			soundbarrier, gear, clutch, backfire, air0, air1, air2, beep;
	private int[] gears;
	private int motorAcc, motorDcc;
	private int clutchIn, clutchOut;
	private int turboBlowoffLow, turboBlowoffHigh, turboBlowoffHigh2;
	private float motorOverallVolume = 1;
	private float wavgain, turboBlowoffVolume;
	private AudioRemote audio;
	private Random r;

	public CarAudio(int motorAcc, int motorDcc, AudioRemote audio) {
		this.audio = audio;
		gear = new Source();
		r = new Random();
		setMotorAcc(motorAcc, motorDcc);
	}

	public void updateVolume() {
		float volume = audio.getVolume(AudioTypes.MASTER) * audio.getVolume(AudioTypes.SFX);

		float gain = (float) (volume * 2.5f);
		wavgain = gain / 2;
		if (wavgain > 1)
			wavgain = 1;

		turboBlowoffVolume = volume;
		turboBlowoff.volume(volume);
		gear.volume(volume);
		redline.volume(volume);
		nos.volume(volume);
		nosMid.volume(volume);
		nosBig.volume(volume);
		nosDown.volume(volume);
		tireboost.volume(volume);
		grind.volume(volume);
		soundbarrier.volume(volume);
		clutch.volume(volume);
		backfire.volume(volume);
		air0.volume(volume);
		air1.volume(volume);
		air2.volume(volume);
	}

	public void straightcutgearsPitch(double speed, double speedTop) {
		double value;
		double maxValue = .8;
		speed = maxValue * speed;

		if (speed <= 0)
			value = 0;
		else {

			value = speed / speedTop;
			value += 0.02;
		}

		straightcutgears.pitch((float) value);
		straightcutgears.volume((float) (value * .5 * wavgain));
	}

	public void airPitch(double speed, double aero) {
		float value;
		if (speed <= 0)
			value = 0;
		else {
			value = (float) speed;
		}
		var pitch = value * .0005f * (float) Math.max((float) aero, 0.2f);
		var volume = Math.min(pitch * 2f, 0.9f) * wavgain;
		
		air2.pitch(pitch);
		air2.volume(0.75f*volume);
		
		pitch = Math.min(pitch, 1);
		
		air0.pitch(pitch);
		air0.volume(5f*volume);
		air1.pitch(pitch);
		air1.volume(3f*volume);
	}

	public void startAlwaysOnSounds(CarStats carStats) {
		if (carStats.sequentialShift)
			straightcutgears.play();
		air0.play();
		air1.play();
		air2.play();
		air0.volume(0);
		air1.volume(0);
		air2.volume(0);
		air0.loop(true);
		air1.loop(true);
		air2.loop(true);

		// Initial
		motorDcc();
	}

	private void randomizeBeep() {
		float pitch = 0.7f + r.nextFloat() * 1.25f;
		beep.pitch(pitch);
	}

	public void motorPitch(double rpm, double totalRPM, double maxValue, float gain) {
		double value;
		rpm = maxValue * rpm;

		if (rpm < 0)
			value = 0;
		else
			value = rpm / totalRPM;

		motor.pitch((float) value);

		if (value > 1.0) {
			motorOverallVolume = 1;
		} else if (value < .6) {
			motorOverallVolume = .6f;
		} else {
			motorOverallVolume = (float) value;
		}

		motorOverallVolume = motorOverallVolume * gain;

		motor.volume(wavgain * motorOverallVolume);
	}

	public void motorAcc(boolean hasTurbo) {
		motor.play(motorAcc);

		if (hasTurbo && !turbospool.isPlaying()) {
			turbospool.play();
			turboBlowoff.stop();
		}
	}

	public void motorDcc() {
		turbospool.stop();
		motor.play(motorDcc);
	}

	public void turboBlowoff(double barspool) {
		float volume = (float) barspool / 2f;
		if (volume > 2)
			volume = 2;
		turboBlowoff.volume(turboBlowoffVolume * volume);

		if (barspool < 6) {
			turboBlowoff.play(turboBlowoffLow);
		} else {
			if (Features.ran.nextBoolean())
				turboBlowoff.play(turboBlowoffHigh);
			else
				turboBlowoff.play(turboBlowoffHigh2);
		}
	}

	public void beep() {
		randomizeBeep();
		beep.play();
	}

	public void turbospoolPitch(float spool, float turboKw, float gain, float percentTurboSuper, float percentRpmToTop) {
		float value;

		float affect = turboKw / 800f;
		if (affect < 1)
			spool = spool * affect;

		if (spool > 1.0f)
			value = 1;
		else
			value = spool;

		
		
		turbospool.pitch(value);
		turbospool.volume(
				value * audio.getVolume(AudioTypes.MASTER)
				* audio.getVolume(AudioTypes.SFX) * gain * percentTurboSuper);

		if (!supercharger.isPlaying())
			supercharger.play();
		supercharger.pitch(percentRpmToTop * .9f);
		var vol = 
				value * audio.getVolume(AudioTypes.MASTER)
				* audio.getVolume(AudioTypes.SFX) * gain * (1f - percentTurboSuper);
		vol *= 0.5f;
		if (vol > 0.2f)
			vol = 0.2f;
		supercharger.volume(vol);

	}

	public void redline() {
		if (!redline.isPlaying())
			redline.play();
	}

	public void redlineStop() {
		if (redline != null && redline.isPlaying()) {
			redline.stop();
		}
	}

	public void tireboost() {
		tireboost.play();
	}

	public void backfire() {
		backfire.play();
	}

	public void gear() {
		int nextSfx = 0;
		nextSfx = r.nextInt(7);
		gear.play(gears[nextSfx]);
	}

	public void gearNeutral() {
		int nextSfx = 7;
		nextSfx += r.nextInt(gears.length - nextSfx);
		gear.play(gears[nextSfx]);
	}

	public void grind() {
		grind.play();
	}

	public void nos(boolean down, double amount) {
		if (down)
			nosDown.play();
		else if (amount < 100)
			nos.play();
		else if (amount < 1000)
			nosMid.play();
		else
			nosBig.play();
	}

	public void soundbarrier() {
		soundbarrier.play();
	}

	public void clutch(boolean in) {
		if (in)
			clutch.play(clutchOut);
		else
			clutch.play(clutchIn);
	}

	public void reset() {
		motor.stop();
		nosDown.stop();
		turboBlowoff.stop();
		turbospool.stop();
		straightcutgears.stop();
		backfire.stop();
		redline.stop();
		tireboost.stop();
		grind.stop();
		nos.stop();
		nosMid.stop();
		nosBig.stop();
		gear.stop();
		soundbarrier.stop();
		clutch.stop();
		air0.stop();
		air1.stop();
		air2.stop();
		supercharger.stop();
		setMotorPosition(new Vec3(0));
	}

	public void delete() {
		motor.delete();
		turboBlowoff.delete();
		turbospool.delete();
		straightcutgears.delete();
		redline.delete();
		tireboost.delete();
		grind.delete();
		nos.delete();
		nosMid.delete();
		nosBig.delete();
		nosDown.delete();
		gear.delete();
		soundbarrier.delete();
		clutch.delete();
		backfire.delete();
		air0.delete();
		air1.delete();
		air2.delete();
		supercharger.delete();
	}

	public void destroy() {
		motor.destroy();
		turboBlowoff.destroy();
		turbospool.destroy();
		straightcutgears.destroy();
		redline.destroy();
		tireboost.destroy();
		grind.destroy();
		nos.destroy();
		nosMid.destroy();
		nosBig.destroy();
		nosDown.destroy();
		gear.destroy();
		soundbarrier.destroy();
		clutch.destroy();
		backfire.destroy();
		air0.destroy();
		air1.destroy();
		air2.destroy();
		supercharger.destroy();
	}

	public void setTurboBlowoff(int turboBlowoffLow, int turboBlowoffHigh, int turboBlowoffHigh2) {
		turboBlowoff = new Source();
		this.turboBlowoffLow = turboBlowoffLow;
		this.turboBlowoffHigh = turboBlowoffHigh;
		this.turboBlowoffHigh2 = turboBlowoffHigh2;
	}

	public void setTurbospool(Source turbospool) {
		this.turbospool = turbospool;
		turbospool.loop(true);
	}

	public void setStraightcut(Source straightcut) {
		this.straightcutgears = straightcut;
		straightcut.loop(true);
	}

	public void setBeep(Source beep) {
		this.beep = beep;
	}

	public void setRedline(Source redline) {
		this.redline = redline;
//		redline.loop(true);
	}

	public void setTireboost(Source tireboost) {
		this.tireboost = tireboost;
	}

	public void setGrind(Source grind) {
		this.grind = grind;
	}

	public void setNos(Source nos, Source nosMid, Source nosBig) {
		this.nos = nos;
		this.nosMid = nosMid;
		this.nosBig = nosBig;
	}

	public void setGears(int[] gears2) {
		this.gears = gears2;
	}

	public void setMotorAcc(int motorAcc, int motorDcc) {
		if (motor != null)
			motor.destroy();
		motor = new Source();
		this.motorAcc = motorAcc;
		this.motorDcc = motorDcc;
		motor.loop(true);
	}

	public void setSoundbarrier(Source soundbarrier) {
		this.soundbarrier = soundbarrier;
	}

	public void setClutch(Source clutch, int clutchIn, int clutchOut) {
		this.clutch = clutch;
		this.clutchIn = clutchIn;
		this.clutchOut = clutchOut;
	}

	public Source getMotor() {
		return motor;
	}

	public void setMotorPosition(Vec3 pos) {
		motor.position(pos);
		turbospool.position(pos);
		straightcutgears.position(pos);
	}

	public void setBackfire(Source source) {
		backfire = source;
	}

	public void setNosDown(Source nosDown) {
		this.nosDown = nosDown;
	}

	public void setAir(Source nosDown, Source air1, Source air2) {
		this.air0 = nosDown;
		this.air1 = air1;
		this.air2 = air2;
	}

	public void setSupercharger(Source source) {
		supercharger = source;
		supercharger.loop(true);
	}

}

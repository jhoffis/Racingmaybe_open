package audio;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.libc.LibCStdlib;

import engine.math.Vec3;
import engine.utils.TwoTypes;
import main.Features;
import main.Main;
import player_local.car.Car;
import player_local.upgrades.TileNames;
import player_local.upgrades.Upgrade;
import settings_and_logging.RSet;

/**
 * 
 * Make some beautiful single sound effects for whenever you press something.
 * Like in aoe1
 * 
 * @author Jens Benz
 *
 */
public class AudioMaster implements AudioRemote {

	public static boolean dontLoadMusic;
	private final ByteBuffer musicBuffer = BufferUtils.createByteBuffer(16000000);
	private final ArrayList<String> musicLoaded = new ArrayList<>();
	private final boolean[] musicPlayed;
	private final Source music;
	private String currentMusic = "none";

	private final List<Integer> buffers;
	private long device, context;
	private float masterVolume, musicVolume, sfxVolume;

	private final HashMap<SfxTypes, Source> sfxs;
	private final HashMap<TileNamesSfxs, Source> upgrades;
//	private final Source upgradeHover;
//	private final int[] upgradesHover;
	private final HashMap<Integer, Source> taunts;
	private final Stack<CarAudio> cars;

	private final int turboBlowoffLow, turboBlowoffHigh, turboBlowoffHigh2, turbospool, straightcut, redline, grind, nos, nosMid, nosBig, tireboost,
			nosDown, soundbarrier, clutchIn, clutchOut, backfire, air, air1, air2, supercharger, beep;

	public AudioMaster(float master, float sfx, float music) {
		this.masterVolume = master;
		this.sfxVolume = sfx;
		this.musicVolume = music;

		try {
			device = ALC10.alcOpenDevice((ByteBuffer) null);
			ALCCapabilities deviceCaps = ALC.createCapabilities(device);

			if (device != 0) {
				context = ALC10.alcCreateContext(device, (IntBuffer) null);
				ALC10.alcMakeContextCurrent(context);
				AL.createCapabilities(deviceCaps);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		setListenerData(0, 0, 0);
		AL11.alDistanceModel(AL11.AL_INVERSE_DISTANCE_CLAMPED);

		/*
		 * LOAD SHIT
		 */
		buffers = new ArrayList<>();
		upgrades = new HashMap<>();
		cars = new Stack<>();
		sfxs = new HashMap<>();
		this.taunts = new HashMap<>();

		this.music = new Source();

		{
			File musicFolder = new File("audio/music");
			for (var file : Objects.requireNonNull(musicFolder.listFiles())) {
				if (file.isFile()) {
					var splittedPath = file.getPath();
					var splittedFilename = splittedPath.split("\\.");
					if (splittedFilename.length > 0 && splittedFilename[splittedFilename.length - 1].equals("ogg")) {
						musicLoaded.add(splittedPath);
					}
				}
			}
			musicPlayed = new boolean[musicLoaded.size()];
		}

		int regularPress = loadSound("audio/sfx/button/press_reg.ogg", buffers);
		int regularHover = loadSound("audio/sfx/button/hover_reg.ogg", buffers);
		int ready = loadSound("audio/sfx/button/ready.ogg", buffers);
		int unready = loadSound("audio/sfx/button/unready.ogg", buffers);
		int buysucc = loadSound("audio/sfx/button/money.ogg", buffers);
		int moneyEarned = loadSound("audio/sfx/button/money_earned.ogg", buffers);
		int buyfail = loadSound("audio/sfx/button/failed.ogg", buffers);
		int undo = loadSound("audio/sfx/button/undo.ogg", buffers);
		int redLight = loadSound("audio/sfx/race/redLight.ogg", buffers);
		int greenLight = loadSound("audio/sfx/race/greenLight.ogg", buffers);
		int countdown = loadSound("audio/sfx/race/tick.ogg", buffers);
		int start = loadSound("audio/sfx/race/tockstart.ogg", buffers);
		int openStore = loadSound("audio/sfx/upgrade/openStore.ogg", buffers);
		int closeStore = loadSound("audio/sfx/upgrade/closeStore.ogg", buffers);
		int joined = loadSound("audio/sfx/button/joined.ogg", buffers);
		int left = loadSound("audio/sfx/button/left.ogg", buffers);
		int chat = loadSound("audio/sfx/button/chat.ogg", buffers);
		int newBonus = loadSound("audio/sfx/bonus/newBonus.ogg", buffers);
		int cancelBonus = loadSound("audio/sfx/bonus/cancelBonus.ogg", buffers);
		int[] bonuses = new int[SfxTypes.bonusSfxAmount];
		for (int i = 0; i < bonuses.length; i++) {
			bonuses[i] = loadSound("audio/sfx/bonus/bonus" + i + ".ogg", buffers);
		}
		int won = loadSound("audio/sfx/race/won.ogg", buffers);
		int lost = loadSound("audio/sfx/race/lost.ogg", buffers);
		int woosh = loadSound("audio/sfx/car/whoosh.ogg", buffers);
		int lostLife = loadSound("audio/sfx/race/lostlife.ogg", buffers);
		int startEngine = loadSound("audio/sfx/car/startMotor.ogg", buffers);
		int unlocked = loadSound("audio/sfx/upgrade/unlocked.ogg", buffers);
		int hyper = loadSound("audio/sfx/bonus/patternsound.ogg", buffers);
		int nextcar = loadSound("audio/sfx/button/nextcar.ogg", buffers);

		for (var t : TileNamesSfxs.values()) {
			int buffer = loadSound("audio/sfx/upgrade/upgrade_" + t.toString().toLowerCase() + ".ogg", buffers);
			upgrades.put(t, new Source(buffer));
		}

//		upgradeHover = new Source();
//		upgradesHover = new int[UpgradeType.values().length];
//		for (var val : UpgradeType.values()) {
//			int buffer = loadSound("audio/sfx/upgrade/upgrade" + val + ".ogg", buffers);
//			upgradesHover[val.ordinal()] = buffer;
//		}

		turboBlowoffLow = loadSound("audio/sfx/car/turboblowofflow.ogg", buffers);
		turboBlowoffHigh = loadSound("audio/sfx/car/turboblowoffhigh.ogg", buffers);
		turboBlowoffHigh2 = loadSound("audio/sfx/car/turboblowoffhigh2.ogg", buffers);
		turbospool = loadSound("audio/sfx/car/turbospool.ogg", buffers);
		straightcut = loadSound("audio/sfx/car/straightcutgears.ogg", buffers);
		redline = loadSound("audio/sfx/car/redline.ogg", buffers);
		grind = loadSound("audio/sfx/car/grind.ogg", buffers);
		nos = loadSound("audio/sfx/car/nos.ogg", buffers);
		nosMid = loadSound("audio/sfx/car/nosMid.ogg", buffers);
		nosBig = loadSound("audio/sfx/car/nosBig.ogg", buffers);
		nosDown = loadSound("audio/sfx/car/nosDown.ogg", buffers);
		soundbarrier = loadSound("audio/sfx/car/soundbarrier.ogg", buffers);
		clutchIn = loadSound("audio/sfx/car/clutch.ogg", buffers);
		clutchOut = loadSound("audio/sfx/car/unclutch.ogg", buffers);
		backfire = loadSound("audio/sfx/car/backfireSequential.ogg", buffers);
		air = loadSound("audio/sfx/car/air.ogg", buffers);
		air1 = loadSound("audio/sfx/car/air1.ogg", buffers);
		air2 = loadSound("audio/sfx/car/air2.ogg", buffers);
		supercharger = loadSound("audio/sfx/car/supercharger.ogg", buffers);
		beep = loadSound("audio/sfx/car/beep.ogg", buffers);

		tireboost = upgrades.get(TileNamesSfxs.Tireboost).getBuffer();

		int[] taunts = { loadSound("audio/sfx/taunt/greetings.ogg", buffers),
				loadSound("audio/sfx/taunt/yes.ogg", buffers), loadSound("audio/sfx/taunt/no.ogg", buffers),
				loadSound("audio/sfx/taunt/hows_your_car_running.ogg", buffers),
				loadSound("audio/sfx/taunt/giveer_the_beans.ogg", buffers),
				loadSound("audio/sfx/taunt/need_more_tireboost.ogg", buffers),
				loadSound("audio/sfx/taunt/woah.ogg", buffers),
				loadSound("audio/sfx/taunt/i_have_more_money_than_you.ogg", buffers),
				loadSound("audio/sfx/taunt/it_be_like_that_sometimes.ogg", buffers),
				loadSound("audio/sfx/taunt/impressive_very_nice.ogg", buffers),
				loadSound("audio/sfx/taunt/i_see.ogg", buffers), loadSound("audio/sfx/taunt/ahaha.ogg", buffers),
				loadSound("audio/sfx/taunt/perhaps.ogg", buffers),
				loadSound("audio/sfx/taunt/i_dont_think_so.ogg", buffers),
				loadSound("audio/sfx/taunt/start_the_game_already.ogg", buffers),
				loadSound("audio/sfx/taunt/main_menu.ogg", buffers),
				loadSound("audio/sfx/taunt/heh_nice_car.ogg", buffers),
				loadSound("audio/sfx/taunt/nowhere.ogg", buffers), loadSound("audio/sfx/taunt/afk.ogg", buffers),
				loadSound("audio/sfx/taunt/doggo.ogg", buffers), loadSound("audio/sfx/taunt/lightspeed.ogg", buffers),
				loadSound("audio/sfx/taunt/nosleftover.ogg", buffers),
				loadSound("audio/sfx/taunt/pistons.ogg", buffers), loadSound("audio/sfx/taunt/weak.ogg", buffers),
				loadSound("audio/sfx/taunt/mom.ogg", buffers), loadSound("audio/sfx/taunt/so_eager.ogg", buffers),
				loadSound("audio/sfx/taunt/another.ogg", buffers), loadSound("audio/sfx/taunt/break.ogg", buffers),
				loadSound("audio/sfx/taunt/i_said.ogg", buffers), loadSound("audio/sfx/taunt/oil.ogg", buffers),
				loadSound("audio/sfx/taunt/please.ogg", buffers), loadSound("audio/sfx/taunt/so.ogg", buffers),
				loadSound("audio/sfx/taunt/kill.ogg", buffers), loadSound("audio/sfx/taunt/shifting.ogg", buffers),
				loadSound("audio/sfx/taunt/fix.ogg", buffers), loadSound("audio/sfx/taunt/take_it.ogg", buffers),
//				loadSound("audio/sfx/taunt/anotherday.ogg", buffers),
				loadSound("audio/sfx/taunt/carisfat.ogg", buffers),
				loadSound("audio/sfx/taunt/what.ogg", buffers),
				loadSound("audio/sfx/taunt/whatdiditellyou.ogg", buffers),
				loadSound("audio/sfx/taunt/bolton.ogg", buffers),
				loadSound("audio/sfx/taunt/monica.ogg", buffers),
				loadSound("audio/sfx/taunt/illtellyou.ogg", buffers),
				loadSound("audio/sfx/taunt/guysssss.ogg", buffers),
				loadSound("audio/sfx/taunt/if you.ogg", buffers),
				loadSound("audio/sfx/taunt/beaten.ogg", buffers),
				loadSound("audio/sfx/taunt/garage.ogg", buffers),
				loadSound("audio/sfx/taunt/hei.ogg", buffers),
				loadSound("audio/sfx/taunt/excuse.ogg", buffers),
				loadSound("audio/sfx/taunt/money.ogg", buffers),
				loadSound("audio/sfx/taunt/anothernos.ogg", buffers),
				loadSound("audio/sfx/taunt/notwithmeonyourtail.ogg", buffers),
				loadSound("audio/sfx/taunt/all units.ogg", buffers),
				loadSound("audio/sfx/taunt/imwaiting.ogg", buffers),
				loadSound("audio/sfx/taunt/millions.ogg", buffers),
				loadSound("audio/sfx/taunt/racestarts.ogg", buffers),
		};

		new Thread(() -> {
			createSfx(SfxTypes.REGULAR_PRESS, regularPress);
			createSfx(SfxTypes.REGULAR_HOVER, regularHover);
			createSfx(SfxTypes.READY, ready);
			createSfx(SfxTypes.UNREADY, unready);
			createSfx(SfxTypes.BUY, buysucc);
			createSfx(SfxTypes.BUY_EARNED, moneyEarned);
			createSfx(SfxTypes.BUY_FAILED, buyfail);
			createSfx(SfxTypes.UNDO, undo);
			createSfx(SfxTypes.REDLIGHT, redLight);
			createSfx(SfxTypes.GREENLIGHT, greenLight);
			createSfx(SfxTypes.COUNTDOWN, countdown);
			createSfx(SfxTypes.START, start);
			createSfx(SfxTypes.OPEN_STORE, openStore);
			createSfx(SfxTypes.CLOSE_STORE, closeStore);
			createSfx(SfxTypes.JOINED, joined);
			createSfx(SfxTypes.LEFT, left);
			createSfx(SfxTypes.CHAT, chat);
			createSfx(SfxTypes.NEW_BONUS, newBonus);
			createSfx(SfxTypes.CANCEL_BONUS, cancelBonus);
			createSfx(SfxTypes.WON, won);
			createSfx(SfxTypes.LOST, lost);
			createSfx(SfxTypes.WHOOSH, woosh);
			createSfx(SfxTypes.LOSTLIFE, lostLife);
			createSfx(SfxTypes.START_ENGINE, startEngine);
			for (int i = 0; i < bonuses.length; i++) {
				createSfx(SfxTypes.valueOf("BOLT_BONUS" + i), bonuses[i]);
			}
			createSfx(SfxTypes.UNLOCKED, unlocked);
			createSfx(SfxTypes.HYPER, hyper);
			createSfx(SfxTypes.NEXTCAR, nextcar);

			for (int i = 0; i < taunts.length; i++) {
				createTaunt(i, taunts[i]);
			}

			updateVolumeSfx();
			updateVolumeMusic();
		}).start();
	}

	public AudioMaster() {
		this((float) RSet.getDouble(RSet.masterVolume), 
				(float) RSet.getDouble(RSet.sfxVolume),
				(float) RSet.getDouble(RSet.musicVolume));
	}

	@Override
	public void setListenerData(Vec3 v) {
		setListenerData(v.x, v.y, v.z);
	}

	@Override
	public void setListenerData(float x, float y, float z) {
		AL11.alListener3f(AL11.AL_POSITION, x, y, z);
		AL11.alListener3f(AL11.AL_VELOCITY, 0, 0, 0);
	}

	public TwoTypes<Integer, ByteBuffer> loadSoundBuffer(String file, List<Integer> buffers, ByteBuffer vorbis) {
		int buffer = AL11.alGenBuffers();
		buffers.add(buffer);
		try (MemoryStack stack = MemoryStack.stackPush()) {
//			System.out.println("file: " + file + ", address: " + stack.address() + ", size: " + stack.getSize());
			try (InputStream source = new BufferedInputStream(new FileInputStream(file));
					ReadableByteChannel rbc = Channels.newChannel(source)) {
				int bufferSize = source.available();
//				System.out.println("size: " + bufferSize);
//				if (bufferSize > 40000)
//					return 0;

				if (vorbis == null)
					vorbis = BufferUtils.createByteBuffer(bufferSize + 1); // Normally you would check more, but we
																			// always assume it fits
				rbc.read(vorbis); // Stream the bytes of the file into vorbis
				
				
//				vorbis.
			} catch (IOException e) {
				e.printStackTrace();
			}

			assert vorbis != null;
			vorbis.flip();

			IntBuffer channels = stack.mallocInt(1), sampleRate = stack.mallocInt(1);
			ShortBuffer pcm = STBVorbis.stb_vorbis_decode_memory(vorbis, channels, sampleRate); // 30mb

			// Copy to buffer
			assert pcm != null;
			// 30 mb
			AL11.alBufferData(buffer, channels.get() == 1 ? AL11.AL_FORMAT_MONO16 : AL11.AL_FORMAT_STEREO16, pcm,
					sampleRate.get());
			vorbis.clear();
			LibCStdlib.free(pcm);
		}
		return new TwoTypes<Integer, ByteBuffer>(buffer, vorbis);
	}

	public int loadSound(String file, List<Integer> buffers) {
		return loadSoundBuffer(file, buffers, null).first();
	}

	public int loadSound(String file) {
		return loadSound(file, buffers);
	}

	public void createSfx(SfxTypes type, int buffer) {
		sfxs.put(type, new Source(buffer));
	}

	public void createTaunt(int i, int buffer) {
		taunts.put(i, new Source(buffer));
	}

	public void destroy() {

		for (int buffer : buffers) {
			AL11.alDeleteBuffers(buffer);
		}
		buffers.clear();

		for (Source s : sfxs.values()) {
			s.destroy();
		}

		for (Source s : upgrades.values()) {
			s.destroy();
		}

		music.destroy();

		while (!cars.isEmpty()) {
			cars.pop().destroy();
		}

		ALC11.alcDestroyContext(context);
		ALC11.alcCloseDevice(device);
	}

	@Override
	public void setVolume(AudioTypes type, float volume) {
		switch (type) {
		case MASTER -> masterVolume = volume;
		case MUSIC -> musicVolume = volume;
		case SFX -> sfxVolume = volume;
		}
	}

	@Override
	public float getVolume(AudioTypes type) {
		switch (type) {
		case MASTER:
			return masterVolume;
		case MUSIC:
			return musicVolume;
		case SFX:
			return sfxVolume;
		default:
			return -1;
		}
	}

	@Override
	public void updateVolumeSfx() {
		if (Main.NO_SOUND)
			return;

		float volume = this.masterVolume * this.sfxVolume;

		for (var sfx : sfxs.values()) {
			sfx.volume(volume);
		}
		for (var taunt : taunts.values()) {
			taunt.volume(volume);
		}
		for (Source up : upgrades.values()) {
			up.volume(volume);
		}
//		upgradeHover.volume(volume);
	}

	@Override
	public void updateVolumeMusic() {
		if (Main.NO_SOUND)
			return;
		music.volume(masterVolume * musicVolume);
	}

	@Override
	public CarAudio getNewCarAudio(String carname) {

		int acc = loadSound("audio/sfx/car/motorAcc0" + carname + ".ogg");
		int dcc = loadSound("audio/sfx/car/motorDcc0" + carname + ".ogg");

		CarAudio car = new CarAudio(acc, dcc, this);

		car.setTurboBlowoff(turboBlowoffLow, turboBlowoffHigh, turboBlowoffHigh2);
		car.setTurbospool(new Source(turbospool));
		car.setSupercharger(new Source(supercharger));
		car.setAir(new Source(air), new Source(air1), new Source(air2));
		car.setStraightcut(new Source(straightcut));
		car.setRedline(new Source(redline));
		car.setTireboost(new Source(tireboost));
		car.setGrind(new Source(grind));
		car.setNos(new Source(nos), new Source(nosMid), new Source(nosBig));
		car.setNosDown(new Source(nosDown));
		car.setSoundbarrier(new Source(soundbarrier));
		car.setClutch(new Source(), clutchIn, clutchOut);
		car.setBackfire(new Source(backfire));
		car.setBeep(new Source(beep));

		int[] gears = new int[11];

		for (int i = 0; i < gears.length; i++) {
			gears[i] = loadSound("audio/sfx/car/gear" + i + ".ogg");
		}

		car.setGears(gears);

		cars.push(car);

		return car;
	}

	@Override
	public void updateCarAudio(Car car) {
		if (car.getAudio() == null)
			return;

		var powerScore = car.getRep().getTotalKW();
		int newLevel = powerScore > 50000 ? 2 : powerScore > 1000 ? 1 : 0;
		if (newLevel > car.getAudio().level) {
			car.getAudio().level = newLevel;
			var carname = car.getRep().getName();
			if (Main.DEBUG)
				newLevel = 2;	
			int acc = loadSound("audio/sfx/car/motorAcc" + newLevel + carname + ".ogg");
			int dcc = loadSound("audio/sfx/car/motorDcc" + newLevel + carname + ".ogg");
			car.getAudio().setMotorAcc(acc, dcc);
		}
	}

	@Override
	public void stop(SfxTypes sfx) {
		if (sfxs.containsKey(sfx))
			sfxs.get(sfx).stop();
	}

	@Override
	public void play(SfxTypes sfx) {
		if (sfxs.containsKey(sfx))
			sfxs.get(sfx).play();
	}

	@Override
	public void playUpgrade(TileNames upgrade) {
		if (upgrade.ordinal() >= TileNames.Pattern0_.ordinal()) {
			upgrades.get(TileNamesSfxs.Pattern).play();
		} else if (upgrade.ordinal() >= TileNamesSfxs.NegTile.ordinal()) {
			upgrades.get(TileNamesSfxs.NegTile).play();
		} else {
			upgrades.get(TileNamesSfxs.values()[upgrade.ordinal()]).play();
		}
	}

	@Override
	public void playUpgradeHover(Upgrade upgrade) {
//		upgradeHover.play(upgradesHover[upgrade.getUpgradeType().ordinal()]);
	}

	@Override
	public Source getTaunt(int i) {
		return taunts.getOrDefault(i, null);
	}

	/**
	 * Sjekker og spiller tilfeldig musikk om ledig og lyd er p�
	 */
	@Override
	public void checkMusic() {
		if (!Main.NO_SOUND && !dontLoadMusic && !music.isPlaying() && masterVolume * musicVolume > 0) {
//			if (!Main.NO_SOUND && musicVolume > 0) {
			boolean reset = true;
			boolean[] musicPlayed = this.musicPlayed;
			for (boolean songPlayed : musicPlayed) {
				if (!songPlayed) {
					reset = false;
					break;
				}
			}
			if (reset) {
				Arrays.fill(musicPlayed, false);
			}

			int nextSong = 0;
			do {
				nextSong = Features.ran.nextInt(musicPlayed.length);
			} while (musicPlayed[nextSong]);

			var splitMusic = musicLoaded.get(nextSong).replace('\\', '/').split("/");
			splitMusic = splitMusic[splitMusic.length - 1].split("\\.");
			currentMusic = splitMusic[0];

			musicPlayed[nextSong] = true;
			this.music.deleteBuffer();
			var newMusic = loadSoundBuffer(musicLoaded.get(nextSong), buffers, musicBuffer);
			this.music.play(newMusic.first());
		}
	}

	public void nextSong() {
		music.stop();
		checkMusic();
	}

	public String getCurrentMusic() {
		return currentMusic;
	}
}

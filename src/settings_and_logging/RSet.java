package settings_and_logging;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import scenes.regular.OptionsScene;

public enum RSet {
	width, 
	height,
	clientFullscreen, 
	vsync, 
	showHints, 
	masterVolume, 
	musicVolume, 
	sfxVolume, 
	username, 
	discID,
	challengesUnlocked, 
	lastMonitor,
	fIndex,
	challengeDayFun,
	challengeDayWeird,
	challengeDayTough,
	challengeWeekFun,
	challengeWeekWeird,
	challengeWeekTough,
	challengeMonthFun,
	challengeMonthWeird,
	challengeMonthTough,
	ip;

	public static Settings settings = init();

	private static void reset() {
		// Get resolution that is set to the computer right now.
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		int width = gd.getDisplayMode().getWidth();
		int height = gd.getDisplayMode().getHeight();
		settings.lines.clear();

		for (var set : values()) {
			set(set, "");
		}
		
		set(RSet.width, width);
		set(RSet.height, height);
		set(RSet.clientFullscreen, 0);
		set(RSet.vsync, false);
		set(RSet.showHints, true);
		set(RSet.masterVolume, 0.5);
		set(RSet.musicVolume, 0.3);
		set(RSet.sfxVolume, 1);
		set(RSet.discID, -1);
		set(RSet.challengesUnlocked, 0);
		set(RSet.lastMonitor, 0);
		set(RSet.fIndex, OptionsScene.lockFpsSet.length - 2);
	}

	public static Settings init() {
		settings = new Settings();
		try {
			settings.init("settings.properties");
			
			var lines = settings.lines;
			var vals = values();
			
			if (lines.size() != vals.length) {
				reset();
				return settings;
			}
			for (int i = 0; i < vals.length; i++) {
				if (!lines.get(i).split("=")[0].toLowerCase().equals(vals[i].toString().toLowerCase())) {
					reset();
					return settings;
				}
			}
		} catch (Exception e) {
			reset();
		}
		return settings;
	}

	public static void set(RSet setting, Object value) {
		if (value instanceof Boolean b)
			value = b ? 1 : 0;
		settings.writeToLine(setting.toString() + "=" + value, setting.ordinal());
	}

	public static long getLong(RSet setting) {
		return settings.getLong(setting.ordinal());
	}

	public static double getDouble(RSet setting) {
		return settings.getDouble(setting.ordinal());
	}

	public static int getInt(RSet setting) {
		return settings.getInt(setting.ordinal());
	}

	public static boolean getBool(RSet setting) {
		return settings.getBool(setting.ordinal());
	}

	public static String get(RSet setting) {
		return settings.get(setting.ordinal());
	}
}

package game_modes;

public enum GameModes {
	SINGLEPLAYER_CHALLENGES, 
	TOTAL,
	SHORT_TOTAL,
	MID_TOTAL, 
	LONG_TOTAL, 
	MEGA_LONG_TOTAL,
	LEADOUT,
	BELOW_MEDIUM_LEADOUT,
	MEDIUM_LEADOUT, 
	LONG_LEADOUT, 
	HARDCORE_LEADOUT,
	BELOW_MEDIUM_HARDCORE_LEADOUT,
	MEDIUM_HARDCORE_LEADOUT, 
	LONG_HARDCORE_LEADOUT, 
	FIRST_TO_3,
	FIRST_TO_5,
	FIRST_TO_7,
	FIRST_TO_9,
	FIRST_TO_12,
	FIRST_TO_18,
	TOTAL_NO_UPGRADES,
	LONG_TOTAL_NO_UPGRADES, 
	MEGA_LONG_TOTAL_NO_UPGRADES,
	TIME_ATTACK,
	PRETTY_FAST_TIME_ATTACK,
	LIGHTNING_TIME_ATTACK,
	DARN_FAST_TIME_ATTACK,
	INSANE_TIME_ATTACK,
	COOP_FUN,
	COOP_WEIRD,
	COOP_TOUGH,
	SANDBOX,
	CLASSIC
	;
	
	public boolean isSingleplayer() {
		return ordinal() < getSingleplayerModesCount();
	}
	
	public static int getSingleplayerModesCount() {
		return SINGLEPLAYER_CHALLENGES.ordinal() + 1;
	}

	public static int getMultiplayerModesCount() {
		return values().length - getSingleplayerModesCount();
	}

}

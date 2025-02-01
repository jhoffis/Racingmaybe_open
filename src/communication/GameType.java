package communication;

public enum GameType {
	
	NONE, DIRECT, SINGLEPLAYER, JOINING_ONLINE, JOINING_LAN, CREATING_ONLINE, CREATING_LAN;
	
	public boolean isCreating() {
		// creating or singleplayer
		return this.ordinal() >= CREATING_ONLINE.ordinal() || this.ordinal() <= SINGLEPLAYER.ordinal();
	}

	public boolean isSinglePlayer() {
		return this.equals(SINGLEPLAYER);
	}
	
	public boolean isSteam() {
		return ordinal() == CREATING_ONLINE.ordinal() || ordinal() == JOINING_ONLINE.ordinal();
	}

    public GameType join() {
		if (this.equals(CREATING_LAN))
			return JOINING_LAN;
		if (this.equals(CREATING_ONLINE))
			return JOINING_ONLINE;
		return this;
    }
}

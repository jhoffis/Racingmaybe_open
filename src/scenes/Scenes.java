package scenes;

import java.util.Stack;

public class Scenes {
	
	private static int index = -1; 
	
	/*
	 * REGULAR indexes
	 */
	public static final int 
		PREVIOUS = index, 
		MAIN_MENU = ++index, 
		SINGLEPLAYER = ++index, 
		MULTIPLAYER = ++index,
		REPLAYLIST = ++index,
		OPTIONS = ++index, 
		HOTKEY_OPTIONS = ++index, 
		JOINING = ++index, 
		LEADERBOARD = ++index, 
		LOBBY = ++index,
		RACE = ++index,
		DESIGNER_NOTES = ++index, 
		TESTING = ++index,
		GENERAL_NONSCENE = ++index,
		AMOUNT = ++index;
	public static int PREVIOUS_REGULAR = MAIN_MENU,
			CURRENT = MAIN_MENU;
	public static final Stack<Integer> HISTORY = new Stack<>();
}

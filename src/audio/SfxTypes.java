package audio;

public enum SfxTypes {

	REGULAR_PRESS, REGULAR_HOVER, READY, UNREADY, BUY, BUY_EARNED, BUY_FAILED, UNDO, NEXTCAR,
	REDLIGHT, GREENLIGHT, COUNTDOWN, START, OPEN_STORE, CLOSE_STORE, JOINED, LEFT,
	CHAT, NEW_BONUS, CANCEL_BONUS, START_ENGINE, WON, LOST, WHOOSH, LOSTLIFE, UNLOCKED, HYPER,
	
	BOLT_BONUS0,
	BOLT_BONUS1,
	BOLT_BONUS2,
	BOLT_BONUS3,
	BOLT_BONUS4;
	
	public static final int bonusSfxAmount = 5;
	public static SfxTypes boltBonus(int i) {
		return SfxTypes.valueOf("BOLT_BONUS" + (i >= bonusSfxAmount ? bonusSfxAmount - 1 : i));
	}
}

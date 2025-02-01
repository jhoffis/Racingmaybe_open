package player_local.upgrades;

import player_local.Player;
import player_local.car.Rep;

public interface UpgradeAction {
	void upgrade(RegVals regularValues, Player player, Rep rep, int goldType, boolean test);
}

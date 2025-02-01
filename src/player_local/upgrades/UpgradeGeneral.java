package player_local.upgrades;

import adt.ICloneStringable;
import player_local.Bank;
import player_local.car.Rep;

public interface UpgradeGeneral extends ICloneStringable {

	UpgradeGeneral clone();
	boolean isOpenForUse();
	boolean isPlaced();
	byte getNameID();
	TileNames getTileName();
	void setPremadePrice(float price);
	float getPremadePrice();
	int getCost(float sale);
	int getSellPrice(int round);
	void sell(Bank bank, Rep rep, Upgrades upgrades, int round);
	UpgradeType getUpgradeType();
    void setVisible(boolean b);
}

package player_local.upgrades;

import java.util.concurrent.atomic.AtomicInteger;

import player_local.Bank;
import player_local.car.Rep;

public class EmptyTile implements UpgradeGeneral {

	@Override
	public void getCloneString(StringBuilder outString, int lvlDeep, String splitter, boolean test) {
	}

	@Override
	public void setCloneString(String[] cloneString, AtomicInteger fromIndex) {
	}

	@Override
	public UpgradeGeneral clone() {
		return this;
	}

	@Override
	public boolean isOpenForUse() {
		return false;
	}

	@Override
	public boolean isPlaced() {
		return false;
	}

	@Override
	public byte getNameID() {
		return -1;
	}

	@Override
	public TileNames getTileName() {
		return null;
	}

	@Override
	public void setPremadePrice(float price) {
	}

	@Override
	public float getPremadePrice() {
		return 0;
	}

	@Override
	public int getCost(float sale) {
		return 0;
	}

	@Override
	public int getSellPrice(int round) {
		return 0;
	}

	@Override
	public void sell(Bank bank, Rep rep, Upgrades upgrades, int round) {
	}

	@Override
	public UpgradeType getUpgradeType() {
		return UpgradeType.EMPTY;
	}

	@Override
	public void setVisible(boolean b) {
	}
}

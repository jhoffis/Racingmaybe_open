package player_local.upgrades;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import communication.Translator;
import engine.graphics.ui.IUIObject;
import engine.graphics.ui.UILabel;
import engine.graphics.ui.modal.UIBonusModal;
import main.Texts;
import player_local.Bank;
import player_local.car.Rep;
import scenes.game.Lobby;
import player_local.Layer;
import player_local.Player;
import player_local.TilePiece;

public class Upgrade implements UpgradeGeneral {

	protected RegVals regularValues, neighbourModifier;
	private float price, priceOG, priceTotal, maxLVL, minLVL = -1; // Samling av all kostnad inkl imp - brukes til salg feks
	private byte nameID;
	public String overrideName;
	public static byte placedNeighbourChangeLVL;
	public static final float sellDivision = .33f;
	private byte[] bonusLVLs, bonusesTaken;
	private RegVals[] bonuses;
	private int lvl, lvlRealHidden, maxLVLRealHidden, placedRound = -1, maxLVLForced = -1;
	private UpgradeType upgradeType;
	public TileNames tileName;
	private final Stack<Byte> unlocks = new Stack<>(), freeUpgrades = new Stack<>();
	private final HashMap<Byte, Float> maxLVLChanger = new HashMap<>();
	public static float priceFactorStd = 0.75f, pricePlacedStd;
	public float priceModifier = priceFactorStd;
	public boolean starterUpgrade;
	private boolean visible, newlyUnlocked;

	private final RegValList gainedValues = new RegValList(0); // samling av verdier som kommer av plassering og
																// upgrading av tilen.
	public int bonusCostOverride, bonusGainOverride = -1;
	private TileNames requiredUpgradeToUnlock;

	public Upgrade() {
	}

	public Upgrade(TileNames tileName) {
		this.tileName = tileName;
		nameID = (byte) tileName.ordinal();
		regularValues = new RegVals();
		neighbourModifier = new RegVals();
		bonusLVLs = new byte[0];
		bonuses = new RegVals[0];
	}

	/**
	 * Completely placed har med at plassering uten og med bonus er litt forskjellig
	 * og fromLVL skal vise riktig navn.
	 */
	public void place(float paidPrice, int round) {
		lvl = lvlRealHidden = 0;
		bonuses = new RegVals[0];
		bonusLVLs = new byte[0];
		bonusesTaken = new byte[0];
		priceOG = Math.round(paidPrice);
		priceModifier = priceFactorStd;
		price = pricePlacedStd;
		placedRound = round;
		
		var values = regularValues.values();
		for (int i = 0; i < values.length; i++) {
			if (i >= Rep.nosBottles || values[i] == null || values[i].removeAtPlacement)
				values[i] = null;
		}
		maxLVLChanger.clear();
	}

	@Override
	public boolean isPlaced() {
		return placedRound != -1;
	}

	@Override
	public Upgrade clone() {
		var res = new Upgrade();
		Translator.setCloneString(res, Translator.getCloneString(this));
		return res;
	}

	@Override
	public void getCloneString(StringBuilder outString, int lvlDeep, String splitter, boolean test) {
		if (lvlDeep > 0)
			outString.append(splitter);
		lvlDeep++;
		outString
				.append(nameID).append(splitter)
				.append(overrideName != null ? overrideName : "").append(splitter)
				.append(price).append(splitter).append(priceOG).append(splitter)
				.append(priceTotal).append(splitter).append(lvl).append(splitter).append(lvlRealHidden).append(splitter)
				.append(maxLVL).append(splitter)
				.append(maxLVLRealHidden).append(splitter)
				.append(maxLVLForced).append(splitter)
				.append(minLVL).append(splitter)
				.append(placedRound)
				.append(splitter).append(bonusGainOverride)
				.append(splitter).append(bonusCostOverride)
				.append(splitter);
		if (bonusLVLs != null && bonusLVLs.length > 0) {
			for (int i = 0; i < bonusLVLs.length; i++) {
				if (i != 0)
					outString.append(":");
				outString.append(bonusLVLs[i]).append(":");
				outString.append(bonusesTaken[i]);
			}
			for (int i = 0; i < bonusLVLs.length; i++) {
				bonuses[i].getCloneString(outString, lvlDeep, splitter, test);
			}
		} else {
			outString.append("x");
		}
		regularValues.getCloneString(outString, lvlDeep, splitter, test);
		neighbourModifier.getCloneString(outString, lvlDeep, splitter, test);
		outString.append(splitter).append(upgradeType.ordinal()).append(splitter);
		if (requiredUpgradeToUnlock != null)
			outString.append(requiredUpgradeToUnlock.ordinal()).append(splitter);
		else
			outString.append("n").append(splitter);
		for (byte b : freeUpgrades) {
			outString.append(b).append(":");
		}
		outString.append(splitter);
		for (byte b : unlocks) {
			outString.append(b).append(":");
		}
		outString.append(splitter);
		for (var bb : maxLVLChanger.entrySet()) {
			outString.append(bb.getKey()).append(",").append(bb.getValue()).append(":");
		}
		outString.append(splitter).append(priceModifier).append(splitter).append(starterUpgrade ? 1 : 0)
				.append(splitter).append(visible ? 1 : 0);

		gainedValues.getCloneString(outString, lvlDeep, splitter, test);

	}

	@Override
	public void setCloneString(String[] cloneString, AtomicInteger fromIndex) {
		nameID = Byte.parseByte(cloneString[fromIndex.getAndIncrement()]);
		overrideName = cloneString[fromIndex.getAndIncrement()];
		if (overrideName.isEmpty())
			overrideName = null;
		price = Float.parseFloat(cloneString[fromIndex.getAndIncrement()]);
		priceOG = Float.parseFloat(cloneString[fromIndex.getAndIncrement()]);
		priceTotal = Float.parseFloat(cloneString[fromIndex.getAndIncrement()]);
		lvl = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]);
		lvlRealHidden = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]);
		maxLVL = Float.parseFloat(cloneString[fromIndex.getAndIncrement()]);
		maxLVLRealHidden = Byte.parseByte(cloneString[fromIndex.getAndIncrement()]);
		maxLVLForced = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]);
		minLVL = Float.parseFloat(cloneString[fromIndex.getAndIncrement()]);
		placedRound = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]);
		bonusGainOverride = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]);
		bonusCostOverride = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]);
		String[] bonusLVLsStr = cloneString[fromIndex.getAndIncrement()].split(":");
		int len = bonusLVLsStr.length / 2;
		bonusLVLs = new byte[len];
		bonusesTaken = new byte[len];
		for (int i = 0; i < len; i++) {
			bonusLVLs[i] = Byte.parseByte(bonusLVLsStr[(i * 2)]);
			bonusesTaken[i] = Byte.parseByte(bonusLVLsStr[(i * 2) + 1]);
		}
		bonuses = new RegVals[len];
		for (int i = 0; i < len; i++) {
			bonuses[i] = new RegVals();
			bonuses[i].setCloneString(cloneString, fromIndex);
		}

		if (regularValues == null)
			regularValues = new RegVals();
		regularValues.setCloneString(cloneString, fromIndex);
		if (neighbourModifier == null)
			neighbourModifier = new RegVals();
		neighbourModifier.setCloneString(cloneString, fromIndex);
		upgradeType = UpgradeType.values()[Integer.parseInt(cloneString[fromIndex.getAndIncrement()])];
		if (cloneString[fromIndex.get()].equals("n")) {
			requiredUpgradeToUnlock = null;
		} else {
			requiredUpgradeToUnlock = TileNames.values()[Integer.parseInt(cloneString[fromIndex.get()])];
		}
		fromIndex.incrementAndGet();
		tileName = TileNames.values()[nameID];

		freeUpgrades.clear();
		for (String strByte : cloneString[fromIndex.getAndIncrement()].split(":")) {
			if (strByte.isBlank())
				continue;
			freeUpgrades.push(Byte.parseByte(strByte));
		}
		unlocks.clear();
		for (String strByte : cloneString[fromIndex.getAndIncrement()].split(":")) {
			if (strByte.isBlank())
				continue;
			unlocks.push(Byte.parseByte(strByte));
		}
		maxLVLChanger.clear();
		for (String strByte : cloneString[fromIndex.getAndIncrement()].split(":")) {
			if (strByte.isBlank())
				continue;
			var idChange = strByte.split(",");
			maxLVLChanger.put(Byte.parseByte(idChange[0]), Float.parseFloat(idChange[1]));
		}
		priceModifier = Float.parseFloat(cloneString[fromIndex.getAndIncrement()]);
		starterUpgrade = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]) != 0;
		visible = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]) != 0;
		gainedValues.setCloneString(cloneString, fromIndex);
	}

//	public void reset(double[] regUpgrades, GameMode gm) {
//		regularValues.setValues(regUpgrades);
//		bonusesTaken = new byte[bonusLVLs.length];
//		lvl = 0;
//	}

	public void updateMaxLVLPosFast(Layer layer, int x, int y) {
		updateMaxLVLPosOnlyNeighbors(layer, x, y);
		updateMaxLVLPos(layer, this, x, y);
	}
	
	public void updateMaxLVLPosOnlyNeighbors(Layer layer, int x, int y) {
//		var neighbours = layer.getNeighbours(x, y);
		var neighbours = TilePiece.getAllNeighboursNonClones(layer, new TilePiece<UpgradeGeneral>(this, x, y));
		for (var neighbour : neighbours) {
			if (neighbour.upgrade() instanceof Upgrade)
				updateMaxLVLPos(layer, (Upgrade) neighbour.upgrade(), neighbour.x(), neighbour.y());
		}
	}
	
	public void updateMaxLVLPos(Layer layer, Upgrade upgrade, int x, int y) {
		var collectiveLvls = 0d;
//		var neighbours = layer.getNeighbours(x, y);
		var neighbours = TilePiece.getAllNeighboursNonClones(layer, new TilePiece<UpgradeGeneral>(upgrade, x, y));
		if (neighbours.size() > 1)
			collectiveLvls++;
		for (var piece : neighbours) {
			if (piece.upgrade() instanceof Upgrade up && up != upgrade) {
				if (Math.abs(piece.x() - x) + Math.abs(piece.y() - y) > 1) {
					collectiveLvls += .25f*((Upgrade) piece.upgrade()).getLVL();
				} else {
					collectiveLvls += ((Upgrade) piece.upgrade()).getLVL();
				}
			}
		}
		collectiveLvls = Math.ceil(collectiveLvls / 2d);
		if (collectiveLvls > 64) collectiveLvls = 64;
//		if (placedRound == -1 || collectiveLvls > upgrade.getMaxLVL())
		upgrade.setMaxLVL((byte) collectiveLvls);
	}

	public double percentLimit() {
		if (isPlaced()) {
        	return Math.min(.5, .025*lvl + .10);
		}
		return .5;
	}

	public boolean upgrade(Player player, int x, int y, boolean test) {
		if (!isFullyUpgraded()) {

			if (!test) {
				lvl++;
				lvlRealHidden++;
			}

			var clonedRegVal = regularValues;
			if (x != -1) {
//				var neighbours = player.layer.getNeighbours(x, y);
				clonedRegVal = modRegValCloned(clonedRegVal, player.layer, x, y, percentLimit());
				if (!test) {
					var neighbours = TilePiece.getAllNeighboursNonClones(player.layer, new TilePiece<UpgradeGeneral>(this, x, y));
					for (var neighbour : neighbours) {
						if (neighbour.upgrade() instanceof Upgrade)
							updateMaxLVLPos(player.layer, (Upgrade) neighbour.upgrade(), 
									neighbour.x(), neighbour.y()
//									!isPlaced()
//									false
									);
					}
					updateMaxLVLPos(player.layer, this, x, y);
				}
			}
			clonedRegVal.upgrade(player, nameID, true);

			if (requiredUpgradeToUnlock == null || player.hasPlacedTileType(requiredUpgradeToUnlock)) {
				player.upgrades.unlock(unlocks);
			}
			for (var changeMaxLVL : this.maxLVLChanger.entrySet()) {
				player.upgrades.changeMaxLVL(changeMaxLVL.getKey(), changeMaxLVL.getValue());
			}

			return true;
		}
		return false;
	}

	public static RegVals modRegValCloned(RegVals regularValues, Layer layer, int x, int y, double limit) {
		if (layer != null) {
			regularValues = regularValues.clone();
			// G� igjennom alle naboer og s� ta heltall f�rst og s� prosenter som ogs� skal
			// p�virke neighbour bonuser som ikke er f�rst.
			// Liksom sorter og s� kj�r endringer.
			var neighbourRegVals = TilePiece.collectNeighbourModifiersWithTools(layer, x, y, true);

			regularValues.combineNeighboursToThis(neighbourRegVals);
			if (layer.getTimesMod(x, y) > 0)
				regularValues.multiplyMainValues(layer.getTimesMod(x, y));

			for (int i = 0; i < Rep.nosBottles; i++) {
				regularValues.limit(i, limit);
			}
		}

		return regularValues;
	}

	public static float getCost(float price, float lvl, float priceModifier, float sale) {
		return Math.round(price * (lvl + 1f) * priceModifier * sale);
	}

	public int getCost(float sale) {
		return (int) getCost(price, lvlRealHidden, priceModifier, sale);
	}

	@Override
	public void setPremadePrice(float price) {
		this.price = price;
		priceOG = Math.round(price * priceModifier);
	}

	@Override
	public float getPremadePrice() {
		return price;
	}

	public void addToPriceTotal(float price) {
		priceTotal += price;
	}

	public float getPriceOG() {
		return priceOG;
	}

	public float getPriceTotal() {
		return priceTotal;
	}

	public int getSellPrice(int round) {
		final float percentKeep = priceTotal < 0 ? 1 : sellDivision * (round == placedRound ? .5f : 1f);
		return (int) (priceTotal * percentKeep);
	}

	public void sell(Bank bank, Rep rep, Upgrades upgrades, int round) {
		bank.add(getSellPrice(round), Bank.MONEY);
		int len = Math.min(gainedValues.values.length, rep.getValues().length);
		for (int i = 0; i < len; i++) {
			var rmVal = gainedValues.values[i];
			var factor = i == Rep.kg ? .5 : .95;
			if (i < Rep.nosBottles && rmVal > rep.get(i) * factor)
				rmVal = rep.get(i) * factor;
			rep.add(i, -rmVal);
		}
		
		if (upgrades.getUpgrade(tileName).first() instanceof Upgrade up) {
			for (var changeMaxLVL : up.maxLVLChanger.entrySet()) {
				upgrades.changeMaxLVL(changeMaxLVL.getKey(), -changeMaxLVL.getValue());
			}
		}
	}

	public byte getNameID() {
		return nameID;
	}

	@Override
	public TileNames getTileName() {
		return tileName;
	}

	public void setNameID(byte nameID) {
		this.nameID = nameID;
	}

	public RegVals getRegVals() {
		return regularValues;
	}

	public void setRegularValues(RegVals regularValues) {
		this.regularValues = regularValues;
	}

	public int getBonusLVL() {
		for (int i = 0; i < bonusLVLs.length; i++) {
			if (lvl < bonusLVLs[i]) {
				return i;
			}
		}
		return bonusLVLs.length;
	}

	public boolean hasBonusReady(boolean checkForMoreBonusesAfterABonus) {
		int bonusLVL = getBonusLVL();
		return bonusLVL < bonusLVLs.length && bonusLVLs[bonusLVL] <= lvl + (checkForMoreBonusesAfterABonus ? 0 : 1)
				&& bonusesTaken[bonusLVL] == 0;
	}

	public byte[] getBonusLVLs() {
		return bonusLVLs;
	}

	public boolean compareId(int possibleId) {
		return nameID == possibleId;
	}

	public void setBonusChoice(int bonusLVL, int val) {
		if (bonusLVL < bonusesTaken.length)
			bonusesTaken[bonusLVL] = (byte) val;
	}

	public RegVals getNeighbourModifier() {
		return neighbourModifier;
	}

	public UpgradeType getUpgradeType() {
		return upgradeType;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Upgrade other = (Upgrade) obj;
		if (!Arrays.equals(bonusLVLs, other.bonusLVLs))
			return false;
		if (!Arrays.equals(bonuses, other.bonuses))
			return false;
		if (!Arrays.equals(bonusesTaken, other.bonusesTaken))
			return false;
		if (!freeUpgrades.equals(other.freeUpgrades))
			return false;
		if (tileName != other.tileName)
			return false;
		if (lvl != other.lvl)
			return false;
		if (maxLVLRealHidden != other.maxLVLRealHidden)
			return false;
		if (nameID != other.nameID)
			return false;
		if (neighbourModifier == null) {
			if (other.neighbourModifier != null)
				return false;
		} else if (!neighbourModifier.equals(other.neighbourModifier))
			return false;
		if (price != other.price)
			return false;
		if (priceModifier != other.priceModifier)
			return false;
		if (regularValues == null) {
			if (other.regularValues != null)
				return false;
		} else if (!regularValues.equals(other.regularValues))
			return false;
		if (starterUpgrade != other.starterUpgrade)
			return false;
		if (!unlocks.equals(other.unlocks))
			return false;
		return upgradeType == other.upgradeType;
	}

	public void requireUpgradeToUnlock(TileNames tileName) {
		requiredUpgradeToUnlock = tileName;
	}
	public void pushUpgradeUnlock(TileNames id) {
		unlocks.push((byte) id.ordinal());
	}

	public void pushUpgradeMaxLVLChange(TileNames id, float change) {
		maxLVLChanger.put((byte) id.ordinal(), change);
	}

	public void setUpgradeType(UpgradeType upgradeType) {
		this.upgradeType = upgradeType;
	}

	public RegVals pushBonus(int lvl) {
		var vals = new RegVals();
		byte[] bonusLVLs = new byte[this.bonusLVLs.length + 1];
		bonusesTaken = new byte[bonusLVLs.length];
		RegVals[] bonuses = new RegVals[bonusLVLs.length];

		for (int i = 0; i < this.bonusLVLs.length; i++) {
			bonusLVLs[i] = this.bonusLVLs[i];
			bonuses[i] = this.bonuses[i];
		}
		bonusLVLs[this.bonusLVLs.length] = (byte) lvl;
		bonuses[this.bonusLVLs.length] = vals;

		this.bonusLVLs = bonusLVLs;
		this.bonuses = bonuses;

		return vals;
	}

	public RegVals[] getBonuses() {
		return bonuses;
	}

	public String[][] getBonusTexts(Upgrades upgrades) {
		String[][] bonusTexts = new String[bonuses.length][];

		for (int i = 0; i < bonusTexts.length; i++) {
			bonusTexts[i] = new String[bonuses[i].getAmountChoices()];
			for (int choice = 0; choice < bonusTexts[i].length; choice++) {
				bonusTexts[i][choice] = bonuses[i].getUpgradeRepString(upgrades, choice, 0);
			}
		}

		return bonusTexts;
	}

	public void addFreeUpgradeStat(byte value) {
		freeUpgrades.push(value);
	}

	public byte[] popFreeUpgradeStats() {
		byte[] frees = new byte[freeUpgrades.size()];
		int i = 0;

		while (!freeUpgrades.isEmpty()) {
			frees[i] = freeUpgrades.pop();
			i++;
		}

		return frees;
	}

	public Stack<Byte> getUnlocks() {
		return unlocks;
	}

	public boolean unlocks(UpgradeGeneral upgrade) {
		if (upgrade instanceof Upgrade)
			return unlocks.contains(upgrade.getNameID());
		return false;
	}

	public int getLVL() {
		return lvl;
	}

	public void setLVL(int lvl) {
		this.lvl = lvl;
	}

	public void setLvlRealHidden(int lvl) {
		lvlRealHidden = lvl;
	}

	public int getLvlRealHidden() {
		return lvlRealHidden;
	}

	public int getMaxLVL() {
		return maxLVLForced == -1 
				? (maxLVL < minLVL ? (int) Math.round(minLVL) : (int) Math.round(maxLVL)) 
				: maxLVLForced;
	}

	public void setMaxLVL(int lvl) {
		this.maxLVLRealHidden = lvl;
		this.maxLVL = lvl;
	}

	public void changeMaxLVL(float change) {
		var changed = maxLVL + change;
		if (changed > maxLVLRealHidden) {
			changed = maxLVLRealHidden;
		} else if (changed < 0) {
			changed = 0;
		}
		maxLVL = changed;
	}
	
	public void setForceMaxLVL(int i) {
		this.maxLVLForced = i;
	}
	
	public void setMinLVL(int i) {
		this.minLVL = i;
	}

	public boolean isFullyUpgraded() {
		return lvl >= getMaxLVL() && maxLVLRealHidden != -1;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
		newlyUnlocked = visible;
	}

	public boolean isVisible() {
		return visible;
	}

	public boolean isNewlyUnlocked() {
		return newlyUnlocked;
	}

	@Override
	public boolean isOpenForUse() {
		return visible && !isFullyUpgraded();
	}

	public IUIObject[] getInfoBonuses(Upgrades upgrades) {

		StringBuilder text = new StringBuilder();
		ArrayList<IUIObject> res = new ArrayList<>();
		if (bonuses != null) {
			for (int bonusLVL = 0; bonusLVL < bonuses.length; bonusLVL++) {
				String color = "#STRONGER_BONUSGOLD" + bonusLVL;
				text.append("    Bonus from having ").append(bonusLVLs[bonusLVL]).append(" tiles:").append(color)
						.append("\n");
				String[] bonusTexts = getBonusTexts(upgrades)[bonusLVL];
				for (int goldTypeIndex = 0; goldTypeIndex < bonusTexts.length; goldTypeIndex++) {
					text.append(
							Lobby.newlineText(generateBonusChoiceLine(bonusTexts, goldTypeIndex, bonusLVL)
									, 42)
									);
				}
				text.append("\n");
			}

			Collections.addAll(res, UILabel.split(text.toString(), "\n"));
		}
		return res.toArray(new IUIObject[0]);
	}

	private String generateBonusChoiceLine(String[] bonusTexts, int goldTypeIndex, int bonusLVL) {
		int goldCost = UIBonusModal.calcBonusCost(this, goldTypeIndex);
		return "$" + goldCost + ": " + bonusTexts[goldTypeIndex] +
				(bonusesTaken[bonusLVL] == 0 ? "" : (bonusesTaken[bonusLVL] == (1+goldTypeIndex) ? "#G" : "#R")) + 
				"\n";
	}

	public static void addGainedValuesDifference(RegValList gainedValues, double[] oldVals, double[] newVals) {
		double[] oldGainedValues = gainedValues.values;
		gainedValues.values = new double[oldVals.length];
		for (int i = 0; i < gainedValues.values.length; i++) {
			if (oldGainedValues != null && oldGainedValues.length > i) {
//				System.out.println("Setting " + oldGainedValues[i] + " to " + Texts.tags[i]);
				gainedValues.values[i] = oldGainedValues[i];
			}
			var newVal = newVals[i];
			if (Double.isInfinite(newVal)) {
				newVal = Math.signum(newVal) * Double.MAX_VALUE;
			}
			var newValBig = new BigDecimal(newVal);
			
			var oldVal = oldVals[i];
			if (Double.isInfinite(oldVal)) {
				oldVal = Math.signum(oldVal) * Double.MAX_VALUE;
			}
			var oldValBig = new BigDecimal(oldVal);
			var diffBig = newValBig.subtract(oldValBig);
			var diff = diffBig.doubleValue();
//			var diff = newVal - oldVal;
			if (diff != 0) {
//				System.out.println("Adding " + diff + " += " + gainedValues.values[i] + " to " + Texts.tags[i]);
				
				if (Double.isInfinite(diff))
					System.out.println("Adding " + diff + " += " + gainedValues.values[i] + " to " + Texts.tags[i]);
				
				gainedValues.values[i] += diff;
			}
		}
	}

	public void addGainedValuesDifference(Player oldPlayer, Player newPlayer) {
		addGainedValuesDifference(gainedValues, oldPlayer.getCarRep().getValues(), newPlayer.getCarRep().getValues());
	}

//	public void getInfoAffectedBy(Upgrades upgrades, StringBuilder text, int upgradeId) {
//		var bonuses = getBonuses();
//		if (bonuses == null)
//			return;
//		boolean addedUpgradeName = false;
//		for (int bonusIndex = 0; bonusIndex < bonuses.length; bonusIndex++) {
//			boolean addedBonusName = false;
//			for (int choice = 0; choice < bonuses[bonusIndex].modifyUpgradeIds.length; choice++) {
//				if (bonuses[bonusIndex].isAffectingId(choice, upgradeId)) {
//					if (!addedUpgradeName) {
//						text.append("\"").append(Texts.getUpgradeTitle(this)).append("\"#").append(UIColors.AI).append("\n");
//						addedUpgradeName = true;
//					}
//					if (!addedBonusName) {
//						text.append("    BONUS from having ").append(getBonusLVLs()[bonusIndex]).append(" tiles:").append("\n");
//						addedBonusName = true;
//					}
//
//					int goldCost = UIBonusModal.calcBonusCost(choice);
//					text.append("$").append(goldCost).append(":  ").append(bonuses[bonusIndex].getUpgradeRepString(upgrades, choice, 0)).append("\n");
//
//				}
//			}
//
//			if (addedBonusName) {
//				text.append("\n");
//			}
//		}
//	}

	public RegValList getGainedValues() {
		return gainedValues;
	}

}

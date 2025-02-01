package player_local.upgrades;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import adt.ICloneStringable;
import communication.Translator;
import engine.graphics.ui.UIColors;
import main.Texts;
import player_local.Player;
import player_local.car.Rep;
import player_local.upgrades.RegVal.RegValType;

/**
 * // if (i == Rep.sale) { // int toID = idOfSale(value); // double as =
 * Math.abs((value - (double) toID) * 100.0); // int realSale = (int)
 * Math.round(100d * (as - 1.0d)); // res.append((realSale > 0 ? "+" : "-") +
 * Math.abs(realSale) + "% sale on "); // if (toID == -1) { // alle //
 * res.append("everything"); // } else { //
 * res.append(Texts.getUpgradeTitle(toID)); // } // continue; // } else if (i ==
 * Rep.freeUpgrade) { // int absVal = Math.abs((int) value); // if (value < 0) {
 * // negative means random amount of upgrades // res.append(absVal + " free
 * random upgrade" + (absVal != 1 ? "s" : "")); // } else { // otherwise a
 * spesific nameid // res.append("Free " + Texts.getUpgradeTitle(absVal - 10) +
 * " upgrade"); // } // continue; // } else
 * <p>
 * <p>
 * <p>
 * <p>
 * // else if (i == Rep.sale) { //// int toID =
 * Integer.parseInt(String.valueOf(value).split("\\.")[0]); //// double as =
 * Math.abs((value - (double) toID) * 100.0); //// float realSale = (float)
 * (Math.round(as * 100f) / 100f); //// if (toID == -1) { // alle //// for (int
 * id = 0; id < Upgrades.size; id++) //// player.bank.addSale(realSale, id,
 * fromID); //// } else { //// player.bank.addSale(realSale, toID, fromID); ////
 * } // continue; // } else if (i == Rep.freeUpgrade) { // Upgrade[] upgrades =
 * player.upgrades.getUpgradesAsUpgrade(); // if ((int) value < 0) { // //
 * StringBuilder sb = new StringBuilder("You got: "); // // for (int amount = 0;
 * amount < (int) -value; amount++) { // byte id = -1; // do { // id = (byte)
 * Features.ran.nextInt(upgrades.length); // } while (id == fromID ||
 * upgrades[id].isFullyUpgraded()); //
 * player.upgrades.setLastFocusedUpgrade(id); // upgrades[id].upgrade(player,
 * -1, -1, false); // upgrades[fromID].addFreeUpgradeStat(id); // // if (amount
 * > 0) { // sb.append(", "); // } // sb.append(Texts.getUpgradeTitle(id)); // }
 * // //// FIXME SceneHandler.showMessage(sb.toString()); //
 * System.out.println("fromID: " + fromID + " free upgrade: " + sb.toString());
 * // } else { // byte id = (byte) (value - 10); // upgrades[id].upgrade(player,
 * -1, -1, false); // upgrades[fromID].addFreeUpgradeStat(id); //
 * player.upgrades.setLastFocusedUpgrade(id); // } // continue; // }
 *
 * @author Jens Benz
 */

public class RegVals implements ICloneStringable {

	private RegVal[][] valuesRefs; // Se i Rep.java for hva hver index tilsier.
	public byte[] hooks;
	public RegVal[] changeAfterUpgrade; // Se i Rep.java for hva hver index tilsier.
	private boolean[] differentNewOnes; // vise verdier som er p�virket av nabotilene i hvilke som er faste.

	public RegVal[] values() {
		return valuesRefs[0];
	}

	public RegVals(RegVal[] values) {
		valuesRefs = new RegVal[0][];
		setValues(values);
	}

	public RegVals() {
		this(new RegVal[0]);
	}

	public RegVal[] addChoiceList() {
		return addChoiceList(-1);
	}

	public RegVal[] addChoiceList(int hook) {
		int oldLen = this.valuesRefs.length;
		var valuesRefs = new RegVal[oldLen + 1][];
		differentNewOnes = new boolean[Rep.size()];
		var hooks = new byte[oldLen];

		for (int i = 0; i < oldLen; i++) {
			valuesRefs[i] = this.valuesRefs[i];
			if (i < this.hooks.length)
				hooks[i] = this.hooks[i];
		}

		valuesRefs[oldLen] = new RegVal[Rep.size()];
		if (oldLen > 0)
			hooks[oldLen - 1] = (byte) hook;

		this.valuesRefs = valuesRefs;
		this.hooks = hooks;

		return valuesRefs[oldLen];
	}

	public void setValues(RegVal[] values) {
		addChoiceList();
		changeAfterUpgrade = new RegVal[Rep.size()];
		System.arraycopy(values, 0, valuesRefs[0], 0, values.length);
	}

	public RegVals clone() {
		var clone = new RegVals();
		Translator.setCloneString(clone, this);
		return clone;
	}

	private void getArrCloneString(RegVal[] values, StringBuilder outString, String splitter) {
		outString.append(values.length);
		int skip = 0;
		for (var val : values) {
			if (val == null) {
				skip++;
			} else {
				if (skip > 0)
					outString.append(splitter).append("s").append(skip);
				skip = 0;
				outString.append(splitter);
				val.getCloneString(outString, 0, splitter, false);
			}
		}

		if (skip > 0)
			outString.append(splitter).append("e");
	}

	private void getArrCloneString(RegVal[][] values, StringBuilder outString, String splitter) {
		outString.append(values.length);
		for (RegVal[] value : values) {
			outString.append(splitter);
			getArrCloneString(value, outString, splitter);
		}
	}

	private void getArrCloneString(byte[] values, StringBuilder outString, String splitter) {
		outString.append(values.length);
		int skip = 0;
		for (var val : values) {
			if (val == 0.0) {
				skip++;
			} else {
				if (skip > 0)
					outString.append(splitter).append("s").append(skip);
				skip = 0;
				outString.append(splitter).append(val);
			}
		}

		if (skip > 0)
			outString.append(splitter).append("e");
	}

	@Override
	public void getCloneString(StringBuilder outString, int lvlDeep, String splitter, boolean test) {
		if (lvlDeep > 0)
			outString.append(splitter);

		getArrCloneString(valuesRefs, outString, splitter);
		outString.append(splitter);
		getArrCloneString(changeAfterUpgrade, outString, splitter);
		outString.append(splitter);
		getArrCloneString(hooks, outString, splitter);
	}

	private void setArrCloneString(RegVal[] values, String[] cloneString, AtomicInteger fromIndex) {
		for (int i = 0; i < values.length; i++) {
			if (cloneString[fromIndex.get()].startsWith("s")) {
				i += Integer.parseInt(cloneString[fromIndex.getAndIncrement()].substring(1)) - 1;
			} else if (cloneString[fromIndex.get()].startsWith("e")) {
				fromIndex.getAndIncrement();
				return;
			} else {
				values[i] = new RegVal(0, RegValType.Decimal);
//				Translator.setCloneString(values[i], cloneString[fromIndex.getAndIncrement()]);
				values[i].setCloneString(cloneString, fromIndex);
			}
		}
	}

	private void setArrCloneString(byte[] values, String[] cloneString, AtomicInteger fromIndex) {
		for (int i = 0; i < values.length; i++) {
			if (cloneString[fromIndex.get()].startsWith("s")) {
				i += Integer.parseInt(cloneString[fromIndex.getAndIncrement()].substring(1)) - 1;
			} else if (cloneString[fromIndex.get()].startsWith("e")) {
				fromIndex.getAndIncrement();
				return;
			} else {
				values[i] = Byte.parseByte(cloneString[fromIndex.getAndIncrement()]);
			}
		}
	}

	@Override
	public void setCloneString(String[] cloneString, AtomicInteger fromIndex) {
		this.valuesRefs = new RegVal[Integer.parseInt(cloneString[fromIndex.getAndIncrement()])][];
		for (int i = 0; i < valuesRefs.length; i++) {
			valuesRefs[i] = new RegVal[Integer.parseInt(cloneString[fromIndex.getAndIncrement()])];
			setArrCloneString(valuesRefs[i], cloneString, fromIndex);
		}

		changeAfterUpgrade = new RegVal[Integer.parseInt(cloneString[fromIndex.getAndIncrement()])];
		setArrCloneString(changeAfterUpgrade, cloneString, fromIndex);
		hooks = new byte[Integer.parseInt(cloneString[fromIndex.getAndIncrement()])];
		setArrCloneString(hooks, cloneString, fromIndex);
	}

	public int getAmountChoices() {
		return valuesRefs.length;
	}

	public ArrayList<String> getUpgradeRepString(Upgrades upgrades, boolean darkerBackground) {
		String line1 = getUpgradeRepString(upgrades, 0, 1);
		String line2 = getUpgradeRepString(upgrades, 0, 2);

		ArrayList<String> lines = new ArrayList<>();

		final var changeIndicateColor = "#" + (darkerBackground ? UIColors.PAOLO_VERONESE_GREEN : UIColors.WON);
		if (line1.length() != 0)
			lines.add(line1 + changeIndicateColor);
		else
			lines.add("--------" + changeIndicateColor);

		if (line2.length() != 0)
			lines.add("<= " + line2 + changeIndicateColor);

		return lines;
	}

	public String getUpgradeRepString(Upgrades upgrades, int choice, int showType) {
		var values = valuesRefs[choice];
		return getUpgradeRepString(upgrades, values, choice, showType);
	}

	/**
	 * showType 1 means only show existing values from before combination showType 2
	 * means only show new values after combination
	 */
	public String getUpgradeRepString(Upgrades upgrades, RegVal[] values, int choice, int showType) {
		var res = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			if (values[i] == null || values[i].value == 0)
				continue;

			if (showType == 1 && differentNewOnes[i] || showType == 2 && !differentNewOnes[i])
				continue;

			if (res.length() > 0)
				res.append(", ");
			else if (choice - 1 >= 0 && choice - 1 < hooks.length && hooks[choice - 1] != -1)
				res.append("... and ");

			if (values[i].type == RegValType.Unlock) {
				switch ((int) values[i].value) {
				case 0 -> res.append("Remove");
				case 1 -> res.append("Unlock");
				case 2 -> res.append("Guarentee");
				}
			} else {
				var isPercent = values[i].isPercent();
				var rmVal = isPercent ? 1d : 0d;

				if (values[i].value < rmVal)
					res.append("-");
				else
					res.append("+");

				if (isPercent) {
					res.append(Texts.formatNumber(Math.abs((values[i].value - rmVal) * 100.0)));
					if (values[i].type == RegValType.AdditionPercent)
						res.append("^");
					res.append("%");
				} else {
					var val = values[i].value;
					if (values[i].unsigned && val < 0)
						val = 0;
					res.append(Texts.formatNumber(Math.abs(val)));
				}
			}

			res.append(" ").append(Texts.tags[i]);
			if (values[i].only)
				res.append(" only");
		}

		return res.toString();
	}

	public static boolean hasDecimals(double d) {
		return (d % 1d) != 0d;
	}

	private double roundDecimals(double value) {
		return Math.round(value * 100.0) / 100.0;
	}

	private double roundPercDecimals(double value) {
		return Math.round(value * 10000.0) / 10000.0;
	}

	public void multiplyAllValues(double d) {
		multiplyAllValues(this.valuesRefs[0], d, true);
	}

	public void multiplyAllValues(RegVal[] values, double d, boolean includeDifferentOnes) {
		for (int i = 0; i < values.length; i++) {
			if (values[i] == null || values[i].value == 0 || values[i].removeAtPlacement || i >= Rep.nosBottles
					|| (!includeDifferentOnes && differentNewOnes[i]))
				continue;
			double value = values[i].value;

			if (values[i].isPercent())
				value = ((value - 1d) * d) + 1d;
			else
				value = roundDecimals(value * d);

			values[i].value = value;
		}
	}

	public void multiplyMainValues(double d) {
		multiplyAllValues(valuesRefs[0], d, false);
	}

	private void upgrade(Player player, RegVal[] values, byte fromID, boolean realAdditionalPercent) {
		Rep rep = player.getCarRep();
		for (int i = 0; i < values.length; i++) {
			if (values[i] == null || values[i].value == 0)
				continue;

			if (values[i].type == RegValType.Unlock) {
				rep.set(i, values[i].value);
				continue;
			}

			if (values[i].isPercent()) {
				if (realAdditionalPercent && values[i].type == RegValType.AdditionPercent && (values[i].ignoreDifferentNewOne || !differentNewOnes[i]))
					rep.set(i, rep.get(i) + roundPercDecimals(values[i].value - 1d));
				else
					rep.set(i, rep.get(i) * roundPercDecimals(values[i].value));
			} else {
				rep.set(i, rep.get(i) + roundDecimals(values[i].value));
			}

			if (values[i].unsigned && rep.get(i) < 0)
				rep.set(i, 0);
		}
	}

	public void upgradeWithHooks(Player player, int i, byte fromID) {
		int hookIndex = i - 1;
		if (hookIndex >= 0 && hookIndex < hooks.length && hooks[hookIndex] >= 0)
			upgradeWithHooks(player, hooks[hookIndex], fromID);

		upgrade(player, valuesRefs[i], fromID, false);
	}

	public void upgrade(Player player, byte fromID, boolean realAdditionalPercent) {
		upgrade(player, valuesRefs[0], fromID, realAdditionalPercent);
	}

//	public void upgrade(Rep rep) {
//		for (int i = 0; i < values.length; i++) {
//			if (values[i] == 0) continue;
//			double value = values[i], valueHere = rep.get(i);
//			double rmThere = toRemove(value);
//			if (isPercent(value)) {
//				rep.set(i, valueHere * roundDecimals(value - (rmThere != 1 ? rmThere : 0)));
//			} else {
//				if (isDecimal(value))
//					value = roundDecimals(value - rmThere);
//				rep.set(i, valueHere + value);
//			}
//			if (rmHere == 0 && rmThere != decimals) {
//				values[i] = (int) values[i];
//			}
//		}
//	}

	public boolean isValuesSame(RegVal[] boostRegUpgrades) {
		int len = boostRegUpgrades.length < valuesRefs[0].length ? boostRegUpgrades.length : valuesRefs[0].length;

		for (int i = 0; i < len; i++) {
			if (valuesRefs[0][i] != boostRegUpgrades[i])
				return false;
		}
		return true;
	}

	public void combineSet(RegVal[] v2) {
		for (int i = 0; i < v2.length; i++) {
			if (v2[i] == null || v2[i].value == 0)
				continue;
			valuesRefs[0][i] = v2[i];
		}
	}

	public void combineChange() {
		combine(valuesRefs[0], changeAfterUpgrade);
	}

	public void combineNeighboursToThis(ArrayList<RegVals> neighbours) {
		// First regular numbers, then regular percents, then rest.

		for (var neighbour : neighbours)
			combine(valuesRefs[0], neighbour.values(), true, true, false, false);
		for (var neighbour : neighbours)
			combine(valuesRefs[0], neighbour.values(), true, false, true, false);
		for (var neighbour : neighbours)
			combine(valuesRefs[0], neighbour.values(), true, false, false, true);
	}

	public void combine(RegVal[] v2) {
		combine(valuesRefs[0], v2);
	}

	private void combine(RegVal[] v1, RegVal[] v2) {
		combine(v1, v2, false, true, true, true);
	}

	private void combine(RegVal[] v1, RegVal[] v2, boolean differentiateNewOnes, boolean regularNumbers,
			boolean regularPercents, boolean rest) {
		for (int i = 0; i < v2.length; i++) {
			if (v2[i] == null || v2[i].value == 0)
				continue;

			// new value
			if (v1[i] == null || v1[i].value == 0) {

				if (!v2[i].only) {
					if (v2[i].isPercent() && !regularPercents)
						continue;
					if (v2[i].type == RegValType.Decimal && !regularNumbers)
						continue;
					v1[i] = v2[i];
					if (differentiateNewOnes) {
						differentNewOnes[i] = true;
					}
				}
				continue;
			}

			switch (v2[i].type) {
			case Decimal:
				if (!regularNumbers)
					break;
				if (v1[i].isPercent())
					break;
				v1[i].value += v2[i].value;
				break;
			case AdditionPercent:
				if (!regularPercents || (v2[i].only && rest))
					break;
				if (v1[i].isPercent()) {
					if (v1[i].value < 1d && v2[i].replaceNeg)
						v1[i].value = (v2[i].value - 1d);
					else
						v1[i].value += (v2[i].value - 1d);
					break;
				}
			case NormalPercent:
				if (!regularPercents || (v2[i].only && rest))
					break;
				if (v1[i].isPercent()) {
					if (v2[i].value < 0)
						v1[i].value = -((v1[i].value - 1d) * 1-v2[i].value) + 1d;
					else
						v1[i].value = ((v1[i].value - 1d) * v2[i].value) + 1d;
				} else {
					v1[i].value *= v2[i].value;
				}
				break;
			case Unlock:
				if (!rest)
					break;
				if (v1[i] == null)
					v1[i] = new RegVal(0, null);
				Translator.setCloneString(v1[i], v2[i]);
				break;
			}
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		var other = (RegVals) obj;
		if (!Arrays.equals(changeAfterUpgrade, other.changeAfterUpgrade))
			return false;
		if (!Arrays.equals(hooks, other.hooks))
			return false;
		if (!Arrays.deepEquals(valuesRefs, other.valuesRefs))
			return false;
		return true;
	}

	public void setValuesRefs(RegVal[][] valuesRefs) {
		this.valuesRefs = valuesRefs;
	}

	public void setHooks(byte[] hooks) {
		this.hooks = hooks;
	}

	public String getMainMarkers(Upgrades upgrades) {

//		return getUpgradeRepString(upgrades, 0, 1);
		var sb = new StringBuilder();
		for (var val : valuesRefs[0]) {
			if (val == null || val.value == 0)
				continue;
			if (val.isPercent()) {
				sb.append("%");
			} else if (val.value > 0) {
				sb.append("+");
			} else {
				sb.append("-");
			}
		}

		return sb.toString();
	}

	public void limit(int typeRepVal, double maxPercent) {
		for (var lists : valuesRefs) {
			var val = lists[typeRepVal];

			if (val == null || val.value == 0 || !val.isPercent())
				continue;

			if (val.value < 1d) {
				if (val.value < 1d - maxPercent)
					val.value = 1d - maxPercent;
			} else if (val.value > 1d + maxPercent) {
				val.value = 1d + maxPercent;
			}
		}
	}
}

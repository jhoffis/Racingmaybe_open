package player_local;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import org.lwjgl.nuklear.Nuklear;

import adt.ICloneStringable;
import communication.Translator;
import engine.graphics.ui.IUIObject;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UILabel;
import engine.io.InputHandler;
import main.Features;
import main.Texts;
import player_local.car.Car;
import player_local.car.Rep;
import player_local.upgrades.TileNames;
import player_local.upgrades.Tool;
import player_local.upgrades.Upgrade;
import player_local.upgrades.Upgrades;
import scenes.regular.ReplayVisual;

public class Player implements ICloneStringable {
	
	// temp vals
	public int readyTime, undoTime, inTheRaceTime;
	public byte ready, finished;
	public long timeLapsedInRace, fastestTimeLapsedInRace = -1;
	public int podium, podiumRace, aheadByPoints;
	public boolean inTheRace, joined, wasHost;

	// 0 == player, 1 == spectator, 2 == hostPlayer, 3 == hostSpectator
	public static final byte UNKNOWN = -1, PLAYER = 0, HOST = 2, COMMENTATOR = 3;
	public static final byte DEFAULT_ID = (byte) -100;

	public String name;
	public long steamID;
	public byte role, id; // also used as channel
	public int gameID;
	public boolean resigned;

	public final Layer layer;
	public final Bank bank;
	public int carSelectionTime;
	public final Car car;
	public final Upgrades upgrades;

	private final ArrayList<String> history = new ArrayList<>();
	private final Stack<String> historyRedo = new Stack<>();
	private int historyIndex, rejoinedHistoryBaseIndex = 0;
	public int canUndoHistory = 1; // based on max size of historyRedo
	public boolean ultimateUndo = false;
	private boolean[] placedTypes = new boolean[TileNames.values().length];
	public static final int rightLobbyStatsInfoSize = 18;
	private static final UILabel[] rightLobbyStatsInfo = new UILabel[rightLobbyStatsInfoSize];
	
	public Player(String name, int id, int role, long steamID) {
		this.name = name;
		this.id = (byte) id;
		this.role = (byte) role;
		wasHost = isHost();
		this.car = new Car();
		layer = new Layer();
		upgrades = new Upgrades(car.getRep(), layer);
		this.bank = new Bank();
		this.steamID = steamID;

		if (rightLobbyStatsInfo[0] == null) {
			for (int i = 0; i < rightLobbyStatsInfo.length; i++) {
				rightLobbyStatsInfo[i] = new UILabel();
			}
			int i = 0;
			rightLobbyStatsInfo[i].setText("Power: #" + UIColors.POWER_RED);
			rightLobbyStatsInfo[i].tooltip = "Power means movement via horsepower from RPM";

			rightLobbyStatsInfo[++i].tooltip = "Peak potential horsepower combining base HP + peak-bar + peak-RPM";

			rightLobbyStatsInfo[++i].tooltip = "Base horsepower unrelated to bar etc.";
			i += 6; 
			rightLobbyStatsInfo[i].setText("Boost: #" + UIColors.SKY_BLUE_CRAYOLA);
			rightLobbyStatsInfo[i].tooltip = "Boost is movement except horsepower from RPM";

			rightLobbyStatsInfo[++i].tooltip = "This is how much potential strength each of your nos-bottles provide";
			i += 5;
			rightLobbyStatsInfo[i].setText("Speed: #" + UIColors.MEDIUM_SPRING_GREEN);
			rightLobbyStatsInfo[i].tooltip = "Determines your gear ratios,\nand the higher the ratio the more torque is needed!";

			rightLobbyStatsInfo[++i].tooltip = "This is your potential top speed.\nAlso your aero (which is wind-resistance-reduction) is listed here.\nThe less your aero % the less power/boost you lose at higher speed!";

			rightLobbyStatsInfo[++i].tooltip = "This is the RPM-range your car has!\nRemember, lower difference from idle to top RPM means\nless time before peak output from boost and power!";

			rightLobbyStatsInfo[++i].tooltip = "More gears makes faster but shifting is slow,\nunless your gearbox is fully upgraded!";
		}
	}

	public Player() {
		this("", DEFAULT_ID, PLAYER, DEFAULT_ID);
	}
//	public String getShortLobbyInfo() {
//		if (car != null)
//			return role + "#" + name + "#" + car.getRep().getName() + "#" + ready;
//		else
//			return "Joining...";
//	}
	
	public void sellTile(TilePiece<?> tile, int round) {
		layer.remove(tile.x(), tile.y());
		layer.reduceTimesMod(tile);
		if (tile.upgrade() instanceof Upgrade up) 
			up.updateMaxLVLPosOnlyNeighbors(layer, tile.x(), tile.y());
		tile.upgrade().sell(bank, car.getRep(), upgrades, round);
		if (upgrades.getUpgradeRef(tile.upgrade()) instanceof Upgrade upgradeRef) {
			if (upgradeRef.getMaxLVL() > 0) {
				upgradeRef.setLVL(upgradeRef.getLVL() - 1);
			}
		} else if (tile.upgrade() instanceof Tool ) {
			layer.addImprovementPoints(3);
		}
	}
	
	/*
	 * ==========================  Cloning ==========================
	 */
	public static String booleanArrayToString(boolean[] boolArray) {
		if (boolArray.length < 33) {
			throw new IllegalArgumentException("Boolean array must have at least 33 entries.");
		}

		int byteArrayLength = (boolArray.length + 7) / 8;
		byte[] byteArray = new byte[byteArrayLength];

		// Convert boolean array to byte array
		for (int i = 0; i < boolArray.length; i++) {
			int byteIndex = i / 8;
			int bitIndex = i % 8;
			if (boolArray[i]) {
				byteArray[byteIndex] |= (byte) (1 << (7 - bitIndex));
			}
		}

		// Encode the byte array as a Base64 string
		return Base64.getEncoder().encodeToString(byteArray);
	}

	public static boolean[] stringToBooleanArray(String encoded) {
		byte[] byteArray = Base64.getDecoder().decode(encoded);
		boolean[] boolArray = new boolean[byteArray.length * 8];

		// Convert byte array back to boolean array
		for (int i = 0; i < boolArray.length; i++) {
			int byteIndex = i / 8;
			int bitIndex = i % 8;
			boolArray[i] = (byteArray[byteIndex] & (1 << (7 - bitIndex))) != 0;
		}

		return boolArray;
	}

	@Override
	public void getCloneString(StringBuilder outString, int lvlDeep, String splitter, boolean test) {
		if (lvlDeep > 0)
			outString.append(splitter);
		lvlDeep++;
		outString
		.append(id).append(splitter)
		.append(gameID).append(splitter).
		append(steamID).append(splitter).
		append(name.replaceAll("#", Translator.hashtag)).append(splitter).
		append(role).append(splitter).
		append(resigned ? 1 : 0).append(splitter).
		append(podium).append(splitter).
		append(fastestTimeLapsedInRace).append(splitter).
		append(aheadByPoints).append(splitter).
		append(undoTime).append(splitter);
		var placedTypesStr = booleanArrayToString(placedTypes);
		outString.append(placedTypesStr);
		
		bank.getCloneString(outString, lvlDeep, splitter, test);
		layer.getCloneString(outString, lvlDeep, splitter, test);
		getCarRep().getCloneString(outString, lvlDeep, splitter, test);
		upgrades.getCloneString(outString, lvlDeep, splitter, test);
	}
	
	@Override
	public void setCloneString(String[] cloneString, AtomicInteger fromIndex) {
		id = Byte.parseByte(cloneString[fromIndex.getAndIncrement()]);
		gameID = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]);
		steamID = Long.parseLong(cloneString[fromIndex.getAndIncrement()]);
		name = cloneString[fromIndex.getAndIncrement()].replaceAll(Translator.hashtag, "#");
		role = Byte.parseByte(cloneString[fromIndex.getAndIncrement()]);
		resigned = Byte.parseByte(cloneString[fromIndex.getAndIncrement()]) != 0;
		podium = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]);
		fastestTimeLapsedInRace = Long.parseLong(cloneString[fromIndex.getAndIncrement()]);
		aheadByPoints = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]);
		undoTime = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]);
		var placedTypesStr = cloneString[fromIndex.getAndIncrement()];
        placedTypes = stringToBooleanArray(placedTypesStr);

		bank.setCloneString(cloneString, fromIndex);
		layer.setCloneString(cloneString, fromIndex);
		
		getCarRep().setCloneString(cloneString, fromIndex);
		upgrades.setCloneString(cloneString, fromIndex);
		upgrades.setLayer(layer);
	}
	
//	public void setClone(Bank bank, Layer layer, Rep rep) {
//		this.bank = bank;
//		this.layer = layer;
//		this.car.setRep(rep);
//	}
	
	public Player getClone() {
		return (Player) Translator.setCloneString(new Player(), this);
	}


	/*
	 * ==========================  Race stuff ==========================
	 */
	
	public void newRace() {
		finished = 0;
		timeLapsedInRace = 0;
		inTheRaceTime++;
		inTheRace = false;
	}

	/*
	 * ==========================  Infos ==========================
	 */

	/**
	 * @return name#ready#host#points
	 */
	public String getLobbyInfo() {
		if (car != null) {
			var points = bank.getLong(Bank.POINT);
			var money = bank.getLong(Bank.MONEY);
			return 
			Texts.podiumConversion(podium)
			+ " - " + name 
			+ ", " + car.getRep().getName() 
			+ ", " + points
			+ " " + pointStr(points)
			+ ", $" + money;
		} else
			return "Joining...";
	}
	
	private String pointStr(long points) {
		return "point" + (points != 1 ? "s" : "");
	}

	public IUIObject[] getPlayerWinHistoryInfo() {
		Rep rep = getCarRep();
		var points = bank.getLong(Bank.POINT);
		long addedPoints = (long) Math.floor(bank.added[Bank.POINT]);
		long achPoints = (long) Math.floor(bank.achieved[Bank.POINT]);
		var labels = UILabel.split(
				"Choices made: " + (historyIndex > 0 ? historyIndex : "none") + " / " + (history.size() - 1) +
				"\n    Round " + getCurrentHistoryRound() + ": \n" + Texts.podiumConversion(podium) + " - " + name + "\n" +
				"" + Texts.formatNumber(points) + " " + pointStr(points) + "\n" +
//				bank.getLong(Bank.BOLTS) + " " + Texts.boltsBonus(bank.getLong(Bank.BOLTS)) + "\n"
				"$" + Texts.formatNumber(bank.getLong(Bank.MONEY)) + " + " + Texts.formatNumber(rep.getInt(Rep.moneyPerTurn)) + Texts.tags[Rep.moneyPerTurn] + "\n"
				+ Texts.formatNumber(100d*rep.get(Rep.interest)) + "% " + Texts.tags[Rep.interest] + "\n"
				+ "Earned this round $" + Texts.formatNumber(Math.floor(bank.added[Bank.MONEY])) + " and " + addedPoints + " " + pointStr(addedPoints) + "\n"
				+ "Achieved overall $" + Texts.formatNumber(Math.floor(bank.achieved[Bank.MONEY])) + " and " + achPoints + " " + pointStr(achPoints) + "\n" +
				"    " + rep.getName() + ":\n" +
						Texts.formatNumber(rep.get(Rep.kW)) + " base " + Texts.tags[Rep.kW] + ", " + Texts.formatNumber(rep.getTotalKW()) + " peak " + Texts.tags[Rep.kW] + "\n" +
						Texts.formatNumber(rep.get(Rep.bar)) + " " + Texts.tags[Rep.bar] + ", " + Texts.formatNumber(rep.get(Rep.spool)) + " " + Texts.tags[Rep.spool]+ ", " + Texts.formatNumber(rep.get(Rep.spoolStart)) + " " + Texts.tags[Rep.spoolStart] + "\n" +
						Texts.formatNumber(rep.get(Rep.turboblow)) + " " + Texts.tags[Rep.turboblow] + ", " + Texts.formatNumber(rep.get(Rep.turboblowRegen)) + " regen, " + Texts.formatNumber(rep.get(Rep.turboblowStrength)) + " strength" + "\n" +
						Texts.formatNumber(rep.get(Rep.rpmIdle)) + " to " + Texts.formatNumber(rep.get(Rep.rpmTop)) + " " + Texts.tags[Rep.rpmTop] + "\n" +
		Texts.formatNumber(rep.get(Rep.kg)) + " "+ Texts.tags[Rep.kg] + " (Power score: " + Texts.formatNumber(rep.getScorePower()) + ")\n    ----\n" +
				Texts.formatNumber(rep.get(Rep.nos)) + " " + (rep.is(Rep.snos) ? "s" : "") + Texts.tags[Rep.nos] + " x" + rep.getInt(Rep.nosBottles) + ", " +Texts.formatNumber( rep.getInt(Rep.nosMs)) + "ms (Nos score: " + Texts.formatNumber(rep.getScoreNos()) + ")\n" +
				Texts.formatNumber(rep.get(Rep.tb)) + " "+ (rep.is(Rep.tbArea) ? "g" : "") +Texts.tags[Rep.tb] + ", " +Texts.formatNumber( rep.getInt(Rep.tbMs)) + "ms, " + Texts.formatNumber(rep.get(Rep.tbHeat)) + " " + Texts.tags[Rep.tbHeat] + " (Tb score: " + Texts.formatNumber(rep.getScoreTb()) + ")"
						
				+ "\n" + "    ----\n" +
						Texts.formatNumber(rep.get(Rep.spdTop)) + " "+ Texts.tags[Rep.spdTop] + " x" + rep.getInt(Rep.gearTop) + (rep.is(Rep.sequential) ? " " + Texts.tags[Rep.sequential] : "") + (rep.is(Rep.twoStep) ? " " + Texts.tags[Rep.twoStep] : "") + (rep.is(Rep.throttleShift) ? " " + Texts.tags[Rep.throttleShift] : "")
				+ "\n" + Texts.formatNumber(100d*rep.get(Rep.aero)) + "% " + Texts.tags[Rep.aero]
						+ "\n ======================================== "
						, "\n");
		labels[0].options = Nuklear.NK_TEXT_ALIGN_CENTERED; 
		labels[labels.length - 1].options = Nuklear.NK_TEXT_ALIGN_CENTERED;
		var car = new Car(); // because of car audio...
		car.setRep(this.car.getRep());
		return ReplayVisual.carInfoAICalc(labels, car);
	}

	public String[] getCarInfo() {
		if (car != null) {
			return car.getRep().getInfo();
		}
		return new String[0];
	}
	
	public String getCarInfoDiff(Player player) {
		String res = "";
		if (car != null) {
			res = car.getRep().getInfoDiff(player.getCarRep());
		}
		return res;
	}

	
	/**
	 * @return name#ready#car#...
	 */
	public String getRaceInfo(boolean allFinished, boolean endscreen, boolean spChallenge) {

		long point = endscreen ? bank.getLong(Bank.POINT) : (long) bank.added[Bank.POINT];

		if (!allFinished)
			return name.replaceAll("#", Translator.hashtag) + "#" + finished + "#" + timeLapsedInRace + "#" + car.getDistanceOnline() + "#x";
		else {
//			double boltsAdded = bank.added[Bank.BOLTS];
			long moneyAdded = (long) bank.added[Bank.MONEY];
			return name.replaceAll("#", Translator.hashtag) + "#" + finished + "#" + timeLapsedInRace + "#" +
					(endscreen ? "" : (point < 0 ? "-" : "+")) + Math.abs(point) + (spChallenge ? " attempt" : " point") + (Math.abs(point) != 1 ? "s" : "") + "#" + 
					(endscreen ? "$" + bank.getLong(Bank.MONEY) 
					+ " " : "+$" + moneyAdded 
						);
		}
	}

	public ArrayList<UILabel> getInfoWin(Rep[] otherCars) {
		ArrayList<String> info = new ArrayList<>();
//		double boltsAchieved = bank.achieved[Bank.BOLTS];
		info.add("    Achieved: " + Texts.formatNumberSimple((long) bank.achieved[Bank.POINT]) + " points, $" + Texts.formatNumberSimple((long) bank.achieved[Bank.MONEY]) + "#" + UIColors.LBEIGE);
		getCarRep().getInfoWin(info, otherCars);
		ArrayList<UILabel> res = new ArrayList<>();
		for (String str : info) {
			res.add(new UILabel(str));
		}
//		String upgrades = "Upgrades: ";
//		for(int u : upgradeLVLs) {
//			upgrades += u + ", ";
//		}
//		upgrades = upgrades.substring(0, upgrades.length() - 2);
//		res[index] = upgrades;
		
		return res;
	}

	private String diff(double d1, double d2, boolean inc, String space, String tagStr, String ends) {
		String res = Texts.formatNumber(d1) +
				space + " " +tagStr;
		if (d1 != d2) {
			var diff = d2 - d1;
			res += (diff >= 0 ? " +" : " -") + Texts.formatNumber(Math.abs(diff));
			var diffColor = (diff >= 0 && inc) || (diff < 0 && !inc) ? UIColors.G : UIColors.R;

			if (ends != null) {
				var endsColor = Texts.removeColor(ends);
				res += endsColor.second();
				if (endsColor.first() != null && endsColor.first().first() != diffColor) {
					diffColor = UIColors.ORANGE;
				}
			}
			res += "#" + diffColor;

		} else if (ends != null) {
			res += ends;
		}
		return res;
	}

	private String diff(double d1, double d2, boolean inc, String space, int tag, String ends) {
		return diff(d1, d2, inc, space, Texts.tags[tag], ends);
	}

	public UILabel[] getLobbyStatsInfo(Rep compareRep) {
		var rep = car.getRep();

//		rightLobbyStatsInfo[0].setText("Car: (" + Texts.CAR_TYPES[car.getRep().getNameID()] + ")");
		int i = 1;
		rightLobbyStatsInfo[i].setText("  " + diff(rep.getTotalKW(), compareRep.getTotalKW(), true, "", Rep.kW, null));

		rightLobbyStatsInfo[++i].setText(	"    = " + diff(rep.get(Rep.kW), compareRep.get(Rep.kW), true, "", "raw HP", null));

		rightLobbyStatsInfo[++i].setText("    + " + diff(rep.get(Rep.bar), compareRep.get(Rep.bar), true, "", Rep.bar, null));
		rightLobbyStatsInfo[i].tooltip = "This is your turbo/supercharger.\nAt peak bar; +" + Texts.formatNumber(rep.getTurboKW()) + " " + Texts.tags[Rep.kW];

		rightLobbyStatsInfo[++i].setText("    + " + diff(rep.get(Rep.rpmTop) - rep.get(Rep.rpmBaseTop), compareRep.get(Rep.rpmTop) - compareRep.get(Rep.rpmBaseTop), true, "", Rep.rpmTop, null));
		rightLobbyStatsInfo[i].tooltip = "Gives more power at higher RPM's only.\nAt peak RPM; +"  + Texts.formatNumber(rep.getRPMKW()) + " " + Texts.tags[Rep.kW];

		rightLobbyStatsInfo[++i].setText("  / " + diff(rep.get(Rep.kg), compareRep.get(Rep.kg), false, "", Rep.kg, null));
		rightLobbyStatsInfo[i].tooltip = "This is the weight of your car.\n" +
				"Less weight means more power because HP is divided by weight.\n" +
				"Windlessness is currently " + Texts.formatNumber(Car.funcs.weightWindless(rep.get(Rep.kg))) + ".\n" +
				"You have " + Texts.formatNumber(rep.getScorePower()) + " power score";

		rightLobbyStatsInfo[++i].setText("  " + diff(rep.get(Rep.spool) * 100d, compareRep.get(Rep.spool) * 100d, true, "%", Rep.spool, (InputHandler.CONTROLLER_EFFECTIVELY ? ", " + Texts.formatNumber(rep.get(Rep.spoolStart))  + " / 1 min" : null)));
		rightLobbyStatsInfo[i].tooltip = "Time from min to peak bar pressure.\nStarting point: " + Texts.formatNumber(rep.get(Rep.spoolStart))  + " / 1 " + Texts.tags[Rep.spoolStart] + "\n| where 1 is where you start at peak bar pressure";

		if (InputHandler.CONTROLLER_EFFECTIVELY) {
			rightLobbyStatsInfo[++i].setText("  " + diff(rep.get(Rep.turboblowStrength) * 100d, compareRep.get(Rep.turboblowStrength) * 100d, true, "%", "TBS", ", +" + Texts.formatNumber(rep.get(Rep.turboblowRegen)) + " pt (" + Texts.formatNumber(rep.get(Rep.turboblow)) + " TB)"));
		} else {
			rightLobbyStatsInfo[++i].setText("  " + diff(rep.get(Rep.turboblowStrength) * 100d, compareRep.get(Rep.turboblowStrength) * 100d, true, "%", Rep.turboblowStrength, null));
			rightLobbyStatsInfo[i].tooltip = "Regen is: +" + Texts.formatNumber(rep.get(Rep.turboblowRegen)) + " per turn\nCurrently you have " + Texts.formatNumber(rep.get(Rep.turboblow)) + " " + Texts.tags[Rep.turboblow];
		}
		++i;
		rightLobbyStatsInfo[++i].setText("  " + diff(rep.get(Rep.nos), compareRep.get(Rep.nos), true, "", Rep.nos, null));

		rightLobbyStatsInfo[++i].setText("    " + diff(rep.getInt(Rep.nosBottles), compareRep.getInt(Rep.nosBottles), true, "", Rep.nosBottles,
				" x " +
						diff(rep.get(Rep.nosMs) / 1000f, compareRep.get(Rep.nosMs) / 1000f, true, "", Texts.seconds, "")));
		rightLobbyStatsInfo[i].tooltip = "Each and every one of your nos-bottles last for " + Texts.formatNumber(compareRep.get(Rep.nosMs) / 1000f) + " seconds";

		rightLobbyStatsInfo[++i].setText("  " + diff(Car.funcs.maxTb(rep), Car.funcs.maxTb(compareRep), true, "", Rep.tb, null));
		rightLobbyStatsInfo[i].tooltip = "This is the potential strength of an off-the-line push you get\nbased on your reaction time.";
		rightLobbyStatsInfo[++i].setText("    " + diff(rep.get(Rep.tb), compareRep.get(Rep.tb), true, "", "raw tb", null));
		rightLobbyStatsInfo[i].tooltip = "This is the potential strength of an off-the-line push you get\nbased on your reaction time.";
		
		rightLobbyStatsInfo[++i].setText("    " + diff(rep.get(Rep.tbMs) / 1000f, compareRep.get(Rep.tbMs) / 1000f, true, "", Texts.seconds, 
				(rep.getInt(Rep.tbArea) == -1 ? "(G)" : "") + ", " + 
				diff(rep.get(Rep.tbHeat), compareRep.get(Rep.tbHeat), true, "", Rep.tbHeat, "")));
		
		
		rightLobbyStatsInfo[i].tooltip = "This is how long your off-the-line push can last.\nYour heat generates at between +" + Texts.formatNumber(100d*Car.funcs.tbHeat(rep.get(Rep.tbHeat), 0)) + "% and +"  + Texts.formatNumber(100d*Car.funcs.tbHeat(rep.get(Rep.tbHeat), 1)) + "% more tb."
				+ "\nYour current tireboost heat is " + Texts.formatNumber(rep.get(Rep.tbHeat)) + "."
				                        + (rep.getInt(Rep.tbArea) == -1 ? "\nYour tireboost is guarenteed to be at 100%!" : "");
		++i;
		rightLobbyStatsInfo[++i].setText("  " + diff(rep.get(Rep.spdTop), compareRep.get(Rep.spdTop), true, "", Rep.spdTop,
				", " +
				diff(rep.get(Rep.aero) * 100f, compareRep.get(Rep.aero) * 100f, false, "%", Rep.aero, ""))
				);

		rightLobbyStatsInfo[++i].setText("  " + Texts.formatNumber(rep.get(Rep.rpmIdle)) + " to " + Texts.formatNumber(rep.get(Rep.rpmTop)) + " " + Texts.tags[Rep.rpmTop]);

		rightLobbyStatsInfo[++i].setText("  " + diff(rep.get(Rep.gearTop), compareRep.get(Rep.gearTop), true, "", Rep.gearTop, null));

		return rightLobbyStatsInfo;
	}

	/*
	 * ==========================  History ==========================
	 */
	
	public static HistoryClean getCleanedHistory(String history) {
		int round = -1;
		if (history != null) {
			var split = history.split(Translator.split, 2);
			round = Integer.parseInt(split[0].replaceFirst("r", ""));
			history = split[1];
		}
		return new HistoryClean(round, history);
	}
	
	private String getHistory(int index) {
		return Player.getCleanedHistory(history.get(index)).cloneString();
	}

	public String getCurrentHistoryRound() {
		if (history.size() == 0)
			return "?";
		var str = history.get(historyIndex);
		if (str != null)
			str = str.split(Translator.split, 2)[0].substring(1);
		else
			str = "?";
		return str;
	}

	public HistoryState addHistory(String cloneString, boolean replaceLast, int round) {
		if (replaceLast && history.size() > 0) {
			history.remove(history.size() - 1);
		}
		cloneString = addHistory(cloneString, round);
		return new HistoryState(historyIndex, cloneString);
	}
	
	public String addHistory(String cloneString, int round) {
		cloneString = "r" + round + Translator.split + cloneString;
		addHistoryClean(cloneString);
		historyRedo.clear();
		return cloneString;
	}
	public void addHistoryClean(String cloneString) {
		history.add(cloneString);
		historyIndex = history.size() - 1;
	}

	public void addHistory(int historyIndex, String cloneString) {
	
		// Convert back to a string so you can clone it
		historyIndex = rejoinedHistoryBaseIndex + historyIndex;

		Features.fillNullListSize(history, historyIndex);
		history.set(historyIndex, cloneString);
		this.historyIndex = history.size() - 1;
		Translator.setCloneString(this, getHistory(this.historyIndex));
	}
	
	public void rejoinResetHistoryIndex() {
		rejoinedHistoryBaseIndex = history.size();
	}

	public int getRejoinedHistoryBaseIndex() {
		return rejoinedHistoryBaseIndex;
	}

	public boolean historyForward() {
		if (isHistoryNow())
			return false;
		var str = getHistory(++historyIndex);
		if (str != null)
			Translator.setCloneString(this, str);
		return true;
	}

	public boolean historyBack() {
		if (historyIndex <= 0)
			return false;
		var str = getHistory(--historyIndex);
		if (str != null)
			Translator.setCloneString(this, str);
		return true;
	}

	public String getHistoryBack() {
		return getHistory(historyIndex);
	}

	public boolean historyBackHome() {
		if (historyIndex == 0)
			return false;
		historyIndex = 0;
		Translator.setCloneString(this, getHistory(historyIndex));
		return true;
	}
	
	public boolean setHistoryNow() {
		if (isHistoryNow()) return false;
		
		historyIndex = history.size() - 1;
		Translator.setCloneString(this, getHistory(historyIndex));
		return true;
	}
	
	public boolean historyForwardRound() {
		if (isHistoryNow())
			return false;
		var current = Player.getCleanedHistory(history.get(historyIndex));
		for (int i = historyIndex; i < history.size(); i++) {
			if (i != history.size() - 1) {
				var hist = Player.getCleanedHistory(history.get(i + 1));
				if (current.round() + 1 >= hist.round())
					continue;
			}
			var hist = Player.getCleanedHistory(history.get(i));
			historyIndex = i;
			Translator.setCloneString(this, hist.cloneString());
			return true;
		}
		return false;
	}

	public boolean historyBackRound() {
		if (historyIndex == 0) 
			return false;
		var current = Player.getCleanedHistory(history.get(historyIndex));
		if (current.round() == 0)
			return false;
		
		for (int i = historyIndex - 1; i >= 0; i--) {
			var hist = Player.getCleanedHistory(history.get(i));
			if (current.round() == hist.round() && i != 0)
				continue;
			historyIndex = i;
			if (hist.cloneString() == null)
				continue;
			Translator.setCloneString(this, hist.cloneString());
			return true;
		}
		
		return false;
	}


	public boolean isHistoryNow() {
		return historyIndex >= history.size() - 1;
	}
	
	public void resetHistory(int round) {
		historyIndex = 0;
		history.clear();
		addHistory(Translator.getCloneString(this), round);
	}

	public void replaceLastHistory(int round) {
		if (!history.isEmpty())
			history.remove(history.size() - 1);
		addHistory(Translator.getCloneString(this), round);
	}

	/**
	 * @return -1 or newLatestIndex
	 */
	public int undoLastHistory(int round) {
		if (history.size() <= 1)
			return -1; // must always have 1 history left or you dont exist?
		var newLatestIndex = history.size() - 2;
		
		var prevHistory = history.get(newLatestIndex);
		var cleaned = Player.getCleanedHistory(prevHistory);
		if (cleaned.cloneString() == null)
			return -1;
		if (!ultimateUndo && round != Player.getCleanedHistory(history.get(history.size() - 1)).round())
			return -1;
		
		historyRedo.push(history.remove(newLatestIndex + 1));
		historyIndex = newLatestIndex;
		Translator.setCloneString(this, cleaned.cloneString());
		return newLatestIndex;
	}
	
	public int redoLastHistory() {
		if (historyRedo.size() == 0) 
			return -1; // must always have 1 history left or you dont exist?
		
		addHistoryClean(historyRedo.pop());
		Translator.setCloneString(this, getHistory(this.historyIndex));
		return historyIndex;
	}

	public void redoHistory(int index) {
		if (historyRedo.size() == 0)
			return; // must always have 1 history left or you dont exist?

		history.set(index, historyRedo.pop());
		historyIndex = history.size() - 1;
		Translator.setCloneString(this, getHistory(this.historyIndex));
	}

	public void undoHistory(int index) {
		historyRedo.push(Player.getCleanedHistory(history.get(index)).cloneString());
		history.set(index, null);
	}

	public void clearRedo() {
		historyRedo.clear();
	}

	public boolean canUndoHistory(int round) {
		return canUndoHistory > historyRedo.size()
				&& history.size() > 1
				&& (ultimateUndo || (historyIndex > 0 && Player.getCleanedHistory(history.get(historyIndex - 1)).round() == round));
	}

	public boolean canRedoHistory() {
		return historyRedo.size() > 0;
	}

	public int getHistoryIndex() {
		return historyIndex;
	}


	public String peekHistory() {
		int size = history.size();
		if (size > 0)
			return history.get(size - 1);
		return Translator.getCloneString(this);
	}

	public boolean historyHasBought() {
		return history.size() > 1;
	}
	
	public List<String> getHistory() {
		return history;
	}
	
	/*
	 * ==========================  Getters ==========================
	 */

	public boolean isHost() {
		return role >= HOST;
	}

	
	public Rep getCarRep() {
		return car.getRep();
	}	

	public int getCarNameID() {
		var rep = getCarRep();
		if (rep != null)
			return rep.getNameID();
		return -1;
	}

	public boolean isReady() {
		return ready != 0;
	}

	public boolean isPlayer() {
		return role < Player.COMMENTATOR;
	}

	public boolean hasPlacedTileType(TileNames requiredUpgradeToUnlock) {
		return placedTypes[requiredUpgradeToUnlock.ordinal()];
	}

	public void setPlaced(TileNames tileName) {
		placedTypes[tileName.ordinal()] = true;
	}
}
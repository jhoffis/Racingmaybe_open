package game_modes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import audio.SfxTypes;
import communication.Translator;
import engine.graphics.ui.UILabel;
import main.Features;
import main.Texts;
import player_local.Bank;
import player_local.Layer;
import player_local.Player;
import player_local.car.Rep;
import player_local.upgrades.Tool;
import player_local.upgrades.Upgrade;

/**
 * Hva om at i coop så når man oppgraderer så endrer man samme bil og så når man
 * kjører så kjører vi begge hver sin kopi av samme bil mot ai. Og den av oss
 * som får best tid er den som gjelder. Og så kan vi sende penger til hverandre?
 * [9:53 PM]j0hffer: Og alle spillere får forskjellig map gen
 * 
 * 
 * TODO fiks for tap av oppgraderinger. I steden for at alle overskriver med sin
 * Rep forandring så kan clients sende RegVals til sin oppgradering til host og
 * så overskrive seg selv basert på hva hosten har som Rep og i mellomtiden anta
 * at slik oppgraderingen fungerer lokalt er sånn det kommer til å bli (for å
 * unngå lagg)
 * 
 * @author Jens Benz
 */
public class CoopMode extends GameMode {

	public int averageTime;
	private int paidRound = -1;
	private String name;
	private final GameModes gamemode;
	private final float stdPrice;
	public final SingleplayerChallengesMode spBase;
	private Player spPlayer;
	private int[] timeToBeats = new int[0];
	private int[] endGoals = new int[0];

	public CoopMode(GameModes gamemode, SingleplayerChallenges challenge, String name) {
		resizeArrs(5);
		this.gamemode = gamemode;
		this.name = name;
		canSaveMoney = true;

		switch (gamemode) {
		case COOP_FUN:
			stdPrice = 200f;
			break;
		case COOP_WEIRD:
			stdPrice = 130f;
			break;
		case COOP_TOUGH:
			stdPrice = 130f;
			break;
		default:
			throw new IllegalArgumentException("Unexpected value: " + gamemode);
		}

		spBase = new SingleplayerChallengesMode(challenge.ordinal());
		spBase.hasAudio = false;
		spBase.extraSeed = Features.ran.nextInt();
		spPlayer = new Player();
		var spPlayers = new HashMap<Byte, Player>();
		spPlayers.put(spPlayer.id, spPlayer);
		spBase.setPlayers(spPlayers);
		spBase.createPrices(spPlayer, Upgrade.priceFactorStd);
		spBase.updateInfo();
		endGoals[0] = 4000;
		lifes = 15;
	}

	@Override
	public void updateInfo() {
		String info = "";
		super.info = UILabel.create(info.split("\n"));
	}

	@Override
	public boolean isGameOverPossible() {
		return false;
	}

	@Override
	public boolean isGameOver() {
		System.out.println("is gmae over" + endGoals[paidRound] + " and avg: " + averageTime);
		return lifes <= 0 || isWinner(null);
	}

	@Override
	public boolean isWinner(Player player) {
		return (paidRound == rounds && endGoals[paidRound] != -1
				&& (averageTime <= endGoals[paidRound] && averageTime >= 0));
	}

	@Override
	public void startNewRaceDown() {
		if (rounds <= 0)
			return;
		for (var p : getPlayers().values()) {
			spBase.costChange(p);
		}

		averageTime = 0;
	}

	@Override
	public int getRandomRaceType() {
		return 0;
	}

	@Override
	public int getNewRaceGoal() {
		return spBase.createRaceGoal(rounds);
	}

	@Override
	public int newEndGoal(int gameLength) {
		endGoal = gameLength;
		spBase.newEndGoal(gameLength);
		timeToBeats[0] = spBase.timeToBeat;
		return gameLength;
	}

	@Override
	public int getEndGoalStandard() {
		return spBase.getEndGoalStandard();
	}

	@Override
	public String getEndGoalTextDown() {
		return "Round: " + rounds + "\nBeat time: " + SingleplayerChallengesMode.timeToBeat(spBase.timeToBeat)
				+ "\nWin time: " + SingleplayerChallengesMode.timeToBeat(endGoals[rounds])
				+ "\nBase income next turn: $" + ((int) baseIncome(rounds))
				+ "\nLives left: " + (int) lifes;
	}

	@Override
	public String getName() {
		return name;
	}

	private float baseIncome(int rounds) {
		final float inflation = rounds / 6f + 1f;
		return inflation * stdPrice;
	}

	@Override
	public void rewardPlayer(int rounds, int place, int amountOfPlayers, int behindBy, long timeBehindFirst,
			Player player, boolean me) {
		if (paidRound == rounds || !me)
			return;
		paidRound = rounds;

		resizeArrs(rounds + 1);

		int inflationSum = 0;
		int mtpSum = 0;
		double avgTime = 0;
		for (var p : getPlayers().values()) {
			if (p.timeLapsedInRace < 0) {
				if (2f * timeToBeats[rounds] > 5000)
					avgTime += 2f * timeToBeats[rounds];
				else
					avgTime += 5000;
			} else
				avgTime += p.timeLapsedInRace;
			inflationSum += p.bank.getLong(Bank.MONEY) * p.getCarRep().get(Rep.interest);
			mtpSum += p.getCarRep().getInt(Rep.moneyPerTurn);
		}
		avgTime /= (double) getPlayers().size();
		averageTime = (int) Math.round(avgTime);

		var baseIncome = Math.round(baseIncome(rounds));
		int moneyAdded = 0;
		int pointAdded = 1;
		if (averageTime <= timeToBeats[rounds]) {
			moneyAdded = baseIncome;
			inflationSum /= getPlayers().size();
			moneyAdded += inflationSum;
			mtpSum /= getPlayers().size();
			moneyAdded += mtpSum;

			moneyExplaination = "$" + baseIncome + "\n" + "+ $" + inflationSum + " avg. " + Texts.tags[Rep.interest]
					+ "\n" + "+ " + mtpSum + " avg. " + Texts.tags[Rep.moneyPerTurn] + "\n" + "| minimum $"
					+ Texts.formatNumber(baseIncome);

		} else {
			moneyAdded = baseIncome / 2;
			moneyExplaination = "$" + moneyAdded;
			lifes--;
			Features.inst.getAudio().play(SfxTypes.LOSTLIFE);
		}

		for (var p : getPlayers().values()) {
			p.bank.add(moneyAdded, Bank.MONEY);
			p.bank.add(pointAdded, Bank.POINT);
		}

		if (player.isHost()) {
			spPlayer.timeLapsedInRace = averageTime;
			spBase.rewardPlayer(0, 0, 0, 0, 0, spPlayer, false);
			if (spBase.timeToBeat < averageTime)
				spBase.timeToBeat = averageTime;
			else if (spBase.timeToBeat / 2 > averageTime)
				spBase.timeToBeat /= 2;

			timeToBeats[rounds + 1] = spBase.timeToBeat;
			spBase.timeToBeat = 0;
			endGoals[rounds + 1] = (int) (endGoals[rounds] * .9);
		}

//		if (!(amountOfPlayers == -1 || place == -1)) {
//
//			worstTime(player.timeLapsedInRace);
////			var money = ;
//
//			int pointsAdded = amountOfPlayers - (place + 1);
//			if (behindBy == endGoal - 1 && place == 0) {
//				pointsAdded++;
//			}
//
//			player.bank.add(pointsAdded, Bank.POINT);
//
//			float extraMoney = 0;
//			if (behindBy >= 0) {
//				var lastGetsAll = place / (amountOfPlayers > 1 ? amountOfPlayers - 1 : 1);
//				extraMoney = moneyAdded * ((float) behindBy * .05f + ((rounds + 1f) * .05f + .05f)) * lastGetsAll;
//				moneyAdded += extraMoney;
//			}
//
//			var mpt = player.getCarRep().getInt(Rep.moneyPerTurn);
//			moneyAdded += mpt;
//
//			final int behindStd = 5000;
//			var timePenalty = -1d;
//			if (rounds > 1 && timeBehindFirst >= behindStd) {
//				timePenalty = (timeBehindFirst - .5 * behindStd) / (4 * behindStd);
//				if (rounds < 5)
//					timePenalty += (1d - timePenalty) / (rounds + 1);
//				if (timePenalty < .33)
//					timePenalty = .33;
//				if (rounds < 5) // avoid div 1 or 0
//					timePenalty /= 6 - rounds;
//				moneyAdded *= timePenalty;
//			}
//
//			var currentMaxMoney = maxIncome(rounds);
//			if (moneyAdded > currentMaxMoney) {
//				moneyAdded = currentMaxMoney;
//			} else if (moneyAdded < baseIncome) {
//				moneyAdded = baseIncome;
//			}
//			player.bank.add(moneyAdded, Bank.MONEY);
//			if (me) {
//				moneyExplaination = "$" + baseIncome + "\n" + "+ $" + money + " * "
//						+ String.format("%.2f", player.getCarRep().get(Rep.interest)).replace(',', '.') + " "
//						+ Texts.tags[Rep.interest] + "\n" + "+ $" + extraMoney + " extra\n" + "+ " + mpt + " "
//						+ Texts.tags[Rep.moneyPerTurn] + "\n"
//						+ (timePenalty != -1 ? ">5 sec behind: -$" + (int) (100f * timePenalty) + "%\n" : "")
//						+ "| lose $ above " + currentMaxMoney + "\n" + "| minimum $" + Texts.formatNumber(baseIncome);
//			}
//		} else {
//			player.bank.add(moneyAdded / 2f, Bank.MONEY);
//			if (me)
//				moneyExplaination = "$" + moneyAdded + " -50%";
//		}

	}

	private void resizeArrs(int rounds) {
		if (timeToBeats.length <= rounds) {
			var timeToBeats = new int[2 * rounds];
			var endGoals = new int[2 * rounds];
			for (int i = 0; i < timeToBeats.length; i++) {
				if (this.timeToBeats.length > i) {
					timeToBeats[i] = this.timeToBeats[i];
					endGoals[i] = this.endGoals[i];
				} else {
					timeToBeats[i] = -1;
					endGoals[i] = -1;
				}
			}
			this.timeToBeats = timeToBeats;
			this.endGoals = endGoals;
		}
	}

	@Override
	protected void setGeneralInfoDown(String[] input, AtomicInteger index) {
		int rounds = Integer.parseInt(input[index.getAndIncrement()]);
		resizeArrs(rounds);
		this.timeToBeats[rounds] = Integer.parseInt(input[index.getAndIncrement()]);
		this.endGoals[rounds] = Integer.parseInt(input[index.getAndIncrement()]);
		this.lifes = Float.parseFloat(input[index.getAndIncrement()]);
		this.name = input[index.getAndIncrement()];
		if (!isGameBegun()) {
			this.spBase.costChange = Integer.parseInt(input[index.getAndIncrement()]);
			spPlayer.setCloneString(input, index);
			setPricesAsString(spPlayer, input, index);
			initPlayers();
		}
	}

	@Override
	protected void getGeneralInfoDown(StringBuilder sb) {
		sb.append(Translator.split).append(rounds).append(Translator.split).append(timeToBeats[rounds])
				.append(Translator.split).append(endGoals[rounds]).append(Translator.split).append(lifes)
				.append(Translator.split).append(name);
		if (!isGameBegun()) {
			sb.append(Translator.split).append(spBase.costChange);
			setPrices(spPlayer.upgrades);
			spPlayer.getCloneString(sb, 1, Translator.split, false);
			sb.append(Translator.split).append(getPricesAsString());
		}
	}

	@Override
	public GameModes getGameModeEnum() {
		return gamemode;
	}

	@Override
	public String getExtraGamemodeRaceInfo() {
		return "(Win: " + Texts.formatNumber((float) endGoals[rounds] / 1000f) + " sec) Beat: "
				+ SingleplayerChallengesMode.timeToBeat(timeToBeats[rounds]);
	}

	@Override
	public int getEndGoal() {
		if (isRacing())
			return endGoals[rounds];

		var paidRound = this.paidRound;
		if (paidRound < 0)
			paidRound = 0;
		else if (paidRound >= endGoals.length)
			paidRound = endGoals.length - 1;

		return endGoals[paidRound];
	}

	@Override
	public List<Player> addExtraPlayers(CopyOnWriteArrayList<Player> sortedPlayers) {
		return sortedPlayers;
	}

	public void initPlayers() {
		if (isGameBegun())
			return;
		var reduce = 1d / (double) getPlayers().values().size();
		
		Rep hostRep = null;
		for (var p : super.getPlayers().values()) {
			if (p.isHost()) {
				hostRep = p.getCarRep();
				break;
			}
		}

		for (var p : super.getPlayers().values()) {
			p.canUndoHistory = 0;

			Translator.setCloneString(p.getCarRep(), hostRep);
			Translator.setCloneString(p.upgrades, spPlayer.upgrades);

			for (var upgrade : p.upgrades.getUpgradesAll()) {
				if (upgrade instanceof Upgrade up) {
					up.getRegVals().multiplyAllValues(reduce);
					up.getNeighbourModifier().multiplyAllValues(reduce);
				} else if (upgrade instanceof Tool tool) {
					tool.setLayer(p.layer);
				}
			}
			var ran = Features.ran;
			switch (gamemode) {
			case COOP_FUN:
				Translator.setCloneString(p.layer,
						new Layer(ran, ran.nextInt(300) + 50, ran.nextInt(3), 3, ran.nextInt(30), 30, ran.nextInt(4)));
				break;
			case COOP_WEIRD:
//				Translator.setCloneString(p.layer, new Layer(ran, ran.nextInt(120), ran.nextInt(6) + 3,
//						ran.nextInt(2), ran.nextInt(20), 20, ran.nextInt(4)));
				break;
			case COOP_TOUGH:
				Translator.setCloneString(p.layer, new Layer(ran, ran.nextInt(80) + 50, ran.nextInt(6) + 8,
						ran.nextInt(3), ran.nextInt(6), 5, ran.nextInt(4)));
				break;
			default:
				// do nothing
				break;
			}
		}
	}

	public int prevtimeToBeat() {
		if (isRacing())
			return timeToBeats[rounds];

		var paidRound = this.paidRound;
		if (paidRound < 0)
			paidRound = 0;
		else if (paidRound >= timeToBeats.length)
			paidRound = timeToBeats.length - 1;
		return timeToBeats[paidRound];
	}
}
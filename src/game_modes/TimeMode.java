package game_modes;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import communication.Translator;
import engine.graphics.ui.UILabel;
import main.Texts;
import player_local.Bank;
import player_local.Player;
import player_local.car.Rep;
import scenes.game.Race;

public class TimeMode extends GameMode {

	private long lowestTime = Integer.MAX_VALUE;
	private int length;
	private final String name;
	private final GameModes gamemode;
	private Player lowestPlayer;

	public TimeMode(GameModes gamemode, int length, String name) {
		this.gamemode = gamemode;
		this.length = length;
		this.name = name;
		canSaveMoney = true;
	}

	@Override
	public boolean isGameOver() {
		if (lowestTime < endGoal && lowestTime >= 0 && lowestPlayer != null) {
			lowestPlayer.bank.add(1, Bank.POINT);
			return true;
		}
		return false;
	}

	@Override
	public void startNewRaceDown() {
	}

	@Override
	public int getRandomRaceType() {
		return 0;
	}

	@Override
	public int getNewRaceGoal() {
		return 120 + (rounds * 60);
	}

	@Override
	public int newEndGoal(int gameLength) {
		endGoal = gameLength;
		return endGoal;
	}

	@Override
	public void updateInfo() {
		String info = "Be the first one to do a time below " + SingleplayerChallengesMode.timeToBeat(length);

		super.info = UILabel.create(info.split("\n"));
	}

	@Override
	public int getEndGoalStandard() {
		return length;
	}

	@Override
	public String getEndGoalTextDown() {
		return "Round: " + rounds + "\nTime needed to win: " + SingleplayerChallengesMode.timeToBeat(length);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void rewardPlayer(int rounds, int place, final int amountOfPlayers, int behindBy, long timeBehindFirst,
			Player player, boolean me) {
		if (rounds < 0)
			rounds = 0;
		final float stdPrice = 200 + 100 * rounds;

		if (place == -1) {
        	place = amountOfPlayers - 1;
        	timeBehindFirst = Integer.MAX_VALUE;
        }

		worstTime(player);

		if (player.timeLapsedInRace < lowestTime) {
			lowestTime = player.timeLapsedInRace;
			lowestPlayer = player;
		}
		var money = player.bank.getLong(Bank.MONEY);

		float moneyAdded = (float) (stdPrice + money * player.getCarRep().get(Rep.interest));

		var mpt = player.getCarRep().getInt(Rep.moneyPerTurn);
		moneyAdded += mpt;

		String extraMoney = null;
		if (place == 0) {
			moneyAdded *= 1.1f;
			extraMoney = "+10%";
		} else if (amountOfPlayers >= 4 && place >= 3) {
			var perc = (((float) place - 2f) / 20f);
			moneyAdded *= 1f + perc;
			extraMoney = "+" + (int) (100f*perc) + "%";
		}

		final int behindStd = 5000;
		double timePenalty = -1;
		if (rounds > 1 && timeBehindFirst >= behindStd) {
			timePenalty = (timeBehindFirst - .5 * behindStd) / (4 * behindStd);
			if (rounds < 5)
				timePenalty += (1d - timePenalty) / (rounds + 1);
			if (timePenalty < .33)
				timePenalty = .33;
			if (rounds < 5) // avoid div 1 or 0
				timePenalty /= 6 - rounds;
			moneyAdded *= timePenalty;
		}

		if (moneyAdded < stdPrice) {
			moneyAdded = stdPrice;
		}
		player.bank.add(moneyAdded, Bank.MONEY);
		if (me) {
			moneyExplaination = 
					"$200\n" 
					+ "+ $100*round\n" 
					+ "+ $" + money + " * " + String.format("%.2f", player.getCarRep().get(Rep.interest)).replace(',', '.') + " " + Texts.tags[Rep.interest] + "\n" 
					+ "+ " + mpt + " " + Texts.tags[Rep.moneyPerTurn] + "\n"
					+ (extraMoney != null ? Texts.podiumConversion(place) + ": " + extraMoney + "\n" : "")
					+ (timePenalty != -1 ? ">5 sec behind: $pt*" + Texts.formatNumber(timePenalty) + "\n" : "")
					+ "| minimum $" + Texts.formatNumber(stdPrice);
		}
	}

	@Override
	public boolean isGameOverPossible() {
		return false;
	}

	@Override
	protected void setGeneralInfoDown(String[] input, AtomicInteger index) {
		length = Integer.parseInt(input[index.getAndIncrement()]);
	}

	@Override
	protected void getGeneralInfoDown(StringBuilder sb) {
		sb.append(Translator.split).append(length);

	}

	@Override
	public GameModes getGameModeEnum() {
		return gamemode;
	}

	@Override
	public String getExtraGamemodeRaceInfo() {
		return "Win: " + Texts.formatNumber((float) endGoal / 1000f) + " sec";
	}

	@Override
	public List<Player> addExtraPlayers(CopyOnWriteArrayList<Player> sortedPlayers) {
		return sortedPlayers;
	}
}

package game_modes;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import communication.Translator;
import engine.graphics.ui.UILabel;
import main.Features;
import main.Main;
import main.Texts;
import player_local.Bank;
import player_local.Player;
import player_local.car.Rep;
import player_local.upgrades.UpgradeGeneral;
import scenes.SceneHandler;
import scenes.game.Race;

public class TotalMode extends GameMode {

	private int racesLeft, racesAdded;
	private int length, withinReach;
	private final String name;
	private final GameModes gamemode;

	public TotalMode(GameModes gamemode, int length, int withinReach, String name) {
		this.gamemode = gamemode;
		this.length = length;
		this.withinReach = withinReach;
		this.name = name;
		canSaveMoney = true;
	}

	private int getRacesLeft() {
		return (racesLeft + racesAdded) - rounds;
	}

	@Override
	public boolean isGameOver() {
		return getRacesLeft() < 1;
	}

	@Override
	public void startNewRaceDown() {

	}

	@Override
	public int getRandomRaceType() {
		return 0;
	}

	@Override
	public int newEndGoal(int gameLength) {
		endGoal = gameLength;
		racesLeft = endGoal;
		return endGoal;
	}

	@Override
	public void updateInfo() {
		String info = "Get the most points within " + endGoal + " races to win!\nIf the leading player loses the final race, the underdog gets a second chance!\nBut the underdog has to be no more than " + withinReach + " points behind!" +
				"\nAlso players who are behind and win a race get extra points to catch up!";

		super.info = UILabel.create(info.split("\n"));
	}

	@Override
	public int getEndGoalStandard() {
		return length;
	}

	@Override
	public String getEndGoalTextDown() {
		return "Round: " + rounds + "\nRaces left: " + getRacesLeft() + "/" + endGoal + "\nMax income next turn: " + "$"
				+ maxIncome(rounds) + "\nIf the winner loses and it is the final race the \nunderdog gets a second chance!\nBut the underdog has to be no more than " + withinReach + " points behind!" +
				"\nAlso players who are behind and win get extra points to\ncatch up! However, losers do not get any catch-up money!";
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	protected int giveNewPrices(UpgradeGeneral upgrade, int i, float priceFactor, Random ran, int premadeOverride) {
    	if (upgrade != null) {
			var pricePremade = upgrade.getPremadePrice(); // premade price
			if (pricePremade != 0) {
				if (premadeOverride == 0)
					upgradePrices[i] = pricePremade / priceFactor;
				else
					upgradePrices[i] = premadeOverride / priceFactor;
			} else {
				upgradePrices[i] = 50f / priceFactor;
			}
			return i + 1;
		}
		return i;
	}
	
	@Override
	protected int maxIncome(int rounds) {
		return 260 * (rounds + 1);
	}

	@Override
	public void rewardPlayer(int rounds, int place, final int amountOfPlayers, int behindBy, long timeBehindFirst,
			Player player, boolean me) {
		final float stdPrice = 200;
		
		if (place == -1) {
        	place = amountOfPlayers - 1;
        	timeBehindFirst = Integer.MAX_VALUE;
        }

		worstTime(player);

		var baseIncome = stdPrice + (100 * rounds);
		var moneyAdded = baseIncome;
		var money = player.bank.getLong(Bank.MONEY);
		var interest = money * player.getCarRep().get(Rep.interest);
		moneyAdded += interest;
		var mpt = player.getCarRep().getInt(Rep.moneyPerTurn);
		
		final int behindStd = 5000;
        var timePenalty = -1d;
        if (rounds > 1 && timeBehindFirst >= behindStd) {
        	timePenalty = timePenalty(timeBehindFirst, rounds);
        	mpt *= timePenalty;
        } 
		moneyAdded += mpt;

		int pointsAdded = amountOfPlayers - (place + 1);
		if (place == 0 && behindBy > 0) {
			pointsAdded += (int) Math.min((behindBy - 1) * .5, 2);
			if (getRacesLeft() <= 1 && behindBy <= withinReach) {
				racesAdded += 1;
				endGoal++;
				SceneHandler.showMessage(player.name + " is catching up! One more round, one more chance...");
			}
		}
		player.bank.add(pointsAdded, Bank.POINT);
		var currentMaxMoney = maxIncome(rounds);
		if (moneyAdded > currentMaxMoney) {
			moneyAdded = currentMaxMoney;
		} else if (moneyAdded < baseIncome) {
			moneyAdded = baseIncome;
		}
		player.bank.add(moneyAdded, Bank.MONEY);
		if (me) {
			moneyExplaination = 
					"$" + stdPrice + "\n"
					+ "+ $100*round\n"
					+ "+ $" + money + " * " + String.format("%.2f", player.getCarRep().get(Rep.interest)).replace(',', '.') + " " + Texts.tags[Rep.interest] + "\n"
					+ "+ " + mpt + " " + Texts.tags[Rep.moneyPerTurn] + "\n"
                    + (timePenalty != -1 ? ">5 sec behind: -$pt" + (100 - (int) (100f * timePenalty)) + "%\n" : "")
					+ "| lose $ above " + currentMaxMoney + "\n"
					+ "| minimum $" + Texts.formatNumber(baseIncome);
		}
	}

	@Override
	public boolean isGameOverPossible() {
		return false;
	}

	@Override
	protected void setGeneralInfoDown(String[] input, AtomicInteger index) {
		length = Integer.parseInt(input[index.getAndIncrement()]);
		racesLeft = Integer.parseInt(input[index.getAndIncrement()]);
		racesAdded = Integer.parseInt(input[index.getAndIncrement()]);
	}

	@Override
	protected void getGeneralInfoDown(StringBuilder sb) {
		sb.append(Translator.split).append(length)
		.append(Translator.split).append(racesLeft)
		.append(Translator.split).append(racesAdded);
	}

	@Override
	public GameModes getGameModeEnum() {
		return gamemode;
	}

	@Override
	public String getExtraGamemodeRaceInfo() {
		return null;
	}

	@Override
	public List<Player> addExtraPlayers(CopyOnWriteArrayList<Player> sortedPlayers) {
		return sortedPlayers;
	}
}

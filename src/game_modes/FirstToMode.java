package game_modes;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import communication.Translator;
import engine.graphics.ui.UILabel;
import main.Features;
import main.Texts;
import player_local.Bank;
import player_local.Player;
import player_local.car.Rep;

public class FirstToMode extends GameMode {

	private int length;
	private String name;
	private final float stdPrice = 120f;

	public FirstToMode(int length) {
		this.length = length;
		canSaveMoney = true;
		updateName();
	}

	private void updateName() {
		this.name = "First To " + length;
	}

	@Override
	public void updateInfo() {
		String info = "Here you win by being the first to get " + endGoal + " points!";
		super.info = UILabel.create(info.split("\n"));
	}

	@Override
	public boolean isGameOverPossible() {
		return false;
	}

	@Override
	public boolean isGameOver() {
		for (var p : getPlayers().values()) {
			if (p.bank.getLong(Bank.POINT) >= endGoal)
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
	protected int newEndGoal(int gameLength) {
		endGoal = gameLength;
		return endGoal;
	}

	@Override
	public int getEndGoalStandard() {
		return length;
	}

	@Override
	public String getEndGoalTextDown() {
		return "Round: " + rounds + "\nGather " + endGoal + " points first to win\nMax income next turn: " + "$"
				+ maxIncome(rounds) + "\nBase income next turn: " + baseIncome(rounds);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	protected int maxIncome(int rounds) {
		return 150 * (rounds+1);
	}
	
	private float baseIncome(int rounds) {
		final float inflation = rounds / 3f + 1f;
		return inflation * stdPrice;
	}

	@Override
	public void rewardPlayer(int rounds, int place, int amountOfPlayers, int behindBy, long timeBehindFirst, Player player, boolean me) {
		var baseIncome = baseIncome(rounds);
		var moneyAdded = baseIncome;
		
		if (place == -1) {
        	place = amountOfPlayers - 1;
        	timeBehindFirst = Integer.MAX_VALUE;
        }
			
		worstTime(player);
		var money = player.bank.getLong(Bank.MONEY);
		var mpt = player.getCarRep().getInt(Rep.moneyPerTurn);

		moneyAdded += mpt;
		moneyAdded += money * player.getCarRep().get(Rep.interest);

		var timeBehind = 1d;
		var timeBehindType = 0;
		if (timeBehindFirst >= 5000) {
			moneyAdded *= 0.75;
			timeBehind *= .75;
			timeBehindType = 5;
			if (timeBehindFirst >= 10000) {
				moneyAdded *= 0.75;
				timeBehind *= .75;
				timeBehindType = 10;
			}
		}
		int pointsAdded = place == 0 ? 1 : 0;

		player.bank.add(pointsAdded, Bank.POINT);

		float extraMoney = 0;
		if (place > 0) {
			extraMoney = baseIncome * (((rounds+1f)*.05f+.05f) * place / (amountOfPlayers > 1 ? amountOfPlayers - 1 : 1));
			if (player.aheadByPoints > 1)
				extraMoney *= .5f;
			moneyAdded += extraMoney;
		}
		var currentMaxMoney = maxIncome(rounds);
		if (moneyAdded > currentMaxMoney) {
			moneyAdded = currentMaxMoney;
		} else if (moneyAdded < baseIncome) {
			moneyAdded = baseIncome;
		}
		player.bank.add(moneyAdded, Bank.MONEY);
		if (me) {
			moneyExplaination = 
					"$" + baseIncome + "\n" 
					+ "+ $" + money + " * " + String.format("%.2f", player.getCarRep().get(Rep.interest)).replace(',', '.') + " " + Texts.tags[Rep.interest] + "\n" 
					+ "+ " + mpt + " " + Texts.tags[Rep.moneyPerTurn] + "\n"
					+ (timeBehind != -1 ? ">" + timeBehindType + " sec behind: -$" + (int) (100f*timeBehind) + "%\n" : "")
					+ "+ $" + extraMoney + " extra\n"
					+ "| lose $ above " + currentMaxMoney + "\n"
					+ "| minimum $" + Texts.formatNumber(stdPrice);
		}
	}

	@Override
	protected void setGeneralInfoDown(String[] input, AtomicInteger index) {
		this.length = Integer.parseInt(input[index.getAndIncrement()]);
		updateName();
	}

	@Override
	protected void getGeneralInfoDown(StringBuilder sb) {
		sb.append(Translator.split).append(length);
	}

	@Override
	public GameModes getGameModeEnum() {
		return GameModes.FIRST_TO_5;
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

package game_modes;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import engine.graphics.ui.UILabel;
import main.Texts;
import player_local.Bank;
import player_local.Player;
import player_local.car.Rep;
import player_local.upgrades.Store;
import player_local.upgrades.Tool;
import player_local.upgrades.UpgradeGeneral;

public class SandboxMode extends GameMode {

	public Store store;

	@Override
	public void updateInfo() {
		super.info = UILabel.create("This mode never ends and you get at least $1000 each round.".split("\n"));
	}

	@Override
	public boolean isGameOverPossible() {
		return false;
	}

	@Override
	public boolean isGameOver() {
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
	public int newEndGoal(int gameLength) {
		endGoal = gameLength;
		return gameLength;
	}

	@Override
	public int getEndGoalStandard() {
		return 0;
	}

	@Override
	public String getEndGoalTextDown() {
		return "Round: " + rounds + "\n";
	}

	@Override
	public String getName() {
		return "Sandbox";
	}

	@Override
	protected int giveNewPrices(UpgradeGeneral upgrade, int i, float priceFactor, Random ran, int premadeOverride) {
		upgradePrices[i] = 50f / priceFactor;
		return i + 1;
	}

	@Override
	public void rewardPlayer(int rounds, int place, int amountOfPlayers, int behindBy, long timeBehindFirst,
			Player player, boolean me) {
		if (rounds == 0) {
			player.canUndoHistory = Integer.MAX_VALUE;
			player.layer.reset();
			player.resetHistory(0);
			player.layer.improvementPointsNeededIncrease = 0;
			Tool.improvementPointsNeeded = 0;
			if (me)
				store.resetTowardsPlayer(player);
		}
		worstTime(player);

		var baseIncome = 10000;
		var moneyAdded = baseIncome;

		var money = player.bank.getLong(Bank.MONEY);
		moneyAdded += money * player.getCarRep().get(Rep.interest);
			
		var mpt = player.getCarRep().getInt(Rep.moneyPerTurn);
		moneyAdded += mpt;
			
		player.bank.add(moneyAdded, Bank.MONEY);
		if (me) {
			moneyExplaination = 
					"$" + baseIncome + "\n" 
					+ "+ $" + money + " * " + String.format("%.2f", player.getCarRep().get(Rep.interest)).replace(',', '.') + " " + Texts.tags[Rep.interest] + "\n" 
					+ "+ " + mpt + " " + Texts.tags[Rep.moneyPerTurn];
		}
	}

	@Override
	protected void setGeneralInfoDown(String[] input, AtomicInteger index) {
	}

	@Override
	protected void getGeneralInfoDown(StringBuilder sb) {
	}

	@Override
	public GameModes getGameModeEnum() {
		return GameModes.SANDBOX;
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

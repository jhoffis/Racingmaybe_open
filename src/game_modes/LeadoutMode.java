package game_modes;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import communication.Translator;
import engine.graphics.ui.UILabel;
import main.Features;
import main.Texts;
import player_local.Bank;
import player_local.Player;
import player_local.car.Rep;
import player_local.upgrades.UpgradeGeneral;

/**
 * If you're behind the leader by 3 points you get knocked out.
 *
 * @author Jens Benz
 */
public class LeadoutMode extends GameMode {

    private boolean hardcore;
    private int length;
    private String name;
    private final GameModes gamemode;
    private final float stdPrice = 120f;


    public LeadoutMode(GameModes gamemode, boolean hardcore, int length, String name) {
        this.gamemode = gamemode;
        this.hardcore = hardcore;
        this.length = length;
        this.name = name;
        canSaveMoney = true;
    }

    @Override
    public void updateInfo() {
        String info = "In this gamemode you win by being at least " + endGoal + " points ahead of the last player!" +
                "\nAKA " + length + " * (\"Amount of Players\" - 1)";
        if (hardcore)
            info += "\nHardcore means you get no bonus money for being last.";
        super.info = UILabel.create(info.split("\n"));
    }

    @Override
    public boolean isGameOverPossible() {
        return false;
    }

    @Override
    public boolean isGameOver() {
        if (playersCount() > 1) {
            for (var p : getPlayers().values()) {
                if (p.aheadByPoints >= endGoal)
                    return true;
            }
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
    public int newEndGoal(int gameLength) {
        endGoal = gameLength;
        return gameLength;
    }

    @Override
    public int getEndGoalStandard() {
        return (playersCount() - 1) * length;
    }

    @Override
    public String getEndGoalTextDown() {
        return "Round: " + rounds + "\nLead by more than " + endGoal + " points to win\nMax income next turn: " + "$"
                + maxIncome(rounds) + "\nBase income next turn: $" + ((int) baseIncome(rounds));
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


    private float baseIncome(int rounds) {
        final float inflation = rounds / 3f + 1f;
        return inflation * stdPrice;
    }
    
    @Override
    public void rewardPlayer(int rounds, int place, int amountOfPlayers, int behindBy, long timeBehindFirst,
                             Player player, boolean me) {
        var baseIncome = baseIncome(rounds);
        var moneyAdded = baseIncome;

        if (place == -1) {
        	place = amountOfPlayers - 1;
        	timeBehindFirst = Integer.MAX_VALUE;
        }

        worstTime(player);
        var money = player.bank.getLong(Bank.MONEY);

        moneyAdded += money * player.getCarRep().get(Rep.interest);

        int pointsAdded = amountOfPlayers - (place + 1);
        if (behindBy == endGoal - 1 && place == 0) {
            pointsAdded++;
        }

        player.bank.add(pointsAdded, Bank.POINT);

        float extraMoney = 0;
        if (!hardcore && behindBy >= 0) {
            var lastGetsAll = place / (amountOfPlayers > 1 ? amountOfPlayers - 1 : 1);
            extraMoney = moneyAdded * ((float) behindBy * .05f + ((rounds + 1f) * .05f + .05f)) * lastGetsAll;
            moneyAdded += extraMoney;
        }

        var mpt = player.getCarRep().getInt(Rep.moneyPerTurn);

        final int behindStd = 5000;
        var timePenalty = -1d;
        if (rounds > 1 && timeBehindFirst >= behindStd) {
        	timePenalty = timePenalty(timeBehindFirst, rounds);
        	mpt *= timePenalty;
        } 
        moneyAdded += mpt;

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
                            + "+ $" + extraMoney + " extra\n"
                            + "+ " + mpt + " " + Texts.tags[Rep.moneyPerTurn] + "\n"	
                            + (timePenalty != -1 ? ">5 sec behind: -$pt" + (100 - (int) (100f * timePenalty)) + "%\n" : "")
                            + "| lose $ above " + currentMaxMoney + "\n"
                            + "| minimum $" + Texts.formatNumber(baseIncome);
        }

    }

    @Override
    protected void setGeneralInfoDown(String[] input, AtomicInteger index) {
        this.hardcore = Integer.parseInt(input[index.getAndIncrement()]) != 0;
        this.length = Integer.parseInt(input[index.getAndIncrement()]);
        this.name = input[index.getAndIncrement()];
    }

    @Override
    protected void getGeneralInfoDown(StringBuilder sb) {
        sb.append(Translator.split).append(hardcore ? 1 : 0).append(Translator.split).append(length)
                .append(Translator.split).append(name);
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

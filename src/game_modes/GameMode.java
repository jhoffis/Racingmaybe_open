package game_modes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import communication.Translator;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UILabel;
import main.Features;
import main.Main;
import player_local.Bank;
import player_local.Player;
import player_local.car.Rep;
import player_local.upgrades.Upgrade;
import player_local.upgrades.UpgradeGeneral;
import player_local.upgrades.Upgrades;

public abstract class GameMode {

	protected Map<Byte, Player> players;
	protected final List<Player> winners = new ArrayList<>();

	public static final int raceLightsLength = 6, raceLightsCanStartDriving = 1;
	
	protected float[] upgradePrices;
	protected boolean racing;
	protected int raceGoal,
			rounds,
			endGoal;
	protected float lifes;
	protected long raceStartedTime;
	protected int waitTimeRandoFactor = 200;
	protected boolean endGame;
	protected boolean canSwitchBetweenGamemodes = true;
	
	protected UILabel[] info;
	
	public boolean canSaveMoney = false;
	public int countdownSetting = Main.DEBUG ? 10 : 60;
	public String moneyExplaination;
	
	private long worstTime = Integer.MAX_VALUE;
	private boolean worstHitMaxSpeed = false;
	private int prevRaceLength;
	
	protected int playersCount() {
		int amount = 0;
		for (var p : getPlayers().values())
			if (p.role != Player.COMMENTATOR)
				amount++;
		return amount;
	}

	public void setPlayers(Map<Byte, Player> players) {
		this.players = players;
	}
	
	public abstract void updateInfo();

	/**
	 * Based on the gamemode rules - where does the asker stand?
	 */
	public String getPodiumPosition(Player asker) {
		int place = 0;
		for (var p : getPlayers().values()) {

			if (p != asker) {

				int otherPoints = (int) p.bank.getLong(Bank.POINT);
				if (asker.bank.getLong(Bank.POINT) < otherPoints) {
					place++;
				}
			}
		}
		return String.valueOf(place);
	}

	protected int maxIncome(int rounds) {
		return (int) (5d * Math.pow(rounds, 2) + 100*rounds + 200);
	}
	
	public static double timePenalty(double timeBehindFirst, int rounds) {
    	if (rounds < 5) // avoid div 1 or 0
    		timeBehindFirst /= 1.5;
        var timePenalty = 1.0 - (Math.log10(timeBehindFirst) / 2.1 - 1.3);
        if (timePenalty < .4)
            timePenalty = .4;
        return timePenalty;
    }

	public boolean isGameBegun() {
		return rounds > 0 || racing;
	}

	public abstract boolean isGameOverPossible();

	public abstract boolean isGameOver();

	/**
	 * @return has the server run "endGame()"?
	 */
	public boolean isGameExplicitlyOver() {
		return endGame;
	}

	/**
	 * Resets and closes down most - if not all - values
	 */
	public void endGameToWin() {
		endGame = true;
	}

	public void startNewRace(long startTime) {
		startNewRaceDown();
		
		for (var player : getPlayers().values())
			player.newRace();

		raceStartedTime = startTime;
	}
	
	/**
	 * Sets everything up as if race has started
	 */
	protected abstract void startNewRaceDown();

	/**
	 * Creates a new racetrack somewhere in the world and with a race type of
	 * choice. For instance regular 1000 m or first to 200 km/h
	 * 
	 * @return type of race
	 */
	public abstract int getRandomRaceType();

	protected void worstTime(Player player) {
		if (player.timeLapsedInRace > 0 && (player.timeLapsedInRace > worstTime || worstTime == Integer.MAX_VALUE)) {
			worstTime = player.timeLapsedInRace;
			var rep = player.getCarRep();
			worstHitMaxSpeed = rep.get(Rep.highestSpdAchived) > .97 * rep.get(Rep.spdTop);
			System.out.println("worstHitMaxSpeed: " + worstHitMaxSpeed);
		}
	}
	
	/**
	 * Checks type of race and determines the length. For instance if it's 1000 m or
	 * 2000 m.
	 * 
	 * @return length of current type of race
	 */
	public int getNewRaceGoal() {
		int newLength;
		if (rounds <= 0 || prevRaceLength <= 0) {
			newLength = 240;
		} else {
			var timeAim = 0.25*rounds + 15;
			
			var timeMaxAim = worstHitMaxSpeed ? 15 : 30;
			if (timeAim > timeMaxAim)
				timeAim = timeMaxAim;
			
//			System.out.println("timeAim: " + timeAim);
//			System.out.println("worstTime: " + worstTime);
			var upOrDown = 
					(worstTime < 500)
					? 2.5d + Features.ran.nextDouble(1.25)
					: (worstTime < 2000)
					? 2.25d + Features.ran.nextDouble(0.95)
					: (worstTime < 5000)
					? 1.75d + Features.ran.nextDouble(0.75)
					: (worstTime < timeAim*1000) 
					? 1.25d + Features.ran.nextDouble(0.5) 
					: 0.25d + Features.ran.nextDouble(.75);
			newLength = (int) Math.round(Math.floor((double) prevRaceLength * upOrDown / 10d) * 10d) + 60;
		}
		worstHitMaxSpeed = false;
		worstTime = Integer.MAX_VALUE;
		prevRaceLength = newLength;
		return newLength;
	}

	public int newEndGoal() {
		return setNewEndGoal(getEndGoalStandard());
	}
	
	public void hostDoStuff() {
	}
	
	public int setNewEndGoal(int endGoal) {
        prepareNextRaceManually(getNewRaceGoal());
        rounds = 0;
		return newEndGoal(endGoal);
	}
	
	public int getEndGoal() {
		return endGoal;
	}
	
	/**
	 * Is it first to 20 points or one with most points after 18 races?
	 * @return 
	 */
	protected abstract int newEndGoal(int gameLength);

	public abstract int getEndGoalStandard();
	
	/**
	 * @return A text that shows the players what the goal of the game is
	 */
	public String getEndGoalText() {
		return "    " + getName() + ":\n" + getEndGoalTextDown();
	}

	protected abstract String getEndGoalTextDown();

	/**
	 * This is run at the end of the game. It looks at points and such to determine
	 * who won based on the rules of the gamemode. This is only run once to not lose
	 * information about players who leave.
	 */
	public void determineWinner() {
		winners.clear();
		for (var player : getPlayers().values()) {
			if (player.resigned) continue;

			if (winners.isEmpty() || player.bank.getLong(Bank.POINT) == winners.get(0).bank.getLong(Bank.POINT)) {
				winners.add(player);
			} else if (player.bank.getLong(Bank.POINT) > winners.get(0).bank.getLong(Bank.POINT)) {
				winners.clear();
				winners.add(player);
			}
		}
	}

	public boolean isWinner(Player player) {
		for(Player winner : winners) {
			if(winner.equals(player))
				return true;
		}
		
		return false;
	}
	
	/**
	 * @param asker - client
	 * @return Winnerstring to show in "WinnerVisual" based on who is asking
	 */
	public String getDeterminedWinnerText(Player asker) {
		String winnerText = null;

		if (asker.bank.getLong(Bank.POINT) == winners.get(0).bank.getLong(Bank.POINT))
			winnerText = youWinnerText(asker);
		else if (winners.size() == 1)
			winnerText = otherSingleWinnerText(asker);
		else
			winnerText = otherMultiWinnerText(asker);

		return winnerText;
	}

	/**
	 * You are the winner
	 */

	public String youWinnerText(Player asker) {
		StringBuilder winnerText = new StringBuilder();
		winnerText.append("You won");

		// Are you the only winner?
		if (winners.size() > 1) {
			winnerText.append(" along with:#" + UIColors.WHITE);
			for (Player player : winners) {
				winnerText.append(";").append(player.name).append(" who drove a ").append(player.getCarRep().getName()).append("#").append(UIColors.WHITE);
			}
		} else {
			winnerText.append("!!!#" + UIColors.WHITE);
		}
		winnerText.append(";You have ").append(asker.bank.getLong(Bank.POINT)).append(" points!#").append(UIColors.WHITE);

		return winnerText.toString();
	}

	/**
	 * One other player won, how are the stats of that player compared to you?
	 */

	public String otherSingleWinnerText(Player asker) {
		return winners.get(0).name + " won!!!#" + UIColors.WHITE + ";;" + "He drove a " + winners.get(0).getCarRep().getName() + "!#" + UIColors.WHITE + ";"
				+ winners.get(0).name + " has " + winners.get(0).bank.getLong(Bank.POINT) + " points!#" + UIColors.WHITE + ";" + "You drove a " + asker.getCarRep().getName() + " and you only have " + asker.bank.getLong(Bank.POINT) + " points!" + "#" + UIColors.WHITE;
	}

	/**
	 * Multiple other players have won. How are their stats compared to yours?
	 */
	public String otherMultiWinnerText(Player asker) {
		String winnerText = "";
		winnerText = "The winners are:#" + UIColors.WHITE;

		for (Player player : winners) {
			winnerText += ";" + player.name + " who drove a " + player.getCarRep().getName() + "#" + UIColors.WHITE;
		}

		winnerText += ";They won with " + winners.get(0).bank.getLong(Bank.POINT) + " points!#" + UIColors.WHITE;
		return winnerText;
	}

	public void prepareNextRaceManually(int length) {
		this.raceGoal = length;
		rounds++;
		updateInfo();
	}

	/**
	 * Name to identify which gamemode to host and init
	 */
	public abstract String getName();

	// Override me
	public String getNameFull() {
		return getName();
	}

	public boolean isAllFinished() {
		for (var p : getPlayers().values()) {
			if (!p.resigned && p.finished == 0 && p.role != Player.COMMENTATOR) {
				return false;
			}
		}
		return true;
	}

	public boolean everyoneInRace() {
		for (var p : getPlayers().values()) {
			if (!p.resigned && !p.inTheRace) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Just give the times in between the racelights. Not actual time.
	 * 
	 * 0 = minimum wait time. 0 red -> when done move to these stats when time done
	 * 1 = 1 red -> 2 = 2 red -> 3 = 3 red -> null = all green
	 * 
	 * @return
	 */
	public long[] createWaitTimeRaceLights() {
		long[] res = new long[GameMode.raceLightsLength];

//		OLD TYPE
//		res[0] = 2000; 
//		res[1] = res[0] + (waitTime - 300 + r.nextInt(1200));
//		res[2] = res[1] + (waitTime - 300 + r.nextInt(1200));
//		res[3] = res[2] + (waitTime - 300 + r.nextInt(1200));
		final var waitFactor = rounds <= 1 ? 1.5 : 0.4;
		final int waitRandom = Features.ran.nextInt((int) (250 * waitFactor));
		
		final var waitFactorBefore = rounds <= 1 ? 1.5 : 1;
		res[0] = (long) ((1100+4f*waitTimeRandoFactor) * waitFactorBefore);
		for (int i = 1; i < GameMode.raceLightsLength; i++)
			res[i] = res[i - 1] + 190 + waitRandom;
		return res;
	}

	/**
	 * Rewards money and points based on position in just finished race. If place ==
	 * -1, that means the player DNF-ed
	 * 
	 * @param amountOfPlayers
	 * @param player
	 */
	public abstract void rewardPlayer(int round, int place, int amountOfPlayers, int behindBy, long timeBehindFirst, Player player, boolean me);

	/**
	 * Normally the length of the track in meters. But can also be time.
	 * @return
	 */
	public int getRaceGoal() {
		return raceGoal;
	}

	public void resetAllFinished() {
		for (var p : getPlayers().values()) {
			p.newRace();
		}
	}

	/**
	 * is the race ongoing so the scene should be racevisual
	 */
	public boolean isRacing() {
		return racing;
	}

	public void setRacing(boolean racing) {
		this.racing = racing;
	}

	protected int giveNewPrices(UpgradeGeneral upgrade, int i, float priceFactor, Random ran, int premadeOverride) {
		if (upgrade != null) {
			var pricePremade = upgrade.getPremadePrice(); // premade price
			if (pricePremade != 0) {
				if (premadeOverride == 0)
					upgradePrices[i] = pricePremade / priceFactor;
				else
					upgradePrices[i] = premadeOverride / priceFactor;
			} else {
				upgradePrices[i] = Math.round((10 + ran.nextInt(12) * 5) / priceFactor * 100f) / 100f;
			}
			return i + 1;
		}
		return i;
	}

	private final int pricesBuffer = 1;
	
	public void createPricesNoReset(Player player, float priceFactor) {
		upgradePrices = new float[Upgrades.size + pricesBuffer];
		upgradePrices[0] = Math.round(40 / priceFactor * 100f) / 100f;
		int realIndex = pricesBuffer;
		for (int i = realIndex; i < upgradePrices.length; i++) {
			realIndex = giveNewPrices(player.upgrades.getUpgrade(i - pricesBuffer), realIndex, priceFactor, Features.ran, 0);
		}
	}
	public void createPrices(Player player, float priceFactor) {
		player.upgrades.resetPrices();
		createPricesNoReset(player, priceFactor);
	}

	public String getPricesAsString() {
		var res = new StringBuilder(Upgrade.priceFactorStd + Translator.split + upgradePrices.length);

		for (float upgradePrice : upgradePrices) {
			res.append(Translator.split).append(upgradePrice);
		}
		return res.toString();
	}
	
	public void setPricesAsString(Player player, String[] input, AtomicInteger index) {
		Upgrade.priceFactorStd = Float.parseFloat(input[index.getAndIncrement()]);
		float[] prices = new float[Integer.parseInt(input[index.getAndIncrement()])];
		for (int i = 0; i < prices.length; i++) {
			prices[i] = Float.parseFloat(input[index.getAndIncrement()]);
		}
		
		setPrices(player.upgrades, prices);
	}
	
	public void setPrices(Upgrades upgrades) {
		setPrices(upgrades, upgradePrices);
	}
	
	public void setPrices(Upgrades upgrades, float[] prices) {
		this.upgradePrices = prices;
		Upgrade.pricePlacedStd = prices[0];
		
		var refs = upgrades.getUpgradesNonNulls();
		for (int i = 0; i < refs.length; i++) {
			refs[i].setPremadePrice(prices[i + pricesBuffer]);
		}
	}

	public float[] getPrices() {
		return upgradePrices;
	}

	public boolean isCanSwitchBetweenGamemodes() {
		return canSwitchBetweenGamemodes;
	}

	protected abstract void setGeneralInfoDown(String[] input, AtomicInteger index);

	protected abstract void getGeneralInfoDown(StringBuilder sb);

	public void setGeneralInfo(String[] input, AtomicInteger index) {
		countdownSetting = Integer.parseInt(input[index.getAndIncrement()]);
		rounds = Integer.parseInt(input[index.getAndIncrement()]);
		raceGoal = Integer.parseInt(input[index.getAndIncrement()]);
		raceStartedTime = Long.parseLong(input[index.getAndIncrement()]);
//		TODO CHECK THIS, isnt it dangerous?? endGame = Integer.parseInt(input[index.getAndIncrement()]) != 0;
		endGoal = Integer.parseInt(input[index.getAndIncrement()]);

		setGeneralInfoDown(input, index);
		updateInfo();
	}

	public String getGeneralInfo() {
		StringBuilder sb = new StringBuilder();

		sb
		.append(countdownSetting)
		.append(Translator.split)
		.append(rounds)
		.append(Translator.split)
		.append(raceGoal)
		.append(Translator.split)
		.append(raceStartedTime)
//		.append(Translator.split)
//		.append(endGame ? 1 : 0)
		.append(Translator.split)
		.append(endGoal);

		getGeneralInfoDown(sb);

		return sb.toString();
	}

	public String getAllInfo() {
		return
				getGameModeEnum().ordinal() +
				Translator.split +
				getGeneralInfo() +
				Translator.split +
						(isRacing() ? 1 : 0) +
				Translator.split +
				getPricesAsString();
	}

	public void setAllInfo(Player player, String[] input, AtomicInteger index) {
		setGeneralInfo(input, index);
		racing = Integer.parseInt(input[index.getAndIncrement()]) != 0;
		setPricesAsString(player, input, index);
	}


	public UILabel[] getGameModeInformation() {
		if (info == null)
			updateInfo();
		return info;
	}

	public abstract GameModes getGameModeEnum();

	public abstract String getExtraGamemodeRaceInfo();
	
	public int getRound() {
		return rounds;
	}
	
	public void setRound(int round) {
		rounds = round;
	}

    public boolean isDNFWhenCheating() {
		return false;
	}

    public float getLifes(Player player) {
		return player.getCarRep().getInt(Rep.life) + lifes;
	}
    
    public List<Player> getWinners() {
    	return winners;
    }

	public abstract List<Player> addExtraPlayers(CopyOnWriteArrayList<Player> sortedPlayers);

	public Map<Byte, Player> getPlayers() {
		return players;
	}
	
	public Player[] getPlayersArr() {
		return players.values().toArray(new Player[1]);
	}

}

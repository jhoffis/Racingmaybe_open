package game_modes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import communication.Translator;
import engine.ai.AI;

import org.lwjgl.nuklear.Nuklear;

import audio.SfxTypes;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UILabel;
import player_local.upgrades.EmptyTile;
import player_local.upgrades.Tool;
import player_local.upgrades.Upgrade;
import player_local.upgrades.UpgradeGeneral;
import player_local.upgrades.UpgradeType;
import player_local.upgrades.RegVal;
import player_local.upgrades.TileNames;
import player_local.upgrades.RegVal.RegValType;
import main.Features;
import main.Main;
import main.Texts;
import player_local.Bank;
import player_local.Layer;
import player_local.Player;
import player_local.car.Car;
import player_local.car.Rep;
import scenes.game.Race;
import scenes.game.racing_subscenes.RaceVisual;
import scenes.regular.LeaderboardScene;
import settings_and_logging.RSet;

/**
 * - drive so and so fast within these attempts - survive for x number of turns
 * and the beat time linearly decreases by like 1 second every turn - drive so
 * and so far within this time-frame
 * 
 * https://www.youtube.com/shorts/J2Vy05aDKqQ
 */
public class SingleplayerChallengesMode extends GameMode {

    public static final int beginnerTime = 7500, casualTime = 5000, intermediateTime = 4000, hardTime = 3500,
            masterTime = 3000, samuraiTime = 3000, expertTime = 3000, accomplishedTime = 2800, senseiTime = 3000, legendaryTime = 2500, nightmarishTime = 2000, unfairTime = 1000,
            megaunfairTime = 3750, thebossTime = 4000;

    public static int maxMoneyScore = 2000;
    private int challengeLevel, attemptsStandard, attemptsLeft, races, endGoalStandard, raceInc, stdRaceLength = 60,
            stdIncome = 100, loseMoney = 50;
    private long prevTime;
    private float smallGoalTimeToBeatHelpPercentage;
    private boolean incomeInc = true, incomeResetAfterOnce = false, differentLifeSystem = false,
            differentTimeMedal = true;
    public int costChange = 0, priceBase = 50, diedAmount = 0;
    private Player player;
    public boolean spLostRace = false, showTimeChange = true, showTimeMedal = false, randomPrices = false;
    public int prevtimeToBeat, timeToBeat, prevraceGoal;
    private final int maxScoreChallenge = SingleplayerChallenges.TheBoss.ordinal();
    private Random randomizer;
    private float startLifes;
    private float raceIncModifier = 1f;
    public int extraSeed;
    public boolean hasAudio = true;

    public SingleplayerChallengesMode(int challengeLevel) {
        this.challengeLevel = challengeLevel;
        super.canSwitchBetweenGamemodes = false;
        waitTimeRandoFactor = 0;
    }

    public float getAttemptsLeft(Player player) {
        return attemptsLeft;
    }

    public void giveStarterPoints() {
        attemptsLeft = attemptsStandard;
    }

    @Override
    public boolean isGameOver() {
        return getAttemptsLeft(player) <= 0 || (prevTime <= endGoal && prevTime >= 0);
    }

    public void costChange(Player player) {
        for (var up : player.upgrades.getUpgrades()) {
            up.setPremadePrice(up.getPremadePrice() + Math.round(costChange / Upgrade.priceFactorStd * 100f) / 100f);
        }
    }

    @Override
    public void startNewRaceDown() {
        if (costChange != 0 && races > 0 && !spLostRace) {
            costChange(player);
        }

//        if (challengeLevel == 7 && races == 10) {
//            var prevTime = timeToBeat;
//            timeToBeat *= .9;
//            SceneHandler.showMessage("Mohaha! Welcome to round 10.\n" +
//                    "The time to beat has changed by -10%!\n" +
//                    "It was " + Texts.formatNumber(prevTime / 1000d) + "s but now it's " + Texts.formatNumber(timeToBeat / 1000d) + "s!");
//        }
        races++;
    }

    @Override
    public int getRandomRaceType() {
        return 0;
    }

    public int createRaceGoal(int races) {
        return (stdRaceLength + (races * (raceInc)));
    }

    @Override
    public int getNewRaceGoal() {
        return createRaceGoal(races);
    }

    @Override
    public int newEndGoal(int gameLength) {
        timeToBeat = gameLength;
        return timeToBeat;
    }

    @Override
    public int getEndGoalStandard() {
        return endGoalStandard;
    }

    @Override
    protected int giveNewPrices(UpgradeGeneral upgrade, int i, float priceFactor, Random ran, int premadeOverride) {
        if (randomPrices)
            return super.giveNewPrices(upgrade, i, priceFactor, this.randomizer, priceBase);

        upgradePrices[i] = priceBase / priceFactor;
        return i + 1;
    }

    @Override
    public String getEndGoalTextDown() {

        return "Round: " + rounds + "\n" + "Attempts left: " + (int) getAttemptsLeft(player)
                + (startLifes != -1 ? "\nLives left: " + getLifes(player) : "") + "\nBeat "
                + Texts.formatNumber(timeToBeat / 1000.0) + " sec in order to earn full income next turn."
//				+ "\nMinimum penalty time is " + minimumPenaltyTime(timeToBeat) + "."
                + "\nWin the challenge by driving in under " + Texts.formatNumber((float) endGoal / 1000f) + " sec!"
                + "\nTimes you have not made it: " + diedAmount + "\nIncome next turn: $" + cashAdded();
    }

    public static String timeToBeat(int timeToBeat) {
        return String.format("%.3f", timeToBeat / 1000.0).replace(',', '.') + " sec";
    }

    public int minimumPenaltyTimeNum(int timeToBeat) {
        return (int) (timeToBeat - minimumTime(timeToBeat));
    }

    public String minimumPenaltyTime(int timeToBeat) {
        return Texts.formatNumber((timeToBeat - minimumTime(timeToBeat)) / 1000d) + " sec";
    }

    @Override
    public String getName() {
        return Texts.leaderboardScoreName(challengeLevel);
    }

    @Override
    public String getNameFull() {
        var lifes = getLifes(player);
        return "SP " + Texts.leaderboardScoreName(challengeLevel) + (lifes >= 0 ? (", " + lifes + " lifes") : "");
    }

    private int cashAdded() {
        if (challengeLevel == 0) {
            return stdIncome + Math.round(25f * (float) Math.pow(races - 1, 1.5f));
        }
        return stdIncome * (incomeInc ? races : 1); // Maks pott
    }

    @Override
    public void rewardPlayer(int rounds, int place, int amountOfPlayers, int behindBy, long timeBehindFirst,
                             Player player, boolean me) {
        var beforeAttempts = attemptsLeft;
        long playerTime = player.timeLapsedInRace;
        int cashAdded = cashAdded();

        final int minimum = stdIncome;

        prevtimeToBeat = timeToBeat;
        prevTime = playerTime;

        float prevRaceLength = createRaceGoal(races - 1);

        float raceLengthDifference = challengeLevel == 0 ? 5
                : ((getNewRaceGoal() / prevRaceLength) - 1f) * smallGoalTimeToBeatHelpPercentage + 1f;

        if (playerTime >= Race.CHEATED_NOT && playerTime <= timeToBeat) {
            moneyExplaination = "$" + cashAdded;
            raceInc = (int) (raceInc * raceIncModifier);
            if (challengeLevel != 0) {
                double beingFastBonusCash = cashAdded * ((double) endGoal / (double) playerTime);
//				var timeFromBeatMul = Math.floor((timeToBeat - playerTime) / 100d) / 10d;
//				beingFastBonusCash += timeFromBeatMul;
                cashAdded += beingFastBonusCash;
                moneyExplaination += "\n +" + Texts.formatNumber(beingFastBonusCash) + " because time";

                var money = player.bank.getLong(Bank.MONEY);
                cashAdded += money * player.getCarRep().get(Rep.interest);
                moneyExplaination += "\n+ $" + money + " * "
                        + String.format("%.2f", player.getCarRep().get(Rep.interest)).replace(',', '.') + " "
                        + Texts.tags[Rep.interest];

            }
            if (cashAdded < minimum)
                cashAdded = minimum;
            moneyExplaination += "\n| minimum $" + minimum;

            var mpt = player.getCarRep().getInt(Rep.moneyPerTurn);
            cashAdded += mpt;
            moneyExplaination += "\n+ " + mpt + " " + Texts.tags[Rep.moneyPerTurn];

            /*
             * Alt under blir til ved 2000ms -> >100ms 8000ms -> >700ms 18000ms -> >1700ms
             */
            var minimumTime = minimumTime(timeToBeat);
            if (minimumTime < 0)
                minimumTime = 0;

            float cappedPlayerTime = playerTime;
            if (differentTimeMedal || cappedPlayerTime > timeToBeat - minimumTime * .1f) {
                // Diamond
                cappedPlayerTime = timeToBeat - minimumTime * .1f;
            } else if (cappedPlayerTime < timeToBeat - minimumTime) {
                cappedPlayerTime = timeToBeat - minimumTime;
            }

            timeToBeat = (int) (cappedPlayerTime * raceLengthDifference);

            spLostRace = false;

            if (incomeResetAfterOnce && rounds == 0) {
                stdIncome = 0;
                moneyExplaination += "\nIncome set to 0";
            }
        } else {
            diedAmount++;
            if (startLifes != -1) {

                var loss = Math.abs(timeToBeat - playerTime) < 250 ? -.5f : -1f;

//				if (getAttemptsLeft(player) <= 3) {
//					loss = -.5f;
//				}

                if (player.getCarRep().get(Rep.life) > 0) {
                    player.getCarRep().add(Rep.life, loss);
                } else {
                    lifes += loss;
                    if (getLifes(player) <= 0) {
                        attemptsLeft = 0;
                    }
                }
            }

            cashAdded = 5 * rounds + loseMoney;
            if (!differentLifeSystem)
                timeToBeat *= 1.1f * raceLengthDifference;
            else
                timeToBeat += 250;
//			else do nothing

            races--; // ikke lenger bane
            if (hasAudio)
                Features.inst.getAudio().play(SfxTypes.LOSTLIFE);
            spLostRace = true;

            moneyExplaination = "$50\n+ $5 * round";
        }

//		prevraceGoal

        if (!isGameOver()) {
            attemptsLeft--;
            player.bank.set(getAttemptsLeft(player), Bank.POINT);
        }
        player.bank.added[Bank.POINT] = Math.round(getAttemptsLeft(player) - beforeAttempts);
        player.bank.add(cashAdded, Bank.MONEY);
    }

    private float minimumTime(int timeToBeat) {
        return (float) Math.pow(0.1 * timeToBeat - 100, 1.1);
    }

    @Override
    public boolean isGameOverPossible() {
        return false;
    }

    @Override
    protected void setGeneralInfoDown(String[] input, AtomicInteger index) {
        challengeLevel = Integer.parseInt(input[index.getAndIncrement()]);
    }

    @Override
    protected void getGeneralInfoDown(StringBuilder sb) {
        sb.append(Translator.split).append(challengeLevel);
    }

    @Override
    public GameModes getGameModeEnum() {
        return GameModes.SINGLEPLAYER_CHALLENGES;
    }

    @Override
    public boolean isWinner(Player player) {
        // does not use life under Rep because rep lifes are removed before base-lifes
        // Check life eq 0 because if -1 then infinite.
        return !(getAttemptsLeft(player) <= 0 || getLifes(player) == 0);
    }

    @Override
    public String getDeterminedWinnerText(Player player) {
        return isWinner(player) ? "You did it! Well done!#" + UIColors.WON : "You didn't make it!#" + UIColors.DNF;
    }

    @Override
    public String getExtraGamemodeRaceInfo() {
        if (challengeLevel == SingleplayerChallenges.Beginner.ordinal())
            return "Win: " + Texts.formatNumber((float) endGoal / 1000f) + " sec";
        return "(Win: " + Texts.formatNumber((float) endGoal / 1000f) + " sec) Beat: " + timeToBeat(timeToBeat);
    }

    private String scoreAlgo() {
        return "Score algorithm: (10AL+1000TimeMs)*LL";
    }

    public int getCreateScoreNum(Player player) {
        var attemptsLeft = getAttemptsLeft(player);
        long timeScore = 0;

        if (attemptsLeft > 0) {

            var time = player.fastestTimeLapsedInRace;

            if (time <= endGoal) {
                timeScore = endGoal - time;
                timeScore *= 1_000;
            }
            if (timeScore < 0)
                timeScore = 0;
        } else {
            var moneyScore = player.bank.achieved[Bank.MONEY];
            if (moneyScore < 0)
                moneyScore = 0;
            if (moneyScore > .95 * maxMoneyScore)
                moneyScore = .95 * maxMoneyScore;
            timeScore = (long) moneyScore;
        }

        var lifes = getLifes(player);
        var lifePart = startLifes == -1 ? 0d : lifes;

        var survived = attemptsLeft > 0 && lifes != 0;
        int score = (int) (survived ?
                Math.ceil(10_000_000d * attemptsLeft + timeScore + lifePart) :
                timeScore);

        if (challengeLevel <= maxScoreChallenge) {

            // Upload the score to steam
            LeaderboardScene.newScore(survived, SingleplayerChallenges.values()[challengeLevel], score,
                    player.getCarNameID());
            if (survived) {
                if (challengeLevel >= RSet.settings.getInt(RSet.challengesUnlocked.ordinal())) {
                    RSet.set(RSet.challengesUnlocked, challengeLevel + 1);
                }
            }
        } else {
            if (survived) {
                var settingVal = RSet.values()[RSet.challengeDayFun.ordinal() + challengeLevel
                        - (maxScoreChallenge + 1)];
                RSet.set(settingVal, createSeed(settingVal));
            }
        }
        return score;
    }

    public String getCreateScore(Player player) {
        return Texts.formatNumberSimple(getCreateScoreNum(player)) + " SCORE! " + scoreAlgo();
    }

    public static String createSeed(int type) {
        var now = LocalDate.now();

        int num = switch (type) {
            case 0:
                yield now.getDayOfYear() + 365;
            case 1:
                yield (int) Math.floor((float) now.getDayOfYear() / 52f) + 52;
            case 2:
                yield now.getMonthValue();
            default:
                throw new IllegalArgumentException("Unexpected value: " + type);
        };

        return String.valueOf(num) + String.valueOf(now.getYear());
    }

    public static String createSeed(RSet type) {
        return createSeed((int) Math.floor((type.ordinal() - RSet.challengeDayFun.ordinal()) / 3f));
    }

    public int makeSureTimeIsProper(int trackDistance, int time) {
        var oldDebug = Main.DEBUG;
        Main.DEBUG = false;
        var car = new Car();
        var aiTime = AI.calculateRace(car, trackDistance);
        if (aiTime > time - 250) {
            time = (int) (aiTime + 200);
        }
        Main.DEBUG = oldDebug;
        return time;
    }

    /**
     * 0 = days, 1 = weeks, 2 = years
     */
    private Random createRan(int type) {
        var seed = Integer.parseInt(createSeed(type)) + extraSeed;
        var ran = new Random(seed + challengeLevel);
        for (int i = 0; i < ran.nextInt(challengeLevel); i++)
            seed = ran.nextInt();
        return new Random(seed);
    }

    private void unlockedBy(Upgrade up, List<UpgradeGeneral> ups, Random ran) {
        if (!up.isOpenForUse()) {
            byte upId = up.getNameID();
            int open;
            do {
                open = ran.nextInt(TileNames.values().length);
            } while (open >= ups.size() || ups.get(open) == null || !(ups.get(open) instanceof Upgrade)
                    || ((Upgrade) ups.get(open)).getNameID() == upId);
            var opener = (Upgrade) ups.get(open);
            if (!opener.unlocks(up)) {
                opener.getUnlocks().push(upId);
                unlockedBy(opener, ups, ran);
            }
        }
    }

    private void deleteTiles(Random ran) {
        int amount = 0 + ran.nextInt(10);
        player.upgrades.createFuel();
        var ups = player.upgrades.getUpgradesAll();
        for (var upGen : ups) {
            if (upGen instanceof Upgrade up) {
                up.getUnlocks().clear();
                up.setVisible(false);
                if (ran.nextFloat() < .05) {
                    up.setMaxLVL(-1);
                } else {
                    up.setMaxLVL(1 + ran.nextInt(9));
                }
            }
        }
        int tries = 0;
        for (int i = 0; i < amount; i++) {
            int rem;
            do {
                rem = ran.nextInt(TileNames.values().length);
                if (tries > 1000) {
                    rem = 0;
                    break;
                }
                tries++;
            } while (rem >= ups.size() || ups.get(rem) == null || switch (TileNames.values()[rem]) {
                case Gears: // must have gears
                    yield true;
                default:
                    yield false;
            });
            ups.set(rem, null);
        }
//		boolean anyOpen = false;
//		for (var upGen : ups)
//			if (upGen instanceof Upgrade up && up.isOpenForUse()) {
//				anyOpen = true;
//				break;
//			}

//		if (!anyOpen) {
        int amountVisible = 1 + ran.nextInt(3);
        for (int i = 0; i < amountVisible; i++) {
            int open;
            do {
                open = ran.nextInt(TileNames.values().length);
            } while (open >= ups.size() || ups.get(open) == null || !(ups.get(open) instanceof Upgrade));

            ((Upgrade) ups.get(open)).setVisible(true);
        }
//		}

        for (var upGen : ups) {
            // needs unlocks
            if (upGen instanceof Upgrade up) {
                unlockedBy(up, ups, ran);
            }
        }

//			if (ups.get(rem) instanceof Upgrade up) {
//				if (up.isOpenForUse()) {
//					var unlocks = up.getUnlocks();
//					while (unlocks.size() > 0) {
//						if (player.upgrades.getUpgrade(unlocks.pop()) instanceof Upgrade newVisible) {
//							newVisible.setVisible(true);
//						}
//					}
//				}
//				int replace;
//				do {
//					replace = ran.nextInt(TileNames.values().length);
//				} while (replace == rem || !(player.upgrades.getUpgrade(replace) instanceof Upgrade upReplace));
//
//				var unlocks = up.getUnlocks();
//				while (unlocks.size() > 0) {
//					upReplace.pushUpgradeUnlock(TileNames.values()[unlocks.pop()]);
//				}
//			}
//			ups.set(rem, null);
//		}
    }

    private void mixupRegVals(RegVal[] vals, Random ran) {
        for (int i = 0; i < Rep.nosBottles; i++) {
            if (vals[i] == null) {
                // add
                if (ran.nextFloat() <= 0.01) {
                    if (ran.nextFloat() < 0.33)
                        vals[i] = new RegVal(.9d + ran.nextDouble(0.2), RegValType.NormalPercent);
                    else
                        vals[i] = new RegVal(ran.nextInt(50), RegValType.Decimal);
                }
                continue;
            }

            // remove
            if (ran.nextFloat() <= 0.02) {
                vals[i] = null;
                continue;
            }

            if (vals[i].type == RegValType.Decimal && ran.nextFloat() <= .05) {
                vals[i].type = RegValType.NormalPercent;
                vals[i].value = 1.05;
            } else if (vals[i].isPercent() && ran.nextFloat() <= .05) {
                vals[i].type = RegValType.Decimal;
                vals[i].value = 10;
            }

            vals[i].value *= ran.nextDouble(0.9, 1.15);
            if (vals[i].type == RegValType.Decimal && vals[i].value > 1d)
                vals[i].value = (int) vals[i].value;

            if (vals[i].only && ran.nextFloat() <= .05)
                vals[i].only = false;

            if (vals[i].removeAtPlacement && ran.nextFloat() <= .05)
                vals[i].removeAtPlacement = false;
        }
    }

    @Override
    public void updateInfo() {
        if (isGameBegun()) {
            return;
        }

        if (getPlayers() != null) {
            player = (Player) getPlayers().values().toArray()[0];
        } else {
            return;
        }

        String name = null;
        int attemptsStandard = 0;
        int bigGoalTimeToBeat = 0;
        int endGoalStandard = 0;
        float lifes = 0;
        int raceInc = stdRaceLength;
        float timeToBeatChanger = 0f;

        try {
            switch (SingleplayerChallenges.values()[challengeLevel]) {
                case Beginner -> {
                    RaceVisual.ShowHints = true;
                    attemptsStandard = 65;
                    bigGoalTimeToBeat = beginnerTime;
                    endGoalStandard = 500_000;
                    lifes = -1;
                    timeToBeatChanger = 1f;
                    raceInc = 0;
                    stdRaceLength = 120;
                    stdIncome = 75;
                    showTimeChange = false;
                    player.layer.reset();
                    player.upgrades.hasTools = false;
                    super.canSaveMoney = false;
                    player.getCarRep().setBool(Rep.throttleShift, true);
                    var upgrades = player.upgrades.getUpgradesAll();
                    for (int i = 0; i < upgrades.size(); i++) {
                        if (upgrades.get(i) == null)
                            continue;
                        if (upgrades.get(i) instanceof Tool) {
                            upgrades.set(i, null);
                        } else if (upgrades.get(i) instanceof Upgrade up) {
                            switch (player.getCarNameID()) {
                                case 0, 3 -> {
                                    switch (up.getTileName()) {
                                        case MoneyPit, Finance, Aero, Boost, Tireboost, BlueNOS, RedNOS -> {
                                            up.setVisible(false);
                                            up.getUnlocks().clear();
                                        }
                                    }
                                }
                                default -> {
                                    switch (up.getTileName()) {
                                        case MoneyPit, Finance, Aero, Turbo, WeightReduction, Block, LighterPistons, Power -> {
                                            up.setVisible(false);
                                            up.getUnlocks().clear();
                                        }
                                    }
                                }
                            }
                            if (up.getTileName() == TileNames.Gears) {
                                up.getUnlocks().clear();
                            }
                            if (up.getTileName() == TileNames.Block) {
                                up.getNeighbourModifier().values()[Rep.tb] = null;
                            }
                        }
                    }

                    var previewUpgrade = new Upgrade(TileNames.Fuel);
                    previewUpgrade.setUpgradeType(UpgradeType.POWER);
                    previewUpgrade.getRegVals().values()[Rep.kW] = new RegVal(30, RegValType.Decimal);
                    previewUpgrade.getNeighbourModifier().values()[Rep.kW] = new RegVal(5, RegValType.Decimal);
                    previewUpgrade.place(0, 0);
                    previewUpgrade.setVisible(true);
                    player.layer.set(previewUpgrade, 1, 2);
                }
                case Casual -> {
                    attemptsStandard = 60;
                    bigGoalTimeToBeat = casualTime;
                    endGoalStandard = 15000;
                    lifes = -1;
                    timeToBeatChanger = .5f;
                    raceInc = stdRaceLength / 2;
                    stdRaceLength = 80;
                    showTimeChange = false;
                    super.canSaveMoney = false;
                    showTimeMedal = false;
                    differentTimeMedal = true;
                    Translator.setCloneString(player.layer, new Layer(Features.ran, 0, 4, 2, 5, 11, 0));

                    player.upgrades.hasTools = false;

                    var upgrades = player.upgrades.getUpgradesAll();
                    for (int i = 0; i < upgrades.size(); i++) {
                        if (upgrades.get(i) == null)
                            continue;
                        if (upgrades.get(i) instanceof Tool) {
                            upgrades.set(i, null);
                        } else if (upgrades.get(i) instanceof Upgrade up) {
                            switch (up.getTileName()) {
                                case MoneyPit, Finance, Aero -> {
                                    up.setVisible(false);
                                    up.getUnlocks().clear();
                                }
                            }
                        }
                    }
                }
                case Intermediate -> {
                    attemptsStandard = 50;
                    bigGoalTimeToBeat = intermediateTime;
                    endGoalStandard = 12000;
                    showTimeChange = false;
                    lifes = -1;
                    timeToBeatChanger = 0.45f;
                    showTimeMedal = false;
                    differentTimeMedal = true;
                    var upgrades = player.upgrades.getUpgradesAll();
                    upgrades.set(TileNames.Interest.ordinal(), null);

                    Translator.setCloneString(player.layer, new Layer(Features.ran, 0, 4, 2, 6, 10, 1));
                }
                case Hard -> {
                    attemptsStandard = 50;
                    bigGoalTimeToBeat = hardTime;
                    endGoalStandard = 10000;
                    lifes = 15;
                    timeToBeatChanger = 0.42f;
                    priceBase = 40;
                    super.canSaveMoney = true;
                    showTimeMedal = false;
                    differentTimeMedal = true;
                }
                case Harder -> {
                    attemptsStandard = 50;
                    bigGoalTimeToBeat = hardTime;
                    endGoalStandard = 10000;
                    lifes = -1;
                    timeToBeatChanger = 0.42f;
                    priceBase = 10;
                    raceInc = 120;
                    raceIncModifier = 1.05f;
                    super.canSaveMoney = true;
                    showTimeMedal = false;
                    differentTimeMedal = true;
                }
                case Master -> {
                    attemptsStandard = 50;
                    bigGoalTimeToBeat = masterTime;
                    endGoalStandard = 10000;
                    lifes = 12;
//				differentLifeSystem = true;
                    timeToBeatChanger = 0.22f;
                    super.canSaveMoney = true;
                    incomeInc = false;
                    stdIncome = 80;
                    priceBase = 40;
                    stdRaceLength = 45;
                    raceInc = 65;
                    showTimeMedal = false;
                    differentTimeMedal = true;
                    player.layer.reset();

                    for (int x = 0; x < player.layer.getWidth(); x++)
                        for (int y = 0; y < player.layer.getHeight(); y++)
                            player.layer.set(new EmptyTile(), x, y);
                    player.layer.set(null, 2, 2);
                    player.layer.setTimesMod(4, 2, 2);

                    player.layer.setTimesMod(2, 0, 2);
                    player.layer.setTimesMod(2, 1, 1);
                    player.layer.setTimesMod(2, 2, 0);
                    player.layer.setTimesMod(2, 3, 1);
                    player.layer.setTimesMod(2, 4, 2);
                    player.layer.setTimesMod(2, 3, 3);
                    player.layer.setTimesMod(2, 2, 4);
                    player.layer.setTimesMod(2, 1, 3);

                    player.layer.setTimesMod(3, 0, 1);
                    player.layer.setTimesMod(3, 1, 0);
                    player.layer.setTimesMod(3, 3, 0);
                    player.layer.setTimesMod(3, 4, 1);
                    player.layer.setTimesMod(3, 0, 3);
                    player.layer.setTimesMod(3, 1, 4);
                    player.layer.setTimesMod(3, 3, 4);
                    player.layer.setTimesMod(3, 4, 3);

                    player.layer.setTimesMod(6, 0, 0);
                    player.layer.setTimesMod(6, 4, 0);
                    player.layer.setTimesMod(6, 0, 4);
                    player.layer.setTimesMod(6, 4, 4);
                    player.layer.placedUnlockEmptyLVL = 1;

                    var ups = player.upgrades.getUpgradesAll();
                    ups.set(TileNames.Interest.ordinal(), null);

                    Upgrade fin = (Upgrade) ups.get(TileNames.Finance.ordinal());
                    fin.setMinLVL(1);
                    Upgrade money = (Upgrade) ups.get(TileNames.MoneyPit.ordinal());
                    money.pushUpgradeUnlock(TileNames.Power);
                    money.pushUpgradeUnlock(TileNames.Boost);
                    var boost = ups.get(TileNames.Boost.ordinal());
                    boost.setVisible(false);
                    var power = ups.get(TileNames.Power.ordinal());
                    power.setVisible(false);
                }
                case Samurai -> {
                    attemptsStandard = 70;
                    bigGoalTimeToBeat = nightmarishTime;
                    endGoalStandard = 10000;
                    lifes = 10;
                    timeToBeatChanger = 0.3f;
                    raceInc = 80;
                    super.canSaveMoney = true;
                    costChange = 5;
                    incomeInc = false;
                    showTimeMedal = false;
                    differentTimeMedal = true;

                    player.layer.reset();
                    var rotator = new Tool(TileNames.LeftRotator, player.layer);
                    player.layer.set(rotator, 2, 2);
                    rotator.setVisible(true);
                    rotator.sellPrice = -10000;
                    rotator.place(0, 0);

                    player.layer.set(new EmptyTile(), 0, 0);
                    player.layer.set(new EmptyTile(), 4, 0);
                    player.layer.set(new EmptyTile(), 4, 4);
                    player.layer.set(new EmptyTile(), 0, 4);

                    player.layer.setNegTile(TileNames.GrindedGears, 3, 2);
                    player.layer.setNegTile(TileNames.GrindedGears, 1, 2);
                    player.layer.setTimesMod(5, 2, 1);
                    player.layer.setTimesMod(5, 2, 3);
                    player.layer.setTimesMod(5, 1, 2);
                    player.layer.setTimesMod(5, 3, 2);
                }
                case Expert -> {
                    attemptsStandard = 50;
                    bigGoalTimeToBeat = expertTime;
                    endGoalStandard = 9000;
                    lifes = 10;
                    timeToBeatChanger = 0.40f;
                    super.canSaveMoney = true;
                    incomeInc = false;
                    showTimeMedal = false;
                    differentTimeMedal = true;

                    var ups = player.upgrades.getUpgradesAll();
                    for (var up : ups) {
                        if (up instanceof Upgrade u) {
                            u.setVisible(!u.isVisible());
                        }
                    }
                    System.out.println("hei");
                }
                case Accomplished -> {
                    attemptsStandard = 80;
                    bigGoalTimeToBeat = accomplishedTime;
                    endGoalStandard = 16000;
                    lifes = 8;
                    priceBase = 80;
                    timeToBeatChanger = 0.3f;
                    raceInc = 80;
                    stdRaceLength = 80;
                    super.canSaveMoney = true;
                    incomeInc = false;
                    player.layer.reset();
                    player.upgrades.getUpgrade(TileNames.Clutch).first().setVisible(true);
                    player.upgrades.getUpgrade(TileNames.Gears).first().setVisible(true);

                    var ups = player.upgrades.getUpgradesAll();
                    for (var tile : ups) {
                        if (tile instanceof Upgrade up) {
                            up.setMaxLVL(8);
                        }
                    }

                    ((Upgrade) player.upgrades.getUpgrade(TileNames.Power).first()).setMaxLVL(-1);
                    ((Upgrade) player.upgrades.getUpgrade(TileNames.Boost).first()).setMaxLVL(-1);
                    ((Upgrade) player.upgrades.getUpgrade(TileNames.Finance).first()).setMaxLVL(-1);
                    ((Upgrade) player.upgrades.getUpgrade(TileNames.Tireboost).first()).setMaxLVL(-1);

                    player.layer.setNegTile(TileNames.Mattress, 0, 0);
                    player.layer.setNegTile(TileNames.Mattress, 2, 0);
                    player.layer.setNegTile(TileNames.Mattress, 4, 0);
                    player.layer.setNegTile(TileNames.Mattress, 0, 2);
                    player.layer.setNegTile(TileNames.Mattress, 4, 2);
                    player.layer.setNegTile(TileNames.Mattress, 0, 4);
                    player.layer.setNegTile(TileNames.Mattress, 2, 4);
                    player.layer.setNegTile(TileNames.Mattress, 4, 4);

                    player.layer.setTimesMod(.75f, 0, 1);
                    player.layer.setTimesMod(.75f, 0, 3);
                    player.layer.setTimesMod(.75f, 4, 1);
                    player.layer.setTimesMod(.75f, 4, 3);
                    player.layer.setTimesMod(.75f, 1, 0);
                    player.layer.setTimesMod(.75f, 3, 0);
                    player.layer.setTimesMod(.75f, 1, 4);
                    player.layer.setTimesMod(.75f, 3, 4);
                    player.layer.setMoney(80, 2, 1);
                    player.layer.setMoney(80, 1, 2);
                    player.layer.setMoney(80, 2, 3);
                    player.layer.setMoney(80, 3, 2);
                    player.layer.setSale(.919f, 1, 1);
                    player.layer.setSale(.919f, 1, 3);
                    player.layer.setSale(.919f, 3, 1);
                    player.layer.setSale(.919f, 3, 3);
                    
                    player.layer.setTimesMod(8, 2, 2);
                }
                case Sensei -> {
                    attemptsStandard = 40;
                    bigGoalTimeToBeat = accomplishedTime;
                    endGoalStandard = 16000;
                    priceBase = 100;
                    costChange = -5;
                    timeToBeatChanger = 0.3f;
                    raceInc = 100;
                    lifes = -1;
                    stdRaceLength = 60;
                    super.canSaveMoney = true;
                    incomeInc = false;
                    if (player.car.getRep().get(Rep.spdTop) > 140) {
	                    for (int i = 0; i < Rep.nosBottles; i++) {
	                    	if (i != Rep.kg)
	                    		player.car.getRep().div(i, 2d);
	                    }
                    }
                    
                    player.layer.setMoney(0, 2, 2);
                    
                    var clutch = new Upgrade(TileNames.Clutch);
                    player.layer.set(clutch, 2, 2);
                    clutch.setVisible(true);
                    clutch.addToPriceTotal(-100000);
                    clutch.place(0, 0);
            		clutch.setUpgradeType(UpgradeType.ECO);
            		clutch.getRegVals().values()[Rep.spdTop] = new RegVal(1.03, RegValType.NormalPercent);
            		clutch.getNeighbourModifier().values()[Rep.spdTop] = new RegVal(15, RegValType.Decimal);
            		clutch.pushUpgradeUnlock(TileNames.Gears);
            		clutch.bonusCostOverride = 150;

                    var ups = player.upgrades.getUpgradesAll();
                    ups.set(TileNames.Clutch.ordinal(), null);
                    ups.set(TileNames.Gears.ordinal(), null);
                    ups.set(TileNames.Finance.ordinal(), null);
                    ups.set(TileNames.Boost.ordinal(), null);
                    ups.set(TileNames.Power.ordinal(), null);
                    ((Upgrade) player.upgrades.getUpgrade(TileNames.Aero).first()).setVisible(true);
                    ((Upgrade) player.upgrades.getUpgrade(TileNames.MoneyPit).first()).setVisible(true);
                    ((Upgrade) player.upgrades.getUpgrade(TileNames.Tireboost).first()).setVisible(true);
                    ((Upgrade) player.upgrades.getUpgrade(TileNames.BlueNOS).first()).setVisible(true);
                    ((Upgrade) player.upgrades.getUpgrade(TileNames.Block).first()).setVisible(true);
                    ((Upgrade) player.upgrades.getUpgrade(TileNames.Block).first()).requireUpgradeToUnlock(null);
                    for (var up : ups) {
                    	if (!(up instanceof Upgrade upgrade)) continue;
                    	if (up.getUpgradeType() == UpgradeType.POWER) {
                    		upgrade.getNeighbourModifier().values()[Rep.rpmTop] = new RegVal(50, RegValType.Decimal);
                    		continue;
                    	} 
                    	if (up.getUpgradeType() == UpgradeType.BOOST) {
                    		upgrade.getNeighbourModifier().values()[Rep.nosMs] = new RegVal(2.5, RegValType.Decimal);
                    		upgrade.getNeighbourModifier().values()[Rep.tbMs] = new RegVal(2.5, RegValType.Decimal);
                    		continue;
                    	}  
                    }
                }
                case Legendary -> {
                    attemptsStandard = 30;
                    bigGoalTimeToBeat = legendaryTime;
                    endGoalStandard = 8500;
                    lifes = 5;
                    timeToBeatChanger = 0.3f;
                    raceInc = 66;
                    super.canSaveMoney = true;
                    incomeInc = false;
                    costChange = 2;
                    showTimeMedal = false;
                    differentTimeMedal = true;

                    var ups = player.upgrades.getUpgradesAll();
                    ups.set(TileNames.Block.ordinal(), null);
                    ups.set(TileNames.BlueNOS.ordinal(), null);
                    if (player.getCarRep().get(Rep.nosBottles) == 0)
                        ups.set(TileNames.RedNOS.ordinal(), null);

                    var powerUnlocks = ((Upgrade) ups.get(TileNames.Power.ordinal())).getUnlocks();
                    powerUnlocks.clear();
                    powerUnlocks.push((byte) TileNames.Turbo.ordinal());
                    powerUnlocks.push((byte) TileNames.Supercharger.ordinal());
                    powerUnlocks.push((byte) TileNames.WeightReduction.ordinal());
                    powerUnlocks.push((byte) TileNames.LighterPistons.ordinal());
                    var boostUnlocks = ((Upgrade) ups.get(TileNames.Boost.ordinal())).getUnlocks();
                    boostUnlocks.clear();
                    boostUnlocks.push((byte) TileNames.Tireboost.ordinal());
                    if (player.getCarRep().get(Rep.nosBottles) > 0)
                        boostUnlocks.push((byte) TileNames.RedNOS.ordinal());

                    player.layer.reset();
                    for (int y = 0; y < Layer.STD_H; y += (Layer.STD_H - 1)) {
                        for (int x = 0; x < Layer.STD_W; x++) {
                            player.layer.set(new EmptyTile(), x, y);
                            if (x % 2 == 0)
                                player.layer.setTimesMod(2f, x, y);
                            else
                                player.layer.setMoney(120, x, y);
                        }
                    }
                    player.layer.setTimesMod(2, 2, 2);
                    player.layer.setTimesMod(4, 0, 2);
                    player.layer.setTimesMod(4, 4, 2);
                    player.layer.improvementPointsNeededIncrease = 3;
                }
                case Nightmarish -> {
                    attemptsStandard = 25;
                    bigGoalTimeToBeat = nightmarishTime;
                    endGoalStandard = 8500;
                    lifes = 3;
                    timeToBeatChanger = 0.3f;
                    raceInc = 80;
                    super.canSaveMoney = true;
                    costChange = 5;
                    incomeInc = false;
                    showTimeMedal = false;
                    differentTimeMedal = true;

                    player.layer.reset();
                    player.layer.setTimesMod(3, 1, 1);
                    player.layer.setTimesMod(2, 3, 1);
                    player.layer.setTimesMod(2, 1, 3);
                    player.layer.setTimesMod(3, 3, 3);
                    player.layer.setTimesMod(.45f, 0, 0);
                    player.layer.setTimesMod(.45f, 4, 0);
                    player.layer.setTimesMod(.45f, 0, 4);
                    player.layer.setTimesMod(.45f, 4, 4);
                    player.layer.setMoney(15, 2, 2);
                    player.layer.setNegTile(TileNames.Mattress, 0, 2);
                    player.layer.setNegTile(TileNames.PuncturedTire, 4, 2);

                    player.upgrades.resetTowardsCar(player.getCarRep(), player.layer);
                }
                case Unfair -> {
                    attemptsStandard = 40;
                    bigGoalTimeToBeat = unfairTime;
                    endGoalStandard = 9500;
                    stdRaceLength = 80;
                    lifes = -1;
                    timeToBeatChanger = 0.35f;
                    raceInc = 130;
                    super.canSaveMoney = true;
                    priceBase = 60;
                    costChange = 6;
                    incomeInc = false;
                    stdIncome = 140;
                    showTimeMedal = false;
                    differentTimeMedal = true;
                    differentLifeSystem = true;

                    player.layer.reset();
                    player.layer.setMoney(30, 1, 0);
                    player.layer.setMoney(30, 3, 4);
                    player.layer.set(new EmptyTile(), 0, 4);
                    player.layer.set(new EmptyTile(), 4, 0);
                    player.layer.set(new EmptyTile(), 1, 1);
                    player.layer.set(new EmptyTile(), 1, 2);
                    player.layer.set(new EmptyTile(), 1, 3);
                    player.layer.set(new EmptyTile(), 1, 4);
                    player.layer.set(new EmptyTile(), 3, 0);
                    player.layer.set(new EmptyTile(), 3, 1);
                    player.layer.set(new EmptyTile(), 3, 2);
                    player.layer.set(new EmptyTile(), 3, 3);
                    player.layer.setTimesMod(4, 2, 2);
                    player.layer.setTimesMod(2, 0, 2);
                    player.layer.setTimesMod(2, 4, 2);
                    player.layer.improvementPointsNeededIncrease = 3;

                    player.upgrades.resetTowardsCar(player.getCarRep(), player.layer);
                    var ups = player.upgrades.getUpgradesAll();

                    ups.set(TileNames.Interest.ordinal(), null);
                    for (var tile : ups) {
                        if (tile instanceof Upgrade up) {
                            up.getRegVals().multiplyAllValues(0.80);
                            up.getRegVals().multiplyAllValues(up.getRegVals().changeAfterUpgrade, 0.80, true);
                            up.getNeighbourModifier().multiplyAllValues(0.80);
                            var vals = up.getNeighbourModifier().values();
                            if (vals[Rep.moneyPerTurn] == null) {
                                vals[Rep.moneyPerTurn] = new RegVal(-1, RegValType.Decimal);
                            }
                            if (up.bonusCostOverride != 0)
                                up.bonusCostOverride += 50;
                            else
                                up.bonusCostOverride = 100;
                            up.bonusGainOverride = 0;
                        }
                    }
                }
                case Unfaircore -> {
                    attemptsStandard = 35;
                    bigGoalTimeToBeat = megaunfairTime;
                    endGoalStandard = 9500;
                    stdRaceLength = 80;
                    lifes = -1;
                    timeToBeatChanger = 0.344f;
                    raceInc = 70;
                    raceIncModifier = 1.1f;
                    super.canSaveMoney = true;
                    priceBase = 30;
                    costChange = 15;
                    incomeInc = false;
                    stdIncome = 100;
                    showTimeMedal = false;
                    differentTimeMedal = true;
                    differentLifeSystem = true;
                    loseMoney = 0;

                    player.layer.reset();
                    player.layer.set(new EmptyTile(), 0, 0);
                    player.layer.set(new EmptyTile(), 1, 0);
                    player.layer.set(new EmptyTile(), 2, 0);
                    player.layer.set(new EmptyTile(), 3, 0);
                    player.layer.set(new EmptyTile(), 4, 0);
                    player.layer.set(new EmptyTile(), 0, 1);
                    player.layer.set(new EmptyTile(), 4, 1);
                    player.layer.set(new EmptyTile(), 0, 2);
                    player.layer.set(new EmptyTile(), 4, 2);
                    player.layer.set(new EmptyTile(), 0, 3);
                    player.layer.set(new EmptyTile(), 4, 3);
                    player.layer.set(new EmptyTile(), 0, 4);
                    player.layer.set(new EmptyTile(), 1, 4);
                    player.layer.set(new EmptyTile(), 2, 4);
                    player.layer.set(new EmptyTile(), 3, 4);
                    player.layer.set(new EmptyTile(), 4, 4);

                    player.layer.setSale(0.9f, 3, 1);

                    var neg = player.layer.setNegTile(TileNames.BirdsNest, 2, 2);
                    neg.getNeighbourModifier().values()[Rep.moneyPerTurn] = new RegVal(2.5, RegValType.Decimal);
                    neg.addToPriceTotal(-150);

                    Tool.improvementPointsNeeded = 30;
                    player.layer.improvementPointsNeededIncrease = 10;
                    player.layer.placedUnlockEmptyLVL = 6;
                    player.layer.placedUnlockEmptyLVLIncrease = 2;
                    player.layer.setTimesMod(2f, 1, 1);
                }
                case TheBoss -> {
                    attemptsStandard = 20;
                    bigGoalTimeToBeat = thebossTime;
                    endGoalStandard = 9500;
                    stdRaceLength = 60;
                    lifes = -1;
                    timeToBeatChanger = 0.344f;
                    raceInc = 70;
                    raceIncModifier = 1.15f;
                    super.canSaveMoney = true;
                    priceBase = 20;
                    costChange = 20;
                    incomeInc = false;
                    stdIncome = 100;
                    showTimeMedal = false;
                    differentTimeMedal = true;
                    differentLifeSystem = true;
                    loseMoney = 0;

                    player.layer.reset();
                    player.layer.placedUnlockEmptyLVL = 4;
                    player.layer.placedUnlockEmptyLVLIncrease = 5;
                    var rotator = new Tool(TileNames.NeighborCollector, player.layer);
                    player.layer.set(rotator, 0, 4);
                    rotator.setVisible(true);
                    rotator.sellPrice = -10000;
                    rotator.place(0, 0);

                    player.layer.set(new EmptyTile(), 0, 0);
                    player.layer.set(new EmptyTile(), 0, 1);
                    player.layer.set(new EmptyTile(), 0, 2);
                    player.layer.set(new EmptyTile(), 1, 3);
                    player.layer.set(new EmptyTile(), 2, 4);
                    player.layer.set(new EmptyTile(), 3, 4);
                    player.layer.set(new EmptyTile(), 4, 4);
                    player.layer.set(new EmptyTile(), 3, 1);


                    var neg = player.layer.setNegTile(TileNames.BirdsNest, 0, 3);
//                    neg.getNeighbourModifier().values()[Rep.kW] = new RegVal(.975, RegValType.NormalPercent);
                    neg.addToPriceTotal(-350);

                    neg = player.layer.setNegTile(TileNames.GrindedGears, 1, 4);
//                    neg.getNeighbourModifier().values()[Rep.spdTop] = new RegVal(.975, RegValType.NormalPercent);
                    neg.addToPriceTotal(-350);

                    player.layer.setTimesMod(.45f, 2, 0);
                    player.layer.setTimesMod(.45f, 3, 0);
                    player.layer.setTimesMod(.45f, 1, 1);
                    player.layer.setTimesMod(.45f, 2, 1);
                    player.layer.setTimesMod(.45f, 3, 1);
                    player.layer.setTimesMod(.45f, 4, 1);
                    player.layer.setTimesMod(.45f, 2, 2);
                    player.layer.setTimesMod(.45f, 3, 2);
                    player.layer.setTimesMod(.45f, 4, 2);
                    player.layer.setTimesMod(.45f, 3, 3);


                    player.layer.setTimesMod(2, 1, 0);
                    player.layer.setTimesMod(4, 4, 0);
                    player.layer.setTimesMod(2, 4, 3);
                    player.layer.setTimesMod(2, 2, 3);
                    player.layer.setTimesMod(2, 1, 2);
                }
                /*
                 * Day challenges
                 */
                case DailyFun -> {
                    var ran = createRan(0);
                    lifes = -1;
                    attemptsStandard = 40;
                    bigGoalTimeToBeat = 2500 + ran.nextInt(1000);
                    endGoalStandard = 9000;
                    timeToBeatChanger = 0.55f;
                    super.canSaveMoney = true;
                    stdRaceLength = 60;
                    raceInc = stdRaceLength;
                    priceBase = 40 + ran.nextInt(30);
                    costChange = 0;
                    incomeInc = true;
                    incomeResetAfterOnce = false;
                    stdIncome = 100 + ran.nextInt(30);
                    showTimeMedal = false;

                    endGoalStandard = makeSureTimeIsProper(stdRaceLength, endGoalStandard);

                    randomPrices = ran.nextFloat() > 0.8f;
                    randomizer = ran;

                    Translator.setCloneString(player.layer,
                            new Layer(ran, ran.nextInt(300) + 50, ran.nextInt(3), 3, ran.nextInt(30), 30, ran.nextInt(4)));

                }
                case DailyWeird -> {
                    var ran = createRan(0);
                    lifes = ran.nextInt(8) + 2;
                    attemptsStandard = ran.nextInt(35) + 10;
                    bigGoalTimeToBeat = 1200 + ran.nextInt(4000);
                    endGoalStandard = 6000 + ran.nextInt(5000);
                    timeToBeatChanger = 0.25f + ran.nextFloat(0.3f);
                    super.canSaveMoney = ran.nextBoolean();
                    raceInc = 40 + ran.nextInt(100);
                    stdRaceLength = 40 + ran.nextInt(100);

                    endGoalStandard = makeSureTimeIsProper(stdRaceLength, endGoalStandard);

                    priceBase = 20 + ran.nextInt(100);
                    costChange = ran.nextInt(5);
                    incomeInc = ran.nextBoolean();
                    incomeResetAfterOnce = ran.nextFloat(1f) > 0.8f;
                    stdIncome = 50 + ran.nextInt(100);
                    showTimeMedal = true;

                    randomPrices = true;
                    randomizer = ran;
                    deleteTiles(ran);
                    Translator.setCloneString(player.layer, new Layer(ran, ran.nextInt(120), ran.nextInt(6) + 3,
                            ran.nextInt(2), ran.nextInt(20), 20, ran.nextInt(4)));
                    if (player.upgrades.getUpgrade(TileNames.Block).first() instanceof Upgrade up)
                        up.requireUpgradeToUnlock(null);

                    for (var storeTile : player.upgrades.getUpgradesAll()) {
                        if (storeTile instanceof Upgrade up) {
                            mixupRegVals(up.getRegVals().values(), ran);
                            mixupRegVals(up.getNeighbourModifier().values(), ran);
                        }
                    }
                }
                case DailyTough -> {
                    var ran = createRan(0);
                    lifes = 5 + ran.nextInt(2);
                    attemptsStandard = ran.nextInt(25) + 20;
                    bigGoalTimeToBeat = 500 + ran.nextInt(1000);
                    endGoalStandard = 0;
                    timeToBeatChanger = 0.35f - ran.nextFloat(0.1f);
                    super.canSaveMoney = true;
                    stdRaceLength = 80 + ran.nextInt(20);
                    raceInc = 40 + ran.nextInt(70);
                    priceBase = 50 + ran.nextInt(20);
                    costChange = ran.nextInt(10);
                    incomeInc = true;
                    incomeResetAfterOnce = ran.nextBoolean();
                    stdIncome = 100 + ran.nextInt(50);
                    showTimeMedal = true;
                    endGoalStandard = makeSureTimeIsProper(stdRaceLength, endGoalStandard);
                    randomPrices = ran.nextFloat() > 0.5f;
                    randomizer = ran;
                    Translator.setCloneString(player.layer, new Layer(ran, ran.nextInt(80) + 50, ran.nextInt(9) + 4,
                            ran.nextInt(3), ran.nextInt(6), 5, ran.nextInt(4)));
                }
                /*
                 * Weekly challenges
                 */
                case WeeklyFun -> {
                    var ran = createRan(1);
                    lifes = -1;
                    attemptsStandard = 40;
                    bigGoalTimeToBeat = 1500 + ran.nextInt(1000);
                    endGoalStandard = 9000;
                    timeToBeatChanger = 0.55f;
                    super.canSaveMoney = true;
                    stdRaceLength = 60;
                    raceInc = stdRaceLength;
                    priceBase = 40 + ran.nextInt(30);
                    costChange = 0;
                    incomeInc = true;
                    incomeResetAfterOnce = false;
                    stdIncome = 100 + ran.nextInt(60);
                    showTimeMedal = false;
                    endGoalStandard = makeSureTimeIsProper(stdRaceLength, endGoalStandard);
                    randomPrices = ran.nextFloat() > 0.8f;
                    randomizer = ran;
                    Translator.setCloneString(player.layer,
                            new Layer(ran, ran.nextInt(300) + 50, ran.nextInt(3), 3, ran.nextInt(30), 30, ran.nextInt(4)));

                }
                case WeeklyWeird -> {
                    var ran = createRan(1);
                    lifes = ran.nextInt(8) + 2;
                    attemptsStandard = ran.nextInt(35) + 10;
                    bigGoalTimeToBeat = 1200 + ran.nextInt(4000);
                    endGoalStandard = 6000 + ran.nextInt(5000);
                    timeToBeatChanger = 0.25f + ran.nextFloat(0.3f);
                    super.canSaveMoney = ran.nextBoolean();
                    raceInc = 40 + ran.nextInt(100);
                    stdRaceLength = 40 + ran.nextInt(100);
                    priceBase = 20 + ran.nextInt(100);
                    costChange = ran.nextInt(5);
                    incomeInc = ran.nextBoolean();
                    incomeResetAfterOnce = ran.nextFloat(1f) > 0.8f;
                    stdIncome = 50 + ran.nextInt(100);
                    showTimeMedal = true;
                    endGoalStandard = makeSureTimeIsProper(stdRaceLength, endGoalStandard);
                    randomPrices = true;
                    randomizer = ran;
                    deleteTiles(ran);

                    Translator.setCloneString(player.layer, new Layer(ran, ran.nextInt(120), ran.nextInt(6) + 3,
                            ran.nextInt(2), ran.nextInt(20), 20, ran.nextInt(4)));
                    if (player.upgrades.getUpgrade(TileNames.Block).first() instanceof Upgrade up)
                        up.requireUpgradeToUnlock(null);
                    for (var storeTile : player.upgrades.getUpgradesAll()) {
                        if (storeTile instanceof Upgrade up) {
                            mixupRegVals(up.getRegVals().values(), ran);
                            mixupRegVals(up.getNeighbourModifier().values(), ran);
                        }
                    }
                }
                case WeeklyTough -> {
                    var ran = createRan(1);
                    lifes = 5 + ran.nextInt(2);
                    attemptsStandard = ran.nextInt(25) + 20;
                    bigGoalTimeToBeat = 500 + ran.nextInt(1000);
                    endGoalStandard = 0;
                    timeToBeatChanger = 0.35f - ran.nextFloat(0.1f);
                    super.canSaveMoney = true;
                    stdRaceLength = 80 + ran.nextInt(20);
                    raceInc = 40 + ran.nextInt(70);
                    priceBase = 50 + ran.nextInt(20);
                    costChange = ran.nextInt(10);
                    incomeInc = true;
                    incomeResetAfterOnce = ran.nextBoolean();
                    stdIncome = 100 + ran.nextInt(50);
                    showTimeMedal = true;
                    endGoalStandard = makeSureTimeIsProper(stdRaceLength, endGoalStandard);
                    randomPrices = ran.nextFloat() > 0.5f;
                    randomizer = ran;
                    Translator.setCloneString(player.layer, new Layer(ran, ran.nextInt(80) + 50, ran.nextInt(6) + 8,
                            ran.nextInt(3), ran.nextInt(6), 5, ran.nextInt(4)));
                }
                /*
                 * Monthly challenges
                 */
                case MonthlyFun -> {
                    var ran = createRan(2);
                    lifes = -1;
                    attemptsStandard = 40;
                    bigGoalTimeToBeat = 500 + ran.nextInt(3000);
                    endGoalStandard = 9000;
                    timeToBeatChanger = 0.55f;
                    super.canSaveMoney = true;
                    stdRaceLength = 60;
                    raceInc = stdRaceLength;
                    priceBase = 40 + ran.nextInt(30);
                    costChange = 0;
                    incomeInc = true;
                    incomeResetAfterOnce = false;
                    stdIncome = 100 + ran.nextInt(60);
                    showTimeMedal = false;
                    endGoalStandard = makeSureTimeIsProper(stdRaceLength, endGoalStandard);
                    randomPrices = ran.nextFloat() > 0.8f;
                    randomizer = ran;
                    Translator.setCloneString(player.layer,
                            new Layer(ran, ran.nextInt(300) + 50, ran.nextInt(3), 3, ran.nextInt(30), 30, ran.nextInt(4)));

                }
                case MonthlyWeird -> {
                    var ran = createRan(2);
                    lifes = ran.nextInt(8) + 2;
                    attemptsStandard = ran.nextInt(35) + 10;
                    bigGoalTimeToBeat = 1200 + ran.nextInt(4000);
                    endGoalStandard = 6000 + ran.nextInt(5000);
                    timeToBeatChanger = 0.25f + ran.nextFloat(0.3f);
                    super.canSaveMoney = ran.nextBoolean();
                    raceInc = 40 + ran.nextInt(100);
                    stdRaceLength = 40 + ran.nextInt(100);
                    priceBase = 20 + ran.nextInt(100);
                    costChange = ran.nextInt(5);
                    incomeInc = ran.nextBoolean();
                    incomeResetAfterOnce = ran.nextFloat(1f) > 0.8f;
                    stdIncome = 50 + ran.nextInt(100);
                    showTimeMedal = true;
                    endGoalStandard = makeSureTimeIsProper(stdRaceLength, endGoalStandard);
                    randomPrices = true;
                    randomizer = ran;
                    deleteTiles(ran);
                    Translator.setCloneString(player.layer, new Layer(ran, ran.nextInt(120), ran.nextInt(6) + 3,
                            ran.nextInt(2), ran.nextInt(20), 20, ran.nextInt(4)));
                    if (player.upgrades.getUpgrade(TileNames.Block).first() instanceof Upgrade up)
                        up.requireUpgradeToUnlock(null);
                    for (var storeTile : player.upgrades.getUpgradesAll()) {
                        if (storeTile instanceof Upgrade up) {
                            mixupRegVals(up.getRegVals().values(), ran);
                            mixupRegVals(up.getNeighbourModifier().values(), ran);
                        }
                    }
                }
                case MonthlyTough -> {
                    var ran = createRan(2);
                    lifes = 5 + ran.nextInt(2);
                    attemptsStandard = ran.nextInt(25) + 20;
                    bigGoalTimeToBeat = 500 + ran.nextInt(1000);
                    endGoalStandard = 0;
                    timeToBeatChanger = 0.35f - ran.nextFloat(0.1f);
                    super.canSaveMoney = true;
                    stdRaceLength = 80 + ran.nextInt(20);
                    raceInc = 40 + ran.nextInt(70);
                    priceBase = 50 + ran.nextInt(20);
                    costChange = ran.nextInt(10);
                    incomeInc = true;
                    incomeResetAfterOnce = ran.nextBoolean();
                    stdIncome = 100 + ran.nextInt(50);
                    showTimeMedal = true;
                    endGoalStandard = makeSureTimeIsProper(stdRaceLength, endGoalStandard);
                    randomPrices = ran.nextFloat() > 0.5f;
                    randomizer = ran;
                    Translator.setCloneString(player.layer, new Layer(ran, ran.nextInt(80) + 50, ran.nextInt(6) + 8,
                            ran.nextInt(3), ran.nextInt(6), 5, ran.nextInt(4)));
                }
                case Sandbox -> {
                    attemptsStandard = Integer.MAX_VALUE;
                    bigGoalTimeToBeat = 1;
                    endGoalStandard = 1;
                    timeToBeatChanger = 1f;
                    priceBase = 0;
                    super.canSaveMoney = true;
                    showTimeMedal = false;
                    differentTimeMedal = true;
                    stdRaceLength = Integer.MAX_VALUE;
                    lifes = -1;
                    raceInc = 0;
                    stdIncome = 1000000;
                    loseMoney = 1000000;
                    Tool.improvementPointsNeeded = 0;
                    player.layer.improvementPointsNeededIncrease = 0;
                    player.layer.placedUnlockEmptyLVL = 0;
                    player.layer.reset();
                    player.upgrades.hasTools = false;
                    player.layer.setTimesMod(6f, 1, 1);
                    player.ultimateUndo = true;
                    Layer.minTimesMod = 1f;
                    if (Main.DEBUG)
                        player.layer.set(new EmptyTile(), 4, 0);
                }
                default -> throw new Exception("wrong gamemode type in singleplayer");
            }
            name = Texts.leaderboardScoreName(challengeLevel);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.raceInc = raceInc;
        this.attemptsStandard = attemptsStandard;
        this.endGoal = bigGoalTimeToBeat;
        this.endGoalStandard = endGoalStandard;
        this.lifes = lifes;
        this.startLifes = lifes;
        this.smallGoalTimeToBeatHelpPercentage = timeToBeatChanger;

        giveStarterPoints();

        player.canUndoHistory = Integer.MAX_VALUE;

        if (!player.upgrades.getUpgrades().isEmpty()) {
            createPrices(player, Upgrade.priceFactorStd);
            setPrices(player.upgrades);
        }

        UILabel title = new UILabel("'" + name + "'", Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE);

        String winTime = Texts.formatNumber((float) bigGoalTimeToBeat / 1000f);
        String info = "=> To win; race under " + winTime + " second" + (bigGoalTimeToBeat != 1000 ? "s" : "")
                + " before running out of attempts!#" + UIColors.BUR + "\n"
                + "=> 1 attempt is always lost per race. At 0 attempts " + (lifes != -1 ? "or lives " : "")
                + "you lose.#" + UIColors.DNF + "\n" + "=> You start with " + attemptsStandard + " attempts"
                + (lifes != -1 ? " and " + Texts.formatNumber(lifes) + " " + (lifes != 1 ? "lives" : "life") : "")
                + ".#" + UIColors.STRONGER_BONUSGOLD2 + "\n"
                + (challengeLevel != SingleplayerChallenges.Beginner.ordinal() ? "=> To receive full income; beat "
                + getNewRaceGoal() + " meters under " + Texts.formatNumber(endGoalStandard / 1000d)
                + " seconds:#" + UIColors.STRONGER_BONUSGOLD0 + "\n"
                + "   - The distance increases each round. You get better, but, so does the road.#"
                + UIColors.STRONGER_BONUSGOLD0 + "\n"
                + "   - Time next turn is determined based on how close you get to the beat-time.#"
                + UIColors.STRONGER_BONUSGOLD0 + "\n"
                + "   - Additional income can be achieved through finishing close to win-time, upgrades, and interest on your moneybag.#"
                + UIColors.STRONGER_BONUSGOLD0 + "\n" : "")
                + "=> A score will then be set based on your performance:#" + UIColors.LGRAY
                + "\n   10,000,000 score per attempts left + 1000 score per millisecond under the win-time (" + winTime
                + "s)";
        if (lifes > 0)
            info += "\nFinally, score is shrunk by lives lost, meaning if you lose half your starting lives you lose half your total score.";

        var infoList = new ArrayList<UILabel>();
        infoList.add(title);
        Collections.addAll(infoList, UILabel.create(info.split("\n")));

        super.info = infoList.toArray(new UILabel[0]);
    }

    public int getChallengeLevel() {
        return challengeLevel;
    }

    public boolean isScoreChallenge() {
        return challengeLevel <= maxScoreChallenge;
    }

    @Override
    public List<Player> addExtraPlayers(CopyOnWriteArrayList<Player> sortedPlayers) {
//		var ghost = new Player();
//		ghost.car.switchTo(sortedPlayers.get(0).car.getRep().getNameID(), false);
//		ghost.car.getModel().ai = true;
//		ghost.car.getModel().timeGoal = timeToBeat;
//		sortedPlayers.add(ghost);
        return sortedPlayers;
    }

    public void setAttemptsLeft(int attemptsLeft) {
        this.attemptsLeft = attemptsLeft;
    }


}


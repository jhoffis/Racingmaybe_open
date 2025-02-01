package player_local.car;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import adt.ICloneStringable;
import communication.Translator;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UILabel;
import player_local.upgrades.RegVals;
import main.Texts;
import player_local.upgrades.Upgrades;

/**
 * Used to store stats about Car and more easily communicate Cars with the
 * server. Also used to restore a players car after they have lost connection
 * and rejoined.
 *
 * @author jonah
 */

public class Rep implements ICloneStringable {
    private int nameId = -1;

    private double[] stats;
    private boolean random;

    private static int statsIndex = 0;
    public static final int
            kW = statsIndex++,
            kg = statsIndex++,
            bar = statsIndex++,
            spdTop = statsIndex++,
            rpmTop = statsIndex++,
            rpmIdle = statsIndex++,
            turboblow = statsIndex++,
            nos = statsIndex++,
            nosMs = statsIndex++,
            tb = statsIndex++,
            tbHeat = statsIndex++,
            tbMs = statsIndex++,
            spool = statsIndex++,
            spoolStart = statsIndex++,
            aero = statsIndex++, // removes wind from the equation
            turboblowRegen = statsIndex++,
            turboblowStrength = statsIndex++,
            moneyPerTurn = statsIndex++,
            interest = statsIndex++,
//            cooling = statsIndex++,
    /*
     *  Ikke mulig å multiplye herifra:
     */
            nosBottles = statsIndex++, // må være øverst her
            spdTopBase = statsIndex++,
            rpmBaseIdle = statsIndex++,
            rpmBaseTop = statsIndex++,
            snos = statsIndex++,
            life = statsIndex++,
            gearTop = statsIndex++,
            nosAuto = statsIndex++,
            nosPush = statsIndex++,
            tbArea = statsIndex++,
            stickyclutch = statsIndex++,
            sequential = statsIndex++,
            nosSoundbarrier = statsIndex++,
            manualClutch = statsIndex++,
            twoStep = statsIndex++,
            throttleShift = statsIndex++,
    //	powerShift = statsIndex++,
    highestSpdAchived = statsIndex++
//	sale = statsIndex++,
//	freeUpgrade = statsIndex++,
            ;

    public static int size() {
        return statsIndex;
    }

    public Rep(int nameid, int nosTimeStandard, int nosBottleAmountStandard, double nosStrengthStandard, double kW,
               double weight, double speedTop, int rpmIdle, int rpmTop, int gearTop, int tireGripTimeStandard,
               double tireGripStrengthStandard, double tireGripArea, double bar, boolean clutchShift, boolean throttleShift, boolean sequential, boolean random,
               double turboblowStrength) {

        init();

        this.nameId = nameid;
        this.random = random;

        set(Rep.nosMs, nosTimeStandard);
        set(Rep.nosBottles, nosBottleAmountStandard);
        set(Rep.nos, nosStrengthStandard);
        set(Rep.kW, kW);
        set(Rep.kg, weight);
        set(Rep.spdTop, speedTop);
        set(Rep.spdTopBase, speedTop * 2f);
        set(Rep.rpmIdle, rpmIdle);
        set(Rep.rpmBaseIdle, rpmIdle);
        set(Rep.rpmTop, rpmTop);
        set(Rep.rpmBaseTop, rpmTop);
        set(Rep.gearTop, gearTop);
        set(Rep.tbMs, tireGripTimeStandard);
        set(Rep.tb, tireGripStrengthStandard);
        set(Rep.tbArea, tireGripArea);
        set(Rep.turboblow, 100);
        set(Rep.turboblowRegen, 25);
        set(Rep.turboblowStrength, turboblowStrength);
        set(Rep.bar, bar);
        set(Rep.spool, 1.0);
        set(Rep.aero, 1);
        set(Rep.interest, .2);
        setBool(Rep.manualClutch, clutchShift);
        setBool(Rep.throttleShift, throttleShift);
        setBool(Rep.sequential, sequential);
    }

    public Rep() {
        init();
    }

    private void init() {
        stats = new double[size()];
    }



    /*
     * Upgrade ids
     */
//		String[] toConvertIds = values[fromFromIndex + fromIndex].split(UPGRADELVL_REGEX);
//		for (int x = 0; x < upgrades.length; x++) {
//			for (int y = 0; y < upgrades[x].length; y++ ) {
//				int possibleId = Integer.parseInt(toConvertIds[x + y]);
//				if ((upgrades[x][y] == null && possibleId != -1) || upgrades[x][y].compareId(possibleId)) {
////					TODO upgrades[x][y] = Upgrades.instantiateTile(possibleId);
//				}
//			}
//		}
//
//		/*
//		 * Bonus lvl
//		 */
//		String[] toConvertBonusesX = values[fromFromIndex + fromIndex].split(UPGRADELVL_REGEX);
//		for (int x = 0; x < toConvertBonusesX.length; x++) {
//			String[] toConvertBonusesXY = toConvertBonusesX[x].split(";");
//			for (int y = 0; y < upgrades.length; y++) {
//				upgrades[x][y].setBonusLVL(Integer.parseInt(toConvertBonusesXY[y].trim()));
//			}
//		}
//		fromFromIndex++;
//
//		/*
//		 * Upgrade lvls
//		 */
//		String[] toConvertLVLs = values[fromFromIndex + fromIndex].split(UPGRADELVL_REGEX);
//		for (int x = 0; x < upgrades.length; x++) {
//			for (int y = 0; y < upgrades[x].length; y++ ) {
//				upgrades[x][y].setLVL(Integer.parseInt(toConvertLVLs[x + y].trim()));
//			}
//		}
//		fromFromIndex++;
//		/*
//		 * Bank
//		 */
//		bank.setAsString(values[fromFromIndex + fromIndex], UPGRADELVL_REGEX);
//		fromFromIndex++;
    /*
     * Carstats
     */

    @Override
    public void getCloneString(StringBuilder outString, int lvlDeep, String splitter, boolean test) {
        if (lvlDeep > 0)
            outString.append(splitter);
        lvlDeep++;
        outString.append("Car").append(splitter).append(random ? -nameId - 1 : nameId);
        for (int i = 0; i < stats.length; i++) {

            if (Double.isInfinite(stats[i])) {
                stats[i] = Math.signum(stats[i]) * Double.MAX_VALUE;
            }

            outString
                    .append(splitter)
                    .append(stats[i]);
        }
        outString.append(splitter).append(0);
    }

    @Override
    public void setCloneString(String[] cloneString, AtomicInteger fromIndex) {
        fromIndex.getAndIncrement();
        /*
         * Names
         */
        var nameId = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]);
        random = nameId < 0;
        if (random)
            nameId = -nameId - 1;

        if (nameId >= Texts.CAR_TYPES.length)
            nameId = Texts.CAR_TYPES.length - 1;

        this.nameId = nameId;

        String clonedVal;
        int i = 0;
        while (cloneString.length > fromIndex.get() && !(clonedVal = cloneString[fromIndex.getAndIncrement()]).equalsIgnoreCase(Upgrades.indicator)) {
            if (i < stats.length) {

                double newValue;
                try {
                    newValue = Double.parseDouble(clonedVal);
                    stats[i] = newValue;
                } catch (NumberFormatException ignored) {
                }
            }
            i++;
        }
        fromIndex.decrementAndGet();
    }

    public Rep getClone() {
        var clone = new Rep();
        StringBuilder sb = new StringBuilder();
        getCloneString(sb, 0, Translator.split, true);
        clone.setCloneString(sb.toString().split(Translator.split), new AtomicInteger(0));
        return clone;
    }

    public double getScorePower() {
        return getTotalKW() / get(kg) 
        		+ Math.max(0, (get(Rep.turboblow) / 100d) 
        		+ (get(Rep.turboblowStrength) / 1000d));
    }

    public double getScoreSpeed() {
        return getInt(spdTop);
    }

    public double getScoreNos() {
        return get(nos) * get(nosBottles) * get(nosMs) / 1000d;
    }

    public double getScoreTb() {
        return Car.funcs.maxTb(this) * get(tbMs) / 1000d;
    }

    public double getScoreMoney() {
        return getInt(Rep.moneyPerTurn);
    }

    public String[] getInfo() {
        return new String[]{
                Texts.formatNumber(getScorePower()),
                Texts.formatNumber(getScoreNos()),
                Texts.formatNumber(getScoreTb()),
                Texts.formatNumber(getScoreSpeed()) + " " + Texts.tags[Rep.spdTop],
                "+$" + Texts.formatNumber(getScoreMoney())
        };
    }

    public String getInfoDiff(Rep rep) {
        return calcDiff(getTotalKW(), rep.getTotalKW())
                + calcDiff(rep, kg)
                + calcDiff(rep, spdTop)
                + calcDiff(rep, nos)
                + calcDiff(rep, tb, true);
    }

    private String calcDiff(Rep rep, int i) {
        var res = calcDiff(get(i), rep.get(i));
        //Special case
        if (i == kg && res.length() > 2) {
            int last = res.length() - 2;
            byte[] chars = res.getBytes();
            if (chars[last] == 'G')
                chars[last] = 'R';
            else
                chars[last] = 'G';

            res = new String(chars);
        }
        return res;
    }

    private String calcDiff(Rep rep, int i, boolean cutLast) {
        var res = calcDiff(get(i), rep.get(i));
        res = res.substring(0, res.length() - 1);
        return res;
    }

    private String calcDiff(double x1, double x2) {
        String res = " ";
        if (x1 != x2) {
            double diff = x1 - x2;

            String format = "%." +
                    (RegVals.hasDecimals(diff) ? "1" : "0") +
                    "f";
            res = String.format(format, diff);
            if (diff > 0)
                res = "+" + res + "#G";
            else
                res += "#R";
        }
        return res + ";";
    }

    public UILabel[] getCarChoiceInfo() {
        String[] res = {
                "Name:      " + Texts.CAR_TYPES[nameId],
                "Power:     " + Math.round(getTotalKW()) + " " + Texts.tags[Rep.kW] + (get(bar) != 0 ? " (" + getInt(kW) + " " + Texts.tags[Rep.kW] + " + " + Texts.formatNumber(get(bar)) + " " + Texts.tags[Rep.bar] + ")" : ""),
                "Weight:    " + getInt(kg) + " " + Texts.tags[Rep.kg],
                "Speed:     " + getInt(spdTop) + " " + Texts.tags[Rep.spdTop],
                "Gears:     " + getInt(gearTop),
                "RPMs:      " + getInt(rpmIdle) + "-" + getInt(rpmTop) + " " + Texts.tags[Rep.rpmTop],
                "NOS Boost: " + Texts.formatNumber(get(nos)) + " " + Texts.tags[Rep.nos] + ", " + Texts.formatNumber(get(Rep.nosMs) / 1000f) + " s" + ", " + getInt(nosBottles) + " bottle" + (getInt(nosBottles) == 1 ? "" : "s"),
                "Tireboost: " + Texts.formatNumber(get(tb)) + " " + Texts.tags[Rep.tb] + ", " + Texts.formatNumber(get(Rep.tbMs) / 1000f) + " s" + (getInt(tbArea) == -1 ? " Guarenteed!" : ""),
                Texts.DESCRIPTION[this.nameId] + "#" + UIColors.LBEIGE,
        };

        return UILabel.create(res);
    }

    private double combineValues(Rep rep, int divide, int... index) {
        double res = rep.get(index[0]);
        for (int i = 1; i < index.length; i++) {
            if (divide == 1)
                res /= index[i];
            else if (divide == 2)
                res += index[i];
            else
                res *= index[i];
        }
        return res;
    }

    public String isMost(Rep[] otherCars, boolean positive, int divide, int... index) {
        boolean most = true, least = true; // 0 = least, 1 = middle, 2 = most
        double myVal = combineValues(this, divide, index);

        for (var rep : otherCars) {
            double theirVal = combineValues(rep, divide, index);

            if (most && myVal < theirVal) {
                if (positive)
                    most = false;
                else
                    least = false;
            } else if (least && myVal > theirVal) {
                if (positive)
                    least = false;
                else
                    most = false;
            }
        }
        if (most)
            return "#" + UIColors.WON;
        else if (least)
            return "#" + UIColors.DNF;
        return "#" + UIColors.LBEIGE;
    }

    public void getInfoWin(ArrayList<String> info, Rep[] otherCars) {
        info.add("    +$" + Texts.formatNumberSimple(getInt(moneyPerTurn)) + " " + Texts.tags[moneyPerTurn] + isMost(otherCars, true, 0, moneyPerTurn));
        info.add("  " + Texts.CAR_TYPES[nameId] + ":#" + UIColors.WHITE);
        info.add("    " + Texts.formatNumber(getTotalKW()) + " " + Texts.tags[Rep.kW] + " / " + getInt(kg) + " " + Texts.tags[Rep.kg] + isMost(otherCars, true, 1, kW, kg));
        info.add("    " + Texts.formatNumberSimple(getInt(spdTop)) + " " + Texts.tags[Rep.spdTop] + ", record: " + Texts.formatNumberSimple(getInt(highestSpdAchived)) + " " + Texts.tags[Rep.spdTop] + isMost(otherCars, true, 0, spdTop));
        info.add("    " + Texts.formatNumber(get(nos)) + " " + Texts.tags[Rep.nos] + " x " + getInt(nosBottles) + " x " + getInt(nosMs) + " ms" + (is(nosSoundbarrier) ? ", " + Texts.tags[Rep.nosSoundbarrier] : "") + (is(nosAuto) ? ", " + Texts.tags[Rep.nosAuto] : "") + isMost(otherCars, true, 0, nos, nosBottles, nosMs));
        info.add("    " + Texts.formatNumber(get(tb)) + " " + Texts.tags[Rep.tb] + (getInt(tbArea) == -1 ? ", guarenteed" : "") + ", " + getInt(tbMs) + " ms" + isMost(otherCars, true, 0, tb, tbMs));
        info.add("    " + Texts.formatNumber(get(bar)) + " " + Texts.tags[Rep.bar] + " (" + getInt(kW) + " + " + ((int) getTurboKW()) + " " + Texts.tags[Rep.kW] + "), " + (Texts.formatNumber(get(spool)) + " " + Texts.tags[Rep.spool]) + isMost(otherCars, true, 0, bar, spool));
        info.add("    " + (is(throttleShift) ? "Has" : "No") + " " + Texts.tags[Rep.throttleShift] + ", "
                + (is(stickyclutch) ? "got" : "no") + " " + Texts.tags[Rep.stickyclutch] + " and "
                + (is(sequential) ? "has " : "no ") + Texts.tags[Rep.sequential] + isMost(otherCars, true, 2, throttleShift, stickyclutch, sequential));
    }

    public double set(int i, double val) {
        return stats[i] = val;
    }

    public void setBool(int i, boolean val) {
        stats[i] = val ? 1 : 0;
    }

    public void add(int i, double val) {
        stats[i] += val;
    }

    public void mul(int i, double val) {
        stats[i] *= val;
    }

    public void div(int i, double val) {
        stats[i] /= val;
    }

    public double get(int i) {
        return stats[i];
    }

    public double getTurboKW() {
        return get(kW) / 4 * get(bar);
    }

    public double getTotalKW() {
        var a = (get(kW) + getTurboKW()) * (get(rpmTop) / get(rpmBaseTop));

        if (Double.isInfinite(a)) {
            a = Math.signum(a) * Double.MAX_VALUE;
        }

        return a;
    }

    public double getRPMKW() {
        return (double) (
                ((get(kW) + getTurboKW())
                        * get(rpmTop) / get(rpmBaseTop)) - (get(kW) + getTurboKW())
        );
    }

    public boolean is(int i) {
        return stats[i] != 0;
    }

    public long getInt(int i) {
        return (long) stats[i];
    }

    public String getName() {
        return Texts.CAR_TYPES[nameId];
    }

    public int getNameID() {
        return nameId;
    }

    public boolean hasTurbo() {
        return is(Rep.bar);
    }

    public boolean hasNOS() {
        return is(Rep.nos);
    }

    public boolean hasTireboost() {
        return is(Rep.tb);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder(String.valueOf(nameId));
        sb.append("\n");
        for (var i = 0; i < stats.length; i++) {
            if (Texts.tags[i] == null)
                continue;
            sb.append(Texts.tags[i]).append(": ").append(stats[i]).append(", \n");
        }
        return sb.toString();
    }

    public double[] getValues() {
        return stats;
    }

    public boolean isRandom() {
        return random;
    }

    public void setRandom(boolean random) {
        this.random = random;
    }

    public void setNameId(int nameId) {
        this.nameId = nameId;
    }

}

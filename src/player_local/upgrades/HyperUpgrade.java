package player_local.upgrades;

import communication.Translator;
import player_local.car.Rep;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class HyperUpgrade extends Upgrade {
    private int[] patterns; // 2-4 patterns
    private int[] patternLevels; // 4 levels/colors with length the same as patterns.length

    private boolean patternDoesContain(int pattern, int exI) {
        for (int i = 0; i < exI; i++) {
            if (patterns[i] == pattern) return true;
        }
        return false;
    }

    public HyperUpgrade() {}

    public HyperUpgrade(Random ran) {
        super(TileNames.Power);
        overrideName = "Hyper Tile";
        setVisible(true);
        setMaxLVL(1);
        setUpgradeType(UpgradeType.HYPER);
        setPremadePrice(500f / priceModifier);

        var len = ran.nextFloat();
        patterns = new int[(len >= 0.85f ? 3 :
                                    len >= 0.4f ? 2 :
                                    len >= 0.1f ? 1 : 0) + 1];
        patternLevels = new int[patterns.length];

        for (int i = 0; i < patterns.length; i++) {
            do {
                patterns[i] = ran.nextInt(16);
            } while (patternDoesContain(patterns[i], i));
            patternLevels[i] = ran.nextInt(4);

            switch (patterns[i]) {
                case 0 -> regularValues.values()[Rep.kW] = new RegVal(
                        switch (patternLevels[i]) {
                            case 0: yield 1.02;
                            case 1: yield 1.03;
                            case 2: yield 1.04;
                            case 3: yield 1.05;
                            default: throw new IllegalStateException("Unexpected value: " + patternLevels[i]);
                        }, RegVal.RegValType.NormalPercent);
                case 1 -> regularValues.values()[Rep.kg] = new RegVal(
                        switch (patternLevels[i]) {
                            case 0: yield .98;
                            case 1: yield .975;
                            case 2: yield .97;
                            case 3: yield .97;
                            default: throw new IllegalStateException("Unexpected value: " + patternLevels[i]);
                        }, RegVal.RegValType.NormalPercent);
                case 2 -> regularValues.values()[Rep.bar] = new RegVal(
                        switch (patternLevels[i]) {
                            case 0: yield 1.005;
                            case 1: yield 1.01;
                            case 2: yield 1.0125;
                            case 3: yield 1.02;
                            default: throw new IllegalStateException("Unexpected value: " + patternLevels[i]);
                        }, RegVal.RegValType.NormalPercent);
                case 3 -> regularValues.values()[Rep.aero] = new RegVal(
                        switch (patternLevels[i]) {
                            case 0: yield .99;
                            case 1: yield .98;
                            case 2: yield .975;
                            case 3: yield .95;
                            default: throw new IllegalStateException("Unexpected value: " + patternLevels[i]);
                        }, RegVal.RegValType.NormalPercent);
                case 4 -> regularValues.values()[Rep.spdTop] = new RegVal(
                        switch (patternLevels[i]) {
                            case 0: yield 1.02;
                            case 1: yield 1.03;
                            case 2: yield 1.04;
                            case 3: yield 1.05;
                            default: throw new IllegalStateException("Unexpected value: " + patternLevels[i]);
                        }, RegVal.RegValType.NormalPercent);
                case 5 -> {
                    regularValues.values()[Rep.nosBottles] = new RegVal(
                            switch (patternLevels[i]) {
                                case 0, 1: yield 1;
                                case 2: yield 2;
                                case 3: yield 3;
                                default: throw new IllegalStateException("Unexpected value: " + patternLevels[i]);
                            }, RegVal.RegValType.Decimal);
                    regularValues.values()[Rep.nosBottles].removeAtPlacement = true;
                }
                case 6 -> regularValues.values()[Rep.nos] = new RegVal(
                        switch (patternLevels[i]) {
                            case 0: yield 1.02;
                            case 1: yield 1.025;
                            case 2: yield 1.03;
                            case 3: yield 1.04;
                            default: throw new IllegalStateException("Unexpected value: " + patternLevels[i]);
                        }, RegVal.RegValType.NormalPercent);
                case 7 -> {
                    regularValues.values()[Rep.tb] = new RegVal(
                            switch (patternLevels[i]) {
                                case 0: yield 1.005;
                                case 1: yield 1.01;
                                case 2: yield 1.0125;
                                case 3: yield 1.015;
                                default: throw new IllegalStateException("Unexpected value: " + patternLevels[i]);
                            }, RegVal.RegValType.NormalPercent);
                }
                case 8 -> regularValues.values()[Rep.bar] = new RegVal(
                        switch (patternLevels[i]) {
                            case 0: yield 1.005;
                            case 1: yield 1.01;
                            case 2: yield 1.0125;
                            case 3: yield 1.015;
                            default: throw new IllegalStateException("Unexpected value: " + patternLevels[i]);
                        }, RegVal.RegValType.NormalPercent);
                case 9 -> {
                    regularValues.values()[Rep.tbHeat] = new RegVal(
                            switch (patternLevels[i]) {
                                case 0: yield 1.02;
                                case 1: yield 1.0225;
                                case 2: yield 1.025;
                                case 3: yield 1.03;
                                default: throw new IllegalStateException("Unexpected value: " + patternLevels[i]);
                            }, RegVal.RegValType.NormalPercent);
                }
                case 10 -> regularValues.values()[Rep.rpmTop] = new RegVal(
                        switch (patternLevels[i]) {
                            case 0: yield 250;
                            case 1: yield 500;
                            case 2: yield 750;
                            case 3: yield 1000;
                            default: throw new IllegalStateException("Unexpected value: " + patternLevels[i]);
                        }, RegVal.RegValType.Decimal);
                case 11 -> regularValues.values()[Rep.nosMs] = new RegVal(
                        switch (patternLevels[i]) {
                            case 0: yield 50;
                            case 1: yield 75;
                            case 2: yield 100;
                            case 3: yield 125;
                            default: throw new IllegalStateException("Unexpected value: " + patternLevels[i]);
                        }, RegVal.RegValType.Decimal);
                case 12 -> regularValues.values()[Rep.tbMs] = new RegVal(
                        switch (patternLevels[i]) {
                            case 0: yield 50;
                            case 1: yield 75;
                            case 2: yield 100;
                            case 3: yield 125;
                            default: throw new IllegalStateException("Unexpected value: " + patternLevels[i]);
                        }, RegVal.RegValType.Decimal);
                case 13 -> regularValues.values()[Rep.turboblow] = new RegVal(
                        switch (patternLevels[i]) {
                            case 0: yield 10;
                            case 1: yield 15;
                            case 2: yield 20;
                            case 3: yield 30;
                            default: throw new IllegalStateException("Unexpected value: " + patternLevels[i]);
                        }, RegVal.RegValType.Decimal);
                case 14 -> regularValues.values()[Rep.turboblowRegen] = new RegVal(
                        switch (patternLevels[i]) {
                            case 0: yield 10;
                            case 1: yield 15;
                            case 2: yield 20;
                            case 3: yield 30;
                            default: throw new IllegalStateException("Unexpected value: " + patternLevels[i]);
                        }, RegVal.RegValType.Decimal);
                case 15 -> regularValues.values()[Rep.turboblowStrength] = new RegVal(
                        switch (patternLevels[i]) {
                            case 0: yield 1.05;
                            case 1: yield 1.075;
                            case 2: yield 1.1;
                            case 3: yield 1.15;
                            default: throw new IllegalStateException("Unexpected value: " + patternLevels[i]);
                        }, RegVal.RegValType.AdditionPercent);
            }
        }
    }


    @Override
    public void getCloneString(StringBuilder outString, int lvlDeep, String splitter, boolean test) {
        super.getCloneString(outString, lvlDeep, splitter, test);
        outString.append(splitter).append(patterns.length);
        for (int i = 0; i < patterns.length; i++) {
            outString.append(splitter).append(patterns[i])
                    .append(Translator.specialSplit).append(patternLevels[i]);
        }
    }

    @Override
    public void setCloneString(String[] cloneString, AtomicInteger fromIndex) {
        super.setCloneString(cloneString, fromIndex);
        patterns = new int[Integer.parseInt(cloneString[fromIndex.getAndIncrement()])];
        patternLevels = new int[patterns.length];
        for (int i = 0; i < patterns.length; i++) {
            var strs = cloneString[fromIndex.getAndIncrement()].split(Translator.specialSplit);
            patterns[i] = Integer.parseInt(strs[0]);
            patternLevels[i] = Integer.parseInt(strs[1]);
        }
    }

    @Override
    public HyperUpgrade clone() {
        var res = new HyperUpgrade();
        Translator.setCloneString(res, Translator.getCloneString(this));
        return res;
    }

    @Override
    public TileNames getTileName() {
        return TileNames.Pattern0_;
    }

    public int[] getPatterns() {
        return patterns;
    }

    public int[] getPatternLevels() {
        return patternLevels;
    }
}

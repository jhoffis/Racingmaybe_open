package test;

import java.util.ArrayList;
import java.util.Random;

import engine.ai.AI;
import main.Main;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import communication.GameInfo;
import communication.GameType;
import communication.Translator;
import main.Features;
import main.Texts;
import player_local.Layer;
import player_local.Player;
import player_local.TilePiece;
import player_local.car.Car;
import player_local.car.Rep;
import player_local.upgrades.Upgrade;
import scenes.game.GameRemoteMaster;
import scenes.game.lobby_subscenes.UpgradesSubscene;

public class TestTranslator {

    private boolean callbackBool;
    private long triedJoiningTime;

    @Test
    void cartime() {
        Main.DEBUG = false;
//        for (int a = 0; a < 2; a++) {
//            System.out.println(a == 0 ? "\nWITHOUT SNOS" : "\nWITH SNOS");
            for (int i = 0; i < 4; i++) {
                var car = new Car();
                car.switchTo(i, false);
                car.getRep().set(Rep.turboblow, 0);
//                car.getRep().set(Rep.turboblowRegen, 0);
//                car.getRep().set(Rep.nosBottles, 1200);
//                car.getRep().set(Rep.nosMs, 3000);
//                car.getRep().set(Rep.nos, 10);
//                car.getRep().set(Rep.spdTop, 10000);
//            car.getRep().setBool(Rep.stickyclutch, true);
//            car.getRep().add(Rep.rpmTop, 3000);
                var raceShort = AI.calculateRace(car, 120);
                var raceMed = AI.calculateRace(car, 240);
                var raceLonger = AI.calculateRace(car, 1000);

//			System.out.println(Texts.CAR_TYPES[i] + ": " + AI.calculateRace(car, 10) + " time in 10m");
                System.out.println();
                System.out.println(Texts.CAR_TYPES[i] + ": " + raceShort + " time in 120m");
                System.out.println(Texts.CAR_TYPES[i] + ": " + raceMed + " time in 240m");
                System.out.println(Texts.CAR_TYPES[i] + ": " + raceLonger + " time in 1000m");
            }
//        }

    }

    @Test
    void timepenalty() {
        Main.DEBUG = false;
        var behindStd = 5000;

        var moneyAdded = 120;

        var ran = new Random();

        for (int rounds = 0; rounds < 10; rounds++) {

            var timeBehindFirst = behindStd + ran.nextInt(10000);

            var timePenalty = (timeBehindFirst - .5 * behindStd) / (4 * behindStd);
            if (rounds < 5)
                timePenalty += (1d - timePenalty) / (rounds + 1);
            if (timePenalty < .2)
                timePenalty = .2;
            moneyAdded *= timePenalty;
        }

    }

    /*
     *
     * @BeforeEach void consoleStart() {
     * System.out.println("\n========== NEW TEST =========="); }
     *
     * @AfterEach void consoleSpacing () {
     * System.out.println("********** END TEST **********\n"); }
     *
     *
     * TileUpgrade findStoreTile(Store store, int id) { for (var tileChoice :
     * store.getStoreTiles()) { if (tileChoice.getUpgradeId() == id) { return
     * tileChoice; } } return null; }
     *
     * void resetUpgrades(Store store, Player player, GameMode gm) {
     * player.upgrades.reset(); for (var up : player.upgrades.getUpgradesNonNulls())
     * { up.setVisible(true); } store.resetTowardsPlayer(player); }
     *
     * @Test void bonusesWontCrash() { // setup Texts.init(); Player player = new
     * Player("test", 0, Player.HOST, 0); player.bank.add(20000, Bank.MONEY); Store
     * store = new Store(); var bm = new UIBonusModal(); store.setBonusModal(bm);
     * GameMode gm = new TotalMode(); gm.init(player, new ConcurrentHashMap<>());
     * resetUpgrades(store, player, gm);
     *
     * for (int boltsCostToGoFor = -1; boltsCostToGoFor < 4; boltsCostToGoFor++) {
     * for (int id = 0; id < Upgrades.size; id++) {
     *
     * // if (id == 9) // System.out.println("stop testing!");
     *
     * var bonusLVLs = player.upgrades.getUpgrade(id).getBonusLVLs(); if
     * (bonusLVLs.length == 0) { var tile = findStoreTile(store, id); if (tile !=
     * null) { // tatt av freeupgrade store.attemptBuyTile(player, tile, new
     * Vec2(2), gm.getRound()); player.sellTile(tile, gm.getRound()); } } else { int
     * expectedBonusLVL = 1; for (int b = 0; b < bonusLVLs.length; b++) {
     * while(player.upgrades.getUpgrade(id).getLVL() < bonusLVLs[b]) {
     *
     * var tile = findStoreTile(store, id);
     *
     * System.out.println(Texts.getUpgradeTitle(tile.getUpgrade()) + " " +
     * tile.getUpgradeId() + ": lvl" + tile.getUpgrade().getLVL());
     *
     * int bonusLVL = tile.getUpgrade().getBonusLVL(); var bonuses =
     * tile.getUpgrade().getBonuses(); if (bonuses.length <= bonusLVL) {
     * System.out.println("feil index til bonusLVL"); }
     *
     * int maxBoltsCost = bonuses[bonusLVL].getAmountChoices() - 1; if
     * (store.attemptBuyTile(player, tile, new Vec2(2), gm.getRound()) ==
     * UpgradeResult.FoundBonus) { // cancel bm.cancel(player);
     * System.out.println("after cancel lvl: " +
     * player.upgrades.getUpgrade(id).getLVL()); // buy again tile =
     * findStoreTile(store, id); store.attemptBuyTile(player, tile, new Vec2(2),
     * gm.getRound()); System.out.println("try again lvl: " +
     * player.upgrades.getUpgrade(id).getLVL());
     * System.out.println("selecting bonus cost: " + boltsCostToGoFor +
     * ", at bonuslvl: " + b); while (bm.select(player, store, boltsCostToGoFor >
     * maxBoltsCost ? maxBoltsCost : boltsCostToGoFor) == UpgradeResult.FoundBonus);
     * } // selg den tilen player.sellTile(tile, gm.getRound()); }
     * System.out.println("after bonus, what is the lvl: " +
     * player.upgrades.getUpgrade(id).getLVL()); int actualBonusLVL =
     * findStoreTile(store, id).getUpgrade().getBonusLVL();
     * assertTrue(Texts.getUpgradeTitle(id) + "(" + id +
     * ") failed to select bonus to expected lvl " + expectedBonusLVL + " was " +
     * actualBonusLVL, actualBonusLVL == expectedBonusLVL); expectedBonusLVL++; } }
     * } resetUpgrades(store, player, gm); } }
     *
     * @Test void freeUpgradesBecomeVisible() { var player = new Player("", 0,
     * Player.HOST, 0); var freeUpgradesUpgrade = new RegVals();
     *
     * //Test � g� p� spesifik, ogs� for mange ganger
     * freeUpgradesUpgrade.values[Rep.freeUpgrade] = 0 + 10;
     *
     * for (int i = 0; i < player.upgrades.getUpgrade(0).getMaxLVL() + 1; i++) {
     * freeUpgradesUpgrade.upgrade(player, (byte) 0); }
     *
     *
     * //test � g� p� alle upgrades som finnes
     *
     * freeUpgradesUpgrade.values[Rep.freeUpgrade] = -10000;
     * freeUpgradesUpgrade.upgrade(player, (byte) 0); var upgrades =
     * player.upgrades.getUpgradesNonNulls(); for (var up : upgrades) { if
     * (up.getMaxLVL() > 0) assertTrue(up.getLVL() <= up.getMaxLVL());
     * assertTrue(up.isVisible()); } }
     *
     * // @Test void playerCloneString() { // setup Texts.init();
     *
     * Player player = new Player("test", 0, Player.HOST, 0); player.bank.add(20000,
     * Bank.MONEY); Store store = new Store(); var bm = new UIBonusModal();
     * store.setBonusModal(bm); GameMode gm = new TotalMode(); gm.init(player, new
     * ConcurrentHashMap<>()); store.resetTowardsPlayer(player);
     *
     * // kj�p en tile
     *
     * var tile = store.getStoreTiles().get(0); int unlockedId =
     * tile.getUpgrade().getUnlocks().peek(); int amountToGetBonus =
     * player.upgrades.getUpgrade(unlockedId).getBonusLVLs()[0]; // Lagre tooltip
     * for senere testing UILabel[] tooltipOld = tile.getInfoTooltip(player.layer,
     * player.upgrades, null);
     *
     * player.layer.setTimesMod(0, 0, 0); // unng� at tooltip blir ulikt pga x2
     * store.attemptBuyTile(player, tile, new Vec2(0), gm.getRound());
     *
     * // Kj�p to til for � teste at jeg kan velge bonus. // Dette b�r v�re mulig
     * uten gui mtp ai. for (int i = 0; i < amountToGetBonus; i++) { if
     * (store.attemptBuyTile(player, findStoreTile(store, unlockedId), new Vec2(2,
     * i), gm.getRound()) == UpgradeResult.FoundBonus) { while (bm.select(player,
     * store, 0) == UpgradeResult.FoundBonus); } } assertTrue(findStoreTile(store,
     * unlockedId).getUpgrade().getBonusLVL() > 0);
     *
     * String toSend = Translator.getCloneString(player, false, true);
     * System.out.println(toSend);
     *
     * Player clonedPlayer = new Player(); Translator.setCloneString(clonedPlayer,
     * toSend);
     *
     * var clonedTile = clonedPlayer.layer.getLinArr().get(0); String recieved =
     * Translator.getCloneString(clonedPlayer, false, true);
     * System.out.println(recieved);
     *
     * assertEquals(toSend, recieved);
     *
     * assertEquals(player.layer.getLinArr().get(0).getUpgrade(),
     * clonedTile.getUpgrade(), "Upgrade ikke klonet skikkelig.");
     *
     * UILabel[] tooltipNew = clonedTile.getInfoTooltip(player.layer,
     * player.upgrades, clonedTile.getLayerPos()); for (int i = 0; i <
     * tooltipOld.length; i++) { assertEquals(tooltipOld[i].getText(),
     * tooltipNew[i].getText()); } }
     *
     * // @Test void pricesAreProper() { GameMode gm = new TotalMode(); Upgrade
     * testUp = new Upgrade(); Player player = new Player(); for (float priceFactor
     * = .01f; priceFactor < 1f; priceFactor += 0.01f) { for (float price :
     * gm.createPrices(player, priceFactor)) { for (int lvl = 0; lvl < 100; lvl++) {
     * float calcPrice = testUp.getCost(price, lvl, priceFactor, 1);
     * assertTrue("Feil mod verdi: " + price + ", " + calcPrice + ", gav % = " +
     * calcPrice % 5f + ", factor: " + priceFactor + ", lvl: " + lvl + " + 1",
     * calcPrice % 5f == 0f && calcPrice > 0); } } } }
     *
     * @Test void fastDriving() { //
     * 0#-1#0#Player#0#2000#51#2#22#14##Layer#5#5#Tile#0#11#Lighter
     * Pistons#53.33#200.0#600.0#4#4#x#1#31#1.1#100.97000009999998#e#31#101.
     * 02000009999999#100.99000009999999#e#0#1#-1#1#31#101.04400009999999#e#31#e#0#1
     * #-1#POWER#Pistons###0.75#0#1#0.0#0.0#8#31#5185.848568453006#-387.194029248163
     * #0.20556983106116422#s15#0.03164590282684365#e#0#Tile#0#11#Lighter
     * Pistons#53.33#100.0#500.0#4#5#x#1#31#1.06#100.99000009999999#e#31#101.
     * 02000009999999#100.99000009999999#e#0#1#-1#1#31#101.04400009999999#e#31#e#0#1
     * #-1#POWER#Pistons###0.75#0#1#0.0#1.0#7#31#5610.828277797689#-410.
     * 31028947930236#e#2#Tile#0#11#Lighter
     * Pistons#53.33#400.0#640.0#3#5#x#1#31#1.1800000000000002#100.93000009999996#e#
     * 31#101.02000009999999#100.99000009999999#e#0#1#-1#1#31#101.04000009999999#e#
     * 31#e#0#1#-1#POWER#Pistons###0.75#0#1#0.0#2.0#15#31#29669.26205787622#-385.
     * 1760447767497#s12#20.0#e#0#Tile#0#11#Lighter
     * Pistons#53.33#350.0#750.0#4#6#x#1#31#1.1600000000000001#100.94000009999996#e#
     * 31#101.02000009999999#100.99000009999999#e#0#1#-1#1#31#101.04400009999999#e#
     * 31#e#0#1#-1#POWER#Pistons###0.75#0#1#0.0#3.0#15#31#10487.142387178677#-421.
     * 2506035067556#s1#49.999999999999886#e#0#Tile#0#11#Lighter
     * Pistons#53.33#300.0#700.0#4#4#x#1#31#1.1400000000000001#100.95000009999997#e#
     * 31#101.02000009999999#100.99000009999999#e#0#1#-1#1#31#101.04400009999999#e#
     * 31#e#0#1#-1#POWER#Pistons###0.75#0#1#0.0#4.0#11#31#11763.269772577627#-881.
     * 8963852050764#s1#123.36466369302275#e#2#Tile#0#12#Turbo#53.33#100.0#220.0#2#5
     * #x#1#31#s1#50.0#500.4000001#e#31#e#0#1#-1#1#31#s2#101.0400001#s15#101.0100001
     * #e#31#e#0#1#-1#POWER#Turbo###0.75#0#1#1.0#0.0#9#31#167.94746542467942#150.0#1
     * .2#e#0#Tile#0#0#Supplementary#53.33#50.0#90.0#1#6#x#1#31#s14#20.0#e#31#e#0#1#
     * -1#1#31#s14#301.50000105367434#e#31#e#0#1#-1#SUPPLEMENTARY#Supplementary##1:3
     * :#0.75#1#1#1.0#1.0#3#31#14.605978679645773#s13#50.0#e#0#Tile#0#3#Money
     * Pit#53.33#50.0#90.0#1#5#x#1#31#s14#510.0000001#e#31#s14#5.0#e#0#1#-1#1#31#s14
     * #5.0#e#31#e#0#1#-1#SUPPLEMENTARY#MoneyPit##4:#0.75#0#1#1.0#2.0#4#31#s3#10.0#
     * s5#0.042641814931199895#s4#30.0#e#0#Tile#0#1#Clutch#53.33#50.0#290.0#3#5#x#1#
     * 31#s3#1.1#e#31#e#0#1#-1#1#31#s3#20.0#e#31#e#0#1#-1#SUPPLEMENTARY#Clutch##2:#0
     * .75#0#1#1.0#3.0#6#31#4972.506719573648#s2#414.58336057717446#s5#0.
     * 30000000000000027#s4#20.0#e#0#Tile#0#2#Gears#53.33#100.0#220.0#2#5#x#1#31#s3#
     * 50.0#e#31#s3#25.0#e#0#1#-1#1#31#s3#101.0300001#e#31#e#0#1#-1#SUPPLEMENTARY#
     * Gears###0.75#0#1#1.0#4.0#6#31#873.6337085920139#s2#340.0#s6#0.
     * 13308492452000031#e#2#Tile#0#11#Lighter
     * Pistons#53.33#450.0#850.0#4#4#x#1#31#1.2000000000000002#100.92000009999995#e#
     * 31#101.02000009999999#100.99000009999999#e#0#1#-1#1#31#101.04400009999999#e#
     * 31#e#0#1#-1#POWER#Pistons###0.75#0#1#2.0#0.0#18#31#58133.40557795528#-347.
     * 10461654928156#0.5353845399554387#s11#75.0#s3#0.05525834345762992#e#0#Tile#0#
     * 3#Money
     * Pit#53.33#100.0#340.0#3#5#x#1#31#s14#515.0000001000001#e#31#s14#5.0#e#0#1#-1#
     * 1#31#s14#10.0#e#31#e#0#1#-1#SUPPLEMENTARY#MoneyPit##4:#0.75#0#1#2.0#1.0#4#31#
     * 122.6651593306176#s8#0.1216071484918293#s4#100.0#e#0#Tile#0#6#N O S
     * Bottle#53.33#50.0#90.0#1#3#x#1#31#s9#500.10000010000005#e#31#e#0#1#-1#1#31#s9
     * #101.0100001#e#31#e#0#1#-1#BOOST#NOS##7:#0.75#0#1#2.0#2.0#3#31#s9#0.
     * 4999999999999999#s4#10.0#s11#1.0#e#0#Tile#0#2#Gears#53.33#150.0#190.0#1#4#x#1
     * #31#s3#75.0#e#31#s3#25.0#e#0#1#-1#1#31#s3#101.0300001#e#31#e#0#1#-1#
     * SUPPLEMENTARY#Gears###0.75#0#1#2.0#3.0#16#31#s3#169.99999999999994#s5#0.
     * 09174140416480281#s1#30.0#30.0#e#0#Tile#0#1#Clutch#53.33#100.0#140.0#1#3#x#1#
     * 31#s3#1.1#e#31#e#0#1#-1#1#31#s3#10.0#e#31#e#0#1#-1#SUPPLEMENTARY#Clutch##2:#0
     * .75#0#1#2.0#4.0#16#31#s3#225.62307475199987#s5#0.09358540638851576#e#0#Tile#0
     * #3#Money
     * Pit#53.33#200.0#320.0#2#4#x#1#31#s14#525.0000001000001#e#31#s14#5.0#e#0#1#-1#
     * 1#31#s14#5.0#e#31#e#0#1#-1#SUPPLEMENTARY#MoneyPit##4:#0.75#0#1#3.0#0.0#14#31#
     * 145.79403821127698#s13#95.0#e#0#Tile#0#3#Money
     * Pit#53.33#150.0#390.0#3#3#x#1#31#s14#520.0000001000001#e#31#s14#5.0#e#0#1#-1#
     * 1#31#s14#10.0#e#31#e#0#1#-1#SUPPLEMENTARY#MoneyPit##4:#0.75#0#1#3.0#1.0#13#31
     * #s9#0.600000000000001#s4#125.0#e#0#s1#Tile#0#5#Boost#53.33#50.0#170.0#2#3#x#1
     * #31#s9#500.2000001#500.2000001#e#31#e#0#1#-1#1#31#s11#15.0#15.0#e#31#e#0#1#-1
     * #BOOST#Boost##8:6:#0.75#1#1#3.0#3.0#2#31#s2#0.04864000000000002#s3#200.0#s2#2
     * .4#1.8000000000000003#s7#0.020100000000000007#e#3#Tile#0#6#N O S
     * Bottle#53.33#100.0#100.0#0#3#x#1#31#s9#500.10000010000005#e#31#e#0#1#-1#1#31#
     * s9#101.0100001#e#31#e#0#1#-1#BOOST#NOS##7:#0.75#0#1#3.0#4.0#12#31#s9#0.
     * 10000000000000009#0.12510798673559975#15.0#15.0#s13#1.0#e#0#s2#Tile#0#6#N O S
     * Bottle#53.33#150.0#190.0#1#3#x#1#31#s9#500.10000010000005#e#31#e#0#1#-1#1#31#
     * s9#101.0100001#e#31#e#0#1#-1#BOOST#NOS##7:#0.75#0#1#4.0#2.0#12#31#s2#0.
     * 18300712445804557#s6#0.5#s8#0.021336555027080095#s7#1.0#e#0#Tile#0#12#Turbo#
     * 53.33#50.0#90.0#1#3#x#1#31#s1#50.0#500.4000001#e#31#e#0#1#-1#1#31#s2#101.
     * 0400001#s15#101.0100001#e#31#e#0#1#-1#POWER#Turbo###0.75#0#1#4.0#3.0#5#31#s1#
     * 100.0#0.8#s7#0.13718129999999995#30.0#30.0#e#0#Tile#0#8#Tireboost#53.33#50.0#
     * 90.0#1#3#x#1#31#s10#500.4000001#e#31#s10#500.10000010000005#e#0#1#-1#1#31#s10
     * #101.0300001#e#31#e#0#1#-1#BOOST#Tireboost###0.75#0#1#4.0#4.0#4#31#s2#0.
     * 03394560000000002#s3#100.0#s3#1.6000000000000003#s7#0.010200999999999905#e#2#
     * Car#1#127361.90971165038#624.0680312346714#3.0065470954746485#1483.
     * 571099022197#300#6500#6800#4#100#4.749575773976349#4.2953742112556#1075#1875#
     * -1#525#0#0#0#1.1385418013115536#0#0#0#0#0#1.4666666666666666#1#3#1428#0#0#0#
     * Upgrades#14#0#Supplementary#66.666664#50.0#0.0#1#1#x#1#31#s14#20.0#e#31#e#0#1
     * #-1#1#31#s14#301.50000105367434#e#31#e#0#1#-1#SUPPLEMENTARY#Supplementary##1:
     * 3:#0.75#1#1#1#Clutch#66.666664#50.0#0.0#2#-1#x#1#31#s3#1.1#e#31#e#0#1#-1#1#31
     * #s3#10.0#e#31#e#0#1#-1#SUPPLEMENTARY#Clutch##2:#0.75#0#1#2#Gears#66.666664#50
     * .0#0.0#3#-1#x#1#31#s3#100.0#e#31#s3#25.0#e#0#1#-1#1#31#s3#101.0300001#e#31#e#
     * 0#1#-1#SUPPLEMENTARY#Gears###0.75#0#1#3#Money
     * Pit#66.666664#50.0#0.0#4#-1#x#1#31#s14#530.0000001000001#e#31#s14#5.0#e#0#1#-
     * 1#1#31#s14#5.0#e#31#e#0#1#-1#SUPPLEMENTARY#MoneyPit##4:#0.75#0#1#4#Bolt
     * Market#66.666664#50.0#0.0#0#3#x#1#31#s15#500.2000001#e#31#e#0#1#-1#1#31#s14#
     * 301.1000101374222#500.0200001#e#31#e#0#1#-1#SUPPLEMENTARY#MoneyPit###0.75#0#1
     * #5#Boost#66.666664#50.0#0.0#1#1#x#1#31#s9#500.2000001#500.2000001#e#31#e#0#1#
     * -1#1#31#s11#15.0#15.0#e#31#e#0#1#-1#BOOST#Boost##8:6:#0.75#1#1#6#N O S
     * Bottle#66.666664#50.0#0.0#3#-1#x#1#31#s9#500.10000010000005#s16#1.0#e#31#e#0#
     * 1#-1#1#31#s9#101.0100001#e#31#e#0#1#-1#BOOST#NOS##7:#0.75#0#1#7#N O
     * S#66.666664#50.0#0.0#2#-1#x#1#31#s9#1.03#e#31#e#0#1#-1#1#31#s9#500.1500001#e#
     * 31#e#0#1#-1#BOOST#NOS###0.75#0#1#8#Tireboost#66.666664#50.0#0.0#1#-1#x#1#31#
     * s10#500.5000001#e#31#s10#500.10000010000005#e#0#1#-1#1#31#s10#101.0300001#e#
     * 31#e#0#1#-1#BOOST#Tireboost###0.75#0#1#9#Power#66.666664#50.0#0.0#1#1#x#1#31#
     * s6#500.0#e#31#e#0#1#-1#1#31#s6#100.0#e#31#e#0#1#-1#POWER#Power##10:12:13:#0.
     * 75#1#1#10#Weight
     * Reduction#66.666664#50.0#0.0#1#-1#x#1#31#s1#0.96#e#31#s1#100.9800001#e#0#1#-1
     * #1#31#s1#100.99000009999999#e#31#e#0#1#-1#POWER#Weight##11:#0.75#0#1#11#
     * Lighter
     * Pistons#66.666664#50.0#0.0#9#-1#x#1#31#1.2200000000000002#100.91000009999995#
     * e#31#101.02000009999999#100.99000009999999#e#0#1#-1#1#31#101.02000009999999#e
     * #31#e#0#1#-1#POWER#Pistons###0.75#0#1#12#Turbo#66.666664#50.0#0.0#2#-1#x#1#31
     * #s1#50.0#500.4000001#e#31#e#0#1#-1#1#31#s2#101.0400001#s15#101.0100001#e#31#e
     * #0#1#-1#POWER#Turbo###0.75#0#1#13#Beefy
     * Block#66.666664#50.0#0.0#2#-1#x#1#31#150.0#1.08#e#31#25.0#200.9900001#e#0#1#-
     * 1#1#31#101.0300001#s9#101.02000009999999#e#31#e#0#1#-1#POWER#Block###0.75#0#1
     *
     * Car car = new Car(); double lastSpeed = 0; car.completeReset();
     * car.getRep().set(Rep.kW, Double.MAX_VALUE); car.getRep().set(Rep.kg,
     * 0.000001); car.getRep().set(Rep.spdTop, Double.MAX_VALUE); car.reset();
     * car.throttle(true, true); car.clutch(false); do { car.updateSpeed(1);
     * Assert.assertNotEquals(lastSpeed, car.getStats().speed); lastSpeed =
     * car.getStats().speed; System.out.println("Speed: " + lastSpeed + " / " +
     * car.getRep().get(Rep.spdTop) + ", RPM: " + car.getStats().rpm);
     *
     * // Assert.assertFalse(car.getStats().rpm > car.getRep().get(Rep.rpmTop));
     * Assert.assertFalse(car.getStats().speed > Car.funcs.gearMax(car.getStats(),
     * car.getRep()));
     *
     * if (lastSpeed >= Car.funcs.gearMax(car.getStats(), car.getRep())) {
     * car.clutch(false); car.shiftUp(1); car.updateSpeed(1); car.clutch(true);
     * System.out.println("Shift to " + car.getStats().gear); } } while (lastSpeed <
     * car.getRep().get(Rep.spdTop) * 0.85); }
     *
     *
     * @Test void lanJoiningStressTest() { GameRemoteMaster game = new
     * GameRemoteMaster(null);
     *
     * var ran = new Random(); var list = new ArrayList<GameInfo>();
     *
     * for (var gm : GameModes.values()) { if (gm.isSingleplayer()) continue;
     * list.clear();
     *
     * var alice = new Player("Alice", Player.DEFAULT_ID, Player.HOST,
     * Features.generateLanId(true)); var aliceCom = new GameInfo(game,
     * GameType.CREATING_LAN); await(); Assert.assertTrue(aliceCom.getGameID() !=
     * 0);
     *
     * aliceCom.join(alice, GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0); await(); //
     * aliceCom.getGamemode().set
     *
     * list.add(aliceCom); boolean failed = false; callbackBool = false;
     *
     * if (gm != aliceCom.getGamemode().getGameModeEnum()) { aliceCom.init(1);
     * System.err.println(gm); await(); testEqualAmountOfPlayers(list.toArray(new
     * GameInfo[0])); }
     *
     *
     * for (int i = 1; i < 2; i++) { var bob = new Player("Bob" + i,
     * Player.DEFAULT_ID, Player.PLAYER, Features.generateLanId(false));
     * list.add(new GameInfo(game, GameType.JOINING_LAN)); await();
     *
     * list.get(i).join(bob, GameInfo.JOIN_TYPE_VIA_CLIENT, (player) -> callbackBool
     * = true, 0, 0);
     *
     * await(); if (!callbackBool) { failed = true; break; }
     * testEqualAmountOfPlayers(list.toArray(new GameInfo[0])); }
     *
     * if (failed) { for (var gi : list) { if (gi == null) break; gi.close(); } }
     * Assert.assertFalse(failed);
     *
     * while (!aliceCom.isGameOver()) { for (var gi : list) { gi.startRace(); } for
     * (var gi : list) { gi.finishRace(gi.player, ran.nextInt(1000)); await(); }
     * System.err.println("ROUND: " + aliceCom.getGamemode().getRound());
     * testEqualAmountOfPlayers(list.toArray(new GameInfo[0])); }
     *
     * for (var gi : list) { if (gi == null) break; gi.close(); } }
     *
     *
     * }
     *
     * @Test void lanMinusPoints() { callbackBool = false; GameRemoteMaster game =
     * new GameRemoteMaster(null);
     *
     * var alice = new Player("Alice", Player.DEFAULT_ID, Player.HOST,
     * Features.generateLanId(true)); var aliceCom = new GameInfo(game,
     * GameType.CREATING_LAN); aliceCom.join(alice, GameInfo.JOIN_TYPE_VIA_CREATOR,
     * null, 0, 0); await();
     *
     * var bob = new Player("Bob", Player.DEFAULT_ID, Player.PLAYER,
     * Features.generateLanId(false)); var bobCom = new GameInfo(game,
     * GameType.JOINING_LAN); bobCom.join(bob, GameInfo.JOIN_TYPE_VIA_CLIENT,
     * (player) -> callbackBool = true, 0, 0); await();
     *
     * if (!callbackBool) { aliceCom.close(); bobCom.close();
     * Assert.assertTrue(false); }
     *
     * var charlie = new Player("charlie", Player.DEFAULT_ID, Player.PLAYER,
     * Features.generateLanId(false)); var charlieCom = new GameInfo(game,
     * GameType.JOINING_LAN); charlieCom.join(charlie,
     * GameInfo.JOIN_TYPE_VIA_CLIENT, (player) -> callbackBool = true, 0, 0);
     * await();
     *
     * if (!callbackBool) { aliceCom.close(); bobCom.close(); charlieCom.close();
     * Assert.assertTrue(false); }
     *
     * testEqualAmountOfPlayers(aliceCom, bobCom, charlieCom);
     *
     * for (var gm : GameModes.values()) { if (gm.isSingleplayer()) continue; if (gm
     * != aliceCom.getGamemode().getGameModeEnum()) { aliceCom.init(1); await();
     * testEqualAmountOfPlayers(aliceCom, bobCom, charlieCom); }
     *
     * for (int i = 0; i < 5; i++) { aliceCom.startRace(); bobCom.startRace();
     * charlieCom.startRace();
     *
     * // * tror problemet er at de to ovenfor avslutter for tidlig // * og s�
     * teller farten p� charlie // * til � v�re 0ms da det er reset verdien.
     *
     * aliceCom.finishRace(alice, Race.CHEATED_GAVE_IN); await();
     * bobCom.finishRace(bob, Race.CHEATED_GAVE_IN); await();
     * charlieCom.finishRace(charlie, Race.CHEATED_GAVE_IN); await();
     *
     * Assert.assertEquals(aliceCom.updateRaceLobby(alice, false),
     * bobCom.updateRaceLobby(bob, false));
     * Assert.assertEquals(aliceCom.updateRaceLobby(alice, false),
     * charlieCom.updateRaceLobby(charlie, false));
     *
     * for (var p : aliceCom.getPlayers()) { if ((int) p.bank.get(Bank.POINT) > 0 ||
     * p.aheadByPoints != 0) { Assert.assertFalse(true); } } for (var p :
     * bobCom.getPlayers()) { if ((int) p.bank.get(Bank.POINT) > 0 ||
     * p.aheadByPoints != 0) { Assert.assertFalse(true); } } for (var p :
     * charlieCom.getPlayers()) { if ((int) p.bank.get(Bank.POINT) > 0 ||
     * p.aheadByPoints != 0) { Assert.assertFalse(true); } }
     *
     * testEqualAmountOfPlayers(aliceCom, bobCom, charlieCom); } }
     *
     * aliceCom.close(); bobCom.close(); }
     *
     * @Test void lanJoinFalseIP() { callbackBool = false; GameRemoteMaster game =
     * new GameRemoteMaster(null);
     *
     * var bob = new Player("Bob", Player.DEFAULT_ID, Player.PLAYER,
     * Features.generateLanId(false)); var bobCom = new GameInfo(game,
     * GameType.JOINING_LAN, "blabla FAKE IP");
     *
     * await(); bobCom.join(bob, GameInfo.JOIN_TYPE_VIA_CLIENT, (player) -> {
     * callbackBool = true; System.out.println("SET callbackBool " + callbackBool);
     * }, 0, 0);
     *
     * await();
     *
     * bobCom.close(); Assert.assertTrue(game.leaveGame); }
     */

    public static void await(double amount) {
        long till = System.currentTimeMillis() + (long) (300L * amount);
        while (System.currentTimeMillis() < till) {
        } // vent
    }

    public static void testEqualAmountOfPlayers(ArrayList<GameInfo> list) {
        var infos = list.toArray(new GameInfo[0]);
        System.out.println("checking " + infos[0].player.name);
        Assertions.assertEquals(infos.length, infos[0].getPlayers().length);
        Assertions.assertNotNull(infos[0].getGamemode());
        for (int i = 1; i < infos.length; i++) {
            if (infos[i] == null)
                break;

            System.out.println("checking " + infos[i].player.name);
            Assertions.assertEquals(infos[0].getPlayers().length, infos[i].getPlayers().length);
            Assertions.assertNotNull(infos[i].getGamemode());
            Assertions.assertEquals(infos[0].getGamemode().getAllInfo(), infos[i].getGamemode().getAllInfo());
            Assertions.assertEquals(infos[0].getCountdown(), infos[i].getCountdown());
            Assertions.assertEquals(infos[0].getRaceLightsString(), infos[i].getRaceLightsString());
            Assertions.assertArrayEquals(infos[0].getRaceLights(), infos[i].getRaceLights());
            Assertions.assertEquals(infos[0].getGameID(), infos[i].getGameID());
            Assertions.assertEquals(infos[0].getPaymentRound(), infos[i].getPaymentRound());
            Assertions.assertEquals(Translator.getCloneString(infos[0]), Translator.getCloneString(infos[1]));
            for (var aP : infos[0].getPlayers()) {
                boolean found = false;
                for (var bP : infos[i].getPlayers()) {
                    if (aP.id == bP.id) {
                        if (aP.id == 0) {
                            Assertions.assertTrue(aP.isHost() && aP.role != Player.DEFAULT_ID);
                            Assertions.assertTrue(bP.isHost() && bP.role != Player.DEFAULT_ID);
                        }
                        Assertions.assertEquals(Translator.getCloneString(aP), Translator.getCloneString(bP));
                        Assertions.assertTrue(aP.joined, "In a0, " + aP.name + " has not joined");
                        Assertions.assertTrue(bP.joined, "In b" + i + ", " + bP.name + " has not joined");
                        found = true;
                        break;
                    }
                }
                Assertions.assertTrue(found, "Couldn't find id " + aP.id + " in b" + i);
            }
        }
    }

    class PlayerCom {
        final Player player;
        final GameInfo info;

        public PlayerCom(int i, GameRemoteMaster game, long id) {
            player = new Player("OtherPlayer" + i, Player.DEFAULT_ID, Player.PLAYER, id);
            info = new GameInfo(game, GameType.JOINING_LAN);
            info.join(player, GameInfo.JOIN_TYPE_VIA_CLIENT, (player) -> callbackBool = true, 0, 0);
        }

        public PlayerCom(int i, GameRemoteMaster game) {
            this(i, game, Features.generateLanId(false));
        }
    }

    /**
     * Test joining, bytte gamemode, sjekke om det er likt, kjør kappløp og kjøp
     * tilfeldige ting og fullfør en hel kamp randomly med veldig veldig mange folk.
     */
    @Test
    void lan() {
        Main.DEBUG = false;
        var ran = new Random();
        var totalGameInfos = new ArrayList<GameInfo>();
        callbackBool = false;
        var game = new GameRemoteMaster(null);

        var alice = new Player("Alice", Player.DEFAULT_ID, Player.HOST, Features.generateLanId(true));
        var aliceCom = new GameInfo(game, GameType.CREATING_LAN);
        aliceCom.join(alice, GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);
        aliceCom.ready(alice, (byte) 1);
        var alicePrices = aliceCom.getGamemode().getPrices();
        Assertions.assertTrue(alicePrices.length > 0 && alicePrices[0] > 0);
        aliceCom.carSelectUpdate(alice, 1, true, false, false);
        totalGameInfos.add(aliceCom);

        System.out.println("""

                ==============================
                Joining
                ==============================

                """);
        var otherPlayers = new PlayerCom[4];
        for (int i = 0; i < otherPlayers.length; i++) {
            otherPlayers[i] = new PlayerCom(i, game);
            totalGameInfos.add(otherPlayers[i].info);
        }
        System.out.println("""

                ==============================
                Ending Joining
                ==============================

                """);
        await(totalGameInfos.size());
        testEqualAmountOfPlayers(totalGameInfos);

        System.out.println("""

                ==============================
                Change of gamemode settings
                ==============================

                """);
        aliceCom.init(1);
        System.out.println("""

                ==============================
                Ending init forwards: Change of gamemode settings
                ==============================

                """);
        await(totalGameInfos.size() * 5);
        testEqualAmountOfPlayers(totalGameInfos);
        aliceCom.init(-1); // Kanskje feil ved leadout
        System.out.println("""

                ==============================
                Ending init backwards: Change of gamemode settings
                ==============================

                """);
        await(totalGameInfos.size());
        testEqualAmountOfPlayers(totalGameInfos);

        System.out.println("""

                ==============================
                Change of cars
                ==============================

                """);
        for (var other : otherPlayers) {
            other.info.carSelectUpdate(other.player, ran.nextInt(Texts.CAR_TYPES.length), true, false, false);
        }
        System.out.println("""

                ==============================
                Ending Change of cars
                ==============================

                """);
        await(totalGameInfos.size());
        testEqualAmountOfPlayers(totalGameInfos);

        while (!aliceCom.isGameOver()) {

            for (var other : otherPlayers) {
                TilePiece<?> upgrade;
                while ((upgrade = UpgradesSubscene.canAffordSomething(other.player)) != null) {
                    if (upgrade.upgrade().isPlaced()) {
                        if (upgrade.upgrade() instanceof Upgrade up) {
                            other.info.attemptImproveTile(other.player, up, upgrade.x(), upgrade.y());
                        }
                    } else {
                        other.info.attemptBuyTile(other.player, upgrade);
                    }
                }
            }
            System.out.println("""

                    ==============================
                    Ending Buying
                    ==============================

                    """);
            await(totalGameInfos.size() * 2);
            testEqualAmountOfPlayers(totalGameInfos);

            System.out.println("""

                    ==============================
                    Ready up
                    ==============================

                    """);
            aliceCom.ready(alice, (byte) 1);
            for (var other : otherPlayers) {
                other.info.ready(other.player, (byte) 1);
            }
            System.out.println("""

                    ==============================
                    Ending Ready up
                    ==============================

                    """);
            await(totalGameInfos.size());
            testEqualAmountOfPlayers(totalGameInfos);

            System.out.println("""

                    ==============================
                    Start race
                    ==============================

                    """);
            Assertions.assertTrue(aliceCom.isEveryoneReady());
            var now = System.currentTimeMillis();

            aliceCom.startRace(now);
            for (var other : otherPlayers) {
                Assertions.assertTrue(other.info.isEveryoneReady());
                other.info.startRace(now);
            }
            System.out.println("""

                    ==============================
                    Ending Start race
                    ==============================

                    """);
            await(totalGameInfos.size());
            testEqualAmountOfPlayers(totalGameInfos);
            Assertions.assertTrue(aliceCom.isGameStarted());
            for (var other : otherPlayers) {
                Assertions.assertTrue(other.info.isGameStarted());
            }

            System.out.println("""

                    ==============================
                    Finish race
                    ==============================

                    """);
            System.out.printf("""

                    Starting finish %s

                    """, alice.name);
            aliceCom.finishRace(alice, -1, 120);
            var baseTrackLength = aliceCom.getTrackLength();
            System.out.printf("""

                    Ending finish %s

                    """, alice.name);
            await(totalGameInfos.size());
            for (var other : otherPlayers) {
                final var tl = other.info.getTrackLength();
                Assertions.assertEquals(baseTrackLength, tl);
                System.out.printf("""

                        Starting finish %s

                        """, other.player.name);
                other.info.finishRace(other.player, AI.calculateRace(other.player.car, tl) + ran.nextInt(3000), 22);
                System.out.printf("""

                        Ending finish %s

                        """, other.player.name);
                await(totalGameInfos.size());
            }
            await(totalGameInfos.size());
            testEqualAmountOfPlayers(totalGameInfos);

            System.out.print("""

                    Leave and join

                    """);
            for (int i = 0; i < otherPlayers.length; i++) {
                var other = otherPlayers[i];
                other.info.leave(other.player, true, false);
                System.out.printf("""

                        Left %s

                        """, other.player.name);
                await(totalGameInfos.size());
                otherPlayers[i] = new PlayerCom(i, game, other.player.steamID);
                System.out.printf("""

                        Joining back in %s

                        """, other.player.name);
                await(totalGameInfos.size());
                testEqualAmountOfPlayers(totalGameInfos);
            }
            System.out.print("""

                    Ending Leave and join

                    """);
            await(totalGameInfos.size());
            testEqualAmountOfPlayers(totalGameInfos);
        }

        Texts.init();
        for (var com : totalGameInfos) {
            System.out.println(com.player.getCarRep().toString());
        }
        System.out.println("done");

//		bobCom.finishRace(bob, 1000);
//		charlieCom.finishRace(charlie, 1001);
//		await();
//		Assertions.assertTrue(aliceCom.isGameStarted());

//		await();
//		if (!callbackBool) {
//			aliceCom.close();
//			bobCom.close();
//			Assertions.fail();
//		}
//		var bobPrices = bobCom.getGamemode().getPrices();
//		Assertions.assertArrayEquals(alicePrices, bobPrices, 0f);
//		testEqualAmountOfPlayers(aliceCom, bobCom);

//		aliceCom.init(1);
//		await();
//		Assertions.assertEquals(aliceCom.getGamemode().getAllInfo(), bobCom.getGamemode().getAllInfo());
//		Assertions.assertEquals(aliceCom.getGamemode().getAllInfo(), charlieCom.getGamemode().getAllInfo());

//		bobCom.leave(bob, false);
//		await();
//		testEqualAmountOfPlayers(aliceCom, charlieCom);
//		bob = new Player("Bob", Player.DEFAULT_ID, Player.PLAYER, bob.steamID);
//		bobCom = new GameInfo(game, GameType.JOINING_LAN);
//		bobCom.join(bob, GameInfo.JOIN_TYPE_VIA_CLIENT, (player) -> {
//			callbackBool = true;
//			System.out.println("SET callbackBool " + callbackBool);
//		}, 0, 0);
//
//		await();
//		Assertions.assertEquals(aliceCom.getGamemode().getAllInfo(), bobCom.getGamemode().getAllInfo());
//		Assertions.assertEquals(aliceCom.getGamemode().getAllInfo(), charlieCom.getGamemode().getAllInfo());
//
//		bobCom.ready(bob, (byte) 1);
//		charlieCom.ready(charlie, (byte) 1);
//		aliceCom.ready(alice, (byte) 1);
//		await();
//		Assertions.assertTrue(aliceCom.isEveryoneReady());
//		Assertions.assertTrue(bobCom.isEveryoneReady());
//		Assertions.assertTrue(charlieCom.isEveryoneReady());
//
//        aliceCom.startRace();
//		bobCom.startRace();
//		charlieCom.startRace();
//
//		Assertions.assertTrue(aliceCom.isGameStarted());
//		Assertions.assertTrue(bobCom.isGameStarted());
//		Assertions.assertTrue(charlieCom.isGameStarted());
//
//		aliceCom.finishRace(alice, -1);
//		bobCom.finishRace(bob, 1000);
//		charlieCom.finishRace(charlie, 1001);
//		await();
//		Assertions.assertTrue(aliceCom.isGameStarted());

    }
    /*
     * @Test void testPlayingSingleplayerManyTimesOver() { Player player = new
     * Player("Jens", 0, Player.HOST, 0); GameInfo game = new GameInfo(null,
     * GameType.SINGLEPLAYER); }
     *
     * void pointsTest(Player[] players, GameInfo info) {
     * players[0].timeLapsedInRace = 100; players[1].timeLapsedInRace = -1;
     *
     * info.determinePositioningFinishedRace(players); // dnf
     * info.determinePositioningFinishedRace(players); // dnf
     * players[1].timeLapsedInRace = 120;
     * info.determinePositioningFinishedRace(players); // last place
     *
     * assertEquals(3, players[0].aheadByPoints); // skal v�re 3 bak player0
     *
     * for (var player : players) player.bank.reset(); }
     *
     * @Test void pointsTest() { Player[] players = { new Player("Jens", 0,
     * Player.HOST, 0), new Player("Gonnar", 1, Player.PLAYER, 0), }; GameInfo info
     * = new GameInfo(null, GameType.SINGLEPLAYER); Assert.assertNotEquals(0,
     * info.getGameID()); info.join(players[0], Player.HOST, null, 0, 0); for (var
     * gm : GameModes.values()) { if (gm == GameModes.SINGLEPLAYER_CHALLENGES) {
     * Player[] spPlayer = { players[0], };
     *
     * assertTrue("Init of singleplayer failed (" + gm + ") returned null",
     * info.init(gm, 0) != null);
     *
     * spPlayer[0].timeLapsedInRace = info.getGamemode().getEndGoalStandard() -
     * 1000; info.determinePositioningFinishedRace(spPlayer);
     *
     * for (var player : spPlayer) player.bank.reset(); } else {
     * assertTrue("Init of gamemode (" + gm + ") returned null, is it implemented?",
     * info.init(gm, 0) != null); pointsTest(players, info); } } }
     *
     * @Test void lobbyCreationWorks() { new Features(null, null, null); var lobby =
     * new Lobby(null, new LobbyTopbar(null, new TopbarInteraction(null)));
     * lobby.createNewLobby("Player", Player.HOST, GameType.SINGLEPLAYER, 0); var
     * role = lobby.getCom().player.role; boolean properRole = false; for (var
     * possibleRole : Player.POSSIBLE_ROLES) { if (role == possibleRole) {
     * properRole = true; break; } }
     *
     * assertTrue(properRole); }
     *
     * @Test void testPlayerRecognizedUpgrades() { Texts.init();
     *
     * Player player = new Player("test", (byte) 0, Player.HOST, 0); var upgradesOG
     * = player.upgrades.getUpgradesNonNulls(); var upgradesCloned =
     * player.getClone().upgrades.getUpgradesNonNulls();
     * Assert.assertArrayEquals(upgradesOG, upgradesCloned); }
     *
     * @Test void testGainedValuesFailedAtSellingTile() { Player player = new
     * Player(); TileUpgrade up = new TileUpgrade(); up.setUpgrade(player,
     * player.upgrades.getUpgrade(0)); up.sell(player.bank, player.getCarRep(), 0);
     * }
     *
     * @Test void canAffordSomethingAtStartOfEveryGameMode() { GameInfo info = new
     * GameInfo(null, GameType.SINGLEPLAYER); var player = new Player("Jens", 0,
     * Player.HOST, 0); info.join(player, Player.HOST, null, 0, 0);
     *
     * for (var gm : GameModes.values()) { info.carSelectUpdate(player, 0, false);
     * info.init(gm, 0); info.getGamemode().setPrices(player.upgrades);
     * info.startRace(); info.finishRace(player, -1);
     *
     * System.out.println("Checking " + gm + " with $" + (int)
     * player.bank.get(Bank.MONEY)); boolean canAffordOne = false; for (var up :
     * player.upgrades.getUpgradesNonNulls()) {
     * System.out.print(Texts.getUpgradeTitle(up)); System.out.print("\n\tVisible: "
     * + up.isVisible() + ", Cost: " + up.getCost(player.bank)); if (up.isVisible()
     * && up.canAfford(player.bank)) { System.out.print(" - Can afford it.");
     * canAffordOne = true; } System.out.println(); }
     * Assert.assertTrue(canAffordOne); } }
     *
     */
//	@Test
//	void upgrade() {
//		Car car = new Car();
//		car.switchTo(2);
//		String ogStr = car.getRep().toString();
//		String expected = "2, 1.0, 1000.0, 0.6, 80.0, 740.0, 204.0, 800.0, 5500.0, 6.0, 1200.0, 0.0, 25.0, 100.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0784313725490196, 0.0";
//		System.out.println(ogStr);
//		System.out.println(expected);
//		assertEquals(expected, ogStr);
//		
//		var og = new RegVals(new double[]{ 0, 100, 1.1, RegVals.decimals + 0.1, 0, RegVals.specialPercent + 1.02});
//		og.upgrade(car.getRep());
//		String modStr = car.getRep().toString();
//		expected = "2, 1.0, 1100.0, 0.66, 80.1, 740.0, 208.08, 800.0, 5500.0, 6.0, 1200.0, 0.0, 25.0, 100.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0784313725490196, 0.0";
//		System.out.println(modStr);
//		System.out.println(expected);
//		assertEquals(expected, modStr);
//		
//		og.multiplyAllValues(10);
//		assertEquals("+1000 nos ms, +100% nos, +1 kW, +20% km/h", og.getUpgradeRepString());
//		
//		og.upgrade(car.getRep());
//		modStr = car.getRep().toString();
//		expected = "2, 1.0, 2100.0, 1.32, 81.1, 740.0, 249.696, 800.0, 5500.0, 6.0, 1200.0, 0.0, 25.0, 100.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0784313725490196, 0.0";
//		System.out.println(modStr);
//		System.out.println(expected);
//		assertEquals(expected, modStr);
//		
//	}

//	@Test
//	void neighbourMod() {
//		var og = new RegVals(new double[]{ 0, 100, 1.1, RegVals.decimals + 0.1});
//		String str = og.getUpgradeRepString(0, 0);
//		assertEquals("+100 nos ms, +10% nos, +0.1 kW", str);
//		
//		var mod = new RegVals(new double[]{ 0, RegVals.decimals + 0.1 , 100, 1.1});
//		og.combineNeighbour(mod);
//		str = og.getUpgradeRepString(0, 0);
//		assertEquals("+100.1 nos ms, +10% nos, +0.11 kW", str);
//
//		mod = new RegVals(new double[]{ 0, 1.1, RegVals.decimals + 0.1 , 100});
//		og.combineNeighbour(mod);
//		str = og.getUpgradeRepString(0, 0);
//		assertEquals("+110.11 nos ms, +10% nos, +100.11 kW", str);
//
//		mod = new RegVals(new double[]{ 0, 0, 0, 0, 0, 1.2, 0.5});
//		mod.values[Rep.tbArea] = RegVals.specialPercent + 1.69;
//		og.combineNeighbour(mod);
//		str = og.getUpgradeRepString(0, 0);
//		assertEquals("+110.11 nos ms, +10% nos, +100.11 kW, +20% km/h, -50% idle-rpm, +69% tb area", str);
//
//		mod = new RegVals(new double[]{ 0, 0, 0, 0, 0, 0.5, 1.2});
//		og.combineNeighbour(mod);
//		str = og.getUpgradeRepString(0, 0);
//		assertEquals("+110.11 nos ms, +10% nos, +100.11 kW, +10% km/h, -60% idle-rpm, +69% tb area", str);
//
//		
//		og = new RegVals(new double[]{0, 0, 0, 0, 0, 1.05});
//		mod = new RegVals(new double[]{0, 0, 0, 0, 0, RegVals.specialPercent + 1.02});
//		og.combineNeighbour(mod);
//		str = og.getUpgradeRepString(0, 0);
//		assertEquals("+7% km/h", str);
//	}

    @Test
    void testLayerTimesmod() {
        Main.DEBUG = false;
        var layer = new Layer();

        for (int i = 0; i < 1000; i++) {
            layer.reset();
            layer.createModifierTiles(2, 6, 10);
            int amount = 0;
            for (int x = 0; x < layer.getWidth(); x++) {
                for (int y = 0; y < layer.getHeight(); y++) {
                    amount += layer.getTimesMod(x, y);
                }
            }
            Assertions.assertEquals(10, amount);
        }
    }

}

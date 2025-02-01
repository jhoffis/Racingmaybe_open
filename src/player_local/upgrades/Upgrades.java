package player_local.upgrades;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import adt.ICloneStringable;
import engine.utils.TwoTypes;
import main.Features;
import player_local.Layer;
import player_local.car.Rep;
import player_local.upgrades.RegVal.RegValType;

public class Upgrades implements ICloneStringable {

	public static byte size = 0, sizeUpgrades = 0, sizeTools = 0;
	public final static String indicator = "~", upgradeIndicator = "[", toolIndicator = "]";

	private final CopyOnWriteArrayList<UpgradeGeneral> upgradeReferences = new CopyOnWriteArrayList<>();
	private final Queue<Byte> lastFocusedUpgrade;

	public boolean hasTools = true;

	public void clear() {
		upgradeReferences.clear();
	}

	public Upgrades(Rep rep, Layer layer) {
		lastFocusedUpgrade = new LinkedList<>();
		resetTowardsCar(rep, layer);
	}

	public static Upgrade createUpgrade(TileNames name, CopyOnWriteArrayList<UpgradeGeneral> upgradeReferences) {
		var upgrade = new Upgrade(name);
		Features.fillNullListSize(upgradeReferences, name.ordinal());
		upgradeReferences.set(name.ordinal(), upgrade);
		return upgrade;
	}

	private void createTool(TileNames name, Layer layer, CopyOnWriteArrayList<UpgradeGeneral> upgradeReferences) {
		var tool = new Tool(name, layer);
		Features.fillNullListSize(upgradeReferences, name.ordinal());
		upgradeReferences.set(name.ordinal(), tool);
	}

	public void resetPrices() {

		for (var upgrade : upgradeReferences) {
			if (upgrade != null)
				upgrade.setPremadePrice(0);
		}
		UpgradeGeneral up;
		up = upgradeReferences.get(TileNames.Power.ordinal());
		if (up != null)
			up.setPremadePrice(120);
		up = upgradeReferences.get(TileNames.Boost.ordinal());
		if (up != null)
			up.setPremadePrice(120);
		up = upgradeReferences.get(TileNames.Finance.ordinal());
		if (up != null)
			up.setPremadePrice(150);
		up = upgradeReferences.get(TileNames.MoneyPit.ordinal());
		if (up != null)
			up.setPremadePrice(60);
		up = upgradeReferences.get(TileNames.Interest.ordinal());
		if (up != null)
			up.setPremadePrice(60);

		up = upgradeReferences.get(TileNames.NeighborCollector.ordinal());
		if (up != null)
			up.setPremadePrice(80);
		up = upgradeReferences.get(TileNames.TimesModPlanter.ordinal());
		if (up != null)
			up.setPremadePrice(80);
		up = upgradeReferences.get(TileNames.Dilator.ordinal());
		if (up != null)
			up.setPremadePrice(80);
		up = upgradeReferences.get(TileNames.NeighborTunnel.ordinal());
		if (up != null)
			up.setPremadePrice(60);
		up = upgradeReferences.get(TileNames.Seeper.ordinal());
		if (up != null)
			up.setPremadePrice(80);
		up = upgradeReferences.get(TileNames.RightRotator.ordinal());
		if (up != null)
			up.setPremadePrice(5);
		up = upgradeReferences.get(TileNames.LeftRotator.ordinal());
		if (up != null)
			up.setPremadePrice(5);
		up = upgradeReferences.get(TileNames.Merchant.ordinal());
		if (up != null)
			up.setPremadePrice(80);
		up = upgradeReferences.get(TileNames.Permanentifier.ordinal());
		if (up != null)
			up.setPremadePrice(80);
		up = upgradeReferences.get(TileNames.Yunomah.ordinal());
		if (up != null)
			up.setPremadePrice(80);
//		up = upgradeReferences.get(TileNames.Uninsion.ordinal());
//		if (up != null)
//			up.setPremadePrice(80);
	}

	public void resetTowardsCar(Rep rep, Layer layer) {
		var upgradeReferences = new CopyOnWriteArrayList<UpgradeGeneral>();
		RegVals bonus;
		Upgrade upgrade;

		/*
		 * POWER
		 */
		upgrade = createUpgrade(TileNames.Power, upgradeReferences);
		upgrade.setMaxLVL(1);
		upgrade.setUpgradeType(UpgradeType.POWER);
		upgrade.starterUpgrade = true;
		upgrade.getRegVals().values()[Rep.rpmTop] = new RegVal(500, RegValType.Decimal);
		upgrade.getRegVals().values()[Rep.rpmIdle] = new RegVal(100, RegValType.Decimal);
		upgrade.getNeighbourModifier().values()[Rep.rpmTop] = new RegVal(100, RegValType.Decimal);
		upgrade.pushUpgradeUnlock(TileNames.Block);

		/*
		 * BOOST
		 */
		upgrade = createUpgrade(TileNames.Boost, upgradeReferences);
		upgrade.setMaxLVL(1);
		upgrade.setUpgradeType(UpgradeType.BOOST);
		upgrade.starterUpgrade = true;
		upgrade.getRegVals().values()[Rep.nos] = new RegVal(0.15, RegValType.Decimal);
		upgrade.getRegVals().values()[Rep.tb] = new RegVal(0.15, RegValType.Decimal);
		upgrade.getNeighbourModifier().values()[Rep.nosMs] = new RegVal(15, RegValType.Decimal);
		upgrade.getNeighbourModifier().values()[Rep.tbMs] = new RegVal(15, RegValType.Decimal);
		upgrade.pushUpgradeUnlock(TileNames.Tireboost);
		upgrade.pushUpgradeUnlock(TileNames.BlueNOS);

		/*
		 * FINANCE
		 */
		upgrade = createUpgrade(TileNames.Finance, upgradeReferences);
		upgrade.setMaxLVL(1);
		upgrade.setUpgradeType(UpgradeType.ECO);
		upgrade.starterUpgrade = true;
		upgrade.getRegVals().values()[Rep.moneyPerTurn] = new RegVal(15, RegValType.Decimal);
		upgrade.getNeighbourModifier().values()[Rep.moneyPerTurn] = new RegVal(1.5f, RegValType.NormalPercent);
		upgrade.getNeighbourModifier().values()[Rep.moneyPerTurn].only = true;
		upgrade.pushUpgradeUnlock(TileNames.MoneyPit);

		/*
		 * CLUTCH
		 */
		upgrade = createUpgrade(TileNames.Clutch, upgradeReferences);
		upgrade.setMaxLVL(3);
		upgrade.setUpgradeType(UpgradeType.ECO);
		upgrade.getRegVals().values()[Rep.spdTop] = new RegVal(1.03, RegValType.NormalPercent);
		upgrade.getNeighbourModifier().values()[Rep.spdTop] = new RegVal(15, RegValType.Decimal);
		upgrade.pushUpgradeUnlock(TileNames.Gears);
		upgrade.bonusCostOverride = 150;

		bonus = upgrade.pushBonus(3);
		var bonusValues = bonus.values(); 
		bonusValues[Rep.throttleShift] = new RegVal(1, RegValType.Unlock);
		bonusValues = bonus.addChoiceList(0);
		bonusValues[Rep.twoStep] = new RegVal(1, RegValType.Unlock);

		/*
		 * GEARS
		 */
		upgrade = createUpgrade(TileNames.Gears, upgradeReferences);
		upgrade.setMaxLVL(3);
		upgrade.setUpgradeType(UpgradeType.ECO);
		upgrade.getRegVals().values()[Rep.spdTop] = new RegVal(25, RegValType.Decimal);
		upgrade.getRegVals().values()[Rep.gearTop] = new RegVal(1, RegValType.Decimal);
		upgrade.getRegVals().values()[Rep.gearTop].removeAtPlacement = true;
		upgrade.getRegVals().changeAfterUpgrade[Rep.spdTop] = new RegVal(15, RegValType.Decimal);
		upgrade.getNeighbourModifier().values()[Rep.spdTop] = new RegVal(1.01, RegValType.AdditionPercent);
		upgrade.pushUpgradeUnlock(TileNames.Aero);

		if (rep != null && !rep.is(Rep.sequential)) {
			upgrade.bonusCostOverride = 300;
			bonus = upgrade.pushBonus(3);
			bonusValues = bonus.values(); 
			bonusValues[Rep.sequential] = new RegVal(1, RegValType.Unlock);
		}

		/*
		 * Aero
		 */
		upgrade = createUpgrade(TileNames.Aero, upgradeReferences);
		upgrade.setMaxLVL(3);
		upgrade.setUpgradeType(UpgradeType.ECO);
		upgrade.getRegVals().values()[Rep.aero] = new RegVal(.97, RegValType.NormalPercent);
		upgrade.getNeighbourModifier().values()[Rep.aero] = new RegVal(.99, RegValType.AdditionPercent);

//		bonus = upgrade.pushBonus(3);
//		bonusValues = bonus.values(); 
//		bonusValues[Rep.aero] = new RegVal(.80, RegValType.NormalPercent);

		/*
		 * MONEY PIT
		 */
		upgrade = createUpgrade(TileNames.MoneyPit, upgradeReferences);
		upgrade.setMaxLVL(-1);
		upgrade.setUpgradeType(UpgradeType.ECO);
		upgrade.getRegVals().values()[Rep.moneyPerTurn] = new RegVal(10, RegValType.Decimal);
		upgrade.getRegVals().changeAfterUpgrade[Rep.moneyPerTurn] = new RegVal(5, RegValType.Decimal);
		upgrade.getNeighbourModifier().values()[Rep.moneyPerTurn] = new RegVal(5, RegValType.Decimal);
		upgrade.pushUpgradeUnlock(TileNames.Interest);

		bonus = upgrade.pushBonus(5);
		bonusValues = bonus.values(); 
		bonusValues[Rep.moneyPerTurn] = new RegVal(1.1, RegValType.NormalPercent);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.moneyPerTurn] = new RegVal(1.15, RegValType.NormalPercent);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.moneyPerTurn] = new RegVal(1.2, RegValType.NormalPercent);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.moneyPerTurn] = new RegVal(1.25, RegValType.NormalPercent);

		/*
		 * INTREST
		 */
		upgrade = createUpgrade(TileNames.Interest, upgradeReferences);
		upgrade.setMaxLVL(3);
		upgrade.setUpgradeType(UpgradeType.ECO);
		upgrade.getRegVals().values()[Rep.interest] = new RegVal(1.02, RegValType.AdditionPercent);
		upgrade.getNeighbourModifier().values()[Rep.moneyPerTurn] = new RegVal(5, RegValType.Decimal);

		/*
		 * NOS bottles
		 */
		upgrade = createUpgrade(TileNames.BlueNOS, upgradeReferences);
		upgrade.setMaxLVL(8);
		upgrade.setUpgradeType(UpgradeType.BOOST);
		upgrade.getRegVals().values()[Rep.nos] = new RegVal(0.1, RegValType.Decimal);
		upgrade.getRegVals().values()[Rep.nosBottles] = new RegVal(1, RegValType.Decimal);
		upgrade.getRegVals().values()[Rep.nosBottles].removeAtPlacement = true;
		upgrade.getRegVals().changeAfterUpgrade[Rep.nos] = new RegVal(0.05, RegValType.Decimal);
		upgrade.getNeighbourModifier().values()[Rep.nos] = new RegVal(1.0025, RegValType.AdditionPercent);
		upgrade.pushUpgradeUnlock(TileNames.RedNOS);
		upgrade.pushUpgradeUnlock(TileNames.Clutch);
		
		bonus = upgrade.pushBonus(3);
		bonusValues = bonus.values(); 
		bonusValues[Rep.nos] = new RegVal(.9, RegValType.NormalPercent);
		bonusValues[Rep.nosMs] = new RegVal(250, RegValType.Decimal);
		
		bonus = upgrade.pushBonus(8);
		bonusValues = bonus.values(); 
		bonusValues[Rep.nosBottles] = new RegVal(1, RegValType.Decimal);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.nosBottles] = new RegVal(2, RegValType.Decimal);

		/*
		 * NOS
		 */
		upgrade = createUpgrade(TileNames.RedNOS, upgradeReferences);
		upgrade.setMaxLVL(-1);
		upgrade.setUpgradeType(UpgradeType.BOOST);
		upgrade.getRegVals().values()[Rep.nos] = new RegVal(1.01, RegValType.NormalPercent);
		upgrade.getNeighbourModifier().values()[Rep.nos] = new RegVal(.1, RegValType.Decimal);
		upgrade.bonusCostOverride = 200;

		bonus = upgrade.pushBonus(5);
		bonusValues = bonus.values(); 
		bonusValues[Rep.nosAuto] = new RegVal(1, RegValType.Unlock);
		
		bonus = upgrade.pushBonus(7);
		bonusValues = bonus.values(); 
		bonusValues[Rep.rpmIdle] = new RegVal(1000, RegValType.Decimal);
		bonusValues = bonus.addChoiceList(0);
		bonusValues[Rep.nos] = new RegVal(1.15, RegValType.NormalPercent);
		bonusValues = bonus.addChoiceList(1);
		bonusValues[Rep.snos] = new RegVal(1, RegValType.Unlock);

		/*
		 * TIREBOOST
		 */
		upgrade = createUpgrade(TileNames.Tireboost, upgradeReferences);
		upgrade.setMaxLVL(-1);
		upgrade.setUpgradeType(UpgradeType.BOOST);
		upgrade.getRegVals().values()[Rep.tb] = new RegVal(0.2, RegValType.Decimal);
		upgrade.getRegVals().changeAfterUpgrade[Rep.tb] = new RegVal(0.2, RegValType.Decimal);
		upgrade.getNeighbourModifier().values()[Rep.tb] = new RegVal(0.1, RegValType.Decimal);
		upgrade.pushUpgradeUnlock(TileNames.Clutch);
		upgrade.pushUpgradeUnlock(TileNames.TireboostHeater);

		bonus = upgrade.pushBonus(5);
		bonusValues = bonus.values(); 
		bonusValues[Rep.tbArea] = new RegVal(1, RegValType.Unlock);
		bonusValues = bonus.addChoiceList(0);
		bonusValues[Rep.tbMs] = new RegVal(250, RegValType.Decimal);
		bonusValues = bonus.addChoiceList(1);
		bonusValues[Rep.aero] = new RegVal(.9, RegValType.NormalPercent);
		bonusValues = bonus.addChoiceList(2);
		bonusValues[Rep.tb] = new RegVal(1.10, RegValType.NormalPercent);

		bonus = upgrade.pushBonus(7);
		bonusValues = bonus.values();
		bonusValues[Rep.tbMs] = new RegVal(200, RegValType.Decimal);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.tbMs] = new RegVal(400, RegValType.Decimal);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.tbMs] = new RegVal(600, RegValType.Decimal);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.tbMs] = new RegVal(800, RegValType.Decimal);
		
		bonus = upgrade.pushBonus(10);
		bonusValues = bonus.values();
		bonusValues[Rep.tb] = new RegVal(1.1, RegValType.NormalPercent);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.tb] = new RegVal(1.15, RegValType.NormalPercent);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.tb] = new RegVal(1.20, RegValType.NormalPercent);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.tb] = new RegVal(1.25, RegValType.NormalPercent);
		
		bonus = upgrade.pushBonus(15);
		bonusValues = bonus.values();
		bonusValues[Rep.tbHeat] = new RegVal(1.1, RegValType.NormalPercent);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.tbHeat] = new RegVal(1.15, RegValType.NormalPercent);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.tbHeat] = new RegVal(1.20, RegValType.NormalPercent);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.tbHeat] = new RegVal(1.25, RegValType.NormalPercent);
		
		/*
		 * TIREBOOST HEATER
		 */
		upgrade = createUpgrade(TileNames.TireboostHeater, upgradeReferences);
		upgrade.setMaxLVL(4);
		upgrade.setUpgradeType(UpgradeType.BOOST);
		upgrade.getRegVals().values()[Rep.tbHeat] = new RegVal(1, RegValType.Decimal);
		upgrade.getRegVals().changeAfterUpgrade[Rep.tbHeat] = new RegVal(0.25, RegValType.Decimal);
		upgrade.getNeighbourModifier().values()[Rep.tb] = new RegVal(1.33, RegValType.NormalPercent);
		upgrade.getNeighbourModifier().values()[Rep.tbHeat] = new RegVal(1.02, RegValType.AdditionPercent);
		upgrade.getNeighbourModifier().values()[Rep.tb].only = true;
		upgrade.pushUpgradeUnlock(TileNames.Block);

		bonus = upgrade.pushBonus(4);
		bonusValues = bonus.values(); 
		bonusValues[Rep.tb] = new RegVal(1.05, RegValType.NormalPercent);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.tb] = new RegVal(1.1, RegValType.NormalPercent);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.tb] = new RegVal(1.15, RegValType.NormalPercent);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.tb] = new RegVal(1.20, RegValType.NormalPercent);


		/*
		 * WEIGHT
		 */
		upgrade = createUpgrade(TileNames.WeightReduction, upgradeReferences);
		upgrade.setMaxLVL(3);
		upgrade.setUpgradeType(UpgradeType.POWER);
		upgrade.getRegVals().values()[Rep.kg] = new RegVal(0.97, RegValType.NormalPercent);
		upgrade.getNeighbourModifier().values()[Rep.kg] = new RegVal(0.9975, RegValType.AdditionPercent);
		upgrade.pushUpgradeUnlock(TileNames.LighterPistons);
		upgrade.pushUpgradeUnlock(TileNames.Clutch);

//		bonus = upgrade.pushBonus(3);
//		bonusValues = bonus.values();
//		bonusValues[Rep.kg] = new RegVal(.97, RegValType.NormalPercent);
//		bonusValues = bonus.addChoiceList();
//		bonusValues[Rep.kg] = new RegVal(.94, RegValType.NormalPercent);
//		bonusValues = bonus.addChoiceList();
//		bonusValues[Rep.kg] = new RegVal(.91, RegValType.NormalPercent);
//		bonusValues = bonus.addChoiceList();
//		bonusValues[Rep.kg] = new RegVal(.88, RegValType.NormalPercent);

		/*
		 * PISTONS
		 */
		upgrade = createUpgrade(TileNames.LighterPistons, upgradeReferences);
		upgrade.setMaxLVL(-1);
		upgrade.setUpgradeType(UpgradeType.POWER);
		upgrade.getRegVals().values()[Rep.kW] = new RegVal(1.01, RegValType.NormalPercent);
		upgrade.getRegVals().values()[Rep.kg] = new RegVal(.99, RegValType.NormalPercent);
		upgrade.getRegVals().values()[Rep.kg].removeAtPlacement = true;
		upgrade.getNeighbourModifier().values()[Rep.kW] = new RegVal(1.005, RegValType.AdditionPercent);
		upgrade.bonusCostOverride = 3000;

		bonus = upgrade.pushBonus(10);
		bonusValues = bonus.values(); 
		bonusValues[Rep.stickyclutch] = new RegVal(1, RegValType.Unlock);

		/*
		 * TURBO
		 */
		upgrade = createUpgrade(TileNames.Turbo, upgradeReferences);
		upgrade.setMaxLVL(6);
		upgrade.setUpgradeType(UpgradeType.POWER);
		upgrade.getRegVals().values()[Rep.bar] = new RegVal(.4, RegValType.Decimal);
		upgrade.getRegVals().values()[Rep.kg] = new RegVal(8, RegValType.Decimal);
		upgrade.getRegVals().values()[Rep.kg].removeAtPlacement = true;
		upgrade.getRegVals().changeAfterUpgrade[Rep.bar] = new RegVal(.2, RegValType.Decimal);
		upgrade.getRegVals().changeAfterUpgrade[Rep.turboblow] = new RegVal(2.5, RegValType.Decimal);
		upgrade.getNeighbourModifier().values()[Rep.turboblowStrength] = new RegVal(1.05, RegValType.AdditionPercent);
		upgrade.getNeighbourModifier().values()[Rep.turboblowStrength].ignoreDifferentNewOne = true;
		upgrade.getNeighbourModifier().values()[Rep.turboblowRegen] = new RegVal(3, RegValType.Decimal);
		upgrade.pushUpgradeUnlock(TileNames.Clutch);
		upgrade.pushUpgradeMaxLVLChange(TileNames.Supercharger, -.5f);

		bonus = upgrade.pushBonus(3);
		bonusValues = bonus.values(); 
		bonusValues[Rep.spool] = new RegVal(.9, RegValType.NormalPercent);
		bonusValues[Rep.turboblowRegen] = new RegVal(30, RegValType.Decimal);
		bonusValues[Rep.turboblowStrength] = new RegVal(1.2, RegValType.NormalPercent);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.spool] = new RegVal(.85, RegValType.NormalPercent);
		bonusValues[Rep.turboblowRegen] = new RegVal(60, RegValType.Decimal);
		bonusValues[Rep.turboblowStrength] = new RegVal(1.3, RegValType.NormalPercent);
		
		bonus = upgrade.pushBonus(6);
		bonusValues = bonus.values(); 
		bonusValues[Rep.turboblowRegen] = new RegVal(2, RegValType.NormalPercent);
		bonusValues = bonus.addChoiceList(0);
		bonusValues[Rep.turboblowStrength] = new RegVal(1.15, RegValType.NormalPercent);
		bonusValues = bonus.addChoiceList(1);
		bonusValues[Rep.bar] = new RegVal(1.25, RegValType.NormalPercent);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.bar] = new RegVal(2, RegValType.NormalPercent);

		/*
		 * SUPERCHARGER
		 */
		upgrade = createUpgrade(TileNames.Supercharger, upgradeReferences);
		upgrade.setMaxLVL(3);
		upgrade.setUpgradeType(UpgradeType.POWER);
		upgrade.getRegVals().values()[Rep.bar] = new RegVal(.8, RegValType.Decimal);
		upgrade.getRegVals().values()[Rep.kW] = new RegVal(0.9, RegValType.NormalPercent);
		upgrade.getRegVals().values()[Rep.kW].removeAtPlacement = true;
		upgrade.getRegVals().values()[Rep.kg] = new RegVal(14, RegValType.Decimal);
		upgrade.getRegVals().values()[Rep.kg].removeAtPlacement = true;
		upgrade.getNeighbourModifier().values()[Rep.spool] = new RegVal(1.02, RegValType.NormalPercent);
		upgrade.getNeighbourModifier().values()[Rep.spoolStart] = new RegVal(.01, RegValType.Decimal);
		upgrade.pushUpgradeUnlock(TileNames.Clutch);
		upgrade.pushUpgradeMaxLVLChange(TileNames.Turbo, -1.83f);
		upgrade.bonusCostOverride = 75;

		bonus = upgrade.pushBonus(3);
		bonusValues = bonus.values(); 
		bonusValues[Rep.spoolStart] = new RegVal(.25, RegValType.Decimal);
		bonusValues[Rep.spool] = new RegVal(1.5, RegValType.NormalPercent);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.spoolStart] = new RegVal(.5, RegValType.Decimal);
		bonusValues[Rep.spool] = new RegVal(2, RegValType.NormalPercent);

		/*
		 * BLOCK
		 */
		upgrade = createUpgrade(TileNames.Block, upgradeReferences);
		upgrade.setMaxLVL(5);
		upgrade.setUpgradeType(UpgradeType.POWER);
		upgrade.getRegVals().values()[Rep.kW] = new RegVal(50, RegValType.Decimal);
		upgrade.getRegVals().changeAfterUpgrade[Rep.kW] = new RegVal(20, RegValType.Decimal);
		upgrade.getRegVals().changeAfterUpgrade[Rep.kg] = new RegVal(10, RegValType.Decimal);
		upgrade.getNeighbourModifier().values()[Rep.kW] = new RegVal(10, RegValType.Decimal);
		upgrade.getNeighbourModifier().values()[Rep.tb] = new RegVal(1.25, RegValType.NormalPercent);
		upgrade.getNeighbourModifier().values()[Rep.tb].only = true;
		upgrade.getNeighbourModifier().values()[Rep.tb].replaceNeg = true;
		upgrade.requireUpgradeToUnlock(TileNames.Power);
		upgrade.pushUpgradeUnlock(TileNames.Turbo);
		upgrade.pushUpgradeUnlock(TileNames.Supercharger);
		upgrade.pushUpgradeUnlock(TileNames.WeightReduction);

		bonus = upgrade.pushBonus(5);
		bonusValues = bonus.values(); 
		bonusValues[Rep.kW] = new RegVal(250, RegValType.Decimal);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.kW] = new RegVal(500, RegValType.Decimal);
		bonusValues = bonus.addChoiceList();
		bonusValues[Rep.kW] = new RegVal(750, RegValType.Decimal);

		createTool(TileNames.NeighborCollector, layer, upgradeReferences);
		createTool(TileNames.TimesModPlanter, layer, upgradeReferences);
		createTool(TileNames.Permanentifier, layer, upgradeReferences);
		createTool(TileNames.Seeper, layer, upgradeReferences);
		createTool(TileNames.RightRotator, layer, upgradeReferences);
		
		createTool(TileNames.Dilator, layer, upgradeReferences);
		createTool(TileNames.Merchant, layer, upgradeReferences);
		createTool(TileNames.Yunomah, layer, upgradeReferences);
		createTool(TileNames.NeighborTunnel, layer, upgradeReferences);
		createTool(TileNames.LeftRotator, layer, upgradeReferences);
//		createTool(TileNames.Uninsion, layer, upgradeReferences);

		sizeUpgrades = 0;
		sizeTools = 0;
		for (var up : upgradeReferences) {
			if (up == null) {
				continue;
			}
			if (up instanceof Upgrade upgradeType) {
				upgradeType.setVisible(upgradeType.starterUpgrade);
				sizeUpgrades++;
			} else if (up instanceof Tool) {
				sizeTools++;
			}
		}
		size = (byte) upgradeReferences.size();
		this.upgradeReferences.clear();
		this.upgradeReferences.addAll(upgradeReferences);
		resetPrices();
	}
	
	public void createFuel() {
		var upgrade = createUpgrade(TileNames.Fuel, upgradeReferences);
		upgrade.setMaxLVL(3);
		upgrade.setUpgradeType(UpgradeType.POWER);
		upgrade.getRegVals().values()[Rep.kW] = new RegVal(2, RegValType.Decimal);
		upgrade.getNeighbourModifier().values()[Rep.kW] = new RegVal(2, RegValType.Decimal);
		upgrade.getRegVals().changeAfterUpgrade[Rep.kW] = new RegVal(102, RegValType.Decimal);
		
		var bonus = upgrade.pushBonus(3);
		var bonusValues = bonus.values(); 
		bonusValues[Rep.spdTop] = new RegVal(250, RegValType.Decimal);
	}

	public ArrayList<Upgrade> getUpgrades() {
		var ups = new ArrayList<Upgrade>();
		for (var u : upgradeReferences) {
			if (u instanceof Upgrade upgrade)
				ups.add(upgrade);
		}
		return ups;
	}

	public ArrayList<Tool> getTools() {
		var ups = new ArrayList<Tool>();
		for (var u : upgradeReferences) {
			if (u instanceof Tool tool)
				ups.add(tool);
		}
		return ups;
	}
//	public static Player upgradeClone(Player player, Upgrade upgrade, Vec2 pos, boolean test) {
//		var clone = new Player();
//		clone.setClone(player.bank, player.layer, player.getCarRep().getClone());
//		upgrade.upgrade(clone, pos, test);
//		return clone;
//	}

	public UpgradeGeneral getUpgrade(int i) {
		return upgradeReferences.size() > i ? upgradeReferences.get(i) : null;
	}

	public TwoTypes<UpgradeGeneral, Integer> getUpgrade(TileNames name) {
		int i = name.ordinal();
		return new TwoTypes<>(getUpgrade(i), i);
	}

	public UpgradeGeneral[] getUpgradesNonNulls() {
		var res = new ArrayList<UpgradeGeneral>();
		for (var up : upgradeReferences) {
			if (up != null)
				res.add(up);
		}
		return res.toArray(new UpgradeGeneral[0]);
	}

	public UpgradeGeneral getUpgradeRef(UpgradeGeneral upgrade) {
		return getUpgrade(upgrade.getNameID());
	}

	public void setLastFocusedUpgrade(byte upgrade) {
//		lastFocusedUpgrade.clear();
		lastFocusedUpgrade.offer(upgrade);
	}

	public int pollLastFocusedUpgrade() {
		return lastFocusedUpgrade.isEmpty() ? -1 : lastFocusedUpgrade.poll();
	}

//	public IUIObject[] getInfoAffectedBy(int upgradeId) {
//		
//		StringBuilder text = new StringBuilder();
//		ArrayList<IUIObject> res = new ArrayList<>();
//		
//		for (var up : upgradeReferences) {
//			if (up instanceof Upgrade) {
//				((Upgrade) up).getInfoAffectedBy(this, text, upgradeId);
//			}
//		}
//		
//		Collections.addAll(res, UILabel.split(text.toString(), "\n"));
//		
//		return res.toArray(new IUIObject[0]);
//	}

	@Override
	public void getCloneString(StringBuilder outString, int lvlDeep, String splitter, boolean test) {
		if (lvlDeep > 0)
			outString.append(splitter);
		lvlDeep++;
		outString.append(indicator).append(splitter).append(upgradeReferences.size());
		for (var up : upgradeReferences) {
			if (up != null) {
				
				var txt = up instanceof Upgrade ? 
						upgradeIndicator : toolIndicator;
				
				outString.append(splitter).append(txt);
				up.getCloneString(outString, lvlDeep, splitter, test);
			} else {
				outString.append(splitter).append("x");
			}
		}
	}

	@Override
	public void setCloneString(String[] cloneString, AtomicInteger fromIndex) {
		fromIndex.getAndIncrement();
		int len = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]);

		clear();
		for (int i = 0; i < len; i++) {
			UpgradeGeneral up;
			switch (cloneString[fromIndex.get()]) {
			default -> {
				fromIndex.getAndIncrement();
				upgradeReferences.add(null);
				continue;
			}
			case upgradeIndicator -> up = new Upgrade();
			case toolIndicator -> up = new Tool();
			}
			fromIndex.getAndIncrement();
			up.setCloneString(cloneString, fromIndex);
			upgradeReferences.add(up);
		}
	}

	public void unlock(Stack<Byte> unlocks) {
		for (var unlockId : unlocks) {
			try {
				if (upgradeReferences.get(unlockId) instanceof Upgrade upgrade)
					upgrade.setVisible(true);
			} catch (IndexOutOfBoundsException ex) {
				System.out.println("Unlock: " + ex.getMessage());
				return;
			}
		}
	}

	public void changeMaxLVL(byte id, float change) {
		try {
			if (upgradeReferences.get(id) instanceof Upgrade upgrade) {
				upgrade.changeMaxLVL(change);
			}
		} catch (IndexOutOfBoundsException ex) {
			System.out.println("changeMaxLVL: " + ex.getMessage());
		}
	}

	public void setLayer(Layer layer) {
		for (var up : upgradeReferences) {
			if (up instanceof Tool tool)
				tool.setLayer(layer);
		}
	}

	public List<UpgradeGeneral> getUpgradesAll() {
		return upgradeReferences;
	}

}

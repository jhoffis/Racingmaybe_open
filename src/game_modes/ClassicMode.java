package game_modes;

import player_local.car.Rep;
import player_local.upgrades.*;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ClassicMode extends TotalMode {

    public ClassicMode(GameModes gamemode) {
        super(gamemode, 18, 3, "Classic");
    }

    public void updateUpgrades() {
        if (rounds >= 1)
            return;

        for (var p : getPlayers().values()) {
        	p.upgrades.clear();
            var ups = p.upgrades.getUpgradesAll();
            var newUps = new CopyOnWriteArrayList<UpgradeGeneral>();
            for (var up : ups)
                if (up instanceof Upgrade)
                    newUps.add(null);
                else
                    newUps.add(up);

            Upgrade upgrade;

            upgrade = Upgrades.createUpgrade(TileNames.Power, newUps);
            upgrade.overrideName = "Upgrade cylinders";
            upgrade.setPremadePrice(50);
            upgrade.setUpgradeType(UpgradeType.POWER);
            upgrade.setVisible(true);
            upgrade.setMaxLVL(-1);
            upgrade.getRegVals().values()[Rep.kW] = new RegVal(75, RegVal.RegValType.Decimal);

            upgrade = Upgrades.createUpgrade(TileNames.WeightReduction, newUps);
            upgrade.overrideName = "Weight reduction bro";
            upgrade.setPremadePrice(80);
            upgrade.setUpgradeType(UpgradeType.POWER);
            upgrade.setVisible(true);
            upgrade.setMaxLVL(-1);
            upgrade.getRegVals().values()[Rep.kg] = new RegVal(0.9, RegVal.RegValType.NormalPercent);

            upgrade = Upgrades.createUpgrade(TileNames.Fuel, newUps);
            upgrade.overrideName = "Better fuel";
            upgrade.setPremadePrice(40);
            upgrade.setUpgradeType(UpgradeType.POWER);
            upgrade.setVisible(true);
            upgrade.setMaxLVL(-1);
            upgrade.getRegVals().values()[Rep.kW] = new RegVal(100, RegVal.RegValType.Decimal);
            upgrade.getRegVals().changeAfterUpgrade[Rep.kW] = new RegVal(.5, RegVal.RegValType.NormalPercent);


            upgrade = Upgrades.createUpgrade(TileNames.Turbo, newUps);
            upgrade.overrideName = "Bigger turbo";
            upgrade.setPremadePrice(160);
            upgrade.setUpgradeType(UpgradeType.POWER);
            upgrade.setVisible(true);
            upgrade.setMaxLVL(-1);
            upgrade.getRegVals().values()[Rep.kW] = new RegVal(400, RegVal.RegValType.Decimal);
            upgrade.getRegVals().values()[Rep.kg] = new RegVal(15, RegVal.RegValType.Decimal);
            upgrade.getRegVals().changeAfterUpgrade[Rep.kW] = new RegVal(.5, RegVal.RegValType.NormalPercent);

            upgrade = Upgrades.createUpgrade(TileNames.RedNOS, newUps);
            upgrade.overrideName = "More NOS";
            upgrade.setPremadePrice(40);
            upgrade.setUpgradeType(UpgradeType.BOOST);
            upgrade.setVisible(true);
            upgrade.setMaxLVL(-1);
            var nos = upgrade.getRegVals().values()[Rep.nosBottles] = new RegVal(1, RegVal.RegValType.Unlock);
            nos.removeAtPlacement = true;
            upgrade.getRegVals().values()[Rep.nos] = new RegVal(0.5, RegVal.RegValType.Decimal);

            upgrade = Upgrades.createUpgrade(TileNames.LighterPistons, newUps);
            upgrade.overrideName = "Lighter pistons";
            upgrade.setPremadePrice(100);
            upgrade.setUpgradeType(UpgradeType.POWER);
            upgrade.setVisible(true);
            upgrade.setMaxLVL(-1);
            upgrade.getRegVals().values()[Rep.kW] = new RegVal(75, RegVal.RegValType.Decimal);
            upgrade.getRegVals().values()[Rep.kg] = new RegVal(-50, RegVal.RegValType.Decimal);

            upgrade = Upgrades.createUpgrade(TileNames.Gears, newUps);
            upgrade.overrideName = "Grippier tyres and gears";
            upgrade.setPremadePrice(100);
            upgrade.setUpgradeType(UpgradeType.ECO);
            upgrade.setVisible(true);
            upgrade.setMaxLVL(-1);
            upgrade.getRegVals().values()[Rep.spdTop] = new RegVal(75, RegVal.RegValType.Decimal);
            upgrade.getRegVals().values()[Rep.kg] = new RegVal(-50, RegVal.RegValType.Decimal);

            upgrade = Upgrades.createUpgrade(TileNames.Block, newUps);
            upgrade.overrideName = "Beefier block";
            upgrade.setPremadePrice(120);
            upgrade.setUpgradeType(UpgradeType.POWER);
            upgrade.setVisible(true);
            upgrade.setMaxLVL(-1);
            upgrade.getRegVals().values()[Rep.kW] = new RegVal(200, RegVal.RegValType.Decimal);
            upgrade.getRegVals().values()[Rep.kg] = new RegVal(15, RegVal.RegValType.Decimal);
            upgrade.getRegVals().changeAfterUpgrade[Rep.kW] = new RegVal(.5, RegVal.RegValType.NormalPercent);

            ups.clear();
            ups.addAll(newUps);

            for (int i = 0; i < p.layer.getWidth(); i++)
                for (int a = 0; a < p.layer.getHeight(); a++)
                    p.layer.setTimesMod(0f, i, a);

            createPricesNoReset(p, Upgrade.priceFactorStd);
        }
    }

    @Override
    protected void setGeneralInfoDown(String[] input, AtomicInteger index) {
        super.setGeneralInfoDown(input, index);
        updateUpgrades();
    }

    @Override
    public void updateInfo() {
        super.updateInfo();
        updateUpgrades();
    }
}

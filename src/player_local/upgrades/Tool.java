package player_local.upgrades;

import communication.GameInfo;
import communication.Translator;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UIRisingTexts;
import engine.graphics.ui.modal.UIBonusModal;
import main.Features;
import player_local.Bank;
import player_local.Layer;
import player_local.Player;
import player_local.car.Rep;
import player_local.upgrades.RegVal.RegValType;
import scenes.Scenes;
import scenes.game.lobby_subscenes.UpgradesSubscene;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tools don't really run anything - they are just checked for as a neighbour
 * nad it then introduces affects.
 */
public class Tool implements UpgradeGeneral {

	private TileNames name;
	private Layer layer;
	private float price;
	public static int improvementPointsNeeded;
	private int placedRound = -1;
	public int sellPrice = 50;
	private TileNames rotator = null;
	public static long nextTimeSwitchRender = 0;
	public static boolean lastRenderedRotator = false;

	public Tool() {
	}

	public Tool(Layer layer) {
		this.layer = layer;
	}

	public Tool(TileNames name, Layer layer) {
		this.name = name;
		this.layer = layer;
	}

	public static boolean checkPlaceRotator(UpgradeGeneral placer, int x, int y, Layer layer) {
		if (placer.getTileName() == TileNames.RightRotator
				|| placer.getTileName() == TileNames.LeftRotator) {
			var placedTile = layer.get(x, y);
            return placedTile != null && placedTile.getUpgradeType() == UpgradeType.TOOL
                    && !(placedTile.getTileName() == TileNames.RightRotator
                    || placedTile.getTileName() == TileNames.LeftRotator)
                    && !((Tool) placedTile).hasRotator();
		}
		return false;
	}

	@Override
	public void getCloneString(StringBuilder outString, int lvlDeep, String splitter, boolean test) {
		if (lvlDeep > 0)
			outString.append(splitter);
//        lvlDeep++;.append(Math.round(stats[i] * 100d) / 100d);
		outString.append(getNameID()).append(splitter)
				.append(Math.round(price * 100d) / 100d).append(splitter)
				.append(placedRound).append(splitter)
				.append(rotator == null
								? 0
								: rotator == TileNames.LeftRotator
									? 1
									: 2).append(splitter)
				.append(sellPrice);
	}

	@Override
	public void setCloneString(String[] cloneString, AtomicInteger fromIndex) {
		name = TileNames.values()[Byte.parseByte(cloneString[fromIndex.getAndIncrement()])];
		price = Float.parseFloat(cloneString[fromIndex.getAndIncrement()]);
		placedRound = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]);
        switch (Integer.parseInt(cloneString[fromIndex.getAndIncrement()])) {
            case 1 -> rotator = TileNames.values()[TileNames.LeftRotator.ordinal()];
            case 2 -> rotator = TileNames.values()[TileNames.RightRotator.ordinal()];
            default -> rotator = null;
        }
		sellPrice = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]);
	}

	@Override
	public Tool clone() {
		Tool tool = new Tool(layer);
		Translator.setCloneString(tool, Translator.getCloneString(this));
		return tool;
	}

	public boolean upgrade(Player player) {
		if (isOpenForUse()) {
			layer.resetImprovementPoints();
			return true;
		}
		return false;
	}

	public static double collectorMultiply(int turn, Tool upgrade) {
		return 1d + (float) (turn - upgrade.getPlacedTurn()) * .5f;
	}

	public static RegVals collectorRegVals(Layer layer, int x, int y, int turn, Tool upgrade) {
		var regVal = new RegVals();
		regVal = Upgrade.modRegValCloned(regVal, layer, x, y, .5);
		regVal.multiplyAllValues(collectorMultiply(turn, upgrade));
		return regVal;
	}
	
	private static void neighborCollectMoney(Player player, int x, int y) {
		var money = player.layer.getMoney(x, y);
		if (money > 0) {
			int rmMoney = Math.round(.25f*money);
			player.layer.addMoney(-rmMoney, x, y);
			player.bank.add(rmMoney, Bank.MONEY);
		}
	}
	
	private static UpgradeGeneral rotateNext(Player player, int x, int y, UpgradeGeneral next) {
		var current = player.layer.remove(x, y);

		if (next != null) {
			player.layer.set(next, x, y);
		}
		return current;
	}
	
	public static void runToolAfterRace(Player player, int turn) {
		var tiles = player.layer.getDobArr();
		var timesModDiff = new float[tiles.length][tiles[0].length];
		for (int x = 0; x < tiles.length; x++) {
			for (int y = 0; y < tiles[x].length; y++) {
				if (tiles[x][y] != null && tiles[x][y] instanceof Tool tool) {
					tool.runTool(player, x, y, turn, timesModDiff);
				}
			}
		}

		for (int x = 0; x < timesModDiff.length; x++)
			for (int y = 0; y < timesModDiff[x].length; y++)
				if (timesModDiff[x][y] > 0)
					UIRisingTexts.pushText(Scenes.LOBBY, UpgradesSubscene.genRealPos(x, y, player.layer), "+ x" + timesModDiff[x][y],
							UIColors.UNBLEACHED_SILK);
	}

	public UpgradeResult buyRunTool(Player player, int turn, int x, int y) {
		if (!player.bank.canAfford(20, Bank.MONEY))
			return UpgradeResult.DidntGoThrough;
		player.bank.buy(20, Bank.MONEY);
		runTool(player, x, y, turn, null);
		return UpgradeResult.Bought;
	}

	public void runTool(Player player, int x, int y, int turn, float[][] timesModDiff) {
		runToolAny(this.name, player, x, y, turn, timesModDiff);
		if (hasRotator()) {
			runToolAny(rotator, player, x, y, turn, timesModDiff);
		}
	}

	private void runToolAny(TileNames name, Player player, int x, int y, int turn, float[][] timesModDiff) {
		var makeOwnTimesModDiff = timesModDiff == null;
		var w = player.layer.getWidth();
		var h = player.layer.getHeight();
		if (makeOwnTimesModDiff) {
			timesModDiff = new float[w][h];
		}
		switch (name) {
		case NeighborCollector -> {
			var regVal = collectorRegVals(player.layer, x, y, turn, this);
			regVal.upgrade(player, getNameID(), true);

			if (w > x + 1)
				neighborCollectMoney(player, x + 1, y);
			if (h > y + 1)
				neighborCollectMoney(player, x, y + 1);
			if (x - 1 >= 0)
				neighborCollectMoney(player, x - 1, y);
			if (y - 1 >= 0)
				neighborCollectMoney(player, x, y - 1);
			for (var neighPos : player.layer.getAllNeighbours(x, y))
				if (Math.abs(x - neighPos.x()) + Math.abs(y - neighPos.y()) > 1)
					neighborCollectMoney(player, neighPos.x(), neighPos.y());

		}
		case TimesModPlanter -> {
			float diff = .2f;
			player.layer.addTimesMod(.1f, x, y);
			timesModDiff[x][y] += .1f;
			if (x > 0) {
				player.layer.addTimesMod(diff, x - 1, y);
				timesModDiff[x - 1][y] += diff;
			}
			if (y > 0) {
				player.layer.addTimesMod(diff, x, y - 1);
				timesModDiff[x][y - 1] += diff;
			}
			if (x < player.layer.getWidth() - 1) {
				player.layer.addTimesMod(diff, x + 1, y);
				timesModDiff[x + 1][y] += diff;
			}
			if (y < player.layer.getHeight() - 1) {
				player.layer.addTimesMod(diff, x, y + 1);
				timesModDiff[x][y + 1] += diff;
			}
		}
		case Seeper -> {
			var neighbors = player.layer.getAllNeighbours(x, y);
			final double initialIncPerc = .1;
			final double existingIncPerc = .05;
			final double initialIncDecimal = .1;
			final double existingIncDecimal = .1;
			for (var ogNeigh : neighbors) {
				if (ogNeigh.upgrade() instanceof Upgrade ogUp) {
					var spreaderRegVals = ogUp.getRegVals();
					var changer = new RegVal[spreaderRegVals.values().length];

					for (var newNeigh : neighbors) {
						if (newNeigh != ogNeigh && newNeigh.upgrade() instanceof Upgrade newUp) {
							RegVals receiverRegVals = newUp.getNeighbourModifier();

							for (int i = 0; i < changer.length; i++) {
								if (spreaderRegVals.values()[i] == null
										|| spreaderRegVals.values()[i].value == 0)
									continue;

								var val = receiverRegVals.values()[i];
								if (val == null) {
									val = spreaderRegVals.values()[i];
									if (val == null || val.value == 0 || changer[i] != null)
										continue;
									if (val.isPercent())
										changer[i] = new RegVal(initialIncPerc * (val.value - 1d) + 1d,
												val.type);
									else
										changer[i] = new RegVal(initialIncDecimal * (val.value), RegValType.Decimal);
									continue;
								}
								if (val.isPercent()) {
									var perc = (val.value - 1d) * existingIncPerc + 1d;
									changer[i] = new RegVal(perc, RegValType.AdditionPercent);
								} else
									changer[i] = new RegVal(val.value * existingIncDecimal, RegValType.Decimal);
							}

							receiverRegVals.combine(changer);
						}
					}
				}
			}

			for (var ogNeigh : neighbors) {
				if (ogNeigh.upgrade() instanceof Upgrade ogUp) {
					ogUp.getNeighbourModifier().multiplyAllValues(1.05);
				}
			}

		}
		case LeftRotator -> {
			UpgradeGeneral next = null;
			boolean rightFound = false, bottomFound = false, leftFound = false, topFound = false;
			boolean topRightFound = false, bottomRightFound = false, bottomLeftFound = false, topLeftFound = false;
			if (w > x + 1) {
				if (h > y + 1) {
					bottomRightFound = true;
					next = player.layer.remove(x + 1, y + 1);
				}
				rightFound = true;
				next = rotateNext(player, x + 1, y, next);
				if (y - 1 >= 0) {
					topRightFound = true;
					next = rotateNext(player, x + 1, y - 1, next);
				}
			}
			if (y - 1 >= 0) {
				topFound = true;
				next = rotateNext(player, x, y - 1, next);
				if (x - 1 >= 0) {
					topLeftFound = true;
					next = rotateNext(player, x - 1, y - 1, next);
				}
			}
			if (x - 1 >= 0) {
				leftFound = true;
				next = rotateNext(player, x - 1, y, next);
				if (h > y + 1) {
					bottomLeftFound = true;
					next = rotateNext(player, x - 1, y + 1, next);
				}
			}

			if (h > y + 1) {
				bottomFound = true;
				next = rotateNext(player, x, y + 1, next);
			}

			if (next != null) {
				if (bottomRightFound) {
					player.layer.set(next, x + 1, y + 1);
				} else if (rightFound) {
					player.layer.set(next, x + 1, y);
				} else if (topRightFound) {
					player.layer.set(next, x + 1, y - 1);
				} else if (topFound) {
					player.layer.set(next, x, y - 1);
				} else if (topLeftFound) {
					player.layer.set(next, x - 1, y - 1);
				} else if (leftFound) {
					player.layer.set(next, x - 1, y);
				} else if (bottomLeftFound) {
					player.layer.set(next, x - 1, y + 1);
				} else if (bottomFound) {
					player.layer.set(next, x, y + 1);
				}
			}
		}
		case RightRotator -> {
			UpgradeGeneral next = null;
			boolean rightFound = false, bottomFound = false, leftFound = false, topFound = false;
			boolean topRightFound = false, bottomRightFound = false, bottomLeftFound = false, topLeftFound = false;
			if (w > x + 1) {
				if (y - 1 >= 0) {
					topRightFound = true;
					next = player.layer.remove(x + 1, y - 1);
				}
				rightFound = true;
				next = rotateNext(player, x + 1, y, next);
				if (h > y + 1) {
					bottomRightFound = true;
					next = rotateNext(player, x + 1, y + 1, next);
				}
			}
			if (h > y + 1) {
				bottomFound = true;
				next = rotateNext(player, x, y + 1, next);
				if (x - 1 >= 0) {
					bottomLeftFound = true;
					next = rotateNext(player, x - 1, y + 1, next);
				}
			}
			if (x - 1 >= 0) {
				leftFound = true;
				next = rotateNext(player, x - 1, y, next);
				if (y - 1 >= 0) {
					topLeftFound = true;
					next = rotateNext(player, x - 1, y - 1, next);
				}
			}
			if (y - 1 >= 0) {
				topFound = true;
				next = rotateNext(player, x, y - 1, next);
			}
			if (next != null) {
				if (topRightFound) {
					player.layer.set(next, x + 1, y - 1);
				} else if (rightFound) {
					player.layer.set(next, x + 1, y);
				} else if (bottomRightFound) {
					player.layer.set(next, x + 1, y + 1);
				} else if (bottomFound) {
					player.layer.set(next, x, y + 1);
				} else if (bottomLeftFound) {
					player.layer.set(next, x - 1, y + 1);
				} else if (leftFound) {
					player.layer.set(next, x - 1, y);
				} else if (topLeftFound) {
					player.layer.set(next, x - 1, y - 1);
				} else if (topFound) {
					player.layer.set(next, x, y - 1);
				}
			}
		}
		case Merchant -> {
			var next = 4 * ((1 + turn) - getPlacedTurn());
			if (w > x + 1) {
				player.layer.addMoney(next, x + 1, y);
				player.layer.addSale(-0.0100001f, x + 1, y);
			}
			if (h > y + 1) {
				player.layer.addMoney(next, x, y + 1);
				player.layer.addSale(-0.0100001f, x, y + 1);
			}
			if (x - 1 >= 0) {
				player.layer.addMoney(next, x - 1, y);
				player.layer.addSale(-0.0100001f, x - 1, y);
			}
			if (y - 1 >= 0) {
				player.layer.addMoney(next, x, y - 1);
				player.layer.addSale(-0.0100001f, x, y - 1);
			}
			for (var neigh : player.layer.getAllNeighbours(x, y)) {
				if (Math.abs(x - neigh.x()) + Math.abs(y - neigh.y()) > 1)
					player.layer.addMoney(next, neigh.x(), neigh.y());
				if (neigh.upgrade() instanceof Upgrade up) {
					var increase = .25f * Math.abs(up.getSellPrice(-1));
					if (increase < 12)
						increase = 12;
					else if (increase > 3000)
						increase = 3000;
					increase /= Upgrade.sellDivision;
					up.addToPriceTotal(increase);
				}
			}
		}
		case Permanentifier -> {
			var neighbors = player.layer.getAllNeighbours(x, y);

			// TODO legg til fjern 1 random lvl av naboer.

			var hasMoreThan1Lvl = new boolean[neighbors.size()];
			boolean anyMoreThan1Lvl = false;

			for (int n = 0; n < neighbors.size(); n++) {
				var neigh = neighbors.get(n);
				if (!(neigh.upgrade() instanceof Upgrade up))
					continue;

				// Remove gained vals
				var gainedVals = up.getGainedValues();
				for (int i = 0; i < Math.min(gainedVals.values.length, Rep.nosBottles); i++) {
					gainedVals.values[i] *= .5;
				}

				// disperse levels
				var lvl = up.getLVL();
				if (lvl > 0) {
					int lowestOther = Integer.MAX_VALUE;
					Upgrade lowestOtherUp = null;
					for (int a = 0; a < neighbors.size(); a++) {
						if (n == a)
							continue;
						var otherNeigh = neighbors.get(a);
						if (!(otherNeigh.upgrade() instanceof Upgrade otherUp))
							continue;
						var otherLVL = otherUp.getLVL();
						if (lvl > otherLVL && otherLVL < lowestOther) {
							lowestOther = otherLVL;
							lowestOtherUp = otherUp;
						}
					}

					if (lowestOtherUp != null) {
						up.setLVL(lvl - 1);
						up.setLvlRealHidden(up.getLvlRealHidden() - 1);
						lowestOtherUp.setLVL(lowestOther + 1);
						lowestOtherUp.setLvlRealHidden(lowestOtherUp.getLvlRealHidden() + 1);
					}
				}
			}
			for (int n = 0; n < neighbors.size(); n++) {
				var neigh = neighbors.get(n);
				if (!(neigh.upgrade() instanceof Upgrade up))
					continue;

				if (up.getLVL() > 0) {
					hasMoreThan1Lvl[n] = true;
					anyMoreThan1Lvl = true;
				}
			}

			if (anyMoreThan1Lvl) {
				while (true) {
					var i = Features.ran.nextInt(hasMoreThan1Lvl.length);
					if (hasMoreThan1Lvl[i]) {
						Upgrade up = ((Upgrade) neighbors.get(i).upgrade());
						up.setLVL(up.getLVL() - 1);
						up.setLvlRealHidden(up.getLVL());
						break;
					}
				}
			}

		}
		case Yunomah -> {
			var neighbors = layer.getAllNeighbours(x, y);
			for (var neighbor : neighbors) {
				if (neighbor.upgrade() instanceof Tool tool) {
					if (tool.name == TileNames.Yunomah
					    || tool.name == TileNames.Dilator
							|| tool.name == TileNames.NeighborTunnel) continue;
					tool.runTool(player, x, y, turn, timesModDiff);
				}
			}

//			RegVals regVal = new RegVals();
//			regVal = Upgrade.modRegValCloned(regVal, layer, x, y);
//
//			var up = new Upgrade(TileNames.Yunomah);
//
//			var bonus = up.pushBonus(1);
//			bonus.setValues(regVal.values());
//
//			GameInfo.bonusModal.pushUpgrade(player, up);
//			GameInfo.bonusModal.setCombination(player);
//			GameInfo.bonusModal.setVisible(true);
////		}
//			// Removes regvals and moves them to their store tile
//			var neighs = player.layer.getNeighbours(x, y);
//			var affectedTiles = new ArrayList<Upgrade>();
//
//			for (var neigh : neighs) {
//				if (neigh.upgrade() instanceof Upgrade up) {
//					var storeUp = (Upgrade) player.upgrades.getUpgrade(up.getTileName()).first();
//					if (storeUp != null)
//						affectedTiles.add(storeUp);
//				}
//			}
//
//			for (var neigh : neighs) {
//				if (neigh.upgrade() instanceof Upgrade up) {
//					var regVals = up.getRegVals().values();
//					for (int i = 0; i < Rep.nosBottles; i++) {
//						if (regVals[i] == null || regVals[i].value == 0)
//							continue;
//						var v = regVals[i].value * (regVals[i].isPercent() ? .01 : .03);
//						if (i == Rep.kg)
//							v = -v;
//						regVals[i].value -= v;
//						for (var storeUp : affectedTiles) {
//							var surv = storeUp.getRegVals().values();
//							if (surv[i] == null) {
//								if (regVals[i].isPercent())
//									surv[i] = new RegVal(1d + v, regVals[i].type);
//								else
//									surv[i] = new RegVal(v, regVals[i].type);
//							} else {
//								surv[i].value += v;
//							}
//						}
//					}
//				}
//			}
		}
//		case Uninsion -> {
//			// Gathers regvals and neighbor bonus into darkness, and darkness makes
//			// the car stronger but redlining is worse.
//			var neighs = player.layer.getNeighbours(x, y);
//
//			for (var neigh : neighs) {
//				if (neigh.upgrade() instanceof Upgrade up) {
//					var regVals = up.getRegVals().values();
//					for (int i = 0; i < Rep.nosBottles; i++) {
//						if (regVals[i] == null || regVals[i].value == 0)
//							continue;
//						if (regVals[i].isPercent()) {
//							if (regVals[i].value < 1d)
//								player.darkness *= 1d -(regVals[i].value - 1d);
//							else
//								player.darkness *= regVals[i].value;
//						} else {
//							if (regVals[i].value < 0d)
//								player.darkness -= regVals[i].value;
//							else
//								player.darkness += regVals[i].value;
//						}
//					}
//				}
//			}
//
////			System.out.println("Darkness; " + player.darkness);
//
//		}
		default -> {
		}
		}

		if (makeOwnTimesModDiff) {
			for (x = 0; x < timesModDiff.length; x++)
				for (y = 0; y < timesModDiff[x].length; y++)
					if (timesModDiff[x][y] > 0)
						UIRisingTexts.pushText(Scenes.LOBBY, UpgradesSubscene.genRealPos(x, y, player.layer), "+ x" + timesModDiff[x][y],
								UIColors.UNBLEACHED_SILK);
		}
	}

	public int getPlacedTurn() {
		return placedRound;
	}

	@Override
	public boolean isOpenForUse() {
		return !isPlaced() && layer != null && layer.getImprovementPoints() >= improvementPointsNeeded(layer);
	}

	public static boolean justUnlocked(Layer layer) {
		return layer.getImprovementPoints() == improvementPointsNeeded(layer);
	}

	public static int improvementPointsNeeded(Layer layer) {
		return improvementPointsNeeded + layer.improvementPointsNeededIncrease * layer.getToolsAmount(null);
	}

	public void place(float paidPrice, int round) {
		placedRound = round;
	}

	@Override
	public boolean isPlaced() {
		return placedRound != -1;
	}

	@Override
	public byte getNameID() {
		return (byte) name.ordinal();
	}

	@Override
	public TileNames getTileName() {
		return name;
	}

	@Override
	public void setPremadePrice(float price) {
		this.price = price;
	}

	@Override
	public float getPremadePrice() {
		return price;
	}

	@Override
	public int getCost(float sale) {
		if (layer == null)
			return 0;
		return (int) Upgrade.getCost(price, layer.getToolsAmount(name), Upgrade.priceFactorStd, sale);
	}

	@Override
	public int getSellPrice(int round) {
		return sellPrice;
	}

	@Override
	public void sell(Bank bank, Rep rep, Upgrades upgrades, int round) {
		bank.add(getSellPrice(round), Bank.MONEY);
	}

	@Override
	public UpgradeType getUpgradeType() {
		return UpgradeType.TOOL;
	}

	@Override
	public void setVisible(boolean b) {

	}

	public void setLayer(Layer layer) {
		this.layer = layer;
	}

	public boolean hasRotator() {
		return rotator != null;
	}

	public void setRotator(TileNames tileName) {
		this.rotator = tileName;
	}

	public int getRotator() {
		return rotator.ordinal();
	}
}

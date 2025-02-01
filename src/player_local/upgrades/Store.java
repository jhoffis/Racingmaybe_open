package player_local.upgrades;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

import audio.SfxTypes;
import communication.Translator;
import engine.graphics.ui.modal.UIBonusModal;
import engine.math.Vec2;
import engine.utils.TwoTypes;
import main.Features;
import player_local.Bank;
import player_local.Layer;
import player_local.Player;
import player_local.TilePiece;
import scenes.game.lobby_subscenes.UpgradesSubscene;

/**
 * @author jhoffis
 *         <p>
 *         Returns plain text and whatever that the actual scene just shows. The
 *         FixCar scene should not deal with figuring out text, just printing.
 */

public class Store {

	public static Consumer<TileVisual> tileInit;
	private TileVisual[] storeTiles;
	private TileVisual[] toolsTiles;
	private TileVisual[] hyperTiles;
	private TileVisual[][] layerTiles;

	private UIBonusModal bonusModal;

	private void deleteList(TileVisual[] list) {
		if (list == null)
			return;
		for (var e : list)
			if (e != null)
				e.delete();
	}

	private void deleteList(TileVisual[][] list) {
		if (list == null)
			return;
		for (var e : list)
			deleteList(e);
	}

	public synchronized void resetTowardsPlayer(Player player) {
		// Må slette alle tiles fordi de refererer til gamle upgrade objekter
		deleteList(storeTiles);
		storeTiles = null;
		deleteList(toolsTiles);
		toolsTiles = null;
		deleteList(layerTiles);
		layerTiles = null;
		deleteList(hyperTiles);
		hyperTiles = null;

		createRemoveTiles(player);
	}

	private TileVisual createTile(Player player, TilePiece<?> upgrade) {
		var tile = new TileVisual(upgrade.upgrade().getNameID());
		tile.reset(player, upgrade);

		if (tileInit != null) {
			tileInit.accept(tile);
		}
		return tile;
	}

	public void createRemoveTiles(Player player) {
		if (UpgradesSubscene.TileSprites == null)
			return;

		var upgrades = player.upgrades.getUpgrades();
		var tools = player.upgrades.getTools();
		var hypers = player.layer.getHypers();

		if (storeTiles == null || storeTiles.length != upgrades.size()) {
			deleteList(storeTiles);
			storeTiles = new TileVisual[upgrades.size()];
		}
		if (toolsTiles == null || toolsTiles.length != tools.size()) {
			deleteList(toolsTiles);
			toolsTiles = new TileVisual[tools.size()];
		}
		if (hyperTiles == null || hyperTiles.length != hypers.length) {
			deleteList(hyperTiles);
			hyperTiles = new TileVisual[hypers.length];
		}
		if (layerTiles == null
				|| layerTiles.length != player.layer.getWidth() && layerTiles[0].length != player.layer.getHeight()) {
			deleteList(layerTiles);
			layerTiles = new TileVisual[player.layer.getWidth()][player.layer.getHeight()];
		}

		float startFactor = 0; // (amountOfNulls + player.upgrades.getAmountStdVisible()) / (float)
								// storeTiles.length;
		float newTop = 0;
		// (UpgradesSubscene.marginY + TileVisual.size() * UpgradesSubscene.spacing) *
		// startFactor +
		// (Window.HEIGHT / 2f - (TileVisual.size() * UpgradesSubscene.spacing *
		// ((float) storeTiles.length - amountOfNulls) / 2f)) * (1f - startFactor);
		int nTilesWidth = 3;

		final Vec2 boardZero = UpgradesSubscene.genRealPos(0, 0, player.layer);
		final float fromX = boardZero.x - (TileVisual.size() *
				(player.layer.getWidth() == 7
				? 1f+(3.2f*(UpgradesSubscene.spacing - 1f))
				: UpgradesSubscene.spacingBig)
		);
		final float fromY = boardZero.y + (player.layer.getWidth() == 7
				? .5f*(TileVisual.size() * UpgradesSubscene.spacing)
				: 0);
		final float sizeSpace = TileVisual.size() * UpgradesSubscene.spacing;

		int nPowerCol = 0;
		int nBoostCol = 0;
		int nEcoCol = 0;
		int nSpeedCol = 0;

		for (int i = 0; i < storeTiles.length; i++) {
			var up = upgrades.get(i);
			if (up == null)
				continue;
			if (storeTiles[i] == null) {
				storeTiles[i] = createTile(player, new TilePiece<>(up, -1, -1));
			} else {
				storeTiles[i].resetExistingPiece(player);
			}

			int x = 0, y = 0;

			if (up.getTileName() == TileNames.Clutch) {
				x = 3;
				y = nSpeedCol;
				nSpeedCol++;
			} else if (up.getTileName() == TileNames.Gears) {
				x = 3;
				y = nSpeedCol;
				nSpeedCol++;
			} else if (up.getTileName() == TileNames.Aero) {
				x = 3;
				y = nSpeedCol;
				nSpeedCol++;
			} else if (up.getUpgradeType() == UpgradeType.POWER) {
				x = 0;
				y = nPowerCol;
				nPowerCol++;
			} else if (up.getUpgradeType() == UpgradeType.BOOST) {
				x = 1;
				y = nBoostCol;
				nBoostCol++;
			} else if (up.getUpgradeType() == UpgradeType.ECO) {
				x = 2;
				y = nEcoCol;
				nEcoCol++;
			}

			storeTiles[i].setPos(-x - 1, y, fromX - (x * sizeSpace), fromY + (y * sizeSpace));
		}
		for (int i = 0; i < storeTiles.length; i++) {
			var up = upgrades.get(i);
			if (up == null)
				continue;
			switch (storeTiles[i].logicalX) {
			case -2:
				storeTiles[i].logicalX += (nPowerCol == 0 ? 1 : 0);
				break;
			case -3:
				storeTiles[i].logicalX += (nPowerCol == 0 ? 1 : 0) + (nBoostCol == 0 ? 1 : 0);
				break;
			case -4:
				storeTiles[i].logicalX += (nPowerCol == 0 ? 1 : 0) + (nBoostCol == 0 ? 1 : 0) + (nEcoCol == 0 ? 1 : 0);
				break;
			}
		}

		for (int i = 0; i < hypers.length; i++) {
			var up = hypers[i];
			if (hyperTiles[i] == null) {
				hyperTiles[i] = createTile(player, new TilePiece<>(up, -1, -1));
			} else {
				hyperTiles[i].resetExistingPiece(player);
			}

			int x = 1 + i, y = 7;
			hyperTiles[i].setPos(-x - 1, y, fromX - (x * sizeSpace), fromY + (y * sizeSpace));
		}

		int x = 4, y = 0;
		int logX = x - ((nPowerCol == 0 ? 1 : 0) + (nBoostCol == 0 ? 1 : 0) + (nEcoCol == 0 ? 1 : 0)
				+ (nSpeedCol == 0 ? 1 : 0));
		nEcoCol = 0;
		for (int i = 0; i < toolsTiles.length; i++) {
			if (tools.get(i) == null)
				continue;
			
			y = nEcoCol;
			nEcoCol++;
			
			if (nEcoCol == 6) {
				x++;
				logX++;
				y = 0;
				nEcoCol = y + 1;
			}
			
			if (toolsTiles[i] == null) {
				toolsTiles[i] = createTile(player, new TilePiece<>(tools.get(i), -1, -1));
			} else {
				toolsTiles[i].resetExistingPiece(player);
			}

			toolsTiles[i].setPos(
					-logX - 1,
					y,
					fromX - (x * sizeSpace),
					fromY + (y * sizeSpace)
			);
		}

		for (x = 0; x < layerTiles.length; x++) {
			for (y = 0; y < layerTiles[x].length; y++) {
				var piece = player.layer.getPiece(x, y);
				var up = piece.upgrade();
				if (up == null) {
					layerTiles[x][y] = null;
					continue;
				} else if (layerTiles[x][y] != null) {
					piece = layerTiles[x][y].piece();
					if (piece.upgrade().getNameID() == up.getNameID()) {
						if (layerTiles[x][y].placed != up.isPlaced())
							layerTiles[x][y].resetExistingPiece(player);
						continue;
					}
				}

				var tile = createTile(player, piece); // ikke lag ny tile om den allerede er der

				var pos = UpgradesSubscene.genRealPos(x, y, player.layer);
				tile.setPos(pos.x, pos.y);
				layerTiles[x][y] = tile;
			}
		}

	}

	public boolean isBonusToChooseFirst(Player player, Upgrade checkUpgrade, boolean checkForMoreBonusesAfterABonus) {
		if (bonusModal == null) {
			System.out.println("There is no bonus modal to push upgrades");
			return false;
		}

		boolean res = false;
		int upgradeID = 0;

		// -1 betyr at det er ingenting mer � sjekke,
		// ellers oppgraderingsID-en.
		do {
			Upgrade u = null;
			upgradeID = player.upgrades.pollLastFocusedUpgrade();

			if (upgradeID == checkUpgrade.getNameID()) {
				u = checkUpgrade;
				var uRep = (Upgrade) player.upgrades.getUpgrade(upgradeID);
//				FIXME �pne opp for � kunne ha forskjellige bonuser for improvements og plasseringer 
				if (u.equals(uRep))
					u = uRep;
				else
					continue;
			} else if (upgradeID != -1) {
				if (player.upgrades.getUpgrade(upgradeID) instanceof Upgrade up) {
					u = up;
				}
			}

			if (u != null && u.hasBonusReady(checkForMoreBonusesAfterABonus)) {
				bonusModal.pushUpgrade(player, u);
				res = true;
			}

		} while (upgradeID >= 0);

		return res;
	}

	private void updateUpgradedTile(Player player, String oldPlayerStr, Upgrade upgrade) {
		var oldPlayer = new Player();
		// Or maybe just clone the player before actually upgrading - instead of
		// afterwards.
		Translator.setCloneString(oldPlayer, oldPlayerStr);
		upgrade.addGainedValuesDifference(oldPlayer, player);
	}

	/**
	 * Gj�r faktisk betaling og oppgradering, men sjekker om det er en bonus f�rst
	 */
	public UpgradeResult upgrade(Player player, Upgrade checkUpgrade, int x, int y,
			boolean checkForMoreBonusesAfterABonus) {
		// check if there is a bonus to adhere first.
		boolean foundBonus = isBonusToChooseFirst(player, checkUpgrade, checkForMoreBonusesAfterABonus);
		if (foundBonus)
			return UpgradeResult.FoundBonus;
		var cost = checkUpgrade.getCost(player.layer.getSale(x, y));
		if (player.bank.canAfford(cost, Bank.MONEY)) {
			player.bank.buyForced(cost, Bank.MONEY);
			if (checkUpgrade.upgrade(player, x, y, false)) {
				checkUpgrade.addToPriceTotal(cost);
				return UpgradeResult.Bought;
			}
		}
		return UpgradeResult.DidntGoThrough;
	}

	private UpgradeResult attemptBuyUpgrade(Player player, Upgrade upgrade, int x, int y) {
		if (!upgrade.isOpenForUse())
			return UpgradeResult.DidntGoThrough;

		// to check for bonuses, cache this upgrade
		setLastFocusedUpgrade(player, upgrade.getNameID());

		return upgrade(player, upgrade, x, y, false);
	}

	public UpgradeResult attemptImproveUpgrade(Player player, Upgrade upgrade, int x, int y) {
		var oldPlayer = Translator.getCloneString(player);
		var result = attemptBuyUpgrade(player, upgrade, x, y);
		if (result != UpgradeResult.DidntGoThrough) {
			player.layer.addImprovementPoints(1);

			if (upgrade.getLVL() == Upgrade.placedNeighbourChangeLVL)
				upgrade.getNeighbourModifier().multiplyAllValues(2);
			else if (upgrade.getLVL() > Upgrade.placedNeighbourChangeLVL)
				upgrade.getNeighbourModifier().multiplyAllValues(1.1);

			if (upgrade.getLVL() >= player.layer.placedUnlockEmptyLVL) {
				var unlockedAny = false;
				var upgs = player.layer.getDobArr();
				if (x - 1 >= 0 && upgs[x - 1][y] instanceof EmptyTile) {
					upgs[x - 1][y] = null;
					unlockedAny = true;
				}
				if (x + 1 < player.layer.getWidth() && upgs[x + 1][y] instanceof EmptyTile) {
					upgs[x + 1][y] = null;
					unlockedAny = true;
				}
				if (y - 1 >= 0 && upgs[x][y - 1] instanceof EmptyTile) {
					upgs[x][y - 1] = null;
					unlockedAny = true;
				}
				if (y + 1 < player.layer.getHeight() && upgs[x][y + 1] instanceof EmptyTile) {
					upgs[x][y + 1] = null;
					unlockedAny = true;
				}

				if (unlockedAny) {
					player.layer.placedUnlockEmptyLVL += player.layer.placedUnlockEmptyLVLIncrease;
					if (Features.inst != null) {
						if (player.layer.attemptGoHyperBoard())
							Features.inst.getAudio().play(SfxTypes.HYPER);
						else
							Features.inst.getAudio().play(SfxTypes.UNLOCKED);
					}
				}
			}
			player.car.reset(player.layer);
			updateUpgradedTile(player, oldPlayer, upgrade);
		}
		return result;
	}

	private TwoTypes<UpgradeResult, Integer> buyTool(Player player,
								  int x, int y,
								  Tool tool) {
		var paidCost = tool.getCost(player.layer.getSale(x, y));
		if (player.bank.canAfford(paidCost - player.layer.getMoney(x, y), Bank.MONEY)
				&& tool.upgrade(player)) {
			player.bank.buyForced(paidCost, Bank.MONEY);
			return new TwoTypes<>(UpgradeResult.Bought, paidCost);
		}
		return new TwoTypes<>(UpgradeResult.DidntGoThrough, 0);
	}

	// FIXME burde egt ikke v�re TileUpgrade slik sett fordi den er jo grafisk. Hva
	// med testesystemene og ai?
	public UpgradeResult attemptBuyTile(Player player, TilePiece<?> tile, int round) {
		var boughtOrBonus = UpgradeResult.DidntGoThrough;
		var money = player.layer.getMoney(tile.x(), tile.y());
		player.bank.add(money, Bank.MONEY);

		if (player.layer.isOpen(tile.x(), tile.y(), tile.upgrade().getTileName())) {
			if (tile.upgrade() instanceof HyperUpgrade upgrade) {
				var placingUp = upgrade.clone();

				boughtOrBonus = attemptBuyUpgrade(player, placingUp, tile.x(), tile.y());
//				System.out.println("Bought tile or bonus value: " + boughtOrBonus);
				if (boughtOrBonus != UpgradeResult.DidntGoThrough) {
					// update store tile
					int paidCost = upgrade.getCost(player.layer.getSale(tile.x(), tile.y()));

					// setup new tile
					player.layer.set(placingUp, tile.x(), tile.y());
					placingUp.place(paidCost, round);
					player.layer.removeHypers();

					createRemoveTiles(player);

					player.car.reset(player.layer);
				}
			} else if (tile.upgrade() instanceof Upgrade upgrade) {
				var storeUp = (Upgrade) player.upgrades.getUpgradeRef(upgrade);
				var placingUp = storeUp.clone();

				var oldPlayer = Translator.getCloneString(player);
				boughtOrBonus = attemptBuyUpgrade(player, placingUp, tile.x(), tile.y());
//				System.out.println("Bought tile or bonus value: " + boughtOrBonus);
				if (boughtOrBonus != UpgradeResult.DidntGoThrough) {
					// update store tile
					int paidCost = storeUp.getCost(player.layer.getSale(tile.x(), tile.y()));
					storeUp.setLVL(placingUp.getLVL());
					storeUp.setLvlRealHidden(placingUp.getLvlRealHidden());
					storeUp.getRegVals().combineChange();

					// setup new tile
					player.layer.set(placingUp, tile.x(), tile.y());
					placingUp.place(paidCost, round);

					createRemoveTiles(player);

					player.car.reset(player.layer);

					if (boughtOrBonus == UpgradeResult.FoundBonus) {
						int maxLVL = (int) Math.ceil(((float) storeUp.getLVL() + 1f) / 2f);
						placingUp.setMinLVL(maxLVL);
						placingUp.setMaxLVL(maxLVL);
					} else {
						updateUpgradedTile(player, oldPlayer, placingUp);
					}
				}
			} else if (tile.upgrade() instanceof Tool tool) {
				var boughtRes = buyTool(player, tile.x(), tile.y(), tool);
				boughtOrBonus = boughtRes.first();
				if (boughtOrBonus != UpgradeResult.DidntGoThrough) {
					var storeUp = (Tool) player.upgrades.getUpgradeRef(tool);
					Tool placingUp = storeUp.clone();
					player.layer.set(placingUp,  tile.x(), tile.y());
					placingUp.place(boughtRes.second(), round);
					createRemoveTiles(player);
					player.car.reset(player.layer);
				}
			}
		}

		if (Tool.checkPlaceRotator(tile.upgrade(), tile.x(), tile.y(), player.layer)) {
            assert tile.upgrade() instanceof Tool;
            var boughtRes = buyTool(player, tile.x(), tile.y(), (Tool) tile.upgrade());
			boughtOrBonus = boughtRes.first();
			if (boughtOrBonus != UpgradeResult.DidntGoThrough) {
				var placedTile = (Tool) player.layer.get(tile.x(), tile.y());
				placedTile.setRotator(tile.upgrade().getTileName());
				createRemoveTiles(player);
				Tool.nextTimeSwitchRender = System.currentTimeMillis() + 2000;
				Tool.lastRenderedRotator = false;
			}
		}

		if (boughtOrBonus != UpgradeResult.DidntGoThrough) {
			player.layer.setMoney(0, tile.x(), tile.y());
			player.setPlaced(tile.upgrade().getTileName());

			if (boughtOrBonus == UpgradeResult.FoundBonus) {
				bonusModal.setCombination(player);
			}
		} else {
			player.bank.add(-money, Bank.MONEY);
		}

		return boughtOrBonus;

	}

	public void setLastFocusedUpgrade(Player player, byte upgradeId) {
		player.upgrades.setLastFocusedUpgrade(upgradeId);
	}

	public TileVisual[] getStoreTiles() {
		return storeTiles;
	}

	public TileVisual[] getToolsTiles() {
		return toolsTiles;
	}

	public TileVisual[] getHyperTiles() {
		return hyperTiles;
	}

	public ArrayList<TileVisual> getAllTilesNonNull(int state) {
		var all = new ArrayList<>(Arrays.asList(switch (state) {
		default -> getStoreTiles();
		case 1 -> getToolsTiles();
		}));
		for (var line : layerTiles) {
			for (var tile : line) {
				if (tile != null)
					all.add(tile);
			}
		}
		return all;
	}

	public ArrayList<TileVisual> getAllStoreTilesNonNull() {
		var all = new ArrayList<TileVisual>();
		if (storeTiles == null || toolsTiles == null || layerTiles == null || hyperTiles == null)
			return all;
		all.addAll(Arrays.asList(storeTiles));
		all.addAll(Arrays.asList(toolsTiles));
		all.addAll(Arrays.asList(hyperTiles));
		return all;
	}

	public ArrayList<TileVisual> getAllTilesNonNull() {
		var all = new ArrayList<TileVisual>();
		if (storeTiles == null || toolsTiles == null || layerTiles == null || hyperTiles == null)
			return all;
		all.addAll(Arrays.asList(storeTiles));
		all.addAll(Arrays.asList(toolsTiles));
		all.addAll(Arrays.asList(hyperTiles));

		for (var line : layerTiles) {
			for (var tile : line) {
				if (tile != null)
					all.add(tile);
			}
		}
		return all;
	}

	public static int FromX() {
		return (int) (UpgradesSubscene.marginX - TileVisual.size() * UpgradesSubscene.spacingBig);
	}

	public static int FromY() {
		return (int) (TileVisual.size() * 1.5f);
	}

	public void setBonusModal(UIBonusModal bonusModal) {
		this.bonusModal = bonusModal;
	}

	public TileVisual[][] getLayerTiles() {
		return layerTiles;
	}

	public TileVisual getStoreTileAt(int x, int y, float speed, int changeX, int changeY) {
//		System.out.println("getting storetile at " + x + ", " + y + ", " + speed);
		if (x < 0) {
			TileVisual potentialTile = null;
			for (var tile : getAllStoreTilesNonNull()) {
				if (tile.logicalX == x && tile.logicalY == y) {
					return tile;
				} else if (changeX < 0 && tile.logicalY == y && tile.logicalX < x) {
					if (potentialTile != null && (Math.abs(x - tile.logicalX) >= Math.abs(x - potentialTile.logicalX)))
						continue;
					potentialTile = tile;
				} else if (changeX > 0 && tile.logicalY == y && tile.logicalX > x) {
					if (potentialTile != null && (Math.abs(x - tile.logicalX) >= Math.abs(x - potentialTile.logicalX)))
						continue;
					potentialTile = tile;
				}
			}
			return potentialTile;
		}
		
		int maxX = layerTiles.length;
		int maxY = layerTiles[0].length;
		if (x >= maxX)
			x = maxX - 1;
		if (y >= maxY)
			y = maxY - 1;

		if (isManiTile(x, y)) {
			return layerTiles[x][y];
		}
		var finds = new ArrayList<TwoTypes<TileVisual, TwoTypes<Integer, Integer>>>();

		for (int lY = 0; lY < maxY; lY++) {
			for (int lX = 0; lX < maxX; lX++) {
				if (lX == (x - changeX) && lY == (y - changeY))
					continue;

				if (isManiTile(lX, lY)) {
					finds.add(new TwoTypes<TileVisual, TwoTypes<Integer, Integer>>(layerTiles[lX][lY],
							new TwoTypes<>(lX - x, lY - y)));
				}
			}
		}

		int lowestDistance = Integer.MAX_VALUE;
		lowestDistance = Integer.MAX_VALUE;
		TileVisual nearest = null;
		for (var found : finds) {
			if (changeX != 0 && ((found.second().first() >= 0) != (changeX >= 0)))
				continue;
			if (changeY != 0 && ((found.second().second() >= 0) != (changeY >= 0)))
				continue;
			var distance = Math.abs(found.second().first()) + Math.abs(found.second().second());
			if (distance < lowestDistance) {
				lowestDistance = distance;
				nearest = found.first();
			}
		}
		if (nearest == null && changeX < 0 && changeY == 0)
			return getStoreTileAt(-1, y, speed, changeX, changeY);
		return nearest;
	}

	private boolean isManiTile(int x, int y) {
		if (layerTiles[x][y] != null) {
			var piece = layerTiles[x][y].piece();
			if (piece != null && (piece.upgrade() instanceof Tool || piece.upgrade() instanceof Upgrade))
				return true;
		}
		return false;
	}

	public TileVisual getTileAt(int x, int y) {
		return layerTiles[x][y];
	}

	public UIBonusModal getBonusModal() {
		return bonusModal;
	}

}

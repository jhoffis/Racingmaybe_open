package player_local;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import adt.ICloneStringable;
import communication.Translator;
import player_local.car.Rep;
import player_local.upgrades.*;
import engine.math.Vec2;
import main.Features;
import main.Main;
import player_local.upgrades.RegVal.RegValType;

/**
 * Basically the mapboard. Holds a list of general upgrades that only care about
 * the stats - aka no visuals. Visuals are handled in the scenes and store
 * instead
 * 
 * @author jh
 *
 */
public class Layer implements ICloneStringable {
	public static final int STD_W = 5, STD_H = 5;
	private int w = STD_W, h =  STD_H;
	public static long FINALIZED_SEED;

	public static float minTimesMod;
	private UpgradeGeneral[][] tiles;
	private HyperUpgrade[] hyperUpgrades;
	private float[][] timesMod, sale;
	private int[][] money;
	private int improvementPoints;
	public byte improvementPointsNeededIncrease = 2, placedUnlockEmptyLVL = 5, placedUnlockEmptyLVLIncrease = 1;

	public void trueReset() {
		Translator.setCloneString(this, new Layer());
	}

	public Layer() {
		this(Features.ran, 70, 5, 2, 6, 10, 3);
	}

	public Layer(Random ran, int maxMoney, int size, int min, int max, int modifierTilePool, int negativeSize) {
		tiles = new UpgradeGeneral[w][h];
		timesMod = new float[w][h];
		sale = new float[w][h];
		money = new int[w][h];

		createModifierTiles(ran, min, max, modifierTilePool);
		do {
			int x = ran.nextInt(w);
			int y = ran.nextInt(h);
			money[x][y] += 5;
			maxMoney -= 5;
		} while (maxMoney > 0);

		sale[ran.nextInt(w)][ran.nextInt(h)] = .75f;

		int n = 0;
		do {
			int x = ran.nextInt(w);
			int y = ran.nextInt(h);
			if (tiles[x][y] == null && timesMod[x][y] <= 1) {
				tiles[x][y] = new EmptyTile();
				n++;
			}
		} while (n < size);

		size = negativeSize;
		n = 0;
		while (n < size) {
			int x = ran.nextInt(w);
			int y = ran.nextInt(h);
			if (tiles[x][y] == null && timesMod[x][y] <= 1) {
				var negativeChance = ran.nextFloat();
				TileNames name;
				if (negativeChance < 1f/6f) {
					name = TileNames.BirdsNest;
				} else if (negativeChance < 2f/6f) {
					name = TileNames.BrokenWindow;
				} else if (negativeChance < 3f/6f) {
					name = TileNames.GrindedGears;
				} else if (negativeChance < 4f/6f) {
					name = TileNames.PuncturedTire;
				} else if (negativeChance < 5f/6f) {
					name = TileNames.CrackedManifold;
				} else {
					name = TileNames.Mattress;
				}
				setNegTile(name, x, y);
				n++;
			}
		} 
	}

	public Upgrade setNegTile(TileNames name, int x, int y) {
		var neighbors = new RegVals();
		switch (name) {
			case BirdsNest -> neighbors.values()[Rep.kW] = new RegVal(0.9, RegValType.NormalPercent);
			case Mattress -> neighbors.values()[Rep.kg] = new RegVal(50, RegValType.Decimal);
			case PuncturedTire -> {
				neighbors.values()[Rep.aero] = new RegVal(1.03, RegValType.NormalPercent);
				neighbors.values()[Rep.nosMs] = new RegVal(.95, RegValType.NormalPercent);
				neighbors.values()[Rep.tbMs] = new RegVal(.95, RegValType.NormalPercent);
			}
			case CrackedManifold -> {
				neighbors.values()[Rep.bar] = new RegVal(0.95, RegValType.NormalPercent);
				neighbors.values()[Rep.nos] = new RegVal(.90, RegValType.NormalPercent);
				neighbors.values()[Rep.tb] = new RegVal(.90, RegValType.NormalPercent);
			}
			case BrokenWindow -> neighbors.values()[Rep.aero] = new RegVal(1.1, RegValType.NormalPercent);
			case GrindedGears -> neighbors.values()[Rep.spdTop] = new RegVal(.95, RegValType.NormalPercent);
		}

		var upgrade = new Upgrade(name);
		upgrade.setForceMaxLVL(0);
		upgrade.setVisible(true);
		upgrade.setUpgradeType(UpgradeType.NEG);
		for (int i = 0; i < neighbors.values().length; i++) {
			upgrade.getNeighbourModifier().values()[i] = neighbors.values()[i];
		}
		upgrade.place(0, 0);
		upgrade.addToPriceTotal(-200);
		upgrade.getGainedValues().values = new double[Rep.size()];
		switch (name) {
            case BirdsNest -> {
				upgrade.getGainedValues().values[Rep.kW] = -25;
			}
            case BrokenWindow -> {
				upgrade.getGainedValues().values[Rep.aero] = 0.01;
            }
            case GrindedGears -> {
				upgrade.getGainedValues().values[Rep.spdTop] = -50;
            }
            case PuncturedTire -> {
				upgrade.getGainedValues().values[Rep.nosMs] = -25;
				upgrade.getGainedValues().values[Rep.tbMs] = -25;
			}
            case Mattress -> {
				upgrade.getGainedValues().values[Rep.kg] = 50;
            }
            case CrackedManifold -> {
				upgrade.getGainedValues().values[Rep.nos] = -1;
				upgrade.getGainedValues().values[Rep.tb] = -1;
            }
        }

		tiles[x][y] = upgrade;
		return upgrade;
	}

	public void reset() {
		for (int x = 0; x < tiles.length; x++) {
			for (int y = 0; y < tiles[x].length; y++) {
				tiles[x][y] = null;
				timesMod[x][y] = 0;
				money[x][y] = 0;
				sale[x][y] = 0;
			}
		}
	}

	@Override
	public void getCloneString(StringBuilder outString, int lvlDeep, String split, boolean test) {
		if (lvlDeep > 0)
			outString.append(split);
		lvlDeep++;
		outString
		.append(improvementPoints)
		.append(split)
		.append(improvementPointsNeededIncrease)
		.append(split)
		.append(placedUnlockEmptyLVL)
		.append(split)
		.append(placedUnlockEmptyLVLIncrease)
		.append(split)
		.append(w)
		.append(split)
		.append(h);

		if (hyperUpgrades == null) {
			outString.append(split).append("n");
		} else {
			for (var hu : hyperUpgrades) {
				hu.getCloneString(outString, lvlDeep, split, test);
			}
		}

		int skip = 0;
		UpgradeType type = null;
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				if (tiles[x][y] != null) {
					var newType = tiles[x][y].getUpgradeType();

					if (skip > 0 && (type != UpgradeType.EMPTY || newType != UpgradeType.EMPTY)) {
						outString.append(split).append("s").append(skip).append(type);
						skip = 0;
					}
					type = newType;
					if (newType == UpgradeType.EMPTY) {
						skip++;
						continue;
					}

					// x?
					outString.append(split).append(newType);
					tiles[x][y].getCloneString(outString, lvlDeep, split, test);

				} else {
					if (skip > 0 && type != null) {
						outString.append(split).append("s").append(skip).append(type);
						type = null;
						skip = 0;
					}
					skip++;
				}
			}
		}

		if (skip > 0) {
			outString.append(split).append("s").append(skip).append(type);
		}

		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {
				String foundText = "";
				if (hasTimesMod(x, y)) {
					foundText = "x" + getTimesMod(x, y);
				}
				if (hasMoney(x, y)) {
					foundText += "€" + getMoney(x, y);
				}
				if (hasSale(x, y)) {
					foundText += "µ" + sale[x][y];
				}

				if (!foundText.isEmpty()) {
					outString.append(split).append(x).append(',').append(y).append(':').append(foundText);
				}
			}
		}

		outString.append(split).append("end");
	}

	@Override
	public void setCloneString(String[] cloneString, AtomicInteger fromIndex) {
		improvementPoints = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]);
		improvementPointsNeededIncrease = Byte.parseByte(cloneString[fromIndex.getAndIncrement()]);
		placedUnlockEmptyLVL = Byte.parseByte(cloneString[fromIndex.getAndIncrement()]);
		placedUnlockEmptyLVLIncrease = Byte.parseByte(cloneString[fromIndex.getAndIncrement()]);
		w = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]);
		h = Integer.parseInt(cloneString[fromIndex.getAndIncrement()]);
		tiles = new UpgradeGeneral[w][h];
		timesMod = new float[w][h];
		sale = new float[w][h];
		money = new int[w][h];

		if (cloneString[fromIndex.get()].equals("n")) {
			fromIndex.getAndIncrement();
			hyperUpgrades = null;
		} else {
			hyperUpgrades = new HyperUpgrade[3];
			for (int i = 0; i < hyperUpgrades.length; i++) {
				hyperUpgrades[i] = new HyperUpgrade();
				hyperUpgrades[i].setCloneString(cloneString, fromIndex);
			}
		}

		reset();
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h; y++) {

				if (cloneString[fromIndex.get()].startsWith("s")) { // skip
					var skipStr = cloneString[fromIndex.getAndIncrement()];
					int i;
					for (i = 1; i < skipStr.length(); i++) {
						if (!Character.isDigit(skipStr.charAt(i))) {
							break;
						}
					}

					var restStr = skipStr.substring(i);
					skipStr = skipStr.substring(1, i);

					int skip = Integer.parseInt(skipStr); // For loopen
															// plusser ogs�

					if (restStr.equals(UpgradeType.EMPTY.toString())) {
						int skipCounter = skip;

						boolean init = true;
						for (int xEmpty = 0; xEmpty < w && skipCounter > 0; xEmpty++) {
							for (int yEmpty = 0; yEmpty < h && skipCounter > 0; yEmpty++) {
								if (init) {
									xEmpty = x;
									yEmpty = y;
									init = false;
								}
								tiles[xEmpty][yEmpty] = new EmptyTile(); // We update the tiles visually later
								skipCounter--;
							}
						}
					}

					y += skip - 1;
					int rest = h - y;
					if (rest <= 0) {
						y = -rest % h;
						x += -rest / h + 1;
					}

					continue;
				}

				switch (UpgradeType.valueOf(cloneString[fromIndex.getAndIncrement()])) {
				case POWER:
				case ECO:
				case BOOST:
				case NEG:
					tiles[x][y] = new Upgrade(); // We update the tiles visually later
					break;
				case HYPER:
					tiles[x][y] = new HyperUpgrade(); // We update the tiles visually later
					break;
				case TOOL:
					tiles[x][y] = new Tool(this); // We update the tiles visually later
					break;
				default:
                    try {
                        throw new Exception("How");
                    } catch (Exception e) {
						e.printStackTrace();
                    }
                    break;
				}
				tiles[x][y].setCloneString(cloneString, fromIndex);
			}
		}

		while (!cloneString[fromIndex.get()].equals("end")) {
			var splitPosAndVal = cloneString[fromIndex.getAndIncrement()].split(":");
			var pos = splitPosAndVal[0].split(",");
			int x = Integer.parseInt(pos[0]);
			int y = Integer.parseInt(pos[1]);

			splitPosAndVal[1] += 'e';
			char type = '0';
			int startI = 0;
			for (int i = 0; i < splitPosAndVal[1].length(); i++) {
				var c = splitPosAndVal[1].charAt(i);
				if (!Character.isDigit(c) && c != '.') {
					switch (type) {
					case 'x' -> timesMod[x][y] = Float.parseFloat(splitPosAndVal[1].substring(startI, i));
					case '€' -> money[x][y] = Integer.parseInt(splitPosAndVal[1].substring(startI, i));
					case 'µ' -> sale[x][y] = Float.parseFloat(splitPosAndVal[1].substring(startI, i));
					}

					type = c;
					startI = i + 1;
				}
			}
		}

		fromIndex.getAndIncrement();
	}

	public boolean isOpen(int x, int y) {
		return get(x, y) == null;
	}

	public boolean isOpen(int x, int y, TileNames name) {
		if (name == TileNames.Block) {
			if (getAmountType(TileNames.Block) != 0) {
				boolean found = false;
				for (var neighbor : getNeighboursAndCorners(x, y)) {
					if (neighbor.upgrade().getTileName() == TileNames.Block) {
						found = true;
						break;
					}
				}
				if (!found)
					return false;
			}
		} else if (name == TileNames.BlueNOS) {
			for (var neighbor : getNeighbours(x, y)) {
				if (neighbor.upgrade().getTileName() == TileNames.BlueNOS) {
					return false;
				}
			}
		}

		return isOpen(x, y);
	}

	public void set(UpgradeGeneral tile, int x, int y) {
		tiles[x][y] = tile;
		if (tile instanceof Upgrade up)
			up.updateMaxLVLPosFast(this, x, y);
	}

	public UpgradeGeneral remove(int x, int y) {
		var removed = tiles[x][y];
		tiles[x][y] = null;
		return removed;
	}

	public UpgradeGeneral get(int x, int y) {
		return tiles[x][y];
	}

	public TilePiece<?> getPiece(int x, int y) {
		return new TilePiece<>(tiles[x][y], x, y);
	}

	private void addTileIfValid(ArrayList<TilePiece<?>> res, int x, int y) {
		UpgradeGeneral tile = tiles[x][y];
		if (tile != null && !(tile instanceof EmptyTile)) {
			res.add(new TilePiece<>(tile, x, y));
		}
	}

	/**
	 * Gets all neighboring tiles and corners that are not empty.
	 */
	public ArrayList<TilePiece<?>> getNeighboursAndCorners(int x, int y) {
		var res = new ArrayList<TilePiece<?>>();

		// Precompute boundary conditions to avoid redundant checks
		boolean hasLeft = x > 0;
		boolean hasRight = x < w - 1;
		boolean hasTop = y > 0;
		boolean hasBottom = y < h - 1;

		// Check each direction based on boundary conditions
		if (hasLeft) {
			addTileIfValid(res, x - 1, y);           // Left
			if (hasTop) addTileIfValid(res, x - 1, y - 1);  // Top-left
			if (hasBottom) addTileIfValid(res, x - 1, y + 1); // Bottom-left
		}

		if (hasRight) {
			addTileIfValid(res, x + 1, y);           // Right
			if (hasTop) addTileIfValid(res, x + 1, y - 1);   // Top-right
			if (hasBottom) addTileIfValid(res, x + 1, y + 1); // Bottom-right
		}

		if (hasTop) addTileIfValid(res, x, y - 1);   // Top
		if (hasBottom) addTileIfValid(res, x, y + 1); // Bottom

		return res;
	}

	/**
	 * Gets all neighboring tiles (up, down, left, right) that are not empty.
	 */
	public ArrayList<TilePiece<?>> getNeighbours(int x, int y) {
		var res = new ArrayList<TilePiece<?>>();

		// Precompute boundary conditions
		boolean hasLeft = x > 0;
		boolean hasRight = x < w - 1;
		boolean hasTop = y > 0;
		boolean hasBottom = y < h - 1;

		// Check each neighbor based on boundary conditions
		if (hasLeft) addTileIfValid(res, x - 1, y);      // Left
		if (hasTop) addTileIfValid(res, x, y - 1);        // Top
		if (hasRight) addTileIfValid(res, x + 1, y);      // Right
		if (hasBottom) addTileIfValid(res, x, y + 1);     // Bottom

		return res;
	}


	public ArrayList<TilePiece<?>> getAllNeighbours(int x, int y) {
		var res = new ArrayList<TilePiece<?>>();
		if (tiles[x][y] == null)
			return res;
		// These are all clones:
		var foundNeighs = TilePiece.getAllNeighbours(this, new TilePiece<UpgradeGeneral>(tiles[x][y], x, y));
		// gather the real ones
		for (var fn : foundNeighs) {
			res.add(new TilePiece<>(tiles[fn.x()][fn.y()], fn.x(), fn.y()));
		}
		return res;
	}

	/**
	 * Henter 2D listen av Tile som 1D og uten null referansene
	 */
	public ArrayList<UpgradeGeneral> getLinArr() {
		var res = new ArrayList<UpgradeGeneral>();
		for (var x : tiles)
			for (var xy : x)
				if (xy != null)
					res.add(xy);
		return res;
	}

	public UpgradeGeneral[][] getDobArr() {
		return tiles;
	}

	public void createModifierTiles(Random ran, int min, int max, int modifierTilePool) {
		createModifierTiles(ran, timesMod, min, max, modifierTilePool);
//		if (Main.DEBUG) {
//			for (int i = 0; i < 5; i++) {
//				setTimesMod(i + 2, i, 0);
//			}
//		}
	}

	public void createModifierTiles(int min, int max, int modifierTilePool) {
		createModifierTiles(Features.ran, min, max, modifierTilePool);
	}

	public void createModifierTiles(Random ran, float[][] map, int min, int max, int modifierTilePool) {
		boolean foundMin = false;
		do {
			int modifier = 0;

			if (!foundMin && min != 2) {
				modifier = min;
				foundMin = true;
			} else {
				var percent = ran.nextFloat();
				int checkNum = max;
				while (checkNum >= 2) {
					if (percent < 1f / checkNum && modifierTilePool >= checkNum && modifierTilePool != checkNum + 1) {
						modifier = checkNum;
						break;
					}
					checkNum--;
				}
				if (modifier == 0) {
					if (modifierTilePool == 3)
						modifier = 3;
					else
						modifier = 2;
				}
			}
			modifierTilePool -= modifier;

			int x, y;
			do {
				x = ran.nextInt(w);
				y = ran.nextInt(h);
			} while (map[x][y] > 0 || tiles[x][y] instanceof EmptyTile);

			map[x][y] = modifier;
		} while (modifierTilePool >= 2);

	}

	public float getTimesMod(int x, int y) {
		return timesMod[x][y];
	}

	public float getTimesMod(Vec2 pos) {
		return timesMod[(int) pos.x][(int) pos.y];
	}

	public String getTimesModText(int x, int y) {
		if (!hasTimesMod(x, y))
			return null;

		if ((int) timesMod[x][y] == timesMod[x][y])
			return String.valueOf((int) timesMod[x][y]);
		return String.valueOf(timesMod[x][y]);
	}

	public boolean hasTimesMod(int x, int y) {
		return timesMod[x][y] > 0 && timesMod[x][y] != 1;
	}

	public void setTimesMod(float modifier, int x, int y) {
		timesMod[x][y] = modifier;
	}

	public void reduceTimesMod(TilePiece<?> tile) {
		if (!(tile.upgrade() instanceof Upgrade))
			return;
		
		for (var n : getNeighbours(tile.x(), tile.y()))
			if (n != null && n.upgrade().getTileName() == TileNames.Permanentifier)
				return;
		
		float timesMod = getTimesMod(tile.x(), tile.y());
		if (timesMod == 0)
			timesMod = 1f;
		float decrease = 0.5f;
		if (timesMod > 3) {
			decrease = 1f;
		} else if (timesMod - decrease <= 1) {
			if (timesMod > 1)
				timesMod = 1;
			boolean foundMerchant = false;
			for (var n : getNeighbours(tile.x(), tile.y())) {
				if (n != null && n.upgrade().getTileName() == TileNames.Merchant) {
					foundMerchant = true;
					break;
				}
			}
			decrease = foundMerchant ? 0.1f : 0.2f;
		}

		if (timesMod - decrease < minTimesMod) {
			decrease = timesMod - minTimesMod;
		}
		setTimesMod(Math.round((timesMod - decrease) * 10f) / 10f, tile.x(), tile.y());
	}

	public void addTimesMod(float modifier, int x, int y) {
		if (timesMod[x][y] == 0)
			timesMod[x][y] = 1;
		else if (timesMod[x][y] >= 5)
			modifier *= .5f;
		timesMod[x][y] = Math.round((timesMod[x][y] + modifier) * 10f) / 10f;
		if (timesMod[x][y] < 1)
			timesMod[x][y] = 1;
	}

	public void remove(UpgradeGeneral tile) {
		for (int x = 0; x < w; x++)
			for (int y = 0; y < h; y++)
				if (tiles[x][y] != null && tiles[x][y].equals(tile))
					tiles[x][y] = null;
	}

	public boolean hasMoney(int x, int y) {
		return money[x][y] > 0;
	}

	public boolean hasSale(int x, int y) {
		return sale[x][y] != 0;
	}

	public int getMoney(int x, int y) {
		if (money[x][y] < 0)
			return 0;

		return (int) money[x][y];
	}

	public void setMoney(int amount, int x, int y) {
		money[x][y] = amount;
	}

	public void addMoney(int amount, int x, int y) {
		money[x][y] += amount;
	}

	public int getWidth() {
		return w;
	}

	public int getHeight() {
		return h;
	}

	public int length() {
		return getWidth() * getHeight();
	}

	public void addImprovementPoints(int improvementPoints) {
		this.improvementPoints += improvementPoints;
	}

	public void resetImprovementPoints() {
		this.improvementPoints = 0;
	}

	public int getImprovementPoints() {
		return improvementPoints;
	}

	public int getToolsAmount(TileNames name) {
		int res = 0;
		for (var x : tiles)
			for (var xy : x)
				if (xy instanceof Tool tool) {
					res++;
					if (tool.getTileName() == name)
						res++;
				}
		return res;
	}

	public void slowlyResetDevastation() {
		for (var x = 0; x < timesMod.length; x++) {
			for (var y = 0; y < timesMod[x].length; y++) {
				if (timesMod[x][y] < 1f && timesMod[x][y] >= minTimesMod) {
					timesMod[x][y] = Math.round((timesMod[x][y] + .05f) * 100f) / 100f;
				}
			}
		}
	}

	public int getAmountType(TileNames name) {
		int count = 0;
		for (int x = 0; x < w; x++)
			for (int y = 0; y < h; y++)
				if (tiles[x][y] != null && tiles[x][y].getTileName() == name)
					count++;
		return count;
	}

	public boolean isEmpty(int x, int y) {
		return tiles[x][y] instanceof EmptyTile;
	}

	public float getSale(int x, int y) {
		if (x == -1 || y == -1 || sale[x][y] <= 0)
			return 1;
		return sale[x][y];
	}

	public void setSale(float sale, int x, int y) {
		this.sale[x][y] = sale;
	}

	public void addSale(float sale, int x, int y) {
		if (this.sale[x][y] == 0) {
			this.sale[x][y] = 1;
		}
		this.sale[x][y] += sale;
		if (this.sale[x][y] <= 0.01f) {
			this.sale[x][y] = 0.01f;
		}
	}

	public boolean attemptGoHyperBoard() {
		if (w == 7) return false;

		var amountLeft = getAmountType(null);
		if (amountLeft == 0) {
			goHyperBoard();
			return true;
		}
		return false;
	}
	
	public void goHyperBoard() {
		var w = 7;
		var h = 7;
		var newTiles = new UpgradeGeneral[w][h];
		var newTimesMod = new float[w][h];
		var newSale = new float[w][h];
		var newMoney = new int[w][h];

		long seed = 0;

		for (int y = 0; y < this.h; y++) {
			for (int x = 0; x < this.w; x++) {
				if (tiles[x][y] != null) {
					seed += tiles[x][y].getNameID();
					if (tiles[x][y] instanceof Upgrade up) {
						seed += up.getLVL();
					}
					newTiles[x + 1][y + 1] = tiles[x][y];
				}
				newTimesMod[x + 1][y + 1] = timesMod[x][y];
				newSale[x + 1][y + 1] = sale[x][y];
				newMoney[x + 1][y + 1] = money[x][y];
			}
		}
		if (FINALIZED_SEED == 0) {
			FINALIZED_SEED = seed;
		}
		var ran = new Random(FINALIZED_SEED);
		hyperUpgrades = new HyperUpgrade[3];
		hyperUpgrades[0] = new HyperUpgrade(ran);
		hyperUpgrades[1] = new HyperUpgrade(ran);
		hyperUpgrades[2] = new HyperUpgrade(ran);

		this.w = w;
		this.h = h;
		this.tiles = newTiles;
		this.timesMod = newTimesMod;
		this.sale = newSale;
		this.money = newMoney;
	}

	public HyperUpgrade[] getHypers() {
		if (hyperUpgrades == null)
			return new HyperUpgrade[0];
		return hyperUpgrades;
	}

	public void removeHypers() {
		hyperUpgrades = null;
	}
}

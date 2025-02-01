package player_local;

import player_local.upgrades.EmptyTile;
import player_local.upgrades.RegVals;
import player_local.upgrades.TileNames;
import player_local.upgrades.Tool;
import player_local.upgrades.Upgrade;
import player_local.upgrades.UpgradeGeneral;
import player_local.upgrades.UpgradeType;
import engine.math.Vec2;

import java.util.ArrayList;
import java.util.List;

public record TilePiece<T extends UpgradeGeneral>(T upgrade, int x, int y) {

	@Override
	public TilePiece<?> clone() {
		return new TilePiece<>(upgrade instanceof Upgrade up ? up.clone() : upgrade, x, y);
	}

	public Vec2 pos() {
		return new Vec2(x, y);
	}

	public boolean isNeighbour(TilePiece<?> piece) {
		if (x == -1 || piece.x == -1)
			return false;
		return Math.abs(x - piece.x) + Math.abs(y - piece.y) < 2;
	}

	private static void findNeighbours(Layer layer, TilePiece<?> tile, List<TilePiece<?>> ignores,
			List<TilePiece<?>> out, int safetynet, boolean clone) {
		if (tile.upgrade instanceof EmptyTile || safetynet > 8)
			return;
		
		int doubleCounter = 0;
		var theseOut = new ArrayList<TilePiece<?>>();
		out.add(tile);
		ignores.add(tile);
		for (var neighbour : layer.getNeighbours(tile.x(), tile.y())) {
			if (ignores.contains(neighbour) || neighbour.upgrade instanceof EmptyTile) {
				continue;
			}
			var neighbourClone = clone ? neighbour.clone() : neighbour;
			out.add(neighbourClone);
			theseOut.add(neighbourClone);
			if (neighbour.upgrade().getUpgradeType() != UpgradeType.TOOL)
				ignores.add(neighbour);

			if (neighbour.upgrade().getTileName() == TileNames.NeighborTunnel) {
				findNeighbours(layer, neighbour, ignores, out, ++safetynet, clone);
			}
			if (tile.upgrade() != null && tile.upgrade().getTileName() == TileNames.NeighborTunnel) {
				if (neighbour.upgrade().getTileName() == TileNames.Dilator) {
					doubleCounter++;
				} else if (neighbour.upgrade().getTileName() == TileNames.Yunomah) {
					for (var yn : layer.getNeighbours(neighbour.x(), neighbour.y())) {
						if (yn.upgrade().getTileName() == TileNames.Dilator)
							doubleCounter++;
					}
				}
			}

			// check this neighbours neighbours if it has neighbourtunnel or dilator
			for (var nn : layer.getNeighbours(neighbour.x(), neighbour.y())) {
				if (ignores.contains(nn)) {
					continue;
				}

				if (nn.upgrade().getTileName() == TileNames.NeighborTunnel) {
					findNeighbours(layer, nn, ignores, out, ++safetynet, clone);
				} else if (nn.upgrade().getTileName() == TileNames.Dilator) {
					if (neighbourClone.upgrade() instanceof Upgrade up) {
						if (clone)
							up.getNeighbourModifier().multiplyAllValues(2);
						out.add(nn);
	                	ignores.add(nn);
					}
				} else if (nn.upgrade().getTileName() == TileNames.Yunomah) {
					if (neighbourClone.upgrade() instanceof Upgrade up) {
						for (var yn : layer.getNeighbours(nn.x(), nn.y())) {
							if (clone && yn.upgrade().getTileName() == TileNames.Dilator)
								up.getNeighbourModifier().multiplyAllValues(2);
						}
						out.add(nn);
	                	ignores.add(nn);
					}
				}
			}
		}

		if (doubleCounter > 0 && clone) {
			for (var n : theseOut) {
				if (n.upgrade() instanceof Upgrade up) {
					up.getNeighbourModifier().multiplyAllValues(2 * doubleCounter);
				}
			}
		}
	}

	public static boolean contains(List<TilePiece<?>> list, TilePiece<?> other) {
		for (var piece : list) {
			if (piece.x == other.x && piece.y == other.y && piece.upgrade.getNameID() == other.upgrade.getNameID()) {
				return true;
			}
		}
		return false;
	}

	public static List<TilePiece<?>> getAllNeighbours(Layer layer, TilePiece<?> piece) {
		List<TilePiece<?>> res = new ArrayList<>();
		findNeighbours(layer, piece, new ArrayList<>(), res, 0, true);
		return res;
	}

	public static List<TilePiece<?>> getAllNeighboursNonClones(Layer layer, TilePiece<?> piece) {
		List<TilePiece<?>> res = new ArrayList<>();
		findNeighbours(layer, piece, new ArrayList<>(), res, 0, false);
		return res;
	}
	
	public RegVals getNeighbourModifierWithTools(Layer layer, int ogX, int ogY, boolean useThis,
			ArrayList<Vec2> ignore) {
		if (this.upgrade instanceof Upgrade upgrade) {
			int x = ogX;
			int y = ogY;
			if (useThis) {
				x = this.x;
				y = this.y;
			}

			if (x == -1)
				return upgrade.getNeighbourModifier();
			var clonedNeighbourRegVals = upgrade.getNeighbourModifier().clone();
			for (var thisNeighbour : layer.getNeighbours(x, y)) {
				if (!(thisNeighbour.upgrade() instanceof Tool) || (thisNeighbour.x() == ogX && thisNeighbour.y() == ogY)
						|| (ignore.contains(thisNeighbour.pos())))
					continue;
				switch (thisNeighbour.upgrade().getTileName()) {
				case Dilator -> clonedNeighbourRegVals.multiplyAllValues(2);
				case Yunomah -> {
					for (var yn : layer.getNeighbours(thisNeighbour.x(), thisNeighbour.y())) {
						if (yn.upgrade().getTileName() == TileNames.Dilator)
							clonedNeighbourRegVals.multiplyAllValues(2);
					}
				}
				default -> {
				}
				}
			}
			return clonedNeighbourRegVals;
		}
		return null;
	}

	public RegVals getNeighbourModifierWithTools(Layer layer, int x, int y, boolean useThis) {
		return getNeighbourModifierWithTools(layer, x, y, useThis, new ArrayList<>());
	}

	public static ArrayList<RegVals> collectNeighbourModifiersWithTools(Layer layer, int x, int y, boolean useThis,
			ArrayList<Vec2> ignore) {
		var neighbourRegVals = new ArrayList<RegVals>();
		ignore.add(new Vec2(x, y));
		for (var neighbour : layer.getNeighbours(x, y)) {
			if (ignore.contains(neighbour.pos()))
				continue;
			if (neighbour.upgrade() instanceof Upgrade) {
				neighbourRegVals.add(neighbour.getNeighbourModifierWithTools(layer, x, y, useThis, ignore));
				ignore.add(neighbour.pos());
			}
		}
		return neighbourRegVals;
	}

	public static ArrayList<RegVals> collectNeighbourModifiersWithTools(Layer layer, int x, int y, boolean useThis) {
		var list = new ArrayList<RegVals>();
		for (var n : getAllNeighbours(layer, new TilePiece<UpgradeGeneral>(null, x, y))) {
			if (n != null && n.upgrade() != null && n.upgrade() instanceof Upgrade upgrade) {
				list.add(upgrade.getNeighbourModifier());
			}
		}
		return list;
//        return collectNeighbourModifiersWithTools(layer, x, y, useThis, new ArrayList<>());
	}

}

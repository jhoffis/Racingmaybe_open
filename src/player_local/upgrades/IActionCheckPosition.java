package player_local.upgrades;

import engine.math.Vec2;

public interface IActionCheckPosition {
	boolean check(TileVisual tile, Vec2 pos);
}

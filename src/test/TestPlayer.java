package test;

import engine.io.Window;
import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.api.Test;
import player_local.Layer;
import player_local.Player;
import player_local.upgrades.TileVisual;
import scenes.game.lobby_subscenes.UpgradesSubscene;

public class TestPlayer {
	
	@Test
	void testHyperlayer() {
		
		var p0 = new Player();
		p0.layer.goHyperBoard();
		var p1 = p0.getClone();
		
		Assertions.assertEquals(p0.layer.getHeight(), p1.layer.getHeight());
	}

	@Test
	void testCheckTilePos() {
		Window.HEIGHT = 720;
		UpgradesSubscene.marginX = 500;
		UpgradesSubscene.marginY = 500;
		UpgradesSubscene.spacing = 1.15f;
		final var size = TileVisual.size() * UpgradesSubscene.spacing;

		var l = new Layer();
		l.goHyperBoard();
		var pos = UpgradesSubscene.checkTilePos(501, 501, false, l);
		System.out.println(pos);
		var pos1 = UpgradesSubscene.checkTilePos(501 + size, 501 + size, false, l);
		System.out.println(pos1);
		var pos2 = UpgradesSubscene.checkTilePos(501 + 2.3f*size, 501 + 2.3f*size, false, l);
		System.out.println(pos2);
	}
}


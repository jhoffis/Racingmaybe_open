package test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;

import elem.History;
import game_modes.GameModes;
import main.Features;
import player_local.Player;
import player_local.upgrades.RegValList;
import player_local.upgrades.Upgrade;
import scenes.game.GameRemoteMaster;

import org.junit.jupiter.api.Test;

import communication.GameInfo;
import communication.GameType;

public class TestHistory {

	@Test
	void testDiffVals() {
		
		int size = 3;
		var gainedVals = new RegValList(size);
		var oldVals = new double[size];
		oldVals[0] = Double.MAX_VALUE * 0.9;
//		oldVals[1] = 1000;
		var newVals = new double[size];
		newVals[0] = Double.POSITIVE_INFINITY;
//		newVals[1] = 250;
		
		Upgrade.addGainedValuesDifference(gainedVals, oldVals, newVals);
		
		System.out.println("heihei");
		for (var val : gainedVals.values) {
			System.out.println(val);
		}
	}
	
	@Test
	void testSameEndGoal() {
        var game = new GameRemoteMaster(null);
		
		var alice = new Player("Alice", Player.DEFAULT_ID, Player.HOST, Features.generateLanId(true));
        var aliceCom = new GameInfo(game, GameType.CREATING_LAN);
        aliceCom.join(alice, GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);
        aliceCom.init(GameModes.LEADOUT, 0, null, null);
        
        var p2 = new Player("Bob", Player.DEFAULT_ID, Player.PLAYER, Features.generateLanId(false));
        var info2 = new GameInfo(game, GameType.JOINING_LAN);
        info2.join(p2, GameInfo.JOIN_TYPE_VIA_CLIENT, (player) -> {}, 0, 0);

        var p3 = new Player("Charlie", Player.DEFAULT_ID, Player.PLAYER, Features.generateLanId(false));
        var info3 = new GameInfo(new GameRemoteMaster(null), GameType.JOINING_LAN);
        info3.join(p3, GameInfo.JOIN_TYPE_VIA_CLIENT, (player) -> {}, 0, 0);
        
//        TestTranslator.await(2);
        Assertions.assertEquals(
        		aliceCom.getGamemode().getEndGoal(),
        		info2.getGamemode().getEndGoal(),
        		info3.getGamemode().getEndGoal()
        		);
        
        aliceCom.startRace(0);

//        TestTranslator.await(1);
        Assertions.assertEquals(
        		aliceCom.getGamemode().getEndGoal(),
        		info2.getGamemode().getEndGoal(),
        		info3.getGamemode().getEndGoal()
        		);
        
        p3 = info3.player;
        info2.leave(p3, true, false);
        info3.getRemote().running = false;

        TestTranslator.await(1);
        Assertions.assertEquals(
        		aliceCom.getGamemode().getEndGoal(),
        		info2.getGamemode().getEndGoal()
        		);
        
        info3 = new GameInfo(game, GameType.JOINING_LAN);
        info3.join(p3, GameInfo.JOIN_TYPE_VIA_CLIENT, (player) -> {}, 0, 0);

        TestTranslator.await(2);
        Assertions.assertEquals(
        		aliceCom.getGamemode().getEndGoal(),
        		info2.getGamemode().getEndGoal(),
        		info3.getGamemode().getEndGoal()
        		);
        
        var list = new ArrayList<GameInfo>();
        list.add(aliceCom);
        list.add(info2);
        list.add(info3);
        
        TestTranslator.testEqualAmountOfPlayers(list);
	}

//	// TODO M� legge til slik at en kan lage til containers eller depth slik at man kan utvide i spesielle plasser, feks med en tile i layer, og skipping i layer.
//	@Test
//	void testHistory() {
//
//		byte id = 2;
//		int gameID = 12312;
//		long steamID = 12938210398l;
//		String name = "adsj";
//		int podium = 23;
//		int aheadByPoints = 1232;
//
//		var h = new History();
//		h.startCreation(false);
//		h.add(id);
//		h.add(gameID);
//		h.add(steamID);
//		h.add(name);
//		h.add(podium);
//		h.add(aheadByPoints);
//		String res = h.endCreation();
//		System.out.println(res);
//
//		var h2 = new History();
//		h2.startRecieve(res);
//		byte id2 = h2.parseByte();
//		int gameID2 = h2.parseInt();
//		long steamID2 = h2.parseLong();
//		String name2 = h2.parseStr();
//		int podium2 = h2.parseInt();
//		int aheadByPoints2 = h2.parseInt();
//		h2.endRecieve();
//
//		assertEquals(id, id2);
//		assertEquals(gameID, gameID2);
//		assertEquals(steamID, steamID2);
//		assertEquals(name, name2);
//		assertEquals(podium, podium2);
//		assertEquals(aheadByPoints, aheadByPoints2);
//
//		id = 23;
//		name = "gonnar";
//		h.startCreation(false);
//		h.add(id);
//		h.add(gameID);
//		h.add(steamID);
//		h.add(name);
//		h.add(podium);
//		h.add(aheadByPoints);
//		res = h.endCreation();
//		System.out.println(res);
//
//		h2.startRecieve(res);
//		id2 = h2.parseByte();
//		gameID2 = h2.parseInt();
//		steamID2 = h2.parseLong();
//		name2 = h2.parseStr();
//		podium2 = h2.parseInt();
//		aheadByPoints2 = h2.parseInt();
//		h2.endRecieve();
//
//		assertEquals(id, id2);
//		assertEquals(gameID, gameID2);
//		assertEquals(steamID, steamID2);
//		assertEquals(name, name2);
//		assertEquals(podium, podium2);
//		assertEquals(aheadByPoints, aheadByPoints2);
//
//		h.startCreation(false);
//		h.add(id);
//		h.add(gameID);
//		h.add(steamID);
//		h.add(name);
//		h.add(podium);
//		h.add(aheadByPoints);
//		res = h.endCreation();
//
//		h2.startRecieve(res);
//		id2 = h2.parseByte();
//		gameID2 = h2.parseInt();
//		steamID2 = h2.parseLong();
//		name2 = h2.parseStr();
//		podium2 = h2.parseInt();
//		aheadByPoints2 = h2.parseInt();
//		h2.endRecieve();
//
//		assertEquals(id, id2);
//		assertEquals(gameID, gameID2);
//		assertEquals(steamID, steamID2);
//		assertEquals(name, name2);
//		assertEquals(podium, podium2);
//		assertEquals(aheadByPoints, aheadByPoints2);
//	}
//
//	@Test
//	void testMultiplePlayers() {
//
//
//	}
}
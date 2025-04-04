package test;

import comNew.*;
import communication.GameType;
import communication.Response;
import communication.Translator;
import communication.GameInfo;
import communication.remote.Message;
import engine.utils.TwoTypes;
import game_modes.GameMode;
import game_modes.GameModes;
import game_modes.TotalMode;
import main.Features;
import main.Main;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import player_local.Player;
import player_local.upgrades.RegVal;
import player_local.upgrades.RegVal.RegValType;
import player_local.upgrades.RegVals;
import scenes.game.GameRemoteMaster;
import scenes.game.Lobby;
import scenes.game.lobby_subscenes.UpgradesSubscene;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kanskje også ha en timestamp ved hver kobling slik at man venter på den
 * forrige først?
 */
public class TestMultiplayer {

	private long time = 0;
	private long averageTime = 0;
	private long calcTime = 0;

	public void startTime() {
		time = System.nanoTime();
	}

	public void time(boolean print) {
		calcTime = System.nanoTime() - time;
		if (averageTime == 0) {
			averageTime = calcTime;
		} else {
			averageTime += calcTime;
			averageTime /= 2;
		}
		if (print)
			printTime();
		time = System.nanoTime();
	}

	public void printTime() {
		System.out.println(calcTime + " ns, avg: " + averageTime + " ns");
	}

	private void snork() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
		}
	}

	@Test
	void sendMsg() {
		var alice = new Remote2();
		var bob = new Remote2();

		startTime();
		for (int i = 0; i < 1000; i++) {
			alice.push(new Message(0, "test"));
			alice.sendDirectly(bob);
			time(false);
		}
		Assertions.assertTrue(bob.inQueue.size() > 0);
		printTime();
		alice.close();
		bob.close();
	}

	@Test
	void sendJoin2Players() {
		var alice = new Remote2();
		var bob = new Remote2();
		var gms = new ArrayList<GameInfo>();
		gms.add(alice.info.gameInfo);
		gms.add(bob.info.gameInfo);

		alice.info.gameInfo.join(new Player("Alice", Player.DEFAULT_ID, Player.HOST, Features.generateLanId(true)),
				GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);
		bob.info.gameInfo.join(new Player("Bob#Bob", Player.DEFAULT_ID, Player.PLAYER, Features.generateLanId(false)),
				GameInfo.JOIN_TYPE_VIA_CLIENT, null, 0, 0);

		var msg = Message.msgJoin(0, bob.info.gameInfo.player);
		Assertions.assertNotNull(Translator.whichMessageType(msg.requestMessage));
		bob.push(msg);
		Assertions.assertTrue(bob.outQueue.size() > 0);
		bob.sendDirectly(alice);
		Assertions.assertEquals(0, bob.outQueue.size());
		Assertions.assertTrue(alice.inQueue.size() > 0);
		alice.collect();
		Assertions.assertEquals(0, alice.inQueue.size());
		alice.sendDirectly(bob);
		bob.collect();
		Assertions.assertTrue(bob.outQueue.size() > 0);
		bob.sendDirectly(alice);
		Assertions.assertTrue(alice.inQueue.size() > 0);
		alice.collect();

		TestTranslator.testEqualAmountOfPlayers(gms);

		Assertions.assertEquals("Alice", alice.info.gameInfo.player.name);
		Assertions.assertEquals("Bob#Bob", bob.info.gameInfo.player.name);
	}

	@Test
	void sendHostLeaveJoin3Players() {
		var alice = new Remote2();
		var bob = new Remote2();
		var charlie = new Remote2();
		var gis = new ArrayList<GameInfo>();
		gis.add(alice.info.gameInfo);
		gis.add(bob.info.gameInfo);

		alice.info.gameInfo.join(new Player("Alice", Player.DEFAULT_ID, Player.HOST, Features.generateLanId(true)),
				GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);
		bob.info.gameInfo.join(new Player("Bob", Player.DEFAULT_ID, Player.PLAYER, Features.generateLanId(false)),
				GameInfo.JOIN_TYPE_VIA_CLIENT, null, 0, 0);
		charlie.info.gameInfo.join(
				new Player("Charlie", Player.DEFAULT_ID, Player.PLAYER, Features.generateLanId(false)),
				GameInfo.JOIN_TYPE_VIA_CLIENT, null, 0, 0);

		var msg = Message.msgJoin(0, bob.info.gameInfo.player);
		bob.push(msg);
		Assertions.assertEquals(1, bob.outQueue.size());
		bob.sendDirectly(alice); // join
		alice.collect();
		alice.sendDirectly(bob); // joinRes
		bob.collect();
		bob.sendDirectly(alice); // joined
		alice.collect();

		TestTranslator.testEqualAmountOfPlayers(gis);
		gis.add(charlie.info.gameInfo);

		msg = Message.msgJoin(0, charlie.info.gameInfo.player);
		charlie.push(msg);
		charlie.sendDirectly(alice); // join
		alice.collect();
		alice.sendDirectly(charlie); // joinRes
		charlie.collect();
		charlie.sendDirectly(alice); // joined
		alice.collect();
		alice.sendDirectly(bob); // joinTell
		bob.collect();

		TestTranslator.testEqualAmountOfPlayers(gis);

		((TestRemote) bob.way).set(alice.info.gameInfo.player.steamID, alice);
		((TestRemote) charlie.way).set(alice.info.gameInfo.player.steamID, alice);
		((TestRemote) alice.way).set(bob.info.gameInfo.player.steamID, bob);
		((TestRemote) charlie.way).set(bob.info.gameInfo.player.steamID, bob);
		((TestRemote) alice.way).set(charlie.info.gameInfo.player.steamID, charlie);
		((TestRemote) bob.way).set(charlie.info.gameInfo.player.steamID, charlie);

		msg = Message.msgLeave(alice.info.gameInfo.player);
		alice.push(msg);
		alice.send();
		charlie.collect();
		bob.collect();
		Assertions.assertEquals(2, bob.info.gameInfo.getPlayers().length);
		Assertions.assertEquals(2, charlie.info.gameInfo.getPlayers().length);
		Assertions.assertEquals(Player.HOST, bob.info.gameInfo.player.role);
		Assertions.assertEquals(Player.PLAYER, charlie.info.gameInfo.player.role);

		gis.remove(0);
		TestTranslator.testEqualAmountOfPlayers(gis);

		System.out.println("rejoin:");
		var rejoinId = alice.info.gameInfo.player.steamID;
		alice.info = new RemoteInfo(alice, new GameInfo());
		alice.info.gameInfo.join(new Player("Alice", Player.DEFAULT_ID, Player.PLAYER, rejoinId),
				GameInfo.JOIN_TYPE_VIA_CLIENT, null, 0, 0);

		msg = Message.msgJoin(bob.info.gameInfo.player.steamID, alice.info.gameInfo.player);
		alice.push(msg);
		alice.send();
		bob.collect();
		Assertions.assertEquals(1, bob.outQueue.size());
		bob.send();
		Assertions.assertEquals(1, alice.inQueue.size());
		alice.collect();
		alice.send();
		bob.collect();
		bob.send();
		charlie.collect();

		Assertions.assertEquals(3, alice.info.gameInfo.getPlayers().length);
		Assertions.assertEquals(3, bob.info.gameInfo.getPlayers().length);
		Assertions.assertEquals(3, charlie.info.gameInfo.getPlayers().length);
		gis.add(alice.info.gameInfo);
		TestTranslator.testEqualAmountOfPlayers(gis);
	}

	@Test
	void sendStartRace() {
		var alice = new Remote2();
		var bob = new Remote2();
		((TestRemote) alice.way).set(1, bob);
		((TestRemote) bob.way).set(0, alice);
		alice.info.gameInfo.join(new Player("Alice", Player.DEFAULT_ID, Player.HOST, 0), GameInfo.JOIN_TYPE_VIA_CREATOR,
				null, 0, 0);
		bob.info.gameInfo.join(new Player("Bob", Player.DEFAULT_ID, Player.HOST, 0), GameInfo.JOIN_TYPE_VIA_CREATOR,
				null, 0, 0);
		var aP = alice.info.gameInfo.player;
		var bP = bob.info.gameInfo.player;
		bP.role = Player.PLAYER;
		aP.steamID = 1;
		bP.steamID = 2;
		aP.id = 1;
		bP.id = 1;
		aP.gameID = alice.info.gameInfo.getGameID();
		bP.gameID = alice.info.gameInfo.getGameID();
		bob.info.gameInfo.setGameID(alice.info.gameInfo.getGameID());

		alice.info.gameInfo.addPlayer(bP);
		alice.info.gameInfo.getGamemode().setRacing(true);
		bob.info.gameInfo.addPlayer(aP);
		alice.run();
		bob.run();

		Assertions.assertEquals(2, alice.info.gameInfo.getPlayers().length);
		Assertions.assertEquals(2, bob.info.gameInfo.getPlayers().length);

		alice.push(Message.msgReady(aP, aP.readyTime, aP.ready));
		while (bob.collected == 0)
			;
		Assertions.assertEquals(aP.ready, bob.info.gameInfo.getPlayerSteamId(aP.steamID).ready);
		Assertions.assertEquals(aP.readyTime, bob.info.gameInfo.getPlayerSteamId(aP.steamID).readyTime);

		alice.push(Message.msgRaceStarted(alice.info.gameInfo.getGamemode().getRound()));
		while (bob.collected == 1)
			;
		Assertions.assertTrue(bob.info.gameInfo.getGamemode().isRacing());

		Assertions.assertEquals(2, alice.info.gameInfo.getPlayers().length);
		Assertions.assertEquals(2, bob.info.gameInfo.getPlayers().length);
		bob.info.gameInfo.finishRace(bP, 2345, 123);
		bob.push(Message.msgFinishRace(bP, 2345, 123));
		Assertions.assertEquals(2, alice.info.gameInfo.getPlayers().length);
		Assertions.assertEquals(2, bob.info.gameInfo.getPlayers().length);

		while (alice.collected == 0)
			;
		Assertions.assertEquals(2, alice.info.gameInfo.getPlayers().length);
		Assertions.assertEquals(2, bob.info.gameInfo.getPlayers().length);
		alice.info.gameInfo.finishRace(aP, 4567, 567);
		alice.push(Message.msgFinishRace(aP, 4567, 567));
		Assertions.assertEquals(2, alice.info.gameInfo.getPlayers().length);
		Assertions.assertEquals(2, bob.info.gameInfo.getPlayers().length);

		while (bob.collected == 2)
			;
		Assertions.assertFalse(alice.info.gameInfo.getGamemode().isRacing());
		Assertions.assertFalse(bob.info.gameInfo.getGamemode().isRacing());
		Assertions.assertEquals(2, alice.info.gameInfo.getPlayers().length);
		Assertions.assertEquals(2, bob.info.gameInfo.getPlayers().length);
		Assertions.assertEquals(Translator.getCloneString(aP),
				Translator.getCloneString(bob.info.gameInfo.getPlayerSteamId(aP.steamID)));
		Assertions.assertEquals(Translator.getCloneString(bP),
				Translator.getCloneString(alice.info.gameInfo.getPlayerSteamId(bP.steamID)));

		bob.running = false;
		alice.running = false;
	}

	@Test
	void sendNewCar() {
		var host = new Remote2();
		var player = host.info.gameInfo.addPlayer(new Player());
		player.role = 2;
		player.joined = true;
		host.info.gameInfo.player = player;
		host.info.gameInfo.init(null, 0, null, null);
		Assertions.assertEquals(0, player.getCarNameID());
		Assertions.assertFalse(player.getCarRep().isRandom());

		var msg = Message.msgSelectCar(player, 0, 2, false);
		var which = Translator.whichMessageType(msg.requestMessage);
		Assertions.assertEquals(MessageType.response, which);

		Translator.understandResponse(msg, new RemoteInfo(null, host.info.gameInfo));
		Assertions.assertNotEquals(2, player.getCarNameID());
		Assertions.assertEquals(0, player.getCarNameID());
		Assertions.assertFalse(player.getCarRep().isRandom());

		msg = Message.msgSelectCar(player, 1, 2, false);

		Translator.understandResponse(msg, new RemoteInfo(null, host.info.gameInfo));
		Assertions.assertEquals(2, player.getCarNameID());
		Assertions.assertFalse(player.getCarRep().isRandom());

		msg = Message.msgSelectCar(player, 2, 3, true);
		Translator.understandResponse(msg, new RemoteInfo(null, host.info.gameInfo));
		Assertions.assertEquals(3, player.getCarNameID());
		Assertions.assertTrue(player.getCarRep().isRandom());
	}

	@Test
	void sendRaceLights() {
		var gi = new GameInfo();
		gi.join(new Player("host", 0, Player.HOST, 0), GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);

		var oldRaceLightsArr = gi.getRaceLights();
		Assertions.assertNotNull(gi.getRaceLights());
		Assertions.assertEquals(6, oldRaceLightsArr.length);

		var oldRaceLights = gi.getRaceLightsString();
		var splitLightsStr = oldRaceLights.split(Translator.split);
		Assertions.assertEquals(oldRaceLightsArr.length, splitLightsStr.length);
		for (int i = 0; i < oldRaceLightsArr.length; i++) {
			Assertions.assertTrue(oldRaceLightsArr[i] > 0);
			Assertions.assertEquals(oldRaceLightsArr[i], Long.parseLong(splitLightsStr[i]));
		}

		var msg = Message.msgRaceLights(oldRaceLights);
		Assertions.assertEquals(MessageType.response, Translator.whichMessageType(msg.requestMessage));

		gi.updateRaceLights();
		Assertions.assertNotEquals(gi.getRaceLightsString(), oldRaceLights);
		Translator.understandResponse(msg, new RemoteInfo(null, gi));
		Assertions.assertEquals(gi.getRaceLightsString(), oldRaceLights);
	}

	@Test
	void sendUpdateClone() {

		var pOld = new Player();
		var pNew = new Player("NewName", 2, 3, 2);

		var gi = new GameInfo();
		pOld = gi.addPlayer(pOld);
		pNew.gameID = pOld.gameID;

		var addedHistory = pNew.addHistory(Translator.getCloneString(pNew), false, 1);
		var msg = Message.msgUpdateClone(pOld, addedHistory.cloneString(), 1);
		var which = Translator.whichMessageType(msg.requestMessage);
		Assertions.assertEquals(MessageType.response, which);

		Translator.understandResponse(msg, new RemoteInfo(null, gi));

		var hist = pOld.getHistory();
		Assertions.assertEquals(2, hist.size());
		Assertions.assertEquals(pNew.peekHistory(), pOld.peekHistory());

		msg = Message.msgUpdateClone(pOld, addedHistory.cloneString(), 10);
		Translator.understandResponse(msg, new RemoteInfo(null, gi));
		Assertions.assertEquals(11, hist.size());
	}

	@Test
	void sendSetInRace() {
		var p = new Player();
		var gi = new GameInfo();
		p = gi.addPlayer(p);
		var msg = Message.msgSetInRace(p, true);
		Assertions.assertEquals(MessageType.response, Translator.whichMessageType(msg.requestMessage));

		Assertions.assertFalse(p.inTheRace);
		Translator.understandResponse(msg, new RemoteInfo(null, gi));
		Assertions.assertTrue(p.inTheRace);
	}

	@Test
	void ready() {
		var p = new Player();
		var gi = new GameInfo();
		p = gi.addPlayer(p);
		var msg = Message.msgReady(p, -1, (byte) 1);
		Assertions.assertEquals(MessageType.response, Translator.whichMessageType(msg.requestMessage));

		Assertions.assertFalse(p.isReady());
		Translator.understandResponse(msg, new RemoteInfo(null, gi));
		Assertions.assertFalse(p.isReady());

		msg = Message.msgReady(p, 2, (byte) 1);
		Assertions.assertFalse(p.isReady());
		Assertions.assertEquals(0, p.readyTime);
		Translator.understandResponse(msg, new RemoteInfo(null, gi));
		Assertions.assertTrue(p.isReady());
		Assertions.assertEquals(2, p.readyTime);
	}

	@Test
	void undo() {
		var p = new Player();
		var gi = new GameInfo();
		p = gi.addPlayer(p);
		var clone = Translator.getCloneString(p);
		p.addHistory(Translator.getCloneString(new Player("asd", 2, 2, 2)), 1);
		p.addHistory(clone, 1);
		p.setHistoryNow();
		var msg = Message.msgUndo(p, 0, 0, 0, false);
		Assertions.assertEquals(MessageType.response, Translator.whichMessageType(msg.requestMessage));

		Assertions.assertEquals(2, p.getHistory().size());
		Translator.understandResponse(msg, new RemoteInfo(null, gi));
		Assertions.assertEquals(2, p.getHistory().size());
		Assertions.assertEquals(clone, Translator.getCloneString(p));

		msg = Message.msgUndo(p, 1, 0, 0, false);
		Translator.understandResponse(msg, new RemoteInfo(null, gi));
		Assertions.assertEquals(2, p.getHistory().size());

		msg = Message.msgUndo(p, 1, 0, 1, false);
		Translator.understandResponse(msg, new RemoteInfo(null, gi));
		Assertions.assertEquals(1, p.getHistory().size());

		msg = Message.msgUndo(p, 1, 0, 1, true);
		Translator.understandResponse(msg, new RemoteInfo(null, gi));
		Assertions.assertEquals(2, p.getHistory().size());
	}

	@Test
	void msgGameModeChange() {
		var gi = new GameInfo();
		var gi2 = new GameInfo();
		var host = new Player("Alice", Player.DEFAULT_ID, Player.HOST, 0);
		gi.join(host, GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);
		gi2.join(host, GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);
		gi.init(1);

		var msg = Message.msgGameModeChange(gi.getGamemode(), 0);
		Assertions.assertEquals(MessageType.response, Translator.whichMessageType(msg.requestMessage));

		Assertions.assertNotEquals(gi.getGamemode().getAllInfo(), gi2.getGamemode().getAllInfo());
		Translator.understandResponse(msg, new RemoteInfo(null, gi2));
		Assertions.assertNotEquals(gi.getGamemode().getAllInfo(), gi2.getGamemode().getAllInfo());
		Assertions.assertNotEquals(gi.getGamemode().getGameModeEnum(), gi2.getGamemode().getGameModeEnum());

		msg = Message.msgGameModeChange(gi.getGamemode(), 1);
		Translator.understandResponse(msg, new RemoteInfo(null, gi2));
		Assertions.assertEquals(gi.getGamemode().getAllInfo(), gi2.getGamemode().getAllInfo());
	}

	@Test
	void msgRaceInfo() {
		var p = new Player();
		p.role = Player.HOST;
		var gi = new GameInfo();
		p = gi.join(p, GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);
		gi.getGamemode().setRacing(true);
		var msg = Message.msgRaceInfo(p, 240, 300, 2, false, 12346L);
		Assertions.assertEquals(MessageType.response, Translator.whichMessageType(msg.requestMessage));

		var model = p.car.getModel();
		Assertions.assertEquals(0, model.getSpeed());
		Assertions.assertEquals(0, model.getSpdInc());
		Assertions.assertEquals(0, model.getLastTime());
		Translator.understandResponse(msg, new RemoteInfo(null, gi));
		Assertions.assertEquals(300, model.getSpeed());
		Assertions.assertEquals(2, model.getSpdInc());
		Assertions.assertEquals(12346, model.getLastTime());
	}

	@Test
	void msgGameModeAllInfo() {
		var gi = new GameInfo();
		var gi2 = new GameInfo();
		var host = new Player("Alice", Player.DEFAULT_ID, Player.HOST, 0);
		gi.join(host, GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);
		gi2.join(host, GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);

		gi.setGameID(host.gameID);
		gi2.setGameID(host.gameID);

		var changedGamemode = gi.getGamemode();
		changedGamemode.setRacing(true);
		changedGamemode.prepareNextRaceManually(120);
		changedGamemode.prepareNextRaceManually(120);

		Assertions.assertEquals(changedGamemode.getGameModeEnum(), gi2.getGamemode().getGameModeEnum());
		Assertions.assertNotEquals(changedGamemode.getAllInfo(), gi2.getGamemode().getAllInfo());

		var msg = Message.msgGameModeAllInfo(host, changedGamemode, 0);
		Assertions.assertEquals(MessageType.response, Translator.whichMessageType(msg.requestMessage));
		Translator.understandResponse(msg, new RemoteInfo(null, gi2));

		Assertions.assertEquals(changedGamemode.getAllInfo(), gi2.getGamemode().getAllInfo());
	}

	@Test
	void msgChat() {
		final var player = new Player("Testnavn", 0, 0, 0);
		final var chatText = "Hei hvordan går det?";
		var msg = Message.msgChat(chatText);

		final int[] amountChatActionRan = { 0 };

		var gi = new GameInfo();
		gi.setActionNewChat(chat -> {
			Assertions.assertFalse(chat.contains("#"));
			if (player.id != 0) {
				Assertions.assertTrue(chat.contains(player.name));
			}
			Assertions.assertTrue(chat.contains(chatText));
			amountChatActionRan[0]++;
		});

		Translator.understandResponse(msg, new RemoteInfo(null, gi));
		Assertions.assertEquals(1, amountChatActionRan[0]);

		Translator.understandResponse(gi.createChat(null, chatText), new RemoteInfo(null, gi));
		Assertions.assertEquals(3, amountChatActionRan[0]);

		player.id = 1;
		Translator.understandResponse(gi.createChat(player, chatText), new RemoteInfo(null, gi));
		Assertions.assertEquals(5, amountChatActionRan[0]);

		gi.setActionNewChat(chat -> {
			Assertions.assertEquals("", chat);
			amountChatActionRan[0]++;
		});
		Translator.understandResponse(gi.createChat(null, ""), new RemoteInfo(null, gi));
		Assertions.assertEquals(7, amountChatActionRan[0]);
	}

	@Test
	void msgTempStatus() {
		GameMode gm = new TotalMode(GameModes.TOTAL, 0, 0, null);
		var msg = Message.msgTempStatus(Integer.MAX_VALUE, gm, 1234, 1000, new Player[] {});
		var gi = new GameInfo();
		gi.setGameMode(gm);

		var countdownOld = gi.getCountdown();
		Translator.understandResponse(msg, new RemoteInfo(null, gi));
		var countdownNew = gi.getCountdown();
		Assertions.assertFalse(gi.isGameStarted());
		Assertions.assertEquals(countdownOld, countdownNew);

		gi.join(new Player("", 0, Player.HOST, 0), GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);
		gi.getGamemode().setRacing(true);
		Assertions.assertTrue(gi.isGameStarted());
		gm = gi.getGamemode();
		msg = Message.msgTempStatus(gi.gmCreationTime, gm, 1234, 1000, new Player[] {});
		Translator.understandResponse(msg, new RemoteInfo(null, gi));
		countdownNew = gi.getCountdown();
		Assertions.assertNotEquals(countdownOld, countdownNew);

		var playerNew = new Player();
		Translator.setCloneString(playerNew, gi.player);
		playerNew.ready = (byte) (playerNew.ready == 0 ? 1 : 0);
		msg = Message.msgTempStatus(gi.gmCreationTime, gm, 0, 0, new Player[] { playerNew });
		Translator.understandResponse(msg, new RemoteInfo(null, gi));
		Assertions.assertNotEquals(playerNew.ready, gi.player.ready);
		Assertions.assertEquals(playerNew.readyTime, gi.player.readyTime);

//		playerNew.readyTime++;
//		Assertions.assertNotEquals(playerNew.readyTime, gi.player.readyTime);
//		msg = Message.msgTempStatus(gi.gmCreationTime, gm, 0, 0, new Player[] { playerNew });
//		Translator.understandResponse(msg, new RemoteInfo(null, gi));
//		Assertions.assertEquals(playerNew.ready, gi.player.ready);
//		Assertions.assertEquals(playerNew.readyTime, gi.player.readyTime);
	}

	void emptyLobbyClearMessages(GameType type) {
		var gi = new GameInfo(new GameRemoteMaster(null), type);
		var p = gi.join(new Player("", 0, Player.HOST, 0), GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);
		Assertions.assertNotNull(p);
		gi.init(1);
		gi.init(1);
		gi.init(1);
		snork();
		Assertions.assertEquals(0, gi.getRemote().outQueue.size());
		gi.getRemote().running = false;
	}

	@Test
	void emptyLobbyClearMessagesDirect() {
		emptyLobbyClearMessages(GameType.DIRECT);
	}

	@Test
	void emptyLobbyClearMessagesLan() {
		emptyLobbyClearMessages(GameType.CREATING_LAN);
	}

	void joiningLan(GameType type) {

//        new Thread(() -> {
//            var timeLeft = System.currentTimeMillis() + 10000;
//            while (System.currentTimeMillis() < timeLeft) snork();
//            Assertions.fail();
//        }).start();

		var remotes = new ArrayList<TwoTypes<Long, Remote2>>();
		var gis = new ArrayList<GameInfo>();
		var alice = new GameInfo(new GameRemoteMaster(null), type);
		var res = alice.join(new Player("Alice", Player.DEFAULT_ID, Player.HOST, 0), GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);
		Assertions.assertNotNull(res);
		gis.add(alice);

		for (int i = 0; i < 16; i++) {
			var joiner = new GameInfo(new GameRemoteMaster(null), type.join());
			long joinerSteamID = i + 1;
			if (type == GameType.DIRECT) {
				((TestRemote) joiner.getRemote().way).set(0, alice.getRemote());
				((TestRemote) alice.getRemote().way).set(joinerSteamID, joiner.getRemote());
				for (var remote : remotes) {
					((TestRemote) joiner.getRemote().way).set(remote.first(), remote.second());
					((TestRemote) remote.second().way).set(joinerSteamID, joiner.getRemote());
				}
			}
			final int[] amountCB = { 0 };

			res = joiner.join(new Player("P" + i, Player.DEFAULT_ID, Player.PLAYER, joinerSteamID),
					GameInfo.JOIN_TYPE_VIA_CLIENT, player -> {
						Assertions.assertEquals(joinerSteamID, player.steamID);
						amountCB[0]++;
					}, 0, 0);
			Assertions.assertNotNull(res);
			remotes.add(new TwoTypes<>(joinerSteamID, joiner.getRemote()));
			gis.add(joiner);

			while (true) {
				if (amountCB[0] == 0) {
					snork();
					continue;
				}
				var found = false;
				for (var gi : gis) {
					if (gi.getPlayers().length != i + 2) {
						snork();
						found = true;
						break;
					}
				}
				if (!found)
					break;
			}
			Assertions.assertEquals(1, amountCB[0]);
			TestTranslator.testEqualAmountOfPlayers(gis);

			for (var gi : gis)
				for (var p : gi.getPlayers())
					Assertions.assertTrue(p.joined);
		}

		for (var remote : remotes) {
			remote.second().running = false;
		}
	}

	@Test
	void joiningDirect() {
		joiningLan(GameType.DIRECT);
	}

	@Test
	void joiningLan() {
		joiningLan(GameType.CREATING_LAN);
	}

	@Test
	void receiveMessageLan() throws IOException {
		var lan0 = new LocalRemote2("localhost", GameType.CREATING_LAN);
		var lan1 = new LocalRemote2("localhost", GameType.JOINING_LAN);
		snork();
		Assertions.assertTrue(lan0.lines.get(0).running);
		Assertions.assertTrue(lan0.lines.get(0).connected);
		Assertions.assertTrue(lan1.lines.get(0).running);
		Assertions.assertTrue(lan1.lines.get(0).connected);
		lan1.sendMessage(Message.msgChat("hei"));
		snork();
		Assertions.assertTrue(lan0.getMessageAvailable() > 0);
		Assertions.assertEquals(0, lan1.getMessageAvailable());
		lan0.clearMessagesTest();

		lan1.leave(new Player("", -1, -1, -1));
		Assertions.assertTrue(lan1.lines.isEmpty());
		snork();
		Assertions.assertFalse(lan0.lines.get(0).running);
		Assertions.assertFalse(lan0.lines.get(0).connected);

		lan1.destroy();
		lan0.destroy();
	}

	@Test
	void joinTell() {
		var player = new Player("other", Player.DEFAULT_ID, Player.PLAYER, 1234);
		var msg = Message.msgJoinTell(player, 2);
		var gi = new GameInfo();
		gi.join(new Player("Alice", Player.DEFAULT_ID, Player.HOST, 0), GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);
		Assertions.assertNotEquals(2, gi.getGamemode().getEndGoal());
		Translator.understandResponse(new Message(0, msg), new RemoteInfo(null, gi));
		Assertions.assertEquals(2, gi.getGamemode().getEndGoal());
		var players = gi.getPlayers();
		Assertions.assertEquals(2, players.length);
		Assertions.assertEquals(0, players[0].steamID);
		Assertions.assertEquals(1234, players[1].steamID);
	}

	@Test
	void joinEndingUpAtRightRequester() {
		var gi = new GameInfo();
		gi.join(new Player("Alice", Player.DEFAULT_ID, Player.HOST, 0), GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);

		var msg = Message.msgJoin(0, new Player());
		msg.requestMessage = Translator.understandRequest(msg, new RemoteInfo(gi.getRemote(), gi));
		Assertions.assertEquals(Player.DEFAULT_ID, msg.requesterID);
	}

	@Test
	void falseMessage() {
		var result = Translator.understandRequest(new Message(0, "asdflaksj"), null);
		Assertions.assertEquals(Response.ENDALL, result);
		result = Translator.understandRequest(new Message(0, Translator.mailRequest + "asdflaksj"), null);
		Assertions.assertEquals(Response.ENDALL, result);
		result = Translator.understandRequest(
				new Message(0, Translator.mailRequest + Translator.join + Translator.split + "asdflaksj"), null);
		Assertions.assertEquals(Response.ENDALL, result);
		result = Translator.understandRequest(new Message(0, Translator.mailRequest + Translator.join + Translator.split
				+ Main.VERSION + Translator.split + "asdflaksj"), null);
		Assertions.assertEquals(Response.ENDALL, result);
	}

	@Test
	void leaveLan() {
		var alice = new GameInfo(new GameRemoteMaster(null), GameType.CREATING_LAN);
		alice.join(new Player("Alice", Player.DEFAULT_ID, Player.HOST, 0), GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);
		var joined = new int[1];
		var bob = new GameInfo(new GameRemoteMaster(null), GameType.JOINING_LAN);
		bob.join(new Player("Bob", Player.DEFAULT_ID, Player.PLAYER, 1), GameInfo.JOIN_TYPE_VIA_CLIENT,
				player -> joined[0]++, 0, 0);
		int prov = 0;
		while (joined[0] == 0) {
			snork();
			prov++;
			if (prov == 30) Assertions.fail();
		}
		Assertions.assertEquals(2, alice.getPlayers().length);
		Assertions.assertEquals(2, bob.getPlayers().length);

		joined[0] = 0;
		var charlie = new GameInfo(new GameRemoteMaster(null), GameType.JOINING_LAN);
		charlie.join(new Player("charlie", Player.DEFAULT_ID, Player.PLAYER, 2), GameInfo.JOIN_TYPE_VIA_CLIENT,
				player -> joined[0]++, 0, 0);
		prov = 0;
		while (bob.getPlayers().length == 2){
			snork();
			prov++;
			if (prov == 30) Assertions.fail();
		}
		Assertions.assertEquals(3, alice.getPlayers().length);
		Assertions.assertEquals(3, bob.getPlayers().length);
		Assertions.assertEquals(3, charlie.getPlayers().length);

		bob.leave(bob.player, true, false);
		prov = 0;
		while (alice.getPlayers().length == 3){
			snork();
			prov++;
			if (prov == 30) Assertions.fail();
		}

		Assertions.assertEquals(2, alice.getPlayers().length);
		Assertions.assertEquals(2, charlie.getPlayers().length);
		Assertions.assertTrue(alice.getRemote().running);
		Assertions.assertFalse(bob.getRemote().running);
		Assertions.assertTrue(charlie.getRemote().running);

		charlie.close();
		bob.close();
		alice.close();
	}

	@Test
	void conflictingIdsUpdate() {
		var gi = new GameInfo();
		var p1 = gi.addPlayer(new Player("asdf1", 2, 3, 4));
		var p2 = gi.addPlayer(new Player("asdf2", 0, 3, 4));
		p1.id = 0;
		p2.id = 2;
		gi.updatePlayersIndices();
		Assertions.assertEquals(2, gi.getPlayers().length);
		Assertions.assertEquals(0, gi.getPlayer((byte) 0).id);
		Assertions.assertEquals(2, gi.getPlayer((byte) 2).id);
	}

	@Test
	void kickVsLeave() {
		// not started and leave
		var gi = new GameInfo();
		var p = gi.join(new Player("Alice", Player.DEFAULT_ID, Player.HOST, 0), GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0,
				0);
		gi.player = null;
		gi.leave(p, false, false);
		Assertions.assertEquals(0, gi.getPlayersIncludingLostOnes().length);

		// started and leave
		gi = new GameInfo();
		p = gi.join(new Player("Alice", Player.DEFAULT_ID, Player.HOST, 0), GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);
		gi.player = null;
		gi.getGamemode().setRacing(true);
		gi.leave(p, false, false);
		Assertions.assertEquals(1, gi.getPlayersIncludingLostOnes().length);

		// started and kick
		gi = new GameInfo();
		p = gi.join(new Player("Alice", Player.DEFAULT_ID, Player.HOST, 0), GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);
		gi.player = null;
		gi.getGamemode().setRacing(true);
		gi.leave(p, false, true);
		Assertions.assertEquals(0, gi.getPlayersIncludingLostOnes().length);
	}

	@Test
	void leaveBeforeJoin() {
		// nullptr ved leave
		var gi = new GameInfo();
		gi.leave(new Player(), false, true);
		gi.leave(null, false, true);
	}

	@Test
	void rejoinKeepDataRightOrWrongId() {
		// rejoina og fikk to ekstra duplikater på klientsiden

		var gis = new ArrayList<GameInfo>();
		var alice = new GameInfo(new GameRemoteMaster(null), GameType.CREATING_LAN);
		alice.join(new Player("Alice", Player.DEFAULT_ID, Player.HOST, 12345), GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0,
				0);
		gis.add(alice);

		var joiner = new GameInfo(new GameRemoteMaster(null), GameType.JOINING_LAN);
		var joined = new int[1];
		joiner.join(new Player("Bob", Player.DEFAULT_ID, Player.PLAYER, 12345), GameInfo.JOIN_TYPE_VIA_CLIENT,
				player -> joined[0]++, 0, 0);
		gis.add(joiner);

		while (true) {
			if (joined[0] != 1) {
				snork();
				continue;
			}
			var found = false;
			for (var gi : gis) {
				if (gi.getPlayers().length != 2) {
					snork();
					found = true;
					break;
				}
			}
			if (!found)
				break;
		}
		TestTranslator.testEqualAmountOfPlayers(gis);

		for (var gi : gis)
			for (var p : gi.getPlayers())
				Assertions.assertTrue(p.joined);

		alice.startRace(0);
		snork();

		var leavingGi = gis.get(1);
		var p = leavingGi.player;
		p = new Player(p.name, Player.DEFAULT_ID, p.role, p.steamID);
		leavingGi.imLeaving();
		snork();
		gis.remove(joiner);
		TestTranslator.testEqualAmountOfPlayers(gis);

		joiner = new GameInfo(new GameRemoteMaster(null), GameType.JOINING_LAN);
		joined[0] = 0;
		joiner.join(p, GameInfo.JOIN_TYPE_VIA_CLIENT, player -> joined[0]++, 0, 0);
		while (joined[0] != 1)	
			snork();
		gis.add(joiner);
		TestTranslator.testEqualAmountOfPlayers(gis);

		joiner.close();
		alice.close();
	}

	@Test
	void Start3Players() {
		var gis = new ArrayList<GameInfo>();
		var alice = new GameInfo(new GameRemoteMaster(null), GameType.CREATING_LAN);
		alice.join(new Player("Alice", Player.DEFAULT_ID, Player.HOST, 0), GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);
		gis.add(alice);

		var expectedSize = 3;
		var joined = new AtomicInteger(1);
		for (int i = 1; i < expectedSize; i++) {
			var joiner = new GameInfo(new GameRemoteMaster(null), GameType.JOINING_LAN);
			joiner.join(new Player("P" + i, Player.DEFAULT_ID, Player.PLAYER, i), GameInfo.JOIN_TYPE_VIA_CLIENT,
					player -> joined.incrementAndGet(), 0, 0);
			gis.add(joiner);
		}
		snork();
		Assertions.assertFalse(joined.get() > expectedSize);
		while (joined.get() != expectedSize)
			snork();

		for (int i = 1; i < expectedSize; i++) {
			gis.get(i).carSelectUpdate(gis.get(i).player, 2, true, true, false);
		}
		
		alice.startRace(0);
		snork();

		for (var gi : gis)
			Assertions.assertTrue(gi.getGamemode().isRacing(), gi.player.name);

		for (var gi : gis)
			gi.close();
	}
	
	@Test
	void buffer() {
		var messageAsBytes = new byte[] { 12, 0, 4, 56 }; 
		var buffer = ByteBuffer.allocateDirect(messageAsBytes.length);
		buffer.clear();
		buffer.put(messageAsBytes);
		buffer.flip();
		var remaining = buffer.remaining();
		buffer.get();
		buffer.position(0);
		buffer.mark();
		Assertions.assertEquals(remaining, buffer.remaining());
	}
	
	@Test
	void limitBug() {
		var regval = new RegVals();
		regval.values()[0] = new RegVal(0.3, RegValType.NormalPercent);
		regval.values()[1] = new RegVal(1.8, RegValType.NormalPercent);
		regval.limit(0, .5);
		regval.limit(1, .5);
		Assertions.assertEquals(0.5, regval.values()[0].value);
		Assertions.assertEquals(1.5, regval.values()[1].value);
		regval.values()[0] = new RegVal(-0.3, RegValType.NormalPercent);
		regval.limit(0, .5);
		Assertions.assertEquals(0.5, regval.values()[0].value);
	}


	@Test
	void negNNBug() {
		var regval = new RegVals();
		regval.values()[0] = new RegVal(0.3, RegValType.NormalPercent);
		var regval2 = new RegVals();
		regval2.values()[0] = new RegVal(-1.01, RegValType.NormalPercent);
		regval.combine(regval2.values());
		System.out.println(regval.values()[0].value);
	}
	
	@Test
	void sendTestDesynced() {
		var alice = new GameInfo(new GameRemoteMaster(null), GameType.CREATING_LAN);
		var res = alice.join(new Player("Alice", Player.DEFAULT_ID, Player.HOST, 0), GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);
		Assertions.assertNotNull(res);
		
		var joined = new AtomicInteger(0);
		var bob = new GameInfo(new GameRemoteMaster(null), GameType.JOINING_LAN);
		res = bob.join(new Player("Bob", Player.DEFAULT_ID, Player.PLAYER, 1), GameInfo.JOIN_TYPE_VIA_CLIENT,
				player -> joined.incrementAndGet(), 0, 0);
		while (joined.get() != 1)
			snork();
		Assertions.assertNotNull(res);
		
		Assertions.assertEquals(alice.getGamemode().getRound(), bob.getGamemode().getRound());
		alice.getGamemode().prepareNextRaceManually(0);
		Assertions.assertNotEquals(alice.getGamemode().getRound(), bob.getGamemode().getRound());
		
		bob.goInTheRace(bob.player);
		bob.getGamemode().setRacing(true);

		for (int i = 0; i < 5; i++) {
			snork();
		}

		var a0 = Lobby.decideEnableReadyBtn(alice);
//		var a1 = UpgradesSubscene.updateReadyBtnEnabled(alice, alice.player);
		var b0 = Lobby.decideEnableReadyBtn(bob);
//		var b1 = UpgradesSubscene.updateReadyBtnEnabled(bob, bob.player);
		
		Assertions.assertNotEquals(a0, b0);
//		Assertions.assertEquals(a1, b1);
	}
	
	@Test
	void sendRace() {
		var alice = new GameInfo(new GameRemoteMaster(null), GameType.CREATING_LAN);
		var res = alice.join(new Player("Alice", Player.DEFAULT_ID, Player.HOST, 0), GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);
		Assertions.assertNotNull(res);
		
		var joined = new AtomicInteger(0);
		var bob = new GameInfo(new GameRemoteMaster(null), GameType.JOINING_LAN);
		var gis = new ArrayList<GameInfo>();
		gis.add(alice);
		gis.add(bob);
		res = bob.join(new Player("Bob", Player.DEFAULT_ID, Player.PLAYER, 1), GameInfo.JOIN_TYPE_VIA_CLIENT,
				player -> joined.incrementAndGet(), 0, 0);
		while (joined.get() != 1)
			snork();
		Assertions.assertNotNull(res);
		
		Assertions.assertEquals(alice.getGamemode().getRound(), bob.getGamemode().getRound());
		alice.getGamemode().prepareNextRaceManually(0);
		Assertions.assertNotEquals(alice.getGamemode().getRound(), bob.getGamemode().getRound());
		
		alice.startRace(0);

		for (int i = 0; i < 5; i++) {
			snork();
		}

		var a0 = Lobby.decideEnableReadyBtn(alice);
//		var a1 = UpgradesSubscene.updateReadyBtnEnabled(alice, alice.player);
		var b0 = Lobby.decideEnableReadyBtn(bob);
//		var b1 = UpgradesSubscene.updateReadyBtnEnabled(bob, bob.player);

		Assertions.assertEquals(a0, b0);
//		Assertions.assertEquals(a1, b1);
		Assertions.assertTrue(alice.getGamemode().isRacing());
		Assertions.assertTrue(bob.getGamemode().isRacing());
		
		var started = bob.startRace(System.currentTimeMillis());
		Assertions.assertTrue(started);
		
		alice.finishRace(alice.player, 12, 100);
		bob.finishRace(bob.player, 123, 110);
		for (int i = 0; i < 5; i++) {
			snork();
		}
		Assertions.assertFalse(alice.getGamemode().isRacing());
		Assertions.assertFalse(bob.getGamemode().isRacing());
		TestTranslator.testEqualAmountOfPlayers(gis);

	}

	@Test
	void createLobbyAndSendBeep() {
		var alice = new Remote2();
		var bob = new Remote2();

		alice.info.gameInfo.join(new Player("Alice", Player.DEFAULT_ID, Player.HOST, Features.generateLanId(true)),
				GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);
		Assertions.assertEquals(1, alice.info.gameInfo.getPlayers().length);

		bob.info.gameInfo.join(new Player("Bob", Player.DEFAULT_ID, Player.PLAYER, Features.generateLanId(false)),
				GameInfo.JOIN_TYPE_VIA_CLIENT, null, 0, 0);
		Assertions.assertEquals(1, bob.info.gameInfo.getPlayers().length);

		var joinMsg = Message.msgJoin(0, bob.info.gameInfo.player);
		bob.push(joinMsg);
		bob.sendDirectly(alice);
		alice.collect();
		alice.sendDirectly(bob);
		bob.collect();
		bob.sendDirectly(alice);
		alice.collect();

		Assertions.assertEquals(2, alice.info.gameInfo.getPlayers().length);
		Assertions.assertEquals(2, bob.info.gameInfo.getPlayers().length);

		var beepMsg = Message.msgBeep(alice.info.gameInfo.player);
		alice.push(beepMsg);
		Assertions.assertEquals(1, alice.outQueue.size());
		alice.sendDirectly(bob);
		Assertions.assertEquals(1, bob.inQueue.size());
		Assertions.assertEquals(beepMsg, bob.inQueue.peek());
		bob.collect();
		Assertions.assertEquals(0, bob.inQueue.size());

		alice.close();
		bob.close();
	}
}


























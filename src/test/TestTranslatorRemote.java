package test;

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import communication.GameInfo;
import communication.GameType;
import communication.Response;
import communication.Translator;
import communication.remote.Message;
import main.Features;
import player_local.Player;
import scenes.game.GameRemoteMaster;

public class TestTranslatorRemote {

//	@Test
//	void testDirectJoining() {
//		var game = new GameRemoteMaster(null);
//        var alice = new Player("Alice", Player.DEFAULT_ID, Player.HOST, Features.generateLanId(true));
//		var info0 = new GameInfo(game, GameType.DIRECT);
//		DirectRemote r0 = (DirectRemote) info0.getRemote();
//		info0.join(alice, GameInfo.JOIN_TYPE_VIA_CREATOR, null, 0, 0);
//
//		var bob = new Player("Bob", Player.DEFAULT_ID, Player.PLAYER, -1);
//		var info1 = new GameInfo(game, GameType.DIRECT);
//		DirectRemote r1 = (DirectRemote) info1.getRemote();
//		r0.others.add(info1);
//		r1.others.add(info0);
//		info1.join(bob, GameInfo.JOIN_TYPE_VIA_CLIENT, null, 0, 0);
//
//
//		var gameInfos = new ArrayList<GameInfo>();
//		gameInfos.add(info0);
//		gameInfos.add(info1);
//
//		TestTranslator.await(1);
//
//		TestTranslator.testEqualAmountOfPlayers(gameInfos);
//	}
//
//	void testRequest(Translator translator, String initial) {
//		try {
//			var res = translator.understandRequest(new Message(0, Translator.mailRequest + initial
////					+ Translator.split + "test"
//					, null));
//			Assertions.assertNotEquals(Response.ENDALL, res);
//		} catch (Exception e) {
//		}
//	}
//
//	void testResponse(Translator translator, String initial) {
//		try {
//			translator.understandResponse(
//					new Message(0, Translator.resAll + initial
//							+ Translator.split + "test"
//							, null));
//			Assertions.fail();
//		} catch (Exception e) {
//
//		}
//	}
//
//	@Test
//	void testTranslator() {
//		Translator translator = new Translator(null, null);
//		testRequest(translator, Translator.join);
//		testResponse(translator, Translator.join);
//		testResponse(translator, Translator.newEndGoal);
//	}
}

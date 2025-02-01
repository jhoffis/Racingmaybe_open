package test;

import communication.GameInfo;
import communication.GameType;
import engine.ai.AI;
import game_modes.GameModes;
import main.Features;
import main.Main;
import main.Texts;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import player_local.Player;
import player_local.TilePiece;
import player_local.upgrades.Upgrade;
import scenes.game.GameRemoteMaster;
import scenes.game.lobby_subscenes.UpgradesSubscene;

import java.util.ArrayList;
import java.util.Random;

public class TestDirect {

//    class PlayerCom {
//        final Player player;
//        final GameInfo info;
//
//        public PlayerCom(int i, GameRemoteMaster game, boolean creating, ArrayList<GameInfo> others) {
//            player = new Player("Player" + i, Player.DEFAULT_ID, creating ? Player.HOST : Player.PLAYER, Features.generateLanId(false));
//            info = new GameInfo(game, GameType.DIRECT);
//            ((DirectRemote) info.getRemote()).others.addAll(others);
//            others.add(info);
//            if (creating)
//                info.setGameID(12314124);
//            info.join(player, creating ? GameInfo.JOIN_TYPE_VIA_CREATOR : GameInfo.JOIN_TYPE_VIA_CLIENT, (player) -> {}, 0, 0);
//        }
//    }
//
//    /**
//     * Test joining, bytte gamemode, sjekke om det er likt,
//     * kjør kappløp og kjøp tilfeldige ting og fullfør en hel kamp randomly med veldig veldig mange folk.
//     */
//    @Test
//    void lan() {
//        Main.DEBUG = false;
//        var ran = new Random();
//        var totalGameInfos = new ArrayList<GameInfo>();
//        var game = new GameRemoteMaster(null);
//
//        var players = new TestDirect.PlayerCom[10];
//        for (int i = 0; i < players.length; i++) {
//            players[i] = new TestDirect.PlayerCom(i, game, i == 0, totalGameInfos);
//        }
//        var alice = players[0].info;
//
//        TestTranslator.await(1);
//        TestTranslator.testEqualAmountOfPlayers(totalGameInfos);
//
//
//        alice.init(1);
//        TestTranslator.await(1);
//        TestTranslator.testEqualAmountOfPlayers(totalGameInfos);
//        alice.init(GameModes.LONG_LEADOUT, 0, null, null); // Kanskje feil ved leadout
//        alice.getRemote().sendGamemodeChange(alice.getGamemode(), alice.gmTime);
//        TestTranslator.await(1);
//        TestTranslator.testEqualAmountOfPlayers(totalGameInfos);
//
//
//        System.out.println("""
//
//                ==============================
//                Change of cars
//                ==============================
//
//                """);
//        for (var other : players) {
//            other.info.carSelectUpdate(other.player, ran.nextInt(Texts.CAR_TYPES.length), true, false, false);
//        }
//        TestTranslator.await(1);
//        TestTranslator.testEqualAmountOfPlayers(totalGameInfos);
//
//
//
//        while (!alice.isGameOver()) {
//
//            for (var other : players) {
//                TilePiece<?> upgrade;
//                while ((upgrade = UpgradesSubscene.canAffordSomething(other.player)) != null) {
//                    if (upgrade.upgrade().isPlaced()) {
//                        if (upgrade.upgrade() instanceof Upgrade up) {
//                            other.info.attemptImproveTile(other.player, up, upgrade.x(), upgrade.y());
//                        }
//                    } else {
//                        other.info.attemptBuyTile(other.player, upgrade);
//                    }
//                }
//            }
//            TestTranslator.await(1);
//            TestTranslator.testEqualAmountOfPlayers(totalGameInfos);
//
//            System.out.println("""
//
//                    ==============================
//                    Ready up
//                    ==============================
//
//                    """);
//            for (var other : players) {
//                other.info.ready(other.player, (byte) 1);
//            }
//            TestTranslator.await(1);
//            TestTranslator.testEqualAmountOfPlayers(totalGameInfos);
//
//            System.out.println("""
//
//                    ==============================
//                    Start race
//                    ==============================
//
//                    """);
//            var now = System.currentTimeMillis();
//            for (var other : players) {
//            	if (!other.info.isEveryoneReady())
//            		Assertions.fail();
//                other.info.startRace(now);
//            }
//            TestTranslator.await(1);
//            TestTranslator.testEqualAmountOfPlayers(totalGameInfos);
//            for (var other : players) {
//                Assertions.assertTrue(other.info.isGameStarted());
//            }
//
//            System.out.println("""
//
//                    ==============================
//                    Finish race
//                    ==============================
//
//                    """);
//            var baseTrackLength = alice.getTrackLength();
//
//            for (var other : players) {
//                final var tl = other.info.getTrackLength();
//                Assertions.assertEquals(baseTrackLength, tl);
//                System.out.printf("""
//
//                        Starting finish %s
//
//                        """, other.player.name);
//                other.info.finishRace(other.player, AI.calculateRace(other.player.car, tl) + ran.nextInt(3000), 100);
//                TestTranslator.await(1);
//                System.out.print("""
//
//                        Ending finish
//
//                        """);
//            }
//            TestTranslator.await(1);
//            TestTranslator.testEqualAmountOfPlayers(totalGameInfos);
//        }
//
//        System.out.println("heihei");
//    }
}

















package test;

import communication.GameInfo;
import communication.Translator;
import game_modes.GameModes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import player_local.Player;
import scenes.regular.ReplayVisual;

import java.util.List;

public class TestReplay {

    @Test
    void TestReplay() {
        var gm = GameInfo.createGameMode(GameModes.TOTAL, 0);

        var players = new Player[]{
                new Player(),
                new Player()
        };

        players[0].addHistoryClean(Translator.getCloneString(players[0]));
        players[0].addHistoryClean(Translator.getCloneString(players[0]));
        players[1].addHistoryClean(Translator.getCloneString(players[1]));

        var replay = new ReplayVisual();
        var content = ReplayVisual.createContent(gm, players);

        var loaded = replay.loadReplay(List.of(content.split("\n")));
        Assertions.assertNotNull(loaded);
        Assertions.assertNotNull(loaded.gm());
        Assertions.assertNotNull(loaded.players());
        Assertions.assertFalse(loaded.players().isEmpty());
        for (int i = 0; i < players.length; i++) {
            var ogPlayer = players[i];
            var clonedPlayer = loaded.players().get(i);

            var expectedHistory = ogPlayer.getHistory();
            var actualHistory = clonedPlayer.getHistory();
            Assertions.assertEquals(expectedHistory.size(), actualHistory.size());
            Assertions.assertEquals(expectedHistory.size(), actualHistory.size());
            for (var h = 0; h < expectedHistory.size(); h++) {
                Assertions.assertEquals(expectedHistory.get(h), actualHistory.get(h));
            }
        }

//        Assertions.assertEquals(Translator.getCloneString(gm), Translator.getCloneString(loaded.gm()));
    }

    @Test
    void TestReplayFiles() {
        var gm = GameInfo.createGameMode(GameModes.TOTAL, 0);

        var players = new Player[]{
                new Player(),
                new Player()
        };

        players[0].addHistoryClean(Translator.getCloneString(players[0]));
        players[0].addHistoryClean(Translator.getCloneString(players[0]));
        players[1].addHistoryClean(Translator.getCloneString(players[1]));

        var replay = new ReplayVisual();
        var file = ReplayVisual.saveReplay(gm, players);

        var loaded = replay.loadReplay(file);
    }
}

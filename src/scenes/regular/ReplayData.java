package scenes.regular;

import game_modes.GameMode;
import player_local.Player;

import java.util.List;

public record ReplayData(
        String update,
        GameMode gm,
        List<Player> players
) {

}

package comNew;

import communication.GameInfo;
import player_local.Player;
import java.util.function.Consumer;

public final class RemoteInfo {
    public final Remote2 remote;
    public final GameInfo gameInfo;
    public Consumer<Player> afterJoined;

    public RemoteInfo(
            Remote2 remote,
            GameInfo gameInfo,
            Consumer<Player> afterJoined
    ) {
        this.remote = remote;
        this.gameInfo = gameInfo;
        this.afterJoined = afterJoined;
    }

    public RemoteInfo(Remote2 alice, GameInfo gameInfo) {
        this(alice, gameInfo, null);
    }

}
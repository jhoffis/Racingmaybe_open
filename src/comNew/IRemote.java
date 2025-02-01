package comNew;

import communication.ResponseState;
import communication.remote.Message;
import player_local.Player;

public interface IRemote {
    ResponseState sendMessage(Message message);

    Message popMessage(int size, Remote2 remote);

    int getMessageAvailable();

    boolean isSteam();

    boolean isOpen();

    void leave(Player player);

    void destroy();
}

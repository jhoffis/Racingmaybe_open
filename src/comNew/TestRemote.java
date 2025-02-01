package comNew;

import communication.ResponseState;
import communication.remote.Message;
import player_local.Player;

import java.util.HashMap;

import static communication.remote.Message.REQUESTER_ID_ALL;

public class TestRemote implements IRemote {

    private HashMap<Long, Remote2> connections = new HashMap<>();

    public void set(long receiver, Remote2 remote) {
        connections.put(receiver, remote);
    }

    @Override
    public ResponseState sendMessage(Message message) {
        if (message.requesterID == REQUESTER_ID_ALL) {
            for (var remote : connections.values()) {
                remote.receive(message);
            }
            return ResponseState.ALL_GOOD;
        }
        for (var remoteEntry : connections.entrySet()) {
            if (remoteEntry.getKey() == message.requesterID) {
                remoteEntry.getValue().receive(message);
            }
        }
        return ResponseState.ALL_GOOD;
    }

    @Override
    public Message popMessage(int size, Remote2 remote) {
        return null;
    }

    @Override
    public int getMessageAvailable() {
        return 0;
    }

    @Override
    public boolean isSteam() {
        return false;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void leave(Player player) {

    }

    @Override
    public void destroy() {

    }
}

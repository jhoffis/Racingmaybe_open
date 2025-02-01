package comNew;

import communication.GameInfo;
import communication.Response;
import communication.ResponseState;
import communication.Translator;
import communication.remote.Message;
import player_local.Player;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/*
Hva kan være at kan forandre seg?
Player
Ready
Car
Race
GameMode

 */
public class Remote2 {

    public IRemote way;
    public final Queue<Message> inQueue = new ConcurrentLinkedDeque<>();
    public final Queue<Message> outQueue = new ConcurrentLinkedDeque<>();
    public boolean running;
    public RemoteInfo info;
    public int collected;

    /**
     * For testing purposes
     */
    public Remote2() {
        info = new RemoteInfo(this, new GameInfo());
        way = new TestRemote();
        running = true;
    }

    public Remote2(GameInfo info, IRemote remote) {
        this.info = new RemoteInfo(this, info);
        this.way = remote;
    }

    public void run() {
        running = true;
        Thread messageCollector = new Thread(this::collectLoop);
        messageCollector.start();
        Thread messageSender = new Thread(this::sendLoop);
        messageSender.start();
    }

	public void close() {
		running = false;
	}

    public void receive(Message message) {
        System.out.println("push in " + message.toString());
        inQueue.offer(message);
    }

    public void push(Message message) {
        var player = info.gameInfo.player;
        if (!running || (player != null && player.isHost() && info.gameInfo.getPlayers().length <= 1))
            return;
        System.out.println("push out " + message.toString());
        outQueue.offer(message);
    }

    /**
     * Handles next message in the inQueue
     */
    public void collect() {
    	var message = inQueue.poll();
        if (message == null || message.requestMessage.isEmpty())
            return;
//        System.out.println("collect " + message.toString());

        if (message.requestMessage.equals(Response.ENDALL)) {
            info.gameInfo.close();
            return;
        }

        switch (Translator.whichMessageType(message.requestMessage)) {
            case request -> {
                message.requestMessage = Translator.understandRequest(message, info);
                push(message);
            }
            case response -> {
                var state = Translator.understandResponse(message, info);
                if (state == ResponseState.KEEP_BACK) {
                    inQueue.offer(message);
                } else if (state == ResponseState.FAIL) {
                    System.err.println("failed a resp: " + message.requestMessage);
                }
            }
            default ->
                    throw new IllegalStateException("Unexpected value: " + Translator.whichMessageType(message.requestMessage));
        }
        collected++;
    }

    public void collectLoop() {
        while (running) {
            collect();
            int msgSize;
            while ((msgSize = way.getMessageAvailable()) != 0) {
                var msg = way.popMessage(msgSize, this);
                if (msg == null)
                    break;
                System.out.println("msg: " + msg.toString());
                inQueue.offer(msg);
            }
        }
        way.destroy();
    }

    /**
     * Test Handles next message in the outQueue
     */
    public void sendDirectly(Remote2 other) {
        var message = outQueue.poll();
        if (message == null)
            return;
        other.receive(message);
    }

    /**
     * Handles next message in the outQueue
     */
    public void send() {
        var message = outQueue.poll();
        if (message == null)
            return;
//        System.out.println("send " + message.toString());
        var state = way.sendMessage(message);
        if (state == ResponseState.KEEP_BACK) {
            outQueue.offer(message);
        }
    }

    public void sendLoop() {
        while (running || !outQueue.isEmpty()) {
            send();
        }
        way.destroy();
    }

    public void clearLoopsOffPlayer(Player player, int gameID) {
        Message[] arr = outQueue.toArray(new Message[0]);
        for (int i = 0; i < outQueue.size(); i++) {
            var message = arr[i];
            if (Objects.requireNonNull(Translator.whichMessageType(message.requestMessage)) == MessageType.response) {
                String[] input = message.requestMessage.split(Translator.split);
                int othersGameID = Integer.parseInt(input[2]);
                if ((gameID != -1) &&
                        (othersGameID == gameID) &&
                        Byte.parseByte(input[1]) == player.id) {
                    outQueue.remove(message);
                }
            }
        }
        arr = inQueue.toArray(new Message[0]);
        for (int i = 0; i < inQueue.size(); i++) {
            var message = arr[i];
            if (Objects.requireNonNull(Translator.whichMessageType(message.requestMessage)) == MessageType.response) {
                String[] input = message.requestMessage.split(Translator.split);
                int othersGameID = Integer.parseInt(input[2]);
                if ((gameID != -1) &&
                        (othersGameID == gameID) &&
                        Byte.parseByte(input[1]) == player.id) {
                    inQueue.remove(message);
                }
            }
        }
    }
}

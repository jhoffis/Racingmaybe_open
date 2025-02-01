package comNew;

import com.codedisaster.steamworks.SteamException;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamNetworking;
import com.codedisaster.steamworks.SteamNetworking.P2PSend;
import com.codedisaster.steamworks.SteamNetworking.P2PSessionError;
import com.codedisaster.steamworks.SteamNetworkingCallback;
import communication.GameInfo;
import communication.ResponseState;
import communication.remote.Message;
import player_local.Player;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class SteamRemote2 implements IRemote, SteamNetworkingCallback {

    private SteamNetworking net;
    private final GameInfo info;

    public SteamRemote2(GameInfo info) {
        this.info = info;
        net = new SteamNetworking(this);
    }

    private ResponseState sendMessage(long requesterID, ByteBuffer buffer) {
    	if (net == null)
    		return ResponseState.FAIL;
        try {
            if (net.sendP2PPacket(SteamID.createFromNativeHandle(requesterID), buffer, P2PSend.Reliable, 0)) { // player.getChannel()
                System.out.println("SUCC_");
                return ResponseState.ALL_GOOD;
            } else {
                System.out.println("NOT_");
                return ResponseState.FAIL;
            }
        } catch (SteamException e) {
            System.out.println("Error with steam sending message: " + e.getMessage());
            return ResponseState.FAIL;
        }
    }

    @Override
    public ResponseState sendMessage(Message message) {
    	if (net == null)
    		return ResponseState.FAIL;
        var messageAsBytes = message.requestMessage.getBytes(StandardCharsets.UTF_8);
        var buffer = ByteBuffer.allocateDirect(messageAsBytes.length);

        buffer.clear();
        buffer.put(messageAsBytes);
        buffer.flip();

        if (message.requesterID == Message.REQUESTER_ID_ALL) {
            for (var p : info.getPlayers()) {
            	if (p.id == info.player.id)
            		continue;
            	buffer.position(0);
        		buffer.mark();
                if (sendMessage(p.steamID, buffer) != ResponseState.ALL_GOOD)
                    return ResponseState.FAIL;
            }
            return ResponseState.ALL_GOOD;
        }
        return sendMessage(message.requesterID, buffer);
    }

    @Override
    public int getMessageAvailable() {
    	if (net == null)
    		return 0;
        return net.isP2PPacketAvailable(0);
    }

    @Override
    public Message popMessage(int size, Remote2 remote) {
    	if (net == null)
    		return null;
    	
        // read the message
        var bb = ByteBuffer.allocateDirect(size);
        var requester = new SteamID();
        try {
            net.readP2PPacket(requester, bb, 0);
        } catch (SteamException e1) {
            e1.printStackTrace();
        }

        // convert the message
        String converted;
        byte[] bytes = new byte[bb.limit()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = bb.get(i);
        }
        converted = new String(bytes, StandardCharsets.UTF_8);
        System.out.println("Popped, converted msg by steamremote");
        bb.clear();
        return new Message(SteamID.getNativeHandle(requester), converted, null);
    }

    @Override
    public void onP2PSessionConnectFail(SteamID remoteID, P2PSessionError sessionError) {
        System.out.println("onP2PSessionConnectFail: " + sessionError + " with " + SteamID.getNativeHandle(remoteID));
        Player player = info.getPlayerSteamId(SteamID.getNativeHandle(remoteID));

        if (player != null) {
            info.leave(player, true, false);
        }
    }

    @Override
    public void onP2PSessionRequest(SteamID remoteID) {
    	if (net != null)
    		net.acceptP2PSessionWithUser(remoteID);
//        if (info.player.isHost()) {
//            info.addJoiner(SteamID.getNativeHandle(remoteID));
//        }
    }

    @Override
    public void destroy() {
    	if (net == null)
    		return;
    	var netDispose = net;
    	net = null;
    	netDispose.dispose();
    }

    @Override
    public boolean isSteam() {
        return true;
    }

    @Override
    public void leave(Player player) {
    }

    @Override
    public boolean isOpen() {
        return true;
    }

}

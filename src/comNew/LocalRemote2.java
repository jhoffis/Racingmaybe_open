package comNew;

import communication.GameType;
import communication.ResponseState;
import communication.Translator;
import communication.remote.Message;
import player_local.Player;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.concurrent.CopyOnWriteArrayList;

public class LocalRemote2 implements IRemote {

    // CLIENT
    public final CopyOnWriteArrayList<ConnectedLine> lines = new CopyOnWriteArrayList<>();
    private final UDPServer udpServer;
    private int handleMessageIndex;
    private final GameType multiplayerType;

    public static String getLocalIP() {
        try {
            // Enumerate all network interfaces
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // Skip loopback and inactive interfaces
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                // Enumerate all IP addresses associated with the interface
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();

                    // Check if the address is not a loopback and is an IPv4 address
                    if (!inetAddress.isLoopbackAddress() && inetAddress.getHostAddress().indexOf(':') == -1) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Error retrieving network interfaces: " + e.getMessage());
        }
        return null;
    }

    public LocalRemote2(String ip, GameType multiplayerType) throws IOException {
        this.multiplayerType = multiplayerType;
        lines.add(new ConnectedLine(ip, multiplayerType, this, null));
        if (multiplayerType.isCreating()) {
            udpServer = new UDPServer();
        } else {
            udpServer = null;
        }
    }

    public void clearMessagesTest() {
        for (var line : this.lines)
            line.messagesToHandle.clear();
    }

    @Override
    public boolean isOpen() {
        for (var line : lines) {
            if (!line.running && line.messagesToHandle.isEmpty()) {
                line.close(false);
                lines.remove(line);
            }
        }
        return !lines.isEmpty();
    }

    @Override
    public ResponseState sendMessage(Message message) {
        if (lines.isEmpty())
            return ResponseState.ALL_GOOD;

        int amountSent = 0;
        for (var line : this.lines) {
            if (!line.connected)
                continue;
            if (message.requesterID != Message.REQUESTER_ID_ALL
                    && line.senderID != -1
                    && line.senderID != message.requesterID)
                continue;

            int timeout = 0;
            while (line.outTo == null && line.running) {
                try {
                    Thread.sleep(1); // needed to avoid getting stuck
                    timeout++;
                    if (timeout == 10000) {
                        break;
                    }
                } catch (InterruptedException e) {
                    System.out.println("Error waiting for LAN: " + e.getMessage());
                    return ResponseState.KEEP_BACK;
                }
            }
            if (timeout == 10000)
                continue;

            try {
                line.outTo.write(message.requestMessage.concat("\n").getBytes(StandardCharsets.UTF_8));
                line.outTo.flush();
                System.out.println(line.multiplayerType + " Out to (" + message.requesterID + "): " + message);
                amountSent++;
                if (message.requesterID != Message.REQUESTER_ID_ALL)
                    break;
            } catch (NullPointerException | IOException e) {
                System.out.println("line.outTo err: " + e.getMessage());
                return ResponseState.FAIL;
            }
        }

        //if (amountSent == 0 && !lines.isEmpty())
        //    return ResponseState.KEEP_BACK;
        return ResponseState.ALL_GOOD;
    }

    @Override
    public int getMessageAvailable() {
        for (var line : this.lines) {
            if (!line.messagesToHandle.isEmpty())
                return 1;
        }
        return 0;
    }

    @Override
    public Message popMessage(int ignored, Remote2 remote) {
        int n = 0;
        for (int i = handleMessageIndex; n < lines.size(); i = (i + 1) % lines.size()) {
            ConnectedLine line;
            try {
                line = lines.get(i);
            } catch (IndexOutOfBoundsException ex) {
                n++;
                continue;
            }

            if (line.messagesToHandle.isEmpty()) {
                n++;
                continue;
            }

            handleMessageIndex = (handleMessageIndex + 1) % lines.size();
            var msg = line.messagesToHandle.poll();
            if (multiplayerType.equals(GameType.CREATING_LAN)) {
                if (Translator.isForAll(msg.requestMessage)) {
                    for (var otherLine : lines) {
                        if (otherLine.equals(line))
                            continue;
                        remote.push(new Message(otherLine.senderID, msg.requestMessage, otherLine));
                    }
                }
            }
            return msg;
        }
        handleMessageIndex = (handleMessageIndex + 1) % lines.size();
        return null;
    }

    @Override
    public boolean isSteam() {
        return false;
    }

    @Override
    public void leave(Player player) {
        for (int i = 0; i < lines.size(); i++) {
            ConnectedLine line;
            try {
                line = lines.get(i);
            } catch (ArrayIndexOutOfBoundsException ignored) {
                continue;
            }

            if (line.senderID == player.steamID) {
                line.running = false;
                line.close(false);
                try {
                    lines.remove(i);
                } catch (IndexOutOfBoundsException ignored) {
                }
                break;
            }
        }
    }

    @Override
    public void destroy() {
        System.out.println("Destroying connections");
        for (var line : this.lines) {
            line.running = false;
            line.close(true);
        }

        lines.clear();
        closeUDP();
    }

    public void openUDP() {
        if (udpServer != null)
            udpServer.open();
    }

    public void closeUDP() {
        if (udpServer != null)
            udpServer.close();
    }

    public void setTitle(String text) {
        udpServer.title = text;
    }
}

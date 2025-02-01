package comNew;

import adt.IActionDouble;

import java.io.IOException;
import java.net.*;
import java.util.function.Consumer;

public class UDPClient {
    public static void sendEcho(IActionDouble<String, String> cb) {
        var baseIP = LocalRemote2.getLocalIP();
        if (baseIP == null) return;
        baseIP = baseIP.substring(0, baseIP.lastIndexOf('.')) + ".";
        for (int i = 1; i <= 254; i++) {
            var ip = baseIP + i;
            new Thread(() -> {
                var msg = "HELLO!!!";
                DatagramSocket socket = null;
                InetAddress address = null;
                try {
                    socket = new DatagramSocket();
                    address = InetAddress.getByName(ip);
                } catch (SocketException | UnknownHostException e) {
//                    System.out.println("could not find: " + ip);
                    return;
                }

                var buf = msg.getBytes();
                var packet = new DatagramPacket(buf, buf.length, address, ConnectedLine.SERVERPORT + 1);
                try {
                    socket.send(packet);
                    buf = new byte[256];
                    packet = new DatagramPacket(buf, buf.length);
                    socket.setSoTimeout(500);
                    socket.receive(packet);
                } catch (IOException e) {
//                    System.out.println("could not send to: " + ip);
                    return;
                }
                socket.close();

                var res = new String(packet.getData(), 0, packet.getLength());
                if (res.startsWith("sup")) {
                    cb.run(ip, res.substring(3));
                }
            }).start();
        }
    }
}

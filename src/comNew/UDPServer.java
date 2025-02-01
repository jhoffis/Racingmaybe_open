package comNew;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Stack;

public class UDPServer {

    private DatagramSocket socket;
    private boolean running;
    private final byte[] buf = new byte[256];
    public String title;

    public UDPServer() {
        open();
    }

    public void open() {
        if (running) return;

        try {
            socket = new DatagramSocket(ConnectedLine.SERVERPORT + 1);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        //                socket.setSoTimeout(1000);
        new Thread(() -> {
            running = true;

            while (running) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    //                socket.setSoTimeout(1000);
                    socket.receive(packet);
                } catch (IOException e) {
                    System.out.println("failed to receive udp packet: " + e.getMessage());
                    continue;
                }

                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                String received = new String(packet.getData(), 0, packet.getLength());

                if (received.equals("HELLO!!!")) {
                    var resp = "sup" + title;
                    packet = new DatagramPacket(resp.getBytes(), resp.length(), address, port);
                } else {
                    continue;
                }

                try {
                    socket.send(packet);
                } catch (IOException e) {
                    System.out.println("failed to send udp packet: " + e.getMessage());
                    continue;
                }
            }
            socket.close();
        }).start();
    }

    public void close() {
        running = false;
        socket.close();
    }

}

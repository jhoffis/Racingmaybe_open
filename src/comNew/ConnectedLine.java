package comNew;

import communication.GameType;
import communication.remote.Message;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
import java.util.Queue;

public class ConnectedLine implements Runnable {

    public static final int SERVERPORT = 20001;

    public final ServerSocket hostServerSocket;
    public final String ip;
    public final GameType multiplayerType;
    public final LocalRemote2 remoteRef;

    public long senderID = -1;

    public boolean running, connected;
    Queue<Message> messagesToHandle = new ArrayDeque<>();
    DataOutputStream outTo;
    BufferedReader inFrom;
    Socket socket;

    public ConnectedLine(String ip, GameType multiplayerType, LocalRemote2 remoteRef, ServerSocket serverSocket) throws IOException {
        running = true;
        this.ip = ip;
        this.multiplayerType = multiplayerType;
        this.remoteRef = remoteRef;
        if (multiplayerType.isCreating() && serverSocket == null) {
            hostServerSocket = new ServerSocket(SERVERPORT);
        } else {
            this.hostServerSocket = serverSocket;
        }

        if (!multiplayerType.isCreating()) {
            try {
                System.out.println("new client socket");
                socket = new Socket(ip, SERVERPORT);
            } catch (Exception e) {
                System.out.println("Close the socket1: " + e.getMessage());
                running = false;
                close(false);
                return;
            }
        }

        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        String message;
        try {
            if (multiplayerType.isCreating()) {
                if (hostServerSocket.isClosed())
                    return;
                System.out.println("new server socket");
                synchronized (hostServerSocket) {
                    do {
//							System.out.println("Looking...");
                        try {
                            if (!running) {
                                close(true);
                                break;
                            }
                            hostServerSocket.setSoTimeout(10000);
                            socket = hostServerSocket.accept();
                            break;
                        } catch (SocketTimeoutException timeout) {
                            // Do nothing
                        }
                    } while (!hostServerSocket.isClosed());
                }
                System.out.println("connection established at server socket");
                remoteRef.lines.add(new ConnectedLine(ip, multiplayerType, remoteRef, hostServerSocket));
                System.out.println(multiplayerType + " connects to socket");
            }
            outTo = new DataOutputStream(socket.getOutputStream());
            inFrom = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (Exception e) {
            System.out.println("Close the socket1: " + e.getMessage());
            close(multiplayerType.isCreating());
            return;
        }

        connected = true;
        try {
            while (running) {
//					System.out.println(mu	ltiplayerType + " reading when a message appears...");
                message = inFrom.readLine();
//                if (Main.DEBUG) {
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
                System.out.println(multiplayerType + " In from " + senderID + ": " + message);
                if (message != null) {
                    messagesToHandle.offer(new Message(senderID, message, this));
                } else {
                    System.out.println("Null loop with: " + senderID);
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Close the socket2: " + e.getMessage());
        }
        close(false);
    }

    public void close(boolean closeServer) {
        running = false;
        connected = false;
        try {
            if (closeServer && hostServerSocket != null) {
                hostServerSocket.close();
                System.out.println("Closing server-socket!");
            }
        } catch (Exception e) {
            System.out.println("Closing socket: " + e.getMessage());
        }
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (Exception e) {
            System.out.println("Closing socket: " + e.getMessage());
        }
        try {
            if (outTo != null) {
                outTo.close();
                outTo = null;
            }
        } catch (Exception e) {
            System.out.println("Closing socket: " + e.getMessage());
        }
        try {
            if (inFrom != null) {
                inFrom.close();
                inFrom = null;
            }
        } catch (Exception e) {
            System.out.println("Closing socket: " + e.getMessage());
        }
    }
}

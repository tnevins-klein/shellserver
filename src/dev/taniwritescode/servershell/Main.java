package dev.taniwritescode.servershell;

import dev.taniwritescode.servershell.net.Connection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class Main {
    static Logger logger = Logger.getLogger("Net");
    static ArrayList<Thread> activeThreads = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        int port = 25565;
        if (args.length != 0) {
            port = Integer.parseInt(args[0]);
        }

        logger.info("Listening on port " + port);

        ServerSocket socket = new ServerSocket(25565);
        socket.setSoTimeout(500);

        while (true) {
            try {
                Socket s = socket.accept();
                logger.info("Connected with " + s.getInetAddress() + ":" + s.getPort());

                Connection c = new Connection(s);
                c.start();
                activeThreads.add(c);

            } catch (SocketTimeoutException e) {
                // logger.warning("Connection timed out");
            }
        }
    }
}

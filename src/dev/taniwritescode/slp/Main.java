package dev.taniwritescode.slp;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws IOException {
        ServerListPing17 s = new ServerListPing17();
        s.setAddress(new InetSocketAddress("localhost", 25565));

        ServerListPing17.StatusResponse r = s.fetchData();
        System.out.println(r);
    }
}

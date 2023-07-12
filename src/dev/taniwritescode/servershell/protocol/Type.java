package dev.taniwritescode.servershell.protocol;

public class Type {
    public static class Packet {
        public static int HANDSHAKE = 0x00;
        public static int STATUS_REQUEST = 0x00;
        public static int STATUS_RESPONSE = 0x00;

    }
    public static class HandshakeState {
        public static int LOGIN = 0x01;
        public static int PLAY = 0x02;
    }
}

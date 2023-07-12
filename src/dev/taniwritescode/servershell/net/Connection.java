package dev.taniwritescode.servershell.net;

import dev.taniwritescode.servershell.protocol.OutboundPacket;
import dev.taniwritescode.servershell.protocol.Type;
import dev.taniwritescode.servershell.util.VarNum;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Logger;

public class Connection extends Thread {
    private static class InboundPacket {
        byte[] data;
        int packetID;

        public InboundPacket(byte[] data, int packetID) {
            this.data = data;
            this.packetID = packetID;
        }

        public int getPacketID() {
            return packetID;
        }

        public byte[] getData() {
            return data;
        }

        public String toString() {
            return "IB:" + packetID + "/" +  Arrays.toString(this.data);
        }
    }

    private final Socket socket;

    private final DataInputStream in;
    private final DataOutputStream out;

    private Logger logger = Logger.getLogger("Connection");

    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        this.socket.setSoTimeout(5000);

        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
    }

    public InboundPacket readPacket() throws IOException {
        int length = VarNum.readVarInt(in);
        int packet_id = VarNum.readVarInt(in);
        int data_length = length - VarNum.varIntLength(packet_id);
        return new InboundPacket(in.readNBytes(data_length), packet_id);
    }

    private String readString(DataInputStream read) throws IOException {
        int stringLength = VarNum.readVarInt(read);
        return new String(read.readNBytes(stringLength), StandardCharsets.UTF_8);
    }

    public int sendHandshake() throws IOException {
        InboundPacket handshake = readPacket();

        if (handshake.packetID != Type.Packet.HANDSHAKE) {
            logger.warning("Instead of HANDSHAKE, got packet with ID " + handshake.packetID + ". Aborting.");
            socket.close();
            return 0;
        }

        logger.info(handshake.toString());
        ByteArrayInputStream stream = new ByteArrayInputStream(handshake.getData());
        DataInputStream read = new DataInputStream(stream);

        int proto_version = VarNum.readVarInt(read);
        String hostname = readString(read);
        int port = read.readUnsignedShort();

        logger.info("Handshake for " + hostname + ":" + port + " successful. Protocol version is " + proto_version + ".");

        return VarNum.readVarInt(read);
    }

    public void run() {
        logger = Logger.getLogger(this.toString());

        try {
            int next_state = sendHandshake();
            if (next_state == Type.HandshakeState.PLAY) {
                logger.info("Client requested state PLAY. Aborting connection.");
                socket.close();
                return;
            }

            logger.info("Waiting for initial packet...");
            InboundPacket next_packet = readPacket();
            if (next_packet.getPacketID() == Type.Packet.STATUS_REQUEST) {
                OutboundPacket statusResponse = new OutboundPacket(Type.Packet.STATUS_RESPONSE, false);
                statusResponse.writeString("{\"version\": {\"name\": \"1.20.1\",\"protocol\": 763},\"players\": {\"max\": -2,\"online\": 1000000000,\"sample\": [{\"name\": \"NotTani\",\"id\": \"c9c45533-ae58-42c0-874b-d9235f67996b\"}]},\"description\": {\"text\": \"Hello from hell (Java)\"},\"favicon\": \"data:image/png;base64,<data>\",\"enforcesSecureChat\": true,\"previewsChat\": true}");
                socket.getOutputStream().write(statusResponse.getBytes());

                logger.info("Successfully answered a STATUS_REQUEST packet.");

                try {
                    next_packet = readPacket();
                    logger.info("Received another packet (hopefully PING)");
                } catch (EOFException | SocketTimeoutException e) {
                    logger.info("No further packets.");
                }
            }

            if (next_packet.getPacketID() == 0x01) {
                logger.info("Ping! Sending pong.");
                OutboundPacket pingResponsePacket = new OutboundPacket(0x01, false);
                pingResponsePacket.writeBytes(next_packet.getData());
                out.write(pingResponsePacket.getBytes());
            }

            OutboundPacket statusResponse = new OutboundPacket(Type.Packet.STATUS_RESPONSE, false);

            statusResponse.writeString("{\"version\": {\"name\": \"1.19.4\",\"protocol\": 762},\"players\": {\"max\": 100,\"online\": 5,\"sample\": [{\"name\": \"NotTani\",\"id\": \"c9c45533-ae58-42c0-874b-d9235f67996b\"}]},\"description\": {\"text\": \"Hello world\"},\"favicon\": \"data:image/png;base64,<data>\",\"enforcesSecureChat\": true,\"previewsChat\": true}");
            socket.getOutputStream().write(statusResponse.getBytes());

            logger.info("Successfully wrote response packet.");

            socket.close();
            logger.info("Connection closed.");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

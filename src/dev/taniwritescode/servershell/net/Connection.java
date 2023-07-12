package dev.taniwritescode.servershell.net;

import dev.taniwritescode.servershell.protocol.OutboundPacket;
import dev.taniwritescode.servershell.protocol.PacketType;
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
        logger.info("Thread opened");
    }

    public InboundPacket readPacket() throws IOException {
        int length = VarNum.readVarInt(in);
        logger.info("Started reading packet: length " + length);

        int packet_id = VarNum.readVarInt(in);
        logger.info("Packet ID: " + packet_id);

        int data_length = length - VarNum.varIntLength(packet_id) - 1;
        return new InboundPacket(in.readNBytes(data_length), packet_id);
    }

    private String readString(DataInputStream read) throws IOException {
        int stringLength = VarNum.readVarInt(read);
        return new String(read.readNBytes(stringLength), StandardCharsets.UTF_8);
    }

    public void run() {
        logger = Logger.getLogger(this.toString());

        try {
            InboundPacket handshake = readPacket();

            if (handshake.packetID != PacketType.HANDSHAKE) {
                logger.warning("Instead of HANDSHAKE, got packet with ID " + handshake.packetID + ". Aborting.");
                socket.close();
                return;
            }

            logger.info(handshake.toString());
            ByteArrayInputStream stream = new ByteArrayInputStream(handshake.getData());
            DataInputStream read = new DataInputStream(stream);

            int proto_version = VarNum.readVarInt(read);
            logger.info("Protocol version: " + proto_version);
            String hostname = readString(read);
            logger.info("Hostname: " + hostname);
            int port = read.readUnsignedShort();
            logger.info("Port: " + port);

            logger.info("New connection has protocol version " + proto_version);

            int next_state = VarNum.readVarInt(read);
            logger.info("Desired state is " + next_state);
            if (next_state != 1) {
                socket.close();
                return;
            }

            logger.info("Waiting for initial packet...");
            InboundPacket next_packet = readPacket();
            logger.info("Got packet! " + next_packet.toString());
            if (next_packet.getPacketID() == 0x00) {
                OutboundPacket statusResponse = new OutboundPacket(0x00, false);
                statusResponse.writeString("{\"version\": {\"name\": \"1.19.4\",\"protocol\": 762},\"players\": {\"max\": 100,\"online\": 5,\"sample\": [{\"name\": \"thinkofdeath\",\"id\": \"4566e69f-c907-48ee-8d71-d7ba5aa00d20\"}]},\"description\": {\"text\": \"Hello world\"},\"favicon\": \"data:image/png;base64,<data>\",\"enforcesSecureChat\": true,\"previewsChat\": true}");
                logger.info("Prepared response packet: " + Arrays.toString(statusResponse.getBytes()));

                socket.getOutputStream().write(statusResponse.getBytes());

                try {
                    next_packet = readPacket();
                    logger.info("Got another packet! " + next_packet);
                } catch (EOFException | SocketTimeoutException e) {
                    logger.info("No further packets.");
                }
            }

            if (next_packet.getPacketID() == 0x01) {
                OutboundPacket pingResponsePacket = new OutboundPacket(0x01, false);
                pingResponsePacket.writeBytes(next_packet.getData());
                out.write(pingResponsePacket.getBytes());
            }
            socket.close();
            logger.info("Connection closed! Success?");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

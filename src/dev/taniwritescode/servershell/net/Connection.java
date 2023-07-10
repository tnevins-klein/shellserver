package dev.taniwritescode.servershell.net;

import dev.taniwritescode.servershell.protocol.OutboundPacket;
import dev.taniwritescode.servershell.util.VarNum;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
    }

    private final Socket socket;
    private final Logger logger = Logger.getLogger("Net");

    private final DataInputStream in;
    private final DataOutputStream out;


    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        this.socket.setSoTimeout(1);

        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        logger.info("Thread opened");
    }

    public InboundPacket readPacket() throws IOException {
        int length = VarNum.readVarInt(in);
        int packet_id = VarNum.readVarInt(in);

        int data_length = length - VarNum.varIntLength(packet_id);
        return new InboundPacket(in.readNBytes(data_length), packet_id);
    }

    private String readString() throws IOException {
        int stringLength = VarNum.readVarInt(in);
        return new String(in.readNBytes(stringLength), StandardCharsets.UTF_8);
    }

    public void run() {
        logger.info("Hello!");

        try {
            InboundPacket handshake = readPacket();
            ByteArrayInputStream stream = new ByteArrayInputStream(handshake.getData());
            DataInputStream read = new DataInputStream(stream);

            int proto_version = VarNum.readVarInt(read);
            String hostname = readString();
            int port = in.readUnsignedShort();

            logger.info("New connection has protocol version " + proto_version);

            int next_state = VarNum.readVarInt(in);
            if (next_state != 1) {
                socket.close();
                return;
            }

            logger.info("Waiting for initial packet...");
            InboundPacket next_packet = readPacket();
            logger.info("Got packet! " + next_packet.getPacketID());
            if (next_packet.packetID == 0x00) {
                OutboundPacket statusResponse = new OutboundPacket(0x00, false);
                statusResponse.writeString("""
                        {
                            "version": {
                                "name": "1.19.4",
                                "protocol": 762
                            },
                            "players": {
                                "max": 100,
                                "online": 5,
                                "sample": [
                                    {
                                        "name": "thinkofdeath",
                                        "id": "4566e69f-c907-48ee-8d71-d7ba5aa00d20"
                                    }
                                ]
                            },
                            "description": {
                                "text": "Hello world"
                            },
                            "favicon": "data:image/png;base64,<data>",
                            "enforcesSecureChat": true,
                            "previewsChat": true
                        }""");
                out.write(statusResponse.getBytes());
                next_packet = readPacket();
            }

            if (next_packet != null && next_packet.packetID == 0x01) {
                OutboundPacket pingResponsePacket = new OutboundPacket(0x01, false);
                out.write(pingResponsePacket.getBytes());
            }
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

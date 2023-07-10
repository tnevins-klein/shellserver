package dev.taniwritescode.servershell.protocol;

import dev.taniwritescode.servershell.util.VarNum;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class OutboundPacket {
    public int packetID;
    public final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final DataOutputStream wrapper = new DataOutputStream(buffer);

    public boolean compressed;

    public OutboundPacket(int type, boolean compressed) {
        this.packetID = type;
        this.compressed = compressed;
    }

    public void writeVarInt(int val) throws IOException {
        VarNum.writeVarInt(wrapper, val);
    }

    public void writeString(String str) throws IOException {
        byte[] stringBytes = str.getBytes(StandardCharsets.UTF_8);
        writeVarInt(stringBytes.length);
        wrapper.write(stringBytes);
    }

    public byte[] getBytes() throws IOException {
        ByteArrayOutputStream packet_out = new ByteArrayOutputStream();
        byte[] raw = buffer.toByteArray();

        VarNum.writeVarInt(new DataOutputStream(packet_out),
                raw.length + VarNum.varIntLength(packetID));

        VarNum.writeVarInt(new DataOutputStream(packet_out), packetID);
        packet_out.write(raw);

        return packet_out.toByteArray();
    }

}
package de.pocketcloud.network.codec;

import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.network.exception.PacketException;
import de.pocketcloud.network.packet.data.PacketData;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Function;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class PacketSerializer {

    private PacketSerializer() {}

    public static byte[] encode(Packet packet, boolean encryptionEnabled, String authenticationKey) throws PacketException {
        try {
            PacketData buffer = new PacketData();
            packet.encode(buffer);
            buffer.write(authenticationKey);

            byte[] bytes = buffer.toByteArray();

            if (encryptionEnabled) {
                bytes = compress(bytes);
            }

            return bytes;
        } catch (Exception e) {
            throw new PacketException(e.getMessage());
        }
    }

    @Nullable
    public static Packet decode(byte[] buffer, boolean encryptionEnabled, String authenticationKey, Function<String, Packet> packetResolver) throws PacketException {
        try {
            if (buffer == null || buffer.length == 0) throw new PacketException("Cannot decode an empty buffer");
            byte[] decompressed = buffer;
            if (encryptionEnabled) {
                decompressed = decompress(buffer);
            }

            PacketData data = PacketData.fromBytes(decompressed);

            if (data.isEmpty()) throw new PacketException("Received buffer is empty");

            String packetName = data.readString();
            if (packetName == null) throw new PacketException("Received buffer does not contain a valid packet name");

            var packet = packetResolver.apply(packetName);
            if (packet == null) return null;

            PacketData decodeData = PacketData.fromBytes(decompressed);
            packet.decode(decodeData);

            if (decodeData.isEmpty()) throw new PacketException("Received packet does not contain an authentication key");

            String givenKey = decodeData.readString();
            if (givenKey == null) throw new PacketException("Received packet does not contain an authentication key");

            if (!givenKey.equals(authenticationKey)) throw new PacketException("Received packet does not contain a valid authentication key");

            return packet;
        } catch (PacketData.PacketDecodeException e) {
            throw new PacketException("Failed to decode packet data: " + e.getMessage(), e);
        } catch (DataFormatException e) {
            throw new PacketException("Failed to decompress data: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new PacketException("IO error during decoding: " + e.getMessage(), e);
        }
    }

    private static byte[] compress(byte[] data) throws IOException {
        Deflater deflater = new Deflater(3);
        try {
            deflater.setInput(data);
            deflater.finish();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
            byte[] buffer = new byte[1024];

            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                outputStream.write(buffer, 0, count);
            }

            outputStream.close();
            return outputStream.toByteArray();
        } finally {
            deflater.end();
        }
    }

    private static byte[] decompress(byte[] data) throws IOException, DataFormatException {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(data);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
            byte[] buffer = new byte[1024];

            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }

            outputStream.close();
            return outputStream.toByteArray();
        } finally {
            inflater.end();
        }
    }
}
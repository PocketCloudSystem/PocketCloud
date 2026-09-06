package de.pocketcloud.network.codec;

import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.network.exception.PacketException;
import de.pocketcloud.network.packet.data.PacketData;
import org.jetbrains.annotations.Nullable;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class PacketSerializer {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final Map<String, SecretKeySpec> KEY_CACHE = new ConcurrentHashMap<>();

    private PacketSerializer() {}

    public static byte[] encode(Packet packet, boolean encryptionEnabled, String authenticationKey) throws PacketException {
        try {
            PacketData buffer = new PacketData();
            packet.encode(buffer);
            buffer.write(authenticationKey);

            byte[] bytes = compress(buffer.toByteArray());

            if (encryptionEnabled) {
                bytes = encrypt(bytes, authenticationKey);
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

            byte[] bytes = buffer;
            if (encryptionEnabled) {
                bytes = decrypt(bytes, authenticationKey);
            }

            byte[] decompressed = decompress(bytes);
            PacketData data = PacketData.fromBytes(decompressed);

            if (data.isEmpty()) throw new PacketException("Received buffer is empty");

            String packetName = data.peek().toString();
            if (packetName == null) throw new PacketException("Received buffer does not contain a valid packet name");

            if (data.isEmpty()) throw new PacketException("Received packet does not contain an authentication key");

            String givenKey = data.readLast().toString();
            if (givenKey == null) throw new PacketException("Received packet does not contain an authentication key");

            if (!givenKey.equals(authenticationKey))
                throw new PacketException("Received packet does not contain a valid authentication key");

            var packet = packetResolver.apply(packetName);
            if (packet == null) return null;

            packet.decode(data);
            return packet;
        } catch (PacketData.PacketDecodeException e) {
            throw new PacketException("Failed to decode packet data: " + e.getMessage(), e);
        } catch (DataFormatException e) {
            throw new PacketException("Failed to decompress data: " + e.getMessage(), e);
        } catch (AEADBadTagException e) {
            throw new PacketException("Failed to decrypt packet: authentication tag mismatch");
        } catch (GeneralSecurityException e) {
            throw new PacketException("Failed to decrypt packet data: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new PacketException("IO error during decoding: " + e.getMessage(), e);
        }
    }

    private static byte[] encrypt(byte[] data, String authenticationKey) throws GeneralSecurityException {
        SecretKeySpec key = deriveKey(authenticationKey);
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        byte[] ciphertext = cipher.doFinal(data);

        byte[] result = new byte[GCM_IV_LENGTH_BYTES + ciphertext.length];
        System.arraycopy(iv, 0, result, 0, GCM_IV_LENGTH_BYTES);
        System.arraycopy(ciphertext, 0, result, GCM_IV_LENGTH_BYTES, ciphertext.length);
        return result;
    }

    private static byte[] decrypt(byte[] data, String authenticationKey) throws GeneralSecurityException, PacketException {
        if (data.length < GCM_IV_LENGTH_BYTES) {
            throw new PacketException("Encrypted buffer is too short to contain an IV");
        }

        SecretKeySpec key = deriveKey(authenticationKey);
        byte[] iv = Arrays.copyOfRange(data, 0, GCM_IV_LENGTH_BYTES);
        byte[] ciphertext = Arrays.copyOfRange(data, GCM_IV_LENGTH_BYTES, data.length);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        return cipher.doFinal(ciphertext);
    }

    private static SecretKeySpec deriveKey(String authenticationKey) throws GeneralSecurityException {
        if (authenticationKey == null || authenticationKey.isEmpty()) {
            throw new GeneralSecurityException("Cannot derive an encryption key from an empty auth token");
        }

        SecretKeySpec cached = KEY_CACHE.get(authenticationKey);
        if (cached != null) return cached;

        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(authenticationKey.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec derived = new SecretKeySpec(keyBytes, "AES");
            KEY_CACHE.put(authenticationKey, derived);
            return derived;
        } catch (NoSuchAlgorithmException e) {
            throw new GeneralSecurityException(e);
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
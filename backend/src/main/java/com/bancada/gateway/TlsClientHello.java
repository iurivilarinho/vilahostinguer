package com.bancada.gateway;

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/**
 * First TLS record of a connection, read only to learn the server name the visitor asked for (SNI).
 * The bytes are relayed as they came: the certificate and the encryption stay with the destination.
 */
public final class TlsClientHello {

    private static final int RECORD_HEADER_BYTES = 5;
    private static final int HANDSHAKE_RECORD = 22;
    private static final int CLIENT_HELLO = 1;
    private static final int SERVER_NAME_EXTENSION = 0;
    private static final int MAX_RECORD_BYTES = 16_384 + 2_048;

    private final byte[] bytes;
    private final String serverName;

    private TlsClientHello(byte[] bytes, String serverName) {
        this.bytes = bytes;
        this.serverName = serverName;
    }

    /** Null when the connection closes before a whole record arrives. */
    public static TlsClientHello read(InputStream in) throws IOException {
        byte[] header = in.readNBytes(RECORD_HEADER_BYTES);
        if (header.length < RECORD_HEADER_BYTES) {
            return null;
        }
        if ((header[0] & 0xff) != HANDSHAKE_RECORD) {
            return new TlsClientHello(header, null);
        }
        int length = ((header[3] & 0xff) << 8) | (header[4] & 0xff);
        if (length > MAX_RECORD_BYTES) {
            throw new IOException("Registro TLS inválido");
        }
        byte[] body = in.readNBytes(length);
        if (body.length < length) {
            return null;
        }
        byte[] all = Arrays.copyOf(header, RECORD_HEADER_BYTES + length);
        System.arraycopy(body, 0, all, RECORD_HEADER_BYTES, length);
        return new TlsClientHello(all, parseServerName(body));
    }

    public byte[] bytes() {
        return bytes;
    }

    /** Lowercase server name, or null when the visitor sent none (an IP typed in the browser, a non-TLS client). */
    public String serverName() {
        return serverName;
    }

    static String parseServerName(byte[] handshake) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(handshake);
            if ((buffer.get() & 0xff) != CLIENT_HELLO) {
                return null;
            }
            skip(buffer, 3);   // handshake length
            skip(buffer, 2);   // client version
            skip(buffer, 32);  // random
            skip(buffer, buffer.get() & 0xff);        // session id
            skip(buffer, buffer.getShort() & 0xffff); // cipher suites
            skip(buffer, buffer.get() & 0xff);        // compression methods
            if (!buffer.hasRemaining()) {
                return null;
            }
            int extensionsEnd = (buffer.getShort() & 0xffff) + buffer.position();
            while (buffer.position() + 4 <= extensionsEnd) {
                int type = buffer.getShort() & 0xffff;
                int length = buffer.getShort() & 0xffff;
                if (type != SERVER_NAME_EXTENSION) {
                    skip(buffer, length);
                    continue;
                }
                int listEnd = (buffer.getShort() & 0xffff) + buffer.position();
                while (buffer.position() + 3 <= listEnd) {
                    int nameType = buffer.get() & 0xff;
                    byte[] name = new byte[buffer.getShort() & 0xffff];
                    buffer.get(name);
                    if (nameType == 0) {
                        return new String(name, StandardCharsets.US_ASCII).toLowerCase(Locale.ROOT);
                    }
                }
                return null;
            }
            return null;
        } catch (BufferUnderflowException | IllegalArgumentException exception) {
            return null;
        }
    }

    private static void skip(ByteBuffer buffer, int count) {
        buffer.position(buffer.position() + count);
    }
}

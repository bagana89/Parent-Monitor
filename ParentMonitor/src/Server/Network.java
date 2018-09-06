package Server;

import java.nio.charset.StandardCharsets;

public final class Network {

    private Network() {

    }

    /**
     * Port used for Client-Server text communication.
     */
    public static final int TEXT_PORT = 2200;

    /**
     * Port used for Client-Server image transmission.
     */
    public static final int IMAGE_PORT = 2500;

    /**
     * 1MB buffer size for sending and receiving images.
     */
    public static final int IMAGE_BUFFER_SIZE = 1024 * 1024;

    /**
     * Image File Format used by this application.
     */
    public static final String PNG = "PNG";

    /**
     * Message sent by server to client ordering the client to send a screen
     * shot.
     *
     * This is no longer needed, instead of sending a slow request to the client
     * for a image, we just expect the client to send a image at all times. This
     * reduces latency.
     */
    @Deprecated
    public static final String REQUEST_IMAGE = "Server Request: Screenshot";

    /**
     * Message sent by server to client ordering the client to close their side
     * of the application.
     */
    public static final String CLOSE_CLIENT = "Server Request: Exit";

    /**
     * Message sent by client to server when client closes their side of the
     * application.
     */
    public static final String CLIENT_EXITED = "Client Action: Exit";

    /**
     * Message sent by server to client ordering the client to shutdown their
     * entire device.
     */
    public static final String PUNISH = "Server Request: Shutdown";

    public static final String encode(String str) {
        byte[] ascii = str.getBytes(StandardCharsets.UTF_8);
        int lastIndex = ascii.length;

        StringBuilder buffer = new StringBuilder(4 * lastIndex);

        --lastIndex;

        for (int index = 0; index < lastIndex; ++index) {
            buffer.append(ascii[index]);
            buffer.append(',');
        }
        buffer.append(ascii[lastIndex]);

        return buffer.toString();
    }

    public static final String decode(String str) {
        String[] split = str.split(",");
        int length = split.length;

        byte[] bytes = new byte[length];

        for (int index = 0; index < length; ++index) {
            bytes[index] = Byte.parseByte(split[index]);
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }
}
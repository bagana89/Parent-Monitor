package Client;

public final class Network {

    private Network() {

    }

    /**
     * Port used for Client-Server Text Communication.
     */
    public static final int TEXT_PORT = 2200;

    /**
     * Port used for Client-Server Image Communication.
     */
    public static final int IMAGE_PORT = 2500;

    /**
     * Image File Format used by this application.
     */
    public static final String PNG = "PNG";

    /**
     * Message sent by server to client ordering the client to send a screen
     * shot.
     *
     * This is no longer needed, instead of responding to a slow signal request
     * by the server for a image, we just constantly send a image to the server
     * at all times. This reduces latency.
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
}
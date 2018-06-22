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
     */
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
}
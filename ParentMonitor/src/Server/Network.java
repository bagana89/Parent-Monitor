package Server;

public final class Network {
    
    private Network() {
        
    }
    
    public static final int TEXT_PORT = 1000;
    public static final int IMAGE_PORT = 1001;
    
    /**
     * Message sent by server to client ordering the client to send a screen shot.
     */
    public static final String REQUEST_IMAGE = "Server Request: Screenshot";
    
    /**
     * Message sent by server to client ordering the client to close their side of the application.
     */
    public static final String CLOSE_CLIENT = "Server Request: Exit";
    
    /**
     * Message sent by client to server when client closes their side of the application.
     */
    public static final String CLIENT_EXITED = "Client Action: Exit";
}
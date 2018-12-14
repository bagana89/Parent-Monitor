package Server;

import static Server.Network.ENCODING;
import Util.MessageEncoder;
import Util.StreamCloser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class TextSocket implements Closeable {

    //Active IPAddresses in use by all TextSockets
    private static final Set<String> ACTIVE_ADDRESSES = Collections.synchronizedSet(new HashSet<>());
    
    //private Address address;
    private Socket socket;
    private BufferedReader recieveText;
    private PrintWriter sendText;
    
    private String address;
    private MessageEncoder encoder;

    public TextSocket(String host, int port) {
        this(new InetSocketAddress(host, port));
    }

    public TextSocket(final InetSocketAddress socketAddress) {
        final InetAddress clientAddress = socketAddress.getAddress();
        
        if (clientAddress == null) {
            System.out.println("Error: " + socketAddress.getHostString() + " is unresolved.");
            return;
        }
     
        final String remoteAddress = clientAddress.getHostAddress();
        
        if (ACTIVE_ADDRESSES.contains(remoteAddress)) {
            System.out.println("Error: " + remoteAddress + " is already in use.");
            return;
        }
        
        final Socket connection = new Socket();

        try {
            connection.setReuseAddress(true);
            //We will only wait 5 seconds for the client to send its system data
            connection.setSoTimeout(5000);
            connection.connect(socketAddress, 1000);
        }
        catch (IOException ex) {
            StreamCloser.close(connection);
            return;
        }

        final Charset encoding = ENCODING;
        final BufferedReader textInput;

        try {
            //stream to get text from client
            textInput = new BufferedReader(new InputStreamReader(connection.getInputStream(), encoding)); //EXCEPTION LINE
        }
        catch (IOException ex) {
            StreamCloser.close(connection);
            ex.printStackTrace();
            return;
        }

        final PrintWriter textOutput;

        try {
            //stream to send text to client 
            textOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), encoding)), true); //EXCEPTION LINE
        }
        catch (IOException ex) {
            //Close all streams above
            StreamCloser.close(connection);
            StreamCloser.close(textInput);
            ex.printStackTrace();
            return;
        }

        socket = connection;
        recieveText = textInput;
        sendText = textOutput;
 
        //already synchronized
        ACTIVE_ADDRESSES.add(address = remoteAddress);
    }

    public boolean isActive() {
        //load instance variables first
        Socket socketReference = socket;
        BufferedReader recieveTextReference = recieveText;
        PrintWriter sendTextReference = sendText;
        return (socketReference == null || recieveTextReference == null || sendTextReference == null) ? false : !socketReference.isClosed();
    }

    @Override
    public void close() {
        //load instance variables first
        Socket socketReference = socket;
        BufferedReader recieveTextReference = recieveText;
        PrintWriter sendTextReference = sendText;
        String addressReference = address;

        //close local references at the same time
        StreamCloser.close(socketReference);
        StreamCloser.close(recieveTextReference);
        StreamCloser.close(sendTextReference);
        
        if (addressReference != null) {
            System.out.println("Disconnecting: " + addressReference);
            ACTIVE_ADDRESSES.remove(addressReference);
            address = null;
        }

        //dispose of instance variables
        socket = null;
        recieveText = null;
        sendText = null;
        encoder = null;
    }

    //The 5 second timeout must be reset to infinite time out
    //after client system data has been recieved, infinte time out is zero here
    public void setReadWaitTime(int waitTime) throws SocketException {
        socket.setSoTimeout(waitTime);
    }

    public String readText() throws IOException {
        MessageEncoder messageEncoder = encoder;
        String recieve = recieveText.readLine();
        return messageEncoder != null && messageEncoder.isValid() ? messageEncoder.decode(recieve) : recieve;
    }

    public void sendText(String text) {
        MessageEncoder messageEncoder = encoder;
        sendText.println(messageEncoder != null && messageEncoder.isValid() ? messageEncoder.encode(text) : text);
    }

    @Override
    public String toString() {
        return socket == null ? "Not Connected" : socket.toString();
    }

    public String getAddress() {
        return address;
    }
    
    public boolean isSecure() {
        MessageEncoder messageEncoder = encoder;
        return messageEncoder != null && messageEncoder.isValid();
    }
    
    public void setEncoder(MessageEncoder messageEncoder) {
        encoder = messageEncoder;
    }
}
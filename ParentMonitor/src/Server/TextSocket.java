package Server;

import Util.StreamCloser;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class TextSocket implements Closeable {
    
    //Active IPAddresses in use by all TextSockets
    private static final Set<Address> ACTIVE_ADDRESSES = Collections.synchronizedSet(new TreeSet<>());

    private Address address;
    private Socket socket;
    private BufferedReader recieveText;
    private PrintWriter sendText;
    
    private static final Map<ByteArray, LocalPortSocketAddress> USED_SOCKET_ADDRESSES = new HashMap<>(65536);
    static long MEMORY_HITS = 0;
    static int CREATED = 0;

    //Return previous InetSocketAddresses as much as possible
    private static LocalPortSocketAddress getSocketAddress(byte[] address, int port) {
        ByteArray remoteHostRawAddress = new ByteArray(address);
        LocalPortSocketAddress previousSocketAddress = USED_SOCKET_ADDRESSES.get(remoteHostRawAddress); //assume port is constant
        if (previousSocketAddress != null) {
            ++MEMORY_HITS;
            remoteHostRawAddress.destroy(); //we can safely destroy this reference, it will never be used again
            return previousSocketAddress;
        }
        try {
            LocalPortSocketAddress socketAddress
                    = new LocalPortSocketAddress(new InetSocketAddress(InetAddress.getByAddress(remoteHostRawAddress.getArray()), port));
            USED_SOCKET_ADDRESSES.put(remoteHostRawAddress, socketAddress);
            //do not destroy this reference, it will live on in the map
            ++CREATED;
            return socketAddress;
        }
        catch (UnknownHostException ex) {
            ex.printStackTrace();
            throw new UncheckedIOException(ex);
        }
    }

    public TextSocket(String host, int port) {
        Address hostAddress = new Address(host);
        
        //already synchronized
        if (ACTIVE_ADDRESSES.contains(hostAddress)) {
            hostAddress.destroy();
            System.out.println(host + " already in use.");
            return;
        }
        
        try {
            (socket = new Socket()).connect(new InetSocketAddress(host, port), 100);
        }
        catch (IOException ex) {
            StreamCloser.close(socket);
            socket = null;
            hostAddress.destroy();
            return;
        }
       
        try {
            //stream to get text from client
            recieveText = new BufferedReader(new InputStreamReader(socket.getInputStream())); //EXCEPTION LINE
        }
        catch (IOException ex) {
            StreamCloser.close(socket);
            socket = null;
            hostAddress.destroy();
            ex.printStackTrace();
            return;
        }
        
        try {
            //stream to send text to client 
            sendText = new PrintWriter(socket.getOutputStream(), true); //EXCEPTION LINE
        }
        catch (IOException ex) {
            //Close all streams above
            StreamCloser.close(socket);
            StreamCloser.close(recieveText);
            socket = null;
            recieveText = null;
            hostAddress.destroy();
            ex.printStackTrace();
            return;
        }
        
        //already synchronized
        ACTIVE_ADDRESSES.add(address = hostAddress);
    }

    public TextSocket(byte[] remoteAddress, int port) {
        //When using a synchronized wrapper, you only need the block
        //as you iterate over the wrapper, all other individual operations
        //are already safely synchronized.
        
        /*
        synchronized (ACTIVE_ADDRESSES) {
            for (Iterator<Address> it = ACTIVE_ADDRESSES.iterator(); it.hasNext();) {
                Address active = it.next();
                if (Arrays.equals(remoteAddress, active.getAddress())) {
                    System.out.println(active.toString() + " already in use.");
                    return; //address already in use
                }
            }
        }
         */

        Address rawAddress = new Address(remoteAddress);

        //already synchronized
        //uses binary search, so should be faster than linear iterator
        if (ACTIVE_ADDRESSES.contains(rawAddress)) {
            System.out.println(rawAddress.toString() + " is already in use.");
            rawAddress.destroy(); //will also destroy internal byte array
            return;
        }

        try {
            socket = new Socket();
            socket.setReuseAddress(true);
            LocalPortSocketAddress connectionAddress = getSocketAddress(rawAddress.getAddress(), port);
            //contains InetSocketAddress and previously used local port, if any
            Integer usedLocalPort = connectionAddress.getLocalPort();
            if (usedLocalPort == null) {
                socket.connect(connectionAddress.getSocketAddress(), 10);
                connectionAddress.setLocalPort(socket.getLocalPort());
            }
            else {
                //reuse native device localport if possible
                InetSocketAddress previousAddress = connectionAddress.getSocketAddress();
                System.out.println("Reusing port: " + usedLocalPort + " for: " + previousAddress.getAddress());
                socket.bind(new InetSocketAddress(previousAddress.getAddress(), usedLocalPort));
                socket.connect(previousAddress, 10);
            }
        }
        catch (IOException ex) {
            StreamCloser.close(socket);
            socket = null;
            rawAddress.destroy();
            return;
        }

        try {
            //stream to get text from client
            recieveText = new BufferedReader(new InputStreamReader(socket.getInputStream())); //EXCEPTION LINE
        }
        catch (IOException ex) {
            StreamCloser.close(socket);
            socket = null;
            ex.printStackTrace();
            rawAddress.destroy();
            return;
        }
        
        try {
            //stream to send text to client 
            sendText = new PrintWriter(socket.getOutputStream(), true); //EXCEPTION LINE
        }
        catch (IOException ex) {
            //Close all streams above
            StreamCloser.close(socket);
            StreamCloser.close(recieveText);
            socket = null;
            recieveText = null;
            rawAddress.destroy();
            ex.printStackTrace();
            return;
        }

        //already synchronized
        ACTIVE_ADDRESSES.add(address = rawAddress);
    }

    public boolean isActive() {
        return (socket == null || recieveText == null || sendText == null) ? false : !socket.isClosed();
    }

    @Override
    public void close() {
        //load instance variables first
        Socket socketReference = socket;
        BufferedReader recieveTextReference = recieveText;
        PrintWriter sendTextReference = sendText;
        Address addressReference = address;
        
        //close local references at the same time
        StreamCloser.close(socketReference);
        StreamCloser.close(recieveTextReference);
        StreamCloser.close(sendTextReference);
        
        //dispose of instance variables
        if (addressReference != null) {
            System.out.println("Disconnecting: " + addressReference);
            //already synchronized
            ACTIVE_ADDRESSES.remove(addressReference);
            addressReference.destroy();
            address = null;
        }
        
        socket = null;
        recieveText = null;
        sendText = null;
    }
   
    public String readText() throws IOException {
        return recieveText.readLine();
    }
    
    public void sendText(String text) {
        sendText.println(text);
    }
    
    public BufferedReader getInputStream() {
        return recieveText;
    }
    
    public PrintWriter getOutputStream() {
        return sendText;
    }
    
    @Override
    public String toString() {
        return socket == null ? "Not Connected" : socket.toString();
    }
    
    public String getAddress() {
        return address.toString();
    }
}
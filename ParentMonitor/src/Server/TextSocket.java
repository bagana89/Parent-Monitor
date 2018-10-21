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
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class TextSocket implements Closeable {
    
    //Active IPAddresses in use by all TextSockets
    private static final Set<Address> ACTIVE_ADDRESSES = Collections.synchronizedSet(new HashSet<>());
    
    private static class Address {

        private static boolean isLocalAddress(InetAddress address) {
            // Check if the address is a valid special local or loop back
            if (address.isAnyLocalAddress() || address.isLoopbackAddress()) {
                return true;
            }

            // Check if the address is defined on any interface
            try {
                return NetworkInterface.getByInetAddress(address) != null;
            }
            catch (SocketException ex) {
                return false;
            }
        }
        
        private static final byte[] LOOP_BACK_ADDRESS = {127, 0, 0, 1};
        
        private byte[] address;
        private Integer addressHash;
        private String textualAddress;        
        
        //Used by IPScanner
        //takes a "raw" address, -128 to 127
        private Address(byte[] remoteAddress) {
            //if input address is localhost
            if (Arrays.equals(LOOP_BACK_ADDRESS, remoteAddress)) {
                System.out.println(Arrays.toString(remoteAddress));
                try {
                    address = InetAddress.getLocalHost().getAddress();
                }
                catch (UnknownHostException ex) {
                    ex.printStackTrace();
                    throw new UncheckedIOException(ex);
                }
            }
            else {
                address = remoteAddress;
            }
        }

        //Used by user
        private Address(String remoteHost) {
            if ("127.0.0.1".equals(remoteHost)) {
                System.out.println("Replacing " + remoteHost + " with localhost");
                remoteHost = "localhost";
                //Allow number format exception to be raised.
            }
            try {
                address = IPScanner.convertTextualAddressToRawAddress(remoteHost);
            }
            catch (NumberFormatException ex) {
                try {
                    System.out.println("Remote Host (Input): " + remoteHost);
                    //When user types in something like "localhost" or "127.0.0.1"
                    //We want to convert to actual address
                    InetAddress backup = InetAddress.getByName(remoteHost);
                    if (isLocalAddress(backup)) {
                        //If remoteHost = "localhost" then the backup will fail
                        //the backup would return 127.0.0.1, which we do not want
                        System.out.println("This is a local address.");
                        backup = InetAddress.getLocalHost(); //remake the backup again
                        //this new backup will return the correct network address of the local computer
                    }
                    System.out.println("Remote Host (Backup): " + backup.getHostAddress());
                    System.out.println("Remote Host (Raw): " + Arrays.toString(address = backup.getAddress()));
                }
                catch (IOException inner) {
                    
                }
            }
        }
        
        public byte[] getAddress() {
            return address;
        }

        @Override
        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            
            Address other = (Address) obj;
            return Arrays.equals(address, other.address);
        }

        @Override
        public int hashCode() {
            //dont re calculate hash!
            if (addressHash != null) {
                return addressHash;
            }
            return addressHash = Arrays.hashCode(address);
        }

        @Override
        public String toString() {
            //Avoid getfield opcode
            //Dont re calculate address
            String textAddress = textualAddress;
            if (textAddress != null) {
                return textAddress;
            }
            return textualAddress = IPScanner.convertRawAddressToTextualAddress(address);
        }
        
        public void destroy() {
            address = null;
            addressHash = null;
            textualAddress = null;
        }
    }
    
    //Light wrapper for byte arrays
    private static class ByteArray {
        
        private byte[] array;
        private Integer arrayHash;
        
        private ByteArray(byte[] data) {
            array = data;
        }
        
        public byte[] getArray() {
            return array;
        }
        
        @Override
        public int hashCode() {
            if (arrayHash != null) {
                return arrayHash;
            }
            return arrayHash = Arrays.hashCode(array);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            ByteArray other = (ByteArray) obj;
            return Arrays.equals(array, other.array);
        }
    }

    private Address address;
    private Socket socket;
    private BufferedReader recieveText;
    private PrintWriter sendText;
    
    private static final Map<ByteArray, InetSocketAddress> USED_SOCKET_ADDRESSES = new HashMap<>(65536);
    //private static final Map<ByteArray, InetAddress> USED_ADDRESSES = new HashMap<>(65536);

    //Return previous InetSocketAddresses as much as possible
    private static InetSocketAddress getSocketAddress(Address address, int port) {
        ByteArray remoteHost = new ByteArray(address.getAddress());
        InetSocketAddress previousSocketAddress = USED_SOCKET_ADDRESSES.get(remoteHost); //assume port is constant
        if (previousSocketAddress != null) {
            return previousSocketAddress;
        }
        try {
            /*
            InetAddress previousAddress = USED_ADDRESSES.get(remoteHost);
            if (previousAddress == null) {
                USED_ADDRESSES.put(remoteHost, previousAddress = InetAddress.getByAddress(remoteHost.getArray()));
            }
             */
            //We should not create a new SocketAddress more than once per address.
            InetSocketAddress create = new InetSocketAddress(InetAddress.getByAddress(remoteHost.getArray()), port);
            USED_SOCKET_ADDRESSES.put(remoteHost, create);
            return create;
        }
        catch (UnknownHostException ex) {
            ex.printStackTrace();
            throw new UncheckedIOException(ex);
        }
    }

    public TextSocket(String host, int port) {
        Address hostAddress = new Address(host);
        
        if (ACTIVE_ADDRESSES.contains(hostAddress)) {
            hostAddress.destroy();
            System.out.println(host + " already in use.");
            return;
        }
        
        try {
            socket = new Socket();
            //wait relatively longer when user manually enters address
            socket.connect(new InetSocketAddress(host, port), 100);
        }
        catch (IOException ex) {
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
        
        ACTIVE_ADDRESSES.add(address = hostAddress);
    }
    
    public TextSocket(byte[] remoteAddress, int port) {
        Address hostAddress = new Address(remoteAddress);

        if (ACTIVE_ADDRESSES.contains(hostAddress)) {
            String host = hostAddress.toString();
            hostAddress.destroy();
            System.out.println(host + " already in use.");
            return;
        }
        
        try {
            socket = new Socket();
            //dont wait that long when scanning
            socket.connect(getSocketAddress(hostAddress, port), 10);   
        }
        catch (IOException ex) {
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

        ACTIVE_ADDRESSES.add(address = hostAddress);
    }

    public boolean isActive() {
        return (socket == null || recieveText == null || sendText == null) ? false : !socket.isClosed();
    }

    @Override
    public void close() {
        Socket localSocket = socket;
        BufferedReader localReader = recieveText;
        PrintWriter localWriter = sendText;
        StreamCloser.close(localSocket);
        StreamCloser.close(localReader);
        StreamCloser.close(localWriter);
        if (address != null) {
            System.out.println("Disconnecting: " + address);
            ACTIVE_ADDRESSES.remove(address);
            address.destroy();
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
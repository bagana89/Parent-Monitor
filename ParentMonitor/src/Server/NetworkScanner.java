package Server;

import static Server.Network.ENCODING;
import static Server.Network.SHA_1;
import Util.MessageEncoder;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import Util.ThreadSafeBoolean;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class NetworkScanner {
    
    private static final int BLOCK_SIZE = 256;
    private static final ArrayList<ConnectionTester> CONNECTORS = new ArrayList<>(BLOCK_SIZE * BLOCK_SIZE);
    
    //Testers can be reused
    private static class ConnectionTester implements Callable<TextSocket>, Recyclable {

        private InetSocketAddress socketAddress;

        private ConnectionTester(byte[] rawAddress) {
            InetSocketAddress remoteSocketAddress;
            try {
                //interally, this stores the raw address as an int, so don't copy raw address here
                remoteSocketAddress = new InetSocketAddress(InetAddress.getByAddress(rawAddress), Network.TEXT_PORT);
            }
            catch (UnknownHostException ex) {
                ex.printStackTrace();
                return;
            }
            socketAddress = remoteSocketAddress;
        }

        @Override
        public void recycle() {
            socketAddress = null;
        }

        @Override
        public TextSocket call() {
            InetSocketAddress remoteSocketAddress = socketAddress;
            if (remoteSocketAddress == null || remoteSocketAddress.isUnresolved()) {
                return null;
            }
            TextSocket connection = new TextSocket(remoteSocketAddress);
            return connection.isActive() ? connection : null;
        }
    }
    
    //helps with memory management
    private static class GarbageCollectorThread extends Thread {
        
        private ServerFrame parent;
        private ThreadSafeBoolean terminate = new ThreadSafeBoolean(false);
        
        private GarbageCollectorThread(ServerFrame frame) {
            super("Garbage Collector Thread");
            parent = frame;
        }
        
        @Override
        public void run() {
            System.out.println("Garbage Collector Thread started.");
            ServerFrame parentFrame = parent;
            ThreadSafeBoolean terminateMarker = terminate;
            //re order conditional so that the terminator marker can break first
            //the terminator marker has a higher chance of breaking earlier than isEnabled
            while (!terminateMarker.get() && parentFrame.isEnabled()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                }
                catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                System.gc();
            }
            System.out.println("Garbage Collector Thread terminated.");
        }
        
        private void terminate() {
            terminate.set(true);
            terminate = null;
            parent = null;
        }
    }

    //assume argument is positive
    private static byte convertUnsignedIntToSignedByte(int num) {
        return (byte) num;
    }
    
    private static int convertSignedByteToUnsignedInt(byte num) {
        return num & 0xFF;
    }
    
    public static final byte[] convertTextualAddressToRawAddress(String IPAddress) {
        int first = IPAddress.indexOf(".");
        
        if (first < 0) {
            return null;
        }

        int firstAfter = first + 1;
        int second = IPAddress.indexOf(".", firstAfter);
        int secondAfter = second + 1;
        int third = IPAddress.indexOf(".", secondAfter);

        return new byte[]{
            (byte) Integer.parseInt(IPAddress.substring(0, first)),
            (byte) Integer.parseInt(IPAddress.substring(firstAfter, second)),
            (byte) Integer.parseInt(IPAddress.substring(secondAfter, third)),
            (byte) Integer.parseInt(IPAddress.substring(third + 1))
        };
    }

    public static final String convertRawAddressToTextualAddress(byte[] address) {
        int last8Bits = 0xFF;

        return (address[0] & last8Bits)
                + "."
                + (address[1] & last8Bits)
                + "."
                + (address[2] & last8Bits)
                + "."
                + (address[3] & last8Bits);
    }

    private static String PREVIOUS_SUBNET;

    //could keep the used threads in memory and ask them to run again
    public static List<TextSocket> getReachableSockets(ServerFrame parent, String subnetText) {
        GarbageCollectorThread garbageCollector = new GarbageCollectorThread(parent);
        garbageCollector.start();

        //check to see if the subnet has changed, if it has, then clear previous connectors
        if (PREVIOUS_SUBNET == null) {
            PREVIOUS_SUBNET = subnetText;
        }
        else {
            if (!PREVIOUS_SUBNET.equals(subnetText)) {
                System.out.println("Subnet has changed!");
                for (int index = CONNECTORS.size() - 1; index >= 0; --index) {
                    CONNECTORS.get(index).recycle();
                }
                CONNECTORS.clear();
            }
            PREVIOUS_SUBNET = subnetText;
        }

        if (CONNECTORS.isEmpty()) {
            final int blockSize = BLOCK_SIZE;
            String[] subnet = subnetText.split(Pattern.quote("."));
            System.out.println("Using Subnet: " + Arrays.toString(subnet));  
            final int subnetLength = subnet.length;

            if (subnetLength <= 0 || subnetLength >= 4) {
                garbageCollector.terminate();
                return Collections.emptyList();
            }

            switch (subnetLength) {
                case 1: {
                    CONNECTORS.ensureCapacity(blockSize * blockSize * blockSize);
                    byte[] bytes = new byte[4];
                    bytes[0] = (byte) Integer.parseInt(subnet[0]);
                    for (int second = 0; second < blockSize; ++second) {
                        bytes[1] = (byte) second;
                        for (int third = 0; third < blockSize; ++third) {
                            bytes[2] = (byte) third;
                            for (int fourth = 0; fourth < blockSize; ++fourth) {
                                if (!parent.isEnabled()) {
                                    garbageCollector.terminate();
                                    for (int index = CONNECTORS.size() - 1; index >= 0; --index) {
                                        CONNECTORS.get(index).recycle();
                                    }
                                    CONNECTORS.clear();
                                    return Collections.emptyList();
                                }
                                bytes[3] = (byte) fourth;
                                CONNECTORS.add(new ConnectionTester(bytes));
                            }
                        }
                    }
                    break;
                }
                case 2: {
                    byte[] bytes = new byte[4];
                    bytes[0] = (byte) Integer.parseInt(subnet[0]);
                    bytes[1] = (byte) Integer.parseInt(subnet[1]);
                    for (int third = 0; third < blockSize; ++third) {
                        bytes[2] = (byte) third;
                        for (int fourth = 0; fourth < blockSize; ++fourth) {
                            if (!parent.isEnabled()) {
                                garbageCollector.terminate();
                                for (int index = CONNECTORS.size() - 1; index >= 0; --index) {
                                    CONNECTORS.get(index).recycle();
                                }
                                CONNECTORS.clear();
                                return Collections.emptyList();
                            }
                            bytes[3] = (byte) fourth;
                            CONNECTORS.add(new ConnectionTester(bytes));
                        }
                    }
                    break;
                }
                default: {
                    byte[] bytes = new byte[4];
                    bytes[0] = (byte) Integer.parseInt(subnet[0]);
                    bytes[1] = (byte) Integer.parseInt(subnet[1]);
                    bytes[2] = (byte) Integer.parseInt(subnet[2]);
                    for (int fourth = 0; fourth < blockSize; ++fourth) {
                        if (!parent.isEnabled()) {
                            garbageCollector.terminate();
                            for (int index = CONNECTORS.size() - 1; index >= 0; --index) {
                                CONNECTORS.get(index).recycle();
                            }
                            CONNECTORS.clear();
                            return Collections.emptyList();
                        }
                        bytes[3] = (byte) fourth;
                        CONNECTORS.add(new ConnectionTester(bytes));
                    }
                    break;
                }
            }
        }

        System.out.println("Starting Thread Pool.");

        /*
        This is so efficient!!!
        We've increased the number of threads in the pool
        from 100 to 500 since we've increased TextSocket wait time
        from 10 to 50 ms, to allow more time for clients to connect
        while preserving speed. This balances out, but uses more CPU
        in the same time (the time range is around 16 seconds) 
         */
        ExecutorService pool = Executors.newFixedThreadPool(BLOCK_SIZE * 10);
        List<Future<TextSocket>> results;

        try {
            results = pool.invokeAll(CONNECTORS);
        }
        catch (InterruptedException ex) {
            pool.shutdown();
            garbageCollector.terminate();
            ex.printStackTrace();
            return Collections.emptyList();
        }
        
        pool.shutdown();

        System.out.println("Closing Thread Pool.");

        List<TextSocket> reachableSockets = new LinkedList<>();

        for (int index = 0, resultCount = results.size(); index < resultCount; ++index) {
            try {
                TextSocket socket = results.get(index).get();
                if (socket != null) {
                    reachableSockets.add(socket);
                }
            }
            catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            }
        }
        
        results.clear();
        garbageCollector.terminate();
        return reachableSockets;
    }

    private static int getNumberOfDigits(int num) {
        int count = 1;
        for (num /= 10; num > 0; num /= 10) {
            ++count;
        }
        return count;
    }

    public static void main(String... args) throws UnknownHostException {
         byte[] securityKey = "255.255.255.255".getBytes(ENCODING);
        securityKey = SHA_1.digest(securityKey);
        securityKey = Arrays.copyOf(securityKey, 16); // use only first 128 bits
        

        MessageEncoder security = new MessageEncoder(securityKey, "AES");
        for (int originalUnsignedInteger = 0; originalUnsignedInteger <= 255; ++originalUnsignedInteger) {
            byte signedByte = convertUnsignedIntToSignedByte(originalUnsignedInteger);
            int convertedUnsignedInteger = convertSignedByteToUnsignedInt(signedByte);
            if (originalUnsignedInteger != convertedUnsignedInteger) {
                throw new Error();
            }
            if (String.valueOf(originalUnsignedInteger).length() != getNumberOfDigits(originalUnsignedInteger)) {
                throw new Error();
            }
            System.out.println("Original: " + originalUnsignedInteger + " Converted: " + convertedUnsignedInteger + " Signed Byte: " + signedByte);
        }
        
        System.out.println("====================================");

        byte[] bytes = new byte[4];
        for (int index = 0; index <= 255; ++index) {
            bytes[0] = (byte) index;
            bytes[1] = (byte) index;
            bytes[2] = (byte) index;
            bytes[3] = (byte) index;

            String original = Arrays.toString(bytes);
            String host = InetAddress.getByAddress(bytes).getHostAddress();

            String text = NetworkScanner.convertRawAddressToTextualAddress(bytes);
            String raw = Arrays.toString(NetworkScanner.convertTextualAddressToRawAddress(text));

            if (!original.equals(raw)) {
                throw new Error();
            }
            else {
                System.out.println("Original: " + original + " Raw: " + raw);
            }

            if (!host.equals(text)) {
                throw new Error();
            }
            else {
                System.out.println("Host: " + host + " Text: " + text);
            }
            
            //System.out.println(new ByteArray(bytes).hashCode());
            System.out.println();
        }
    }

    public static int[] convertPrefixLengthToIPv4Address(int length) {
        int[][] address = new int[4][8];
        int row = 0;
        int column = 0;
        while (length-- > 0) {
            if (column == 8) {
                column = 0;
                ++row;
            }
            address[row][column++] = 1;
        }
        int[] result = new int[4];
        for (row = 0; row < 4; ++row) {
            String current = "";
            for (column = 0; column < 8; ++column) {
                current += Integer.toString(address[row][column]);
            }
            result[row] = Integer.parseInt(current, 2);
        }
        return result;
    }
}
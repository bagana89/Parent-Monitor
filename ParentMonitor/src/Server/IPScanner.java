package Server;

import Util.IteratorWrapper;
import Util.ThreadSafeBoolean;
import java.io.IOException;
import static java.lang.System.out;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import static Server.TextSocket.MEMORY_HITS;
import static Server.TextSocket.CREATED;

public final class IPScanner {
    
    private static final int BLOCK_SIZE = 256;
    private static final byte[] TABLE = new byte[BLOCK_SIZE];
    private static final Map<Byte, Integer> REVERSE_TABLE = new HashMap<>(BLOCK_SIZE);
    private static final List<ConnectionTester> CONNECTORS = new ArrayList<>(BLOCK_SIZE * BLOCK_SIZE);
    private static final List<ConnectionThread> THREADS = new ArrayList<>(BLOCK_SIZE * BLOCK_SIZE);

    //Testers can be reused
    private static class ConnectionTester implements Runnable {

        private ServerFrame parent;
        private byte[] address;
        private TextSocket socket;

        private ConnectionTester(ServerFrame frame, byte[] rawAddress) {
            parent = frame;
            address = new byte[]{rawAddress[0], rawAddress[1], rawAddress[2], rawAddress[3]};
        }

        //Will be re used
        @Override
        public void run() {
            if (!parent.isEnabled()) {
                return;
            }
            TextSocket connection = new TextSocket(address, Network.TEXT_PORT);
            if (connection.isActive()) {
                socket = connection;
            }
            else {
                //close the stream 
                connection.close();
            }
        }
        
        private TextSocket getSocket() {
            return socket;
        }
        
        private void reset() {
            socket = null;
        }
        
        private void destroy() {
            parent = null;
            address = null;
            socket = null;
        }
    }
    
    //Wrapper for ConnectionTesters, threads cannot be reused
    private static class ConnectionThread extends Thread {
        
        private static final String SAME_NAME = "";
        private ConnectionTester tester;
        
        private ConnectionThread(ConnectionTester connectionTester) {
            super(SAME_NAME);
            tester = connectionTester;
        }
        
        @Override
        public void run() {
            tester.run();
            tester = null;
        }
    }
    
    private static class GarbageCollectorThread extends Thread {
        
        private ServerFrame parent;
        private ThreadSafeBoolean terminate = new ThreadSafeBoolean(false);
        
        private GarbageCollectorThread(ServerFrame frame) {
            super("Garbage Collector Thread");
            parent = frame;
        }
        
        @Override
        public void run() {
            while (parent.isEnabled() && !terminate.get()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000);
                }
                catch (InterruptedException ex) {
                    
                }
                System.gc();
            }
            parent = null;
        }
    }

    static {
        TABLE[0] = 0;
        TABLE[1] = 1;
        TABLE[2] = 2;
        TABLE[3] = 3;
        TABLE[4] = 4;
        TABLE[5] = 5;
        TABLE[6] = 6;
        TABLE[7] = 7;
        TABLE[8] = 8;
        TABLE[9] = 9;
        TABLE[10] = 10;
        TABLE[11] = 11;
        TABLE[12] = 12;
        TABLE[13] = 13;
        TABLE[14] = 14;
        TABLE[15] = 15;
        TABLE[16] = 16;
        TABLE[17] = 17;
        TABLE[18] = 18;
        TABLE[19] = 19;
        TABLE[20] = 20;
        TABLE[21] = 21;
        TABLE[22] = 22;
        TABLE[23] = 23;
        TABLE[24] = 24;
        TABLE[25] = 25;
        TABLE[26] = 26;
        TABLE[27] = 27;
        TABLE[28] = 28;
        TABLE[29] = 29;
        TABLE[30] = 30;
        TABLE[31] = 31;
        TABLE[32] = 32;
        TABLE[33] = 33;
        TABLE[34] = 34;
        TABLE[35] = 35;
        TABLE[36] = 36;
        TABLE[37] = 37;
        TABLE[38] = 38;
        TABLE[39] = 39;
        TABLE[40] = 40;
        TABLE[41] = 41;
        TABLE[42] = 42;
        TABLE[43] = 43;
        TABLE[44] = 44;
        TABLE[45] = 45;
        TABLE[46] = 46;
        TABLE[47] = 47;
        TABLE[48] = 48;
        TABLE[49] = 49;
        TABLE[50] = 50;
        TABLE[51] = 51;
        TABLE[52] = 52;
        TABLE[53] = 53;
        TABLE[54] = 54;
        TABLE[55] = 55;
        TABLE[56] = 56;
        TABLE[57] = 57;
        TABLE[58] = 58;
        TABLE[59] = 59;
        TABLE[60] = 60;
        TABLE[61] = 61;
        TABLE[62] = 62;
        TABLE[63] = 63;
        TABLE[64] = 64;
        TABLE[65] = 65;
        TABLE[66] = 66;
        TABLE[67] = 67;
        TABLE[68] = 68;
        TABLE[69] = 69;
        TABLE[70] = 70;
        TABLE[71] = 71;
        TABLE[72] = 72;
        TABLE[73] = 73;
        TABLE[74] = 74;
        TABLE[75] = 75;
        TABLE[76] = 76;
        TABLE[77] = 77;
        TABLE[78] = 78;
        TABLE[79] = 79;
        TABLE[80] = 80;
        TABLE[81] = 81;
        TABLE[82] = 82;
        TABLE[83] = 83;
        TABLE[84] = 84;
        TABLE[85] = 85;
        TABLE[86] = 86;
        TABLE[87] = 87;
        TABLE[88] = 88;
        TABLE[89] = 89;
        TABLE[90] = 90;
        TABLE[91] = 91;
        TABLE[92] = 92;
        TABLE[93] = 93;
        TABLE[94] = 94;
        TABLE[95] = 95;
        TABLE[96] = 96;
        TABLE[97] = 97;
        TABLE[98] = 98;
        TABLE[99] = 99;
        TABLE[100] = 100;
        TABLE[101] = 101;
        TABLE[102] = 102;
        TABLE[103] = 103;
        TABLE[104] = 104;
        TABLE[105] = 105;
        TABLE[106] = 106;
        TABLE[107] = 107;
        TABLE[108] = 108;
        TABLE[109] = 109;
        TABLE[110] = 110;
        TABLE[111] = 111;
        TABLE[112] = 112;
        TABLE[113] = 113;
        TABLE[114] = 114;
        TABLE[115] = 115;
        TABLE[116] = 116;
        TABLE[117] = 117;
        TABLE[118] = 118;
        TABLE[119] = 119;
        TABLE[120] = 120;
        TABLE[121] = 121;
        TABLE[122] = 122;
        TABLE[123] = 123;
        TABLE[124] = 124;
        TABLE[125] = 125;
        TABLE[126] = 126;
        TABLE[127] = 127;
        TABLE[128] = -128;
        TABLE[129] = -127;
        TABLE[130] = -126;
        TABLE[131] = -125;
        TABLE[132] = -124;
        TABLE[133] = -123;
        TABLE[134] = -122;
        TABLE[135] = -121;
        TABLE[136] = -120;
        TABLE[137] = -119;
        TABLE[138] = -118;
        TABLE[139] = -117;
        TABLE[140] = -116;
        TABLE[141] = -115;
        TABLE[142] = -114;
        TABLE[143] = -113;
        TABLE[144] = -112;
        TABLE[145] = -111;
        TABLE[146] = -110;
        TABLE[147] = -109;
        TABLE[148] = -108;
        TABLE[149] = -107;
        TABLE[150] = -106;
        TABLE[151] = -105;
        TABLE[152] = -104;
        TABLE[153] = -103;
        TABLE[154] = -102;
        TABLE[155] = -101;
        TABLE[156] = -100;
        TABLE[157] = -99;
        TABLE[158] = -98;
        TABLE[159] = -97;
        TABLE[160] = -96;
        TABLE[161] = -95;
        TABLE[162] = -94;
        TABLE[163] = -93;
        TABLE[164] = -92;
        TABLE[165] = -91;
        TABLE[166] = -90;
        TABLE[167] = -89;
        TABLE[168] = -88;
        TABLE[169] = -87;
        TABLE[170] = -86;
        TABLE[171] = -85;
        TABLE[172] = -84;
        TABLE[173] = -83;
        TABLE[174] = -82;
        TABLE[175] = -81;
        TABLE[176] = -80;
        TABLE[177] = -79;
        TABLE[178] = -78;
        TABLE[179] = -77;
        TABLE[180] = -76;
        TABLE[181] = -75;
        TABLE[182] = -74;
        TABLE[183] = -73;
        TABLE[184] = -72;
        TABLE[185] = -71;
        TABLE[186] = -70;
        TABLE[187] = -69;
        TABLE[188] = -68;
        TABLE[189] = -67;
        TABLE[190] = -66;
        TABLE[191] = -65;
        TABLE[192] = -64;
        TABLE[193] = -63;
        TABLE[194] = -62;
        TABLE[195] = -61;
        TABLE[196] = -60;
        TABLE[197] = -59;
        TABLE[198] = -58;
        TABLE[199] = -57;
        TABLE[200] = -56;
        TABLE[201] = -55;
        TABLE[202] = -54;
        TABLE[203] = -53;
        TABLE[204] = -52;
        TABLE[205] = -51;
        TABLE[206] = -50;
        TABLE[207] = -49;
        TABLE[208] = -48;
        TABLE[209] = -47;
        TABLE[210] = -46;
        TABLE[211] = -45;
        TABLE[212] = -44;
        TABLE[213] = -43;
        TABLE[214] = -42;
        TABLE[215] = -41;
        TABLE[216] = -40;
        TABLE[217] = -39;
        TABLE[218] = -38;
        TABLE[219] = -37;
        TABLE[220] = -36;
        TABLE[221] = -35;
        TABLE[222] = -34;
        TABLE[223] = -33;
        TABLE[224] = -32;
        TABLE[225] = -31;
        TABLE[226] = -30;
        TABLE[227] = -29;
        TABLE[228] = -28;
        TABLE[229] = -27;
        TABLE[230] = -26;
        TABLE[231] = -25;
        TABLE[232] = -24;
        TABLE[233] = -23;
        TABLE[234] = -22;
        TABLE[235] = -21;
        TABLE[236] = -20;
        TABLE[237] = -19;
        TABLE[238] = -18;
        TABLE[239] = -17;
        TABLE[240] = -16;
        TABLE[241] = -15;
        TABLE[242] = -14;
        TABLE[243] = -13;
        TABLE[244] = -12;
        TABLE[245] = -11;
        TABLE[246] = -10;
        TABLE[247] = -9;
        TABLE[248] = -8;
        TABLE[249] = -7;
        TABLE[250] = -6;
        TABLE[251] = -5;
        TABLE[252] = -4;
        TABLE[253] = -3;
        TABLE[254] = -2;
        TABLE[255] = -1;
        for (int index = 0; index < BLOCK_SIZE; ++index) {
            REVERSE_TABLE.put(TABLE[index], index);
        }
    }
    
    public static final byte[] convertTextualAddressToRawAddress(String IPAddress) {
        int first = IPAddress.indexOf(".");
        
        if (first == -1) {
            throw new NumberFormatException("Invalid Numerical IP Address: " + IPAddress);
        }

        int firstAfter = first + 1;
        int second = IPAddress.indexOf(".", firstAfter);
        int secondAfter = second + 1;
        int third = IPAddress.indexOf(".", secondAfter);

        return new byte[]{
            TABLE[Integer.parseInt(IPAddress.substring(0, first))],
            TABLE[Integer.parseInt(IPAddress.substring(firstAfter, second))],
            TABLE[Integer.parseInt(IPAddress.substring(secondAfter, third))],
            TABLE[Integer.parseInt(IPAddress.substring(third + 1))]
        };
    }

    public static final String convertRawAddressToTextualAddress(byte[] address) {
        int first = REVERSE_TABLE.get(address[0]);
        int second = REVERSE_TABLE.get(address[1]);
        int third = REVERSE_TABLE.get(address[2]);
        int fourth = REVERSE_TABLE.get(address[3]);

        StringBuilder builder = new StringBuilder(
                3 + getNumberOfDigits(first)
                + getNumberOfDigits(second)
                + getNumberOfDigits(third)
                + getNumberOfDigits(fourth));

        builder.append(first).append(".");
        builder.append(second).append(".");
        builder.append(third).append(".");
        builder.append(fourth);

        return builder.toString();
    }

    //could keep the used threads in memory and ask them to run again
    public static Iterator<TextSocket> getReachableSockets(ServerFrame parent, String subnetText) {
        out.println("Created: " + CREATED + " Memory Hits: " + MEMORY_HITS);
        GarbageCollectorThread garbageCollector = new GarbageCollectorThread(parent);
        garbageCollector.start();
        
        //Use previous ConnectionTesters
        if (!CONNECTORS.isEmpty()) {
            System.out.println("Using: " + CONNECTORS.size() + " previous testers.");
            System.out.println("Thread count: " + THREADS.size());
            for (Iterator<ConnectionTester> it = CONNECTORS.iterator(); it.hasNext();) {
                ConnectionThread thread = new ConnectionThread(it.next());
                thread.start();
                THREADS.add(thread);
            }
        }
        else {
            final int blockSize = BLOCK_SIZE;
            String[] subnet = subnetText.split(Pattern.quote("."));
            System.out.println("Subnet: " + Arrays.toString(subnet));
            final int subnetLength = subnet.length;

            if (subnetLength <= 0 || subnetLength >= 4) {
                return Collections.emptyIterator();
            }

            switch (subnetLength) {
                case 1: {
                    byte[] bytes = new byte[4];
                    bytes[0] = TABLE[Integer.parseInt(subnet[0])];
                    for (int second = 0; second < blockSize; ++second) {
                        bytes[1] = TABLE[second];
                        for (int third = 0; third < blockSize; ++third) {
                            bytes[2] = TABLE[third];
                            for (int fourth = 0; fourth < blockSize; ++fourth) {
                                if (!parent.isEnabled()) {
                                    garbageCollector.terminate.set(true);
                                    THREADS.clear();
                                    return Collections.emptyIterator();
                                }
                                bytes[3] = TABLE[fourth];
                                ConnectionTester tester = new ConnectionTester(parent, bytes);
                                ConnectionThread thread = new ConnectionThread(tester);
                                thread.start();
                                CONNECTORS.add(tester);
                                THREADS.add(thread);
                            }
                        }
                    }
                    break;
                }
                case 2: {
                    byte[] bytes = new byte[4];
                    bytes[0] = TABLE[Integer.parseInt(subnet[0])];
                    bytes[1] = TABLE[Integer.parseInt(subnet[1])];
                    for (int third = 0; third < blockSize; ++third) {
                        bytes[2] = TABLE[third];
                        for (int fourth = 0; fourth < blockSize; ++fourth) {
                            if (!parent.isEnabled()) {
                                garbageCollector.terminate.set(true);
                                THREADS.clear();
                                return Collections.emptyIterator();
                            }
                            bytes[3] = TABLE[fourth];
                            ConnectionTester tester = new ConnectionTester(parent, bytes);
                            ConnectionThread thread = new ConnectionThread(tester);
                            thread.start();
                            CONNECTORS.add(tester);
                            THREADS.add(thread);
                        }
                    }
                    break;
                }
                default: {
                    byte[] bytes = new byte[4];
                    bytes[0] = TABLE[Integer.parseInt(subnet[0])];
                    bytes[1] = TABLE[Integer.parseInt(subnet[1])];
                    bytes[2] = TABLE[Integer.parseInt(subnet[2])];
                    for (int fourth = 0; fourth < blockSize; ++fourth) {
                        if (!parent.isEnabled()) {
                            garbageCollector.terminate.set(true);
                            THREADS.clear();
                            return Collections.emptyIterator();
                        }
                        bytes[3] = TABLE[fourth];
                        ConnectionTester tester = new ConnectionTester(parent, bytes);
                        ConnectionThread thread = new ConnectionThread(tester);
                        thread.start();
                        CONNECTORS.add(tester);
                        THREADS.add(thread);
                    }
                    break;
                }
            }
        }
        
        final int threadCount = THREADS.size();
        
        if (threadCount != CONNECTORS.size()) {
            throw new Error();
        }
        
        //Hold until all threads have finished processing
        HOLD:
        do {
            for (int index = 0; index < threadCount; ++index) {
                if (!parent.isEnabled()) {
                    garbageCollector.terminate.set(true);
                    THREADS.clear();
                    return Collections.emptyIterator();
                }
                if (THREADS.get(index).isAlive()) {
                    System.out.println("Waiting for all threads to finish.");
                    continue HOLD;
                }
            }
            //We have not found any active threads, so stop holding
            break;
        }
        while (true);
        
        garbageCollector.terminate.set(true);

        List<TextSocket> reachableSockets = new LinkedList<>();

        for (int index = 0; index < threadCount; ++index) {
            ConnectionTester tester = CONNECTORS.get(index);
            TextSocket socket = tester.getSocket();
            if (socket != null) {
                reachableSockets.add(socket);
                tester.reset();
            }
        }

        THREADS.clear();
        return reachableSockets.iterator();
    }

    public static int getNumberOfDigits(int num) {
        int count = 0;
        while (num > 0) {
            num /= 10;
            ++count;
        }
        return count;
    }
    
    public static void main(String... args) throws UnknownHostException {
        if (true) {
            byte[] bytes = new byte[4];
            for (int index = 0; index <= 255; ++index) {
                bytes[0] = TABLE[index];
                bytes[1] = TABLE[index];
                bytes[2] = TABLE[index];
                bytes[3] = TABLE[index];
                
                String original = Arrays.toString(bytes);
                String host = InetAddress.getByAddress(bytes).getHostAddress();
                
                String text = IPScanner.convertRawAddressToTextualAddress(bytes);
                String raw = Arrays.toString(IPScanner.convertTextualAddressToRawAddress(text));
                
                if (!original.equals(raw)) {
                    throw new Error();
                }
                else {
                    out.println("Original: " + original + " Raw: " + raw);
                }
                
                if (!host.equals(text)) {
                    throw new Error();
                }
                else {
                    out.println("Host: " + host + " Text: " + text);
                }
            }

            return;
        }
        InetAddress ip = InetAddress.getByName("0.255.255.0");


        byte[] bytes = ip.getAddress();
        List<Byte> list = new ArrayList<>();
        Map<Integer, Byte> map = new HashMap<>();
        for (byte b = Byte.MIN_VALUE; true; ++b) {
            if (list.contains(b)) {
                break;
            }
            list.add(b);
            //System.out.println("Byte: " + b + " Integer: " + (b & 0xFF));
            map.put((b & 0xFF), b);
        }
        
        Set<Integer> set = new TreeSet<>(map.keySet());
        out.println(set.size());
        for (int key : set) {
            out.println("table[" + key + "] = " + map.get(key) + "; ");
        }
        
        if (true) {
            return;
        }
        

        out.println();
        out.println(Byte.MIN_VALUE);
        out.println(Byte.MAX_VALUE);
        for (int b = -128; b <= 127; ++b) {
            byte bb = (byte) b;
            System.out.println(InetAddress.getByAddress(new byte[]{bb, bb, bb, bb}));
        }

        bytes = new byte[4];
        bytes[0] = Byte.parseByte("10");
        for (int second = -127; second < 128; ++second) {
            bytes[1] = (byte) second;
            for (int third = -127; third < 128; ++third) {
                bytes[2] = (byte) third;
                for (int fourth = -127; fourth < 128; ++fourth) {
                    bytes[3] = (byte) fourth;
                    out.println(InetAddress.getByAddress(bytes).getHostAddress());
                }
                out.println();
            }
            out.println();
        }
    }

    public static Iterator<String> getReachableHosts() {
        InetAddress localDevice;
        try {
            localDevice = InetAddress.getLocalHost();
        }
        catch (UnknownHostException ex) {
            ex.printStackTrace();
            return null;
        }
        NetworkInterface networkInterface;
        try {
            networkInterface = NetworkInterface.getByInetAddress(localDevice);
        }
        catch (SocketException ex) {
            ex.printStackTrace();
            return null;
        }
        List<InterfaceAddress> addresses = networkInterface.getInterfaceAddresses();
        if (addresses.isEmpty()) {
            return null;
        }
        int prefixLength = addresses.get(0).getNetworkPrefixLength();
        int[] subnetMask = convertPrefixLengthToIPv4Address(prefixLength);
        
        String localAddress = localDevice.getHostAddress();
        Iterator<String> subnetMaskIterator = getReachableHosts(localAddress, subnetMask[0] + "." + subnetMask[1] + "." + subnetMask[2]).iterator();
        
        byte[] localHostAddress = localDevice.getAddress();
        
        String[] split = localAddress.split(Pattern.quote("."));
        Iterator<String> large = getReachableHosts(localAddress, split[0]).iterator();
        
        return new IteratorWrapper<>(subnetMaskIterator, large);
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

    public static List<String> getReachableHosts(String localName, String subnetText) {
        String[] subnet = subnetText.split(Pattern.quote("."));
        subnet[1] = "230";
        System.out.println(Arrays.toString(subnet));
        final int subnetLength = subnet.length;
        
        boolean lengthIs2 = (subnetLength == 2);
        
        if (!(lengthIs2 || subnetLength == 3)) {
            return new ArrayList<>(0);
        }
        
        final int timeout = 1000;
        
        class ConnectionThread extends Thread {
            
            private final String remoteHost;
            private String remoteName;
            
            private ConnectionThread(String remoteAddress) {
                remoteHost = remoteAddress;
            }

            @Override
            public void run() {
                try {
                    InetAddress remoteAddress = InetAddress.getByName(remoteHost);
                    if (remoteAddress.isReachable(timeout)) {
                        remoteName = remoteAddress.getHostName();
                    }
                }
                catch (IOException ex) {

                }
            }
        }

        List<ConnectionThread> threads;

        if (lengthIs2) {
            threads = new ArrayList<>(255 * 255);
            String first = subnet[0];
            String second = subnet[1];
            //Create all threads in proper order
            for (int third = 1; third < 255; ++third) {
                for (int fourth = 1; fourth < 255; ++fourth) {
                    threads.add(new ConnectionThread(first + "." + second + "." + third + "." + fourth));
                }
            }
        }
        else {
            threads = new ArrayList<>(255);
            String first = subnet[0];
            String second = subnet[1];
            String third = subnet[2];
            //Create all threads in proper order
            for (int fourth = 1; fourth < 255; ++fourth) {
                threads.add(new ConnectionThread(first + "." + second + "." + third + "." + fourth));
            }
        }

        //Start all threads
        for (Iterator<ConnectionThread> it = threads.iterator(); it.hasNext();) {
            it.next().start();
        }

        //Hold until all threads have finishted processing
        HOLD:
        while (true) {
            for (Iterator<ConnectionThread> it = threads.iterator(); it.hasNext();) {
                if (it.next().isAlive()) { //If we find any thread that is still running, keep waiting
                    continue HOLD;
                }
            }
            //We have not found any active threads, so stop holding
            break;
        }

        int threadCount = threads.size();
        List<String> valid = new ArrayList<>(threadCount);

        for (int index = 0; index < threadCount; ++index) {
            ConnectionThread thread = threads.get(index);
            String reached = thread.remoteName;
            if (reached != null) {
                if (!localName.equals(thread.remoteHost)) {
                    valid.add(reached + " " + thread.remoteHost);
                }
            }
        }

        return valid;
    }
}

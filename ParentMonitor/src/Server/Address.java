package Server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

public final class Address implements Comparable<Address> {

    private static final byte[] LOOP_BACK_ADDRESS = {127, 0, 0, 1};
    private static final String LOOP_BACK = "127.0.0.1";

    private ByteArray address;
    private String textualAddress;

    //Used by IPScanner
    //takes a "raw" address, -128 to 127
    public Address(byte[] remoteAddress) {
        //if input address is localhost
        if (Arrays.equals(LOOP_BACK_ADDRESS, remoteAddress)) {
            try {
                address = new ByteArray(InetAddress.getLocalHost().getAddress());
            }
            catch (UnknownHostException ex) {
                ex.printStackTrace();
                throw new UncheckedIOException(ex); //pass exception upwards
            }
        }
        else {
            address = new ByteArray(remoteAddress);
        }
    }

    //Used by user
    public Address(String remoteHost) {
        if (LOOP_BACK.equals(remoteHost)) {
            remoteHost = "localhost";
            //allow NumberFormatException to be raised
        }
        try {
            address = new ByteArray(IPScanner.convertTextualAddressToRawAddress(remoteHost));
        }
        catch (NumberFormatException outer) {
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
                System.out.println("Remote Host (Raw): " + (address = new ByteArray(backup.getAddress())));
            }
            catch (IOException inner) {
                inner.printStackTrace();
            }
        }
    }

    public byte[] getAddress() {
        return address.getArray(); //shallow copy
    }

    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Address)) {
            return false;
        }
        return address.equals(((Address) obj).address);
    }

    @Override
    public int compareTo(Address other) {
        return address.compareTo(other.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public String toString() {
        //Avoid getfield opcode
        //Dont re calculate address
        String textAddress = textualAddress;
        if (textAddress != null) {
            return textAddress;
        }
        return textualAddress = IPScanner.convertRawAddressToTextualAddress(address.getArray());
    }

    public void destroy() {
        ByteArray addressReference = address;
        if (addressReference != null) {
            addressReference.destroy();
            address = null;
        }
        textualAddress = null;
    }

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
}
package Server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

//This class is not used anymore.
public final class Address implements Comparable<Address>, Recyclable {

    private static final byte[] LOOP_BACK_ADDRESS = {127, 0, 0, 1};
    private static final String LOOP_BACK = "127.0.0.1";

    private ByteArray address;

    //Used by NetworkScanner
    //takes a "raw" address, -128 to 127
    public Address(byte[] remoteAddress) {
        //if input address is localhost
        if (Arrays.equals(LOOP_BACK_ADDRESS, remoteAddress)) {
            try {
                address = new ByteArray(InetAddress.getLocalHost().getAddress());
            }
            catch (UnknownHostException ex) {
                ex.printStackTrace();
            }
        }
        else {
            address = new ByteArray(remoteAddress);
        }
    }

    //Used by user
    public Address(String remoteHost) {
        if (LOOP_BACK.equals(remoteHost)) {
            InetAddress localHost;
            try {
                localHost = InetAddress.getLocalHost();
            }
            catch (UnknownHostException ex) {
                ex.printStackTrace();
                return;
            }
            address = ByteArray.getLightWrapper(localHost.getAddress());
        }
        else {
            byte[] rawAddress = NetworkScanner.convertTextualAddressToRawAddress(remoteHost);
            if (rawAddress == null) {
                try {
                    System.out.println("Remote Host (Input): " + remoteHost);
                    //When user types in something like "Jackie's Computer", "JavaComputer"
                    //We want to convert to actual address, resolve by name.
                    InetAddress backupAddress = InetAddress.getByName(remoteHost);
                    if (Network.isLocalAddress(backupAddress)) {
                        //If remoteHost = "localhost" then the backup will fail
                        //the backup would return 127.0.0.1, which we do not want
                        System.out.println("This is a local address.");
                        backupAddress = InetAddress.getLocalHost(); //remake the backup again
                        //this new backup will return the correct network address of the local computer
                    }
                    rawAddress = backupAddress.getAddress();
                }
                catch (UnknownHostException ex) {
                    ex.printStackTrace();
                    return;
                }
            }
            address = ByteArray.getLightWrapper(rawAddress);
        }
    }

    public boolean isValid() {
        return address != null;
    }

    @Override
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
        return NetworkScanner.convertRawAddressToTextualAddress(address.getInternalArray());
    }

    @Override
    public void recycle() {
        ByteArray addressReference = address;
        if (addressReference != null) {
            addressReference.recycle();
            address = null;
        }
    }
}
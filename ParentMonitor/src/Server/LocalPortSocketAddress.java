package Server;

import java.net.InetSocketAddress;

public final class LocalPortSocketAddress {
    
    private InetSocketAddress socketAddress;
    private Integer localPort;
    
    public LocalPortSocketAddress(InetSocketAddress address) {
        socketAddress = address;
    }
    
    public LocalPortSocketAddress(InetSocketAddress address, int port) {
        socketAddress = address;
        localPort = port;
    }
    
    public void setAddress(InetSocketAddress address) {
        socketAddress = address;
    }
    
    public void setLocalPort(int port) {
        localPort = null;
        localPort = port;
    }
    
    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }
    
    public Integer getLocalPort() {
        return localPort;
    }
}
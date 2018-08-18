package Server;

import Util.StreamCloser;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public final class TextSocket implements Closeable {
    
    private Socket socket;
    private BufferedReader recieveText;
    private PrintWriter sendText;
    
    public TextSocket(String host, int port) {
        try {
            socket = new Socket(host, port);
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
       
        try {
            //stream to get text from client
            recieveText = new BufferedReader(new InputStreamReader(socket.getInputStream())); //EXCEPTION LINE
        }
        catch (IOException ex) {
            StreamCloser.close(socket);
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
            ex.printStackTrace();
        }
    }

    public boolean isActive() {
        return (socket == null || recieveText == null || sendText == null) ? false : !socket.isClosed();
    }

    @Override
    public void close() {
        StreamCloser.close(socket);
        StreamCloser.close(recieveText);
        StreamCloser.close(sendText);
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
}
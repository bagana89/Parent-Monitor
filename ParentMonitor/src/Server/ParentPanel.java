package Server;

import static Server.Network.CLIENT_EXITED;
import static Server.Network.CLOSE_CLIENT;
import Util.StreamCloser;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JEditorPane;
import javax.swing.JTabbedPane;

public final class ParentPanel extends JPanel implements Runnable {
 
    private JTabbedPane tabs;
    
    private ClientPanel client;
    private TextPanel text;
    
    //stream variables
    private Socket textConnection;
    private BufferedReader recieveText;
    private PrintWriter sendText;
    
    private Map<String, String> clientEnvironment = new HashMap<>();
    private String clientName;
    
    private boolean terminated = false;
    
    @SuppressWarnings("CallToThreadStartDuringObjectConstruction")
    public ParentPanel(JTabbedPane parentTabs, Socket clientTextConnection, Socket clientImageConnection) {
        tabs = parentTabs;
        
        final BufferedReader textInput;
        final PrintWriter textOutput;
       
        try {
            //stream to get text from client
            textInput = new BufferedReader(new InputStreamReader(clientTextConnection.getInputStream())); //EXCEPTION LINE
        }
        catch (IOException ex) {
            StreamCloser.close(clientTextConnection);
            ex.printStackTrace();
            return;
        }
        
        try {
            //stream to send text to client 
            textOutput = new PrintWriter(clientTextConnection.getOutputStream(), true); //EXCEPTION LINE
        }
        catch (IOException ex) {
            //Close all streams above
            StreamCloser.close(clientTextConnection);
            StreamCloser.close(textInput);
            ex.printStackTrace();
            return;
        }

        //NOT SAFE YET, MUST PERFORM INITIAL READ
        try {
            //contains all client data
            //Device Name
            //Device OS
            //Device User Name
            //Device SystemEnv
            String[] data = textInput.readLine().split(Pattern.quote("|"));
            System.out.println("Reading System Data from: " + clientTextConnection.toString());
            for (String pair : data) {
                String[] entry = pair.split(Pattern.quote("->"));
                System.out.println("Read: " + entry[0] + " -> " + entry[1]);
                clientEnvironment.put(entry[0], entry[1]);
            }
        }
        catch (IOException ex) {
            //Close all streams above
            StreamCloser.close(clientTextConnection);
            StreamCloser.close(textInput);
            StreamCloser.close(textOutput);
            ex.printStackTrace();
            return;
        }
        
        //internally checks streams
        client = new ClientPanel(clientName = clientEnvironment.get("USERNAME"), clientImageConnection);
        
        //All streams can be safely set up now
        textConnection = clientTextConnection;
        recieveText = textInput;
        sendText = textOutput;
        
        super.setToolTipText("Client Username: " + clientName);
        super.setLayout(new GridLayout(1, 2));
        super.add(client);
        super.add(text = new TextPanel());
        super.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        text.setOutput(textOutput);
        
        new Thread(this, clientName + " Main Client Manager Thread").start();
    }

    @Override
    public String getName() {
        return clientName;
    }
    
    public String getClientSystemInfo() {
        StringBuilder builder = new StringBuilder("Client System Information:\n");
        for (Iterator<Map.Entry<String, String>> it = clientEnvironment.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, String> entry = it.next();
            builder.append(entry.getKey()).append(" -> ").append(entry.getValue());
            if (it.hasNext()) {
                builder.append("\n");
            }
            else {
                break;
            }
        }
        return builder.toString();
    }
    
    @Override
    public Component[] getComponents() {
        return new Component[]{client, text};
    }
    
    public void saveCurrentShot() {
        client.saveCurrentShot();
    }
    
    public void toggleUpdate() {
        client.toggleUpdate();
    }
    
    public void terminate(boolean serverClosedClient) {
        if (terminated) {
            return;
        }

        terminated = true;
        tabs.remove(this);
        tabs = null;

        if (serverClosedClient) { //indicates wheather the server intentionally closed the client
            sendText.println(CLOSE_CLIENT);
        }

        StreamCloser.close(textConnection);
        StreamCloser.close(recieveText);
        StreamCloser.close(sendText);
        
        textConnection = null;
        recieveText = null;
        sendText = null;
        
        client.destroy();
        client = null;
        text = null;
    }

    @Override
    public final void run() {
        while (!terminated) {
            if (client == null) {
                System.out.println("Client already nullified.");
                return;
            }
            
            if (textConnection == null) {
                System.out.println("Connection already nullified.");
                return;
            }
            
            if (textConnection.isClosed()) {
                System.out.println("Connection already closed.");
                return;
            }
            
            /**
             * BIG BUG AHEAD!!!              *
             * BUG: If the server requests an image from the client and suddenly
             * stops requesting, the last message will be sent as "Server
             * Request: Screenshot"
             *
             * This causes the BufferedReader readLine() below to hold until the
             * client texts back in a conversation message or a closing
             * notification. This means that until the client communicates back,
             * and the updateScreenShot flag has been set to false, this method
             * (and Thread) will be frozen!!! Yet we still have to use
             * readLine() for other client activities, so we may have to resort
             * to using a thread safe stack here...
             *
             * Fortunately there are no other significant problems save this
             * one.
             *
             * SOLUTIONS: - A stupid workaround would be to instead send a
             * message to the client that the server is not requesting
             * screenshots, and the client would promptly respond back,
             * preventing a holdup here.
             *
             * A side effect of this naive solution is that this application may
             * take up unnecessary network operation space
             *
             * For now we will use the naive solution, since I'd rather not make
             * a concurrent stack and keep accessing it repeatedly.
             * 
             * UPDATE: The naive solution works, but is unstable if the host
             * and the client are constantly chatting, which clogs up the 
             * BufferedReader
             * 
             * Therefore, we will implement a 2-Socket solution. This has been done.
             */
            
            //The following line of code is based off the naive solution
            //sendText.println(updateScreenShot.get() ? REQUEST_IMAGE : STOP_REQUEST_IMAGE);

            String fromClient;

            try {
                fromClient = recieveText.readLine();
            }
            catch (IOException ex) {
                ex.printStackTrace();
                continue;
            }

            if (CLIENT_EXITED.equals(fromClient)) {
                terminate(false);
                return;
            }
            else {
                JEditorPane chat = text.getEditorPane();
                String previousText = chat.getText();
                fromClient = clientName + ": " + fromClient;
                chat.setText(previousText.isEmpty() ? fromClient : previousText + "\n" + fromClient);
            }
        }
    }
}
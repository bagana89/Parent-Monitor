package Client;

import static Client.Network.CLIENT_EXITED;
import static Client.Network.CLOSE_CLIENT;
import static Client.Network.ENCODING;
import static Client.Network.IMAGE_BUFFER_SIZE;
import static Client.Network.IMAGE_PORT;
import static Client.Network.PNG;
import static Client.Network.PUNISH;
import static Client.Network.SHA_1;
import static Client.Network.TEXT_PORT;
import Util.StreamCloser;
import java.awt.AWTException;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

//The person being "spied on" waits for the parent to connect to it
public class ClientFrame extends JFrame implements Runnable {

    public static final Rectangle SCREEN_BOUNDS = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

    //stream variables
    private ServerSocket textServer;
    private Socket parentConnection;
    private BufferedReader textInput;
    private PrintWriter textOutput;

    private ImageSenderWorkerThread worker;

    private ImageIcon icon;

    private JScrollPane scrollPane;
    private JEditorPane editorPane;
    private JTextField textField;
    private JButton button;

    //private final List<String> lines = new ArrayList<>();
    //private final Object linesLock = new Object(); //alternative to Collections synch methods
    
    //Initialize components first, then streams
    @SuppressWarnings({"Convert2Lambda", "CallToThreadStartDuringObjectConstruction"})
    public ClientFrame() {
        try {
            textServer = new ServerSocket(TEXT_PORT);
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return;
        }
        
        //Attempt to load the icon image.
        try {
            BufferedImage iconImage = ImageIO.read(ClientFrame.class.getResourceAsStream("/Images/Eye.jpg"));
            icon = new ImageIcon(iconImage);
            super.setIconImage(iconImage);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }

        super.setTitle("Parent Monitor - Client");

        JMenuBar menuBar = new JMenuBar();

        JMenuItem info = new JMenuItem("View Address");
        info.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                info.setArmed(true);
                info.repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                info.setArmed(false);
                info.repaint();
            }
        });
        info.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                String hostAddress;
                String hostName;
                try {
                    InetAddress localHost = InetAddress.getLocalHost();
                    hostAddress = localHost.getHostAddress();
                    hostName = localHost.getHostName();
                }
                catch (UnknownHostException ex) {
                    hostAddress = hostName = "Unresolved";
                }
                JOptionPane.showMessageDialog(ClientFrame.this, "The following may be used by a server to connect to you via LAN:\nIPv4 Address: " + hostAddress + "\nDevice Name: " + hostName, "Connection Address", JOptionPane.INFORMATION_MESSAGE, icon);
            }
        });

        menuBar.add(info);

        super.setJMenuBar(menuBar);

        scrollPane = new JScrollPane();
        
        editorPane = new JEditorPane();
        editorPane.setEditable(false);
        
        scrollPane.setViewportView(editorPane);

        textField = new JTextField("Waiting for a server to connect...");
        textField.setEditable(false);
        textField.setToolTipText("Enter Message");
        textField.addFocusListener(new FocusListener() {

            private boolean beenFocused = false;

            @Override
            public void focusGained(FocusEvent event) {
                if (textField.isEditable()) {
                    if (!beenFocused) {
                        textField.setText("");
                    }
                    else {
                        beenFocused = true;
                    }
                }
            }

            @Override
            public void focusLost(FocusEvent event) {

            }
        });

        textField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent event) {

            }

            @Override
            public void keyPressed(KeyEvent event) {
                PrintWriter textOutputReference = textOutput;
                JTextField textFieldReference = textField;
                JEditorPane editorPaneReference = editorPane;
                if (textOutputReference != null) {
                    if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                        String message = textFieldReference.getText().trim();
                        textOutputReference.println(message); //send message to parent
                        message = "You: " + message;
                        textFieldReference.setText("");
                        String previousText = editorPaneReference.getText();
                        editorPaneReference.setText(previousText.isEmpty() ? message : previousText + "\n" + message);
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent event) {

            }
        });

        button = new JButton();
        button.setText("Send Message");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                PrintWriter textOutputReference = textOutput;
                JTextField textFieldReference = textField;
                JEditorPane editorPaneReference = editorPane;
                if (textOutputReference != null) {
                    String message = textFieldReference.getText().trim();
                    textOutputReference.println(message); //send message to parent
                    message = "You: " + message;
                    textFieldReference.setText("");
                    String previousText = editorPaneReference.getText();
                    editorPaneReference.setText(previousText.isEmpty() ? message : previousText + "\n" + message);
                }
                else {
                    JOptionPane.showMessageDialog(ClientFrame.this, "Error: Cannot send messages, no server has connected with you yet.", "Not Connected", JOptionPane.ERROR_MESSAGE, icon);
                }
            }
        });

        GridBagLayout layout = new GridBagLayout();
        layout.columnWidths = new int[]{10, 0, 65, 5, 0};
        layout.rowHeights = new int[]{10, 0, 30, 5, 0};
        layout.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, 1.0E-4};
        layout.rowWeights = new double[]{0.0, 1.0, 0.0, 0.0, 1.0E-4};

        Container contentPane = super.getContentPane();
        contentPane.setLayout(layout);

        contentPane.add(scrollPane, new GridBagConstraints(1, 1, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 5), 0, 0));

        contentPane.add(textField, new GridBagConstraints(1, 2, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 5), 0, 0));

        contentPane.add(button, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 5), 0, 0));

        final int width = SCREEN_BOUNDS.width / 2;
        final int height = SCREEN_BOUNDS.height;
        final Dimension frameArea = new Dimension(width, height);
        super.setSize(frameArea);
        super.setPreferredSize(frameArea);
        super.setMinimumSize(frameArea);
        super.setMaximumSize(frameArea);
        super.setLocation((SCREEN_BOUNDS.width / 2) - (SCREEN_BOUNDS.width / 4), (SCREEN_BOUNDS.height / 2) - (height / 2));

        super.setVisible(true);
        super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        super.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                PrintWriter textOutputReference = textOutput;
                if (JOptionPane.showConfirmDialog(ClientFrame.this,
                        "Are you sure you want to exit?", "Exit?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, icon) == JOptionPane.YES_OPTION) {
                    //notify parent
                    if (textOutputReference != null) {
                        textOutputReference.println(CLIENT_EXITED);
                    }
                    dispose();
                }
                setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            }
        });

        try {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    System.out.println("System Shutdown Detected!");
                }
            });
        }
        catch (IllegalStateException | SecurityException ex) {
            ex.printStackTrace();
        }

        new Thread(this, "Server Listener Thread").start();
        (worker = new ImageSenderWorkerThread(IMAGE_PORT)).start();
    }

    @Override
    public void dispose() {
        //load all instance variables first
        final ServerSocket textServerReference = textServer;
        final Socket parentConnectionReference = parentConnection;
        final BufferedReader textInputReference = textInput;
        final PrintWriter textOutputReference = textOutput;

        final ImageSenderWorkerThread workerReference = worker;

        final JScrollPane scrollPaneReference = scrollPane;
        final JEditorPane editorPaneReference = editorPane;
        final JTextField textFieldReference = textField;
        final JButton buttonReference = button;
        
        if (!super.isEnabled()) {
            System.out.println("Frame already disposed.");
            return;
        }
        
        //Destroy frame resources
        super.setEnabled(false);
        super.setVisible(false);
        super.dispose(); //Destroy the frame
        super.removeAll(); //remove all sub-components
        
        //Close connections
        StreamCloser.close(textServerReference);
        StreamCloser.close(parentConnectionReference);
        StreamCloser.close(textInputReference);
        StreamCloser.close(textOutputReference);
        
        //Close worker thread
        StreamCloser.close(workerReference);
        
        textServer = null;
        parentConnection = null;
        textInput = null;
        textOutput = null;
        
        worker = null;

        icon = null;

        if (scrollPaneReference != null) {
            scrollPaneReference.setEnabled(false);
            scrollPaneReference.removeAll();
            scrollPane = null;
        }

        if (editorPaneReference != null) {
            editorPaneReference.setEnabled(false);
            editorPaneReference.removeAll();
            editorPane = null;
        }

        if (textFieldReference != null) {
            textFieldReference.setEnabled(false);
            textFieldReference.removeAll();
            textField = null;
        }

        if (buttonReference != null) {  
            buttonReference.setEnabled(false);
            buttonReference.removeAll();
            button = null;
        }
        
        System.out.println("Frame disposal complete.");
        //System.exit(0); //Allow threads to clean up
    }

    private class ImageSenderWorkerThread extends Thread implements Closeable {

        private ServerSocket screenshotServer;
        private Socket screenshotConnection;
        private DataOutputStream screenshotSender;

        private ImageSenderWorkerThread(int port) {
            super("Image Sender Worker Thread");
            try {
                screenshotServer = new ServerSocket(port);
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public final void run() {
            //Wait until a stable connection has been found
            ServerSocket screenshotServerReference = screenshotServer;

            if (screenshotServerReference == null) {
                dispose();
                System.out.println(getName() + " Exiting.");
                return;
            }

            Socket screenshotConnectionReference;

            while (true) {
                if (screenshotServerReference.isClosed()) {
                    //No streams have been setup
                    dispose();
                    System.out.println(getName() + " Exiting.");
                    return;
                }
                try {
                    screenshotConnectionReference = screenshotServerReference.accept();
                    break;
                }
                catch (IOException ex) {
                    StreamCloser.close(screenshotServerReference);
                    dispose();
                    System.out.println(getName() + " Exiting.");
                    ex.printStackTrace();
                    return;
                }
            }
            
            DataOutputStream screenshotSenderReference;

            try {
                screenshotSenderReference = new DataOutputStream(new BufferedOutputStream(screenshotConnectionReference.getOutputStream(), IMAGE_BUFFER_SIZE));
            }
            catch (IOException ex) {
                StreamCloser.close(screenshotServerReference);
                StreamCloser.close(screenshotConnectionReference);
                dispose();
                System.out.println(getName() + " Exiting.");
                ex.printStackTrace();
                return;
            }

            //Use local variables as much as possible here, performance critical!!!
            final Robot screenCapturer;
            try {
                screenCapturer = new Robot();
            }
            catch (AWTException ex) {
                StreamCloser.close(screenshotServerReference);
                StreamCloser.close(screenshotConnectionReference);
                StreamCloser.close(screenshotSenderReference);
                dispose();
                System.out.println(getName() + " Exiting.");
                ex.printStackTrace();   
                return;
            }
            
            screenshotServer = screenshotServerReference;
            screenshotConnection = screenshotConnectionReference;
            screenshotSender = screenshotSenderReference;
            
            final Rectangle deviceScreenSize = SCREEN_BOUNDS;
            final String screenshotImageFormat = PNG;

            //Technically, the server no longer "requests" for an image, its always
            //demands for it, and we always send, except when the server is dealing with
            //multiple clients, clients that are repainted do not update screens
            for (ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream(IMAGE_BUFFER_SIZE); screenshotSender != null; byteBuffer.reset()) {
                try {
                    ImageIO.write(screenCapturer.createScreenCapture(deviceScreenSize), screenshotImageFormat, byteBuffer);
                    screenshotSenderReference.writeInt(byteBuffer.size());
                    byteBuffer.writeTo(screenshotSenderReference); //write directly to the output stream, no slow copy
                    screenshotSenderReference.flush();
                }
                catch (IOException | NullPointerException | IllegalArgumentException ex) {
                    ex.printStackTrace();
                    /*
                     * Do not break out of loop in case of error!!! An error
                     * will happen if server closes the lid, the image will not
                     * be received by the server (causing exception to be thrown
                     * here), but when server reopens lid, the image stream will
                     * be dead while other operations (text communication) are
                     * still active. So we keep the loop alive, and keep trying
                     * to send images to server. When the text communication
                     * thread is dead, this loop will terminate, then this
                     * thread will end as well.
                    
                     * EDIT: When server closes lid, the socket itself is destroyed.
                     * We would have to create a new socket to reconnect the image stream.
                     * Keep break for now.
                     */
                    break;

                    /*
                    //Close this socket's resources
                    close();
                    System.out.println(getName() + " Exiting.");
                    worker = null;
                    //Restart the connection
                    worker = new ImageSenderWorkerThread(IMAGE_PORT);
                    return;
                     */
                }
            }

            close();
            System.out.println(getName() + " Exiting.");
        }

        @Override
        public final void close() {
            ServerSocket screenshotServerReference = screenshotServer;
            Socket screenshotConnectionReference = screenshotConnection;
            DataOutputStream screenshotSenderReference = screenshotSender;
            
            StreamCloser.close(screenshotServerReference);
            StreamCloser.close(screenshotConnectionReference);
            StreamCloser.close(screenshotSenderReference);

            screenshotServer = null;
            screenshotConnection = null;
            screenshotSender = null;
        }
    }
    
    @Override
    public final void run() {
        final ServerSocket textServerReference = textServer;
        final JEditorPane editorReference = editorPane;
        final JTextField textFieldReference = textField;
        
        if (textServerReference == null) {
            dispose();
            System.out.println("Closing without connection."); //Happens when a client closes without a connection
            System.out.println("Server Listener Thread Exiting.");
            return;
        }
    
        final MessageEncoder security;

        try {
            InetAddress localDeviceNetworkAddress = InetAddress.getLocalHost();
            byte[] securityKey = localDeviceNetworkAddress.getHostAddress().getBytes(ENCODING);
            securityKey = SHA_1.digest(securityKey);
            securityKey = Arrays.copyOf(securityKey, 16); // use only first 128 bits
            security = new MessageEncoder(securityKey, "AES");
        }
        catch (UnknownHostException ex) {
            dispose();
            ex.printStackTrace();
            return;
        }
        
        final BufferedReader textInputReference;
        final PrintWriter textOutputReference;
    
        //Note: We wait for the parent to connect to us, so use only 1 connection.

        //Loop until all streams have been properly set up.
        //We do not support reconnecting, once server has told client to shutdown, we do so.
        while (true) {
            if (textServerReference.isClosed()) {
                dispose();
                System.out.println("Closing without connection."); //Happens when a client closes without a connection
                System.out.println("Server Listener Thread Exiting.");
                return;
            }

            final Socket parentConnectionTest;
            final BufferedReader textInputTest;
            final PrintWriter textOutputTest;

            try {
                parentConnectionTest = textServerReference.accept();
            }
            catch (IOException ex) {
                ex.printStackTrace();
                continue;
            }

            try {
                textInputTest = new BufferedReader(new InputStreamReader(parentConnectionTest.getInputStream())) {
                    @Override
                    public String readLine() throws IOException {
                        String line = super.readLine();
                        return line == null ? null : security.decode(line);
                    }
                };
            }
            catch (IOException ex) {
                StreamCloser.close(parentConnectionTest);
                ex.printStackTrace();
                continue;
            }

            try {
                textOutputTest = new PrintWriter(new BufferedWriter(new OutputStreamWriter(parentConnectionTest.getOutputStream(), StandardCharsets.UTF_8)), true) {
                    @Override
                    public void println(String line) {
                        super.println(security.encode(line));
                    }
                };
            }
            catch (IOException ex) {
                StreamCloser.close(parentConnectionTest);
                StreamCloser.close(textInputTest);
                ex.printStackTrace();
                continue;
            }

            //All streams have been properly set up, so initialize here 
            parentConnection = parentConnectionTest;
            textInput = textInputReference = textInputTest;
            textOutput = textOutputReference = textOutputTest;

            break;
        }

        {
            //Once streams have been set up
            //Send Infomation to server immediately for validation
            StringBuilder buffer = new StringBuilder(2000);

            for (Iterator<Map.Entry<String, String>> it = System.getenv().entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, String> entry = it.next();
                buffer.append(Network.encode(entry.getKey())).append("->").append(Network.encode(entry.getValue()));
                if (it.hasNext()) {
                    buffer.append("|");
                }
                else {
                    break;
                }
            }

            //send client infomation to server
            textOutputReference.println(buffer.toString());
            buffer.setLength(0); //clear the buffer
        }

        //after all infomation has been forwarded, enable chatting
        textFieldReference.setText("Enter Message...");
        textFieldReference.setEditable(true);

        boolean punished = false;

        while (textInput != null) {
            try {
                String fromServer = textInputReference.readLine();
                //server request that we close
                if (CLOSE_CLIENT.equals(fromServer)) {
                    System.out.println(CLOSE_CLIENT);
                    if (super.isVisible()) {
                        JOptionPane.showMessageDialog(ClientFrame.this, "The server has disconnected you.", "System Closing", JOptionPane.WARNING_MESSAGE, icon);
                    }
                    else {
                        System.out.println("Server disconnect dialog should not be displayed, frame is disposed already.");
                    }
                    //This message is slightly misleading when server is exiting normally
                    break;
                }
                if (PUNISH.equals(fromServer)) {
                    punished = true;
                    break;
                }
                if (fromServer != null) {
                    String previousText = editorReference.getText();
                    editorReference.setText(previousText.isEmpty() ? "Server: " + fromServer : previousText + "\nServer: " + fromServer);
                }
                else {
                    break;
                }
            }
            catch (IOException ex) {
                ex.printStackTrace();
                if (super.isVisible()) {
                    JOptionPane.showMessageDialog(ClientFrame.this, "The server has shutdown.", "System Closing", JOptionPane.WARNING_MESSAGE, icon);
                }
                else {
                    System.out.println("Server shutdown dialog should not be displayed, frame is disposed already.");
                }
                break;
            }
        }

        dispose();
        System.out.println("Server Listener Thread Exiting.");
        
        if (punished) {
            System.out.println("Server has punished you!");
            shutdown();
        }
    }

    private void shutdown() {
        try {
            String operatingSystem = System.getProperty("os.name");
            if (operatingSystem != null) {
                if (operatingSystem.contains("Linux") || operatingSystem.contains("Mac OS X")) {
                    Runtime.getRuntime().exec("shutdown -h now");
                }
                else if (operatingSystem.contains("Windows")) {
                    Runtime.getRuntime().exec("shutdown.exe -s -t 0");
                }
            }
        }
        catch (SecurityException | IOException ex) {
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public static void main(String[] args) {
        ImageIO.setUseCache(false);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }
        new ClientFrame();
    }
}
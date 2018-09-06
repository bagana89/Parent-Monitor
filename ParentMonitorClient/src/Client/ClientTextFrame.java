package Client;

import static Client.Network.CLIENT_EXITED;
import static Client.Network.CLOSE_CLIENT;
import static Client.Network.IMAGE_BUFFER_SIZE;
import static Client.Network.IMAGE_PORT;
import static Client.Network.PNG;
import static Client.Network.PUNISH;
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
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
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
public class ClientTextFrame extends JFrame implements Runnable {

    public static final Rectangle SCREEN_BOUNDS = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

    //stream variables
    private ServerSocket textServer;
    private Socket parentConnection;
    private BufferedReader textInput;
    private PrintWriter textOutput;

    private ImageSenderWorkerThread worker;
    private Robot robot;

    private ImageIcon icon;

    private JScrollPane scroll;
    private JEditorPane editor;
    private JTextField field;
    private JButton button;

    //private final List<String> lines = new ArrayList<>();
    //private final Object linesLock = new Object(); //alternative to Collections synch methods
    
    //Initialize components first, then streams
    @SuppressWarnings({"Convert2Lambda", "CallToThreadStartDuringObjectConstruction"})
    public ClientTextFrame() {
        try {
            BufferedImage iconImage = ImageIO.read(ClientTextFrame.class.getResourceAsStream("/Images/Eye.jpg"));
            icon = new ImageIcon(iconImage);
            super.setIconImage(iconImage);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }

        super.setTitle("Parent Monitor - Client");

        JMenuBar menuBar = new JMenuBar();

        /* //No chat saving since it's pointless and can easily be manipulated
        JMenu file = new JMenu("File");

        JMenuItem save = new JMenuItem("Save Chat History");
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (editor.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(ClientTextFrame.this, "Error: There is no message history to save.", "No Message History", JOptionPane.ERROR_MESSAGE, icon);
                    return;
                }
                JFileChooser save = new JFileChooser();
                save.setFileSelectionMode(JFileChooser.FILES_ONLY);
                save.addChoosableFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file == null ? false : file.getAbsolutePath().toLowerCase().endsWith(".txt");
                    }

                    @Override
                    public String getDescription() {
                        return ".txt Files";
                    }
                });
                if (save.showSaveDialog(ClientTextFrame.this) == JFileChooser.APPROVE_OPTION) {
                    File chosen = save.getSelectedFile();
                    if (chosen == null) {
                        return;
                    }
                    String name = chosen.getName();
                    String fileName = chosen.getAbsolutePath();
                    if (name == null || fileName == null) {
                        return;
                    }
                    if (name.isEmpty() || fileName.isEmpty()) {
                        return;
                    }
                    if (!name.toLowerCase().endsWith(".txt") || !fileName.toLowerCase().endsWith(".txt")) {
                        JOptionPane.showMessageDialog(ClientTextFrame.this, "Error: The file to save or overwrite: " + Quotes.surroundWithDoubleQuotes(fileName) + "\nis not a \".txt\" file.", "Invalid File Type", JOptionPane.ERROR_MESSAGE, icon);
                        return;
                    }
                    if (chosen.exists()) {
                        if (JOptionPane.showConfirmDialog(ClientTextFrame.this, "The file: " + Quotes.surroundWithDoubleQuotes(fileName) + " already exists, do you wish to overwrite it?", "Overwrite?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, icon) == JOptionPane.YES_OPTION) {
                            PrintWriter writer;
                            try {
                                writer = new PrintWriter(fileName, "UTF-8");
                                writer.println("Original Location: " + Quotes.surroundWithDoubleQuotes(fileName));
                                writer.println("Date Created: " + new Date());
                                synchronized (linesLock) { //only 1 thread can access linesLock synch block at any time
                                    for (Iterator<String> it = lines.iterator(); it.hasNext();) {
                                        String line = it.next();
                                        if (it.hasNext()) {
                                            writer.println(line);
                                        }
                                        else {
                                            writer.print(line);
                                            break;
                                        }
                                    }
                                }
                            }
                            catch (FileNotFoundException | UnsupportedEncodingException ex) {
                                JOptionPane.showMessageDialog(ClientTextFrame.this, "Error: " + fileName + " could not be saved.", "Save Error", JOptionPane.ERROR_MESSAGE, icon);
                                ex.printStackTrace();
                                return;
                            }
                            writer.close();
                            JOptionPane.showMessageDialog(ClientTextFrame.this, fileName + " has been saved successfully.", "Save Successful", JOptionPane.INFORMATION_MESSAGE, icon);
                        }
                        else {
                            return;
                        }
                    }
                    else if (JOptionPane.showConfirmDialog(ClientTextFrame.this, "Are you sure you want to save the new file: " + Quotes.surroundWithDoubleQuotes(fileName) + "?", "Save File?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, icon) == JOptionPane.YES_OPTION) {
                        PrintWriter writer;
                        try {
                            writer = new PrintWriter(fileName, "UTF-8");
                            writer.println("Original Location: " + Quotes.surroundWithDoubleQuotes(fileName));
                            writer.println("Date Created: " + new Date());
                            synchronized (linesLock) { //only 1 thread can access linesLock synch block at any time
                                for (Iterator<String> it = lines.iterator(); it.hasNext();) {
                                    String line = it.next();
                                    if (it.hasNext()) {
                                        writer.println(line);
                                    }
                                    else {
                                        writer.print(line);
                                        break;
                                    }
                                }
                            }
                        }
                        catch (FileNotFoundException | UnsupportedEncodingException ex) {
                            JOptionPane.showMessageDialog(ClientTextFrame.this, "Error: " + fileName + " could not be saved.", "Save Error", JOptionPane.ERROR_MESSAGE, icon);
                            ex.printStackTrace();
                            return;
                        }
                        writer.close();
                        JOptionPane.showMessageDialog(ClientTextFrame.this, fileName + " has been saved successfully.", "Save Successful", JOptionPane.INFORMATION_MESSAGE, icon);
                    }
                }
            }
        });

        file.add(save);
         */
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
                JOptionPane.showMessageDialog(ClientTextFrame.this, "The following may be used by a server to connect to you via LAN:\nIPv4 Address: " + hostAddress + "\nDevice Name: " + hostName, "Connection Address", JOptionPane.INFORMATION_MESSAGE, icon);
            }
        });

        //menuBar.add(file);
        menuBar.add(info);

        super.setJMenuBar(menuBar);

        scroll = new JScrollPane();
        
        editor = new JEditorPane();
        editor.setEditable(false);
        
        scroll.setViewportView(editor);

        field = new JTextField("Waiting for a server to connect...");
        field.setEditable(false);
        field.setToolTipText("Enter Message");
        field.addFocusListener(new FocusListener() {

            private boolean beenFocused = false;

            @Override
            public void focusGained(FocusEvent event) {
                if (field.isEditable()) {
                    if (!beenFocused) {
                        field.setText("");
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

        field.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent event) {

            }

            @Override
            public void keyPressed(KeyEvent event) {
                if (textOutput != null) {
                    if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                        String message = field.getText().trim();
                        textOutput.println(message); //send message to parent
                        message = "You: " + message;
                        //synchronized (linesLock) {
                        //lines.add(message);
                        //}
                        field.setText("");
                        String previousText = editor.getText();
                        editor.setText(previousText.isEmpty() ? message : previousText + "\n" + message);
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
                if (textOutput != null) {
                    String message = field.getText().trim();
                    textOutput.println(message); //send message to parent
                    message = "You: " + message;
                    //synchronized (linesLock) {
                    //lines.add(message);
                    //}
                    field.setText("");
                    String previousText = editor.getText();
                    editor.setText(previousText.isEmpty() ? message : previousText + "\n" + message);
                }
                else {
                    JOptionPane.showMessageDialog(ClientTextFrame.this, "Error: Cannot send messages, no server has connected with you yet.", "Not Connected", JOptionPane.ERROR_MESSAGE, icon);
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

        contentPane.add(scroll, new GridBagConstraints(1, 1, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 5, 5), 0, 0));

        contentPane.add(field, new GridBagConstraints(1, 2, 2, 1, 0.0, 0.0,
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
                if (JOptionPane.showConfirmDialog(ClientTextFrame.this,
                        "Are you sure you want to exit?", "Exit?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, icon) == JOptionPane.YES_OPTION) {
                    //notify parent
                    if (textOutput != null) {
                        textOutput.println(CLIENT_EXITED);
                    }
                    dispose();
                }
                setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            }
        });

        try {
            textServer = new ServerSocket(TEXT_PORT);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }

        try {
            robot = new Robot();
        }
        catch (AWTException ex) {
            ex.printStackTrace();
        }
        
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
        if (!isVisible()) {
            return;
        }
        
        super.setEnabled(false);
        super.setVisible(false);
        super.dispose(); //Destroy the frame
        removeAll(); //remove all sub-components

        StreamCloser.close(textServer);
        StreamCloser.close(parentConnection);
        StreamCloser.close(textInput);
        StreamCloser.close(textOutput);

        textServer = null;
        parentConnection = null;
        textInput = null;
        textOutput = null;

        worker.close();
        worker = null;

        icon = null;

        robot = null;

        scroll.removeAll();
        scroll = null;

        editor.removeAll();
        editor = null;

        field.removeAll();
        field = null;

        button.removeAll();
        button = null;

        System.out.println("Exiting");
        //System.exit(0); //Allow threads to clean up
    }

    private class ImageSenderWorkerThread extends Thread {

        private ServerSocket imageServer;
        private Socket imageChannel;
        private DataOutputStream imageSender;

        private ImageSenderWorkerThread(int port) {
            super("Image Sender Worker Thread");
            try {
                imageServer = new ServerSocket(port);
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public final void run() {
            //Wait until a stable connection has been found
            while (true) {
                if (imageServer == null || imageServer.isClosed()) {
                    //No streams have been setup 
                    System.out.println(getName() + " Exiting.");
                    return;
                }
                try {
                    imageChannel = imageServer.accept();
                    break;
                }
                catch (IOException ex) {
                    StreamCloser.close(imageServer);
                    imageServer = null;
                    System.out.println(getName() + " Exiting.");
                    ex.printStackTrace();
                    return;
                }
            }

            try {
                imageSender = new DataOutputStream(new BufferedOutputStream(imageChannel.getOutputStream(), IMAGE_BUFFER_SIZE));
            }
            catch (IOException ex) {
                StreamCloser.close(imageServer);
                StreamCloser.close(imageChannel);
                imageServer = null;
                imageChannel = null;
                System.out.println(getName() + " Exiting.");
                ex.printStackTrace();
                return;
            }

            //Use local variables as much as possible here, performance critical!!!
            final Robot screenCapturer = robot;
            final Rectangle screenSize = SCREEN_BOUNDS;
            final String format = PNG;
            final DataOutputStream sendImage = imageSender;

            //Technically, the server no longer "requests" for an image, its always
            //demands for it, and we always send, except when the server is dealing with
            //multiple clients, clients that are repainted do not update screens
            for (ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream(IMAGE_BUFFER_SIZE); imageSender != null; byteBuffer.reset()) {
                try {
                    ImageIO.write(screenCapturer.createScreenCapture(screenSize), format, byteBuffer);
                    sendImage.writeInt(byteBuffer.size());
                    byteBuffer.writeTo(sendImage); //write directly to the output stream, no slow copy
                    sendImage.flush();
                }
                catch (IOException | NullPointerException | IllegalArgumentException ex) {
                    ex.printStackTrace();
                    break;
                }
            }

            close();
            System.out.println(getName() + " Exiting.");
        }

        private void close() {
            StreamCloser.close(imageServer);
            StreamCloser.close(imageChannel);
            StreamCloser.close(imageSender);

            imageServer = null;
            imageChannel = null;
            imageSender = null;
        }
    }

    @Override
    public final void run() {
        //Note: We wait for parent to connect to us, so only 1 connection

        //loop until all streams have been properly set up
        //We do not support reconnecting, once server has told client
        //to shutdown, we do so
        while (true) {
            if (textServer == null || textServer.isClosed()) {
                System.out.println("Premature."); //Happens when a client closes without a connection
                System.out.println("Server Listener Thread Exiting.");
                dispose();
                return;
            }

            final Socket parentConnectionTest;
            final BufferedReader textInputTest;
            final PrintWriter textOutputTest;

            try {
                parentConnectionTest = textServer.accept();
            }
            catch (IOException ex) {
                ex.printStackTrace();
                continue;
            }

            try {
                textInputTest = new BufferedReader(new InputStreamReader(parentConnectionTest.getInputStream()));
            }
            catch (IOException ex) {
                StreamCloser.close(parentConnectionTest);
                ex.printStackTrace();
                continue;
            }

            try {
                textOutputTest = new PrintWriter(parentConnectionTest.getOutputStream(), true);
            }
            catch (IOException ex) {
                StreamCloser.close(parentConnectionTest);
                StreamCloser.close(textInputTest);
                ex.printStackTrace();
                continue;
            }

            //All streams have been properly set up, so initialize here 
            parentConnection = parentConnectionTest;
            textInput = textInputTest;
            textOutput = textOutputTest;

            break;
        }

        {
            //Once streams have been set up
            //Send Infomation to server immediately for validation
            StringBuilder buffer = new StringBuilder(2000);

            for (Iterator<Map.Entry<String, String>> it = System.getenv().entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, String> entry = it.next();
                buffer.append(entry.getKey()).append('➾').append(entry.getValue());
                if (it.hasNext()) {
                    buffer.append('∥');
                }
                else {
                    break;
                }
            }

            //send client infomation to server
            textOutput.println(buffer.toString());
            buffer.setLength(0); //clear the buffer
        }

        //after all infomation has been forwarded, enable chatting
        field.setText("Enter Message...");
        field.setEditable(true);

        boolean punished = false;

        while (textInput != null) {
            try {
                String fromServer = textInput.readLine();
                //server request that we close
                if (CLOSE_CLIENT.equals(fromServer)) {
                    System.out.println(CLOSE_CLIENT);
                    if (isVisible()) {
                        JOptionPane.showMessageDialog(ClientTextFrame.this, "The server has disconnected you.", "System Closing", JOptionPane.WARNING_MESSAGE, icon);
                    }
                    else {
                        System.out.println("Server disconnect dialog should not be displayed, frame is disposed already.");
                    }
                    //This message is slightly misleading when server is exiting normally
                    break;
                }
                else if (PUNISH.equals(fromServer)) {
                    punished = true;
                    break;
                }
                else if (fromServer != null) {
                    String previousText = editor.getText();
                    //synchronized (linesLock) {
                    //lines.add(fromServer = "Server: " + fromServer);
                    //}
                    editor.setText(previousText.isEmpty() ? "Server: " + fromServer : previousText + "\nServer: " + fromServer);
                }
                else {
                    break;
                }
            }
            catch (IOException ex) {
                ex.printStackTrace();
                if (isVisible()) {
                    JOptionPane.showMessageDialog(ClientTextFrame.this, "The server has shutdown.", "System Closing", JOptionPane.WARNING_MESSAGE, icon);
                }
                else {
                    System.out.println("Server shutdown dialog should not be displayed, frame is disposed already.");
                }
                break;
            }
        }

        System.out.println("Server Listener Thread Exiting.");
        dispose();
        
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
        new ClientTextFrame();
    }
}
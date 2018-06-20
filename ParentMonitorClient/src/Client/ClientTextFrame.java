package Client;

import static Client.Network.CLIENT_EXITED;
import static Client.Network.CLOSE_CLIENT;
import static Client.Network.IMAGE_PORT;
import static Client.Network.REQUEST_IMAGE;
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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

//The person being "spied on" waits for the parent to connect to it
public class ClientTextFrame extends JFrame implements Runnable {

    private final ImageIcon icon = new ImageIcon();
    
    private final JScrollPane scroll;
    private final JEditorPane editor;
    private final JTextField field;
    private final JButton button;
    
    //stream variables
    private ServerSocket textServer;
    private Socket parentConnection;
    private BufferedReader textInput;
    private PrintWriter textOutput;
    
    private ImageSenderWorkerThread worker;
    
    private Robot robot;
   
    public static final Rectangle SCREEN_BOUNDS = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

    @SuppressWarnings({"Convert2Lambda", "CallToThreadStartDuringObjectConstruction"})
    public ClientTextFrame() {
        try {
            BufferedImage iconImage = ImageIO.read(ClientTextFrame.class.getResourceAsStream("/Images/Eye.jpg"));
            icon.setImage(iconImage);
            super.setIconImage(iconImage);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        
        super.setTitle("Parent Monitor - Client");
       
        scroll = new JScrollPane();
        editor = new JEditorPane();
        
        field = new JTextField("Enter Message...");
        field.setEditable(false);
        field.setToolTipText("Enter Message");
        field.addFocusListener(new FocusListener() {

            private boolean beenFocused = false;

            @Override
            public void focusGained(FocusEvent fe) {
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
            public void focusLost(FocusEvent fe) {

            }
        });

        field.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent ke) {

            }

            @Override
            public void keyPressed(KeyEvent ke) {
                if (textOutput != null) {
                    if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                        String message = field.getText().trim();
                        textOutput.println(message); //send message to parent
                        field.setText("");
                        String previousText = editor.getText();
                        editor.setText(previousText.isEmpty() ? "You: " + message : previousText + "\nYou: " + message);
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent ke) {

            }
        });

        button = new JButton();
        button.setText("Send Message");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (textOutput != null) {
                    String message = field.getText().trim();
                    textOutput.println(message); //send message to parent
                    field.setText("");
                    String previousText = editor.getText();
                    editor.setText(previousText.isEmpty() ? "You: " + message : previousText + "\nYou: " + message);
                }
            }
        });

        Container contentPane = super.getContentPane();

        contentPane.setLayout(new GridBagLayout());
        ((GridBagLayout) contentPane.getLayout()).columnWidths = new int[]{10, 0, 65, 5, 0};
        ((GridBagLayout) contentPane.getLayout()).rowHeights = new int[]{10, 0, 30, 5, 0};
        ((GridBagLayout) contentPane.getLayout()).columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, 1.0E-4};
        ((GridBagLayout) contentPane.getLayout()).rowWeights = new double[]{0.0, 1.0, 0.0, 0.0, 1.0E-4};

        editor.setText("");
        editor.setEditable(false);
        scroll.setViewportView(editor);

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
            public void windowClosing(WindowEvent we) {
                if (JOptionPane.showConfirmDialog(ClientTextFrame.this,
                        "Are you sure you want to exit?", "Exit?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, icon) == JOptionPane.YES_OPTION) { //not equal to the exitFromNetworkGame option, keep playing
                    //notify parent
                    if (textOutput != null) {
                        textOutput.println(CLIENT_EXITED);
                    }
                    exit();
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
        
        new Thread(this, "Socket Thread").start();
        (worker = new ImageSenderWorkerThread(IMAGE_PORT)).start();
    }
    
    public void exit() {
        StreamCloser.close(textServer);
        StreamCloser.close(parentConnection);
        StreamCloser.close(textInput);
        StreamCloser.close(textOutput);
        worker.exit();
        System.out.println("Exiting");
        System.exit(0);
    }
    
    private class ImageSenderWorkerThread extends Thread {
    
        private ServerSocket imageServer;
        private Socket serverImageChannel;
        private BufferedReader serverImageRequestReader;
        private DataOutputStream serverImageRequestSender;
        
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
                try {
                    serverImageChannel = imageServer.accept();
                    break;
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            try {
                serverImageRequestReader = new BufferedReader(new InputStreamReader(serverImageChannel.getInputStream()));
            }
            catch (IOException ex) {
                StreamCloser.close(imageServer);
                StreamCloser.close(serverImageChannel);
                ex.printStackTrace();
                return;
            }
            
            try {
                serverImageRequestSender = new DataOutputStream(serverImageChannel.getOutputStream());
            }
            catch (IOException ex) {
                StreamCloser.close(imageServer);
                StreamCloser.close(serverImageChannel);
                StreamCloser.close(serverImageRequestReader);
                ex.printStackTrace();
                return;
            }

            while (true) {
                try {
                    String requestFromServer = serverImageRequestReader.readLine();
                    if (REQUEST_IMAGE.equals(requestFromServer)) {
                        //send image
                        BufferedImage screenShot = robot.createScreenCapture(SCREEN_BOUNDS);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(screenShot, "png", baos);
                        serverImageRequestSender.writeInt(baos.size());
                        serverImageRequestSender.write(baos.toByteArray());
                        serverImageRequestSender.flush();
                    }
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                    return;
                }
            }
        }
        
        private void exit() {
            StreamCloser.close(imageServer);
            StreamCloser.close(serverImageChannel);
            StreamCloser.close(serverImageRequestReader);
            StreamCloser.close(serverImageRequestSender);
            
            imageServer = null;
            serverImageChannel = null;
            serverImageRequestReader = null;
            serverImageRequestSender = null;
        }
    }
    
    @Override
    public final void run() {
        //Note: We wait for parent to connect to us, so only 1 connection

        //loop until all streams have been properly set up
        while (true) {
            if (textServer.isClosed()) {
                exit();
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

            //All streams have been properly set up, so initialize outside loop
            parentConnection = parentConnectionTest;
            textInput = textInputTest;
            textOutput = textOutputTest;
            
            break;
        }

        //Once streams have been set up
        //Send Infomation to server immediately for validation
        Map<String, String> systemEnvironment = System.getenv();
        StringBuilder buffer = new StringBuilder();

        for (Iterator<Map.Entry<String, String>> it = systemEnvironment.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, String> entry = it.next();
            buffer.append(entry.getKey()).append("->").append(entry.getValue());
            if (it.hasNext()) {
                buffer.append("|");
            }
            else {
                break;
            }
        }
        
        //send client infomation to server
        textOutput.println(buffer.toString());
        buffer.setLength(0);
        
        //after all infomation has been forwarded, enable chatting
        field.setEditable(true);

        while (true) {
            try {
                String fromServer = textInput.readLine();
                //server request that we close
                if (CLOSE_CLIENT.equals(fromServer)) {
                    JOptionPane.showMessageDialog(ClientTextFrame.this, "The server has disconnected you.", "System Closing", JOptionPane.WARNING_MESSAGE, icon);
                    exit(); //does not return normally
                }
                else {
                    String previousText = editor.getText();
                    fromServer = "Server: " + fromServer;
                    editor.setText(previousText.isEmpty() ? fromServer : previousText + "\n" + fromServer);
                }
            }
            catch (IOException ex) {
                JOptionPane.showMessageDialog(ClientTextFrame.this, "The server has shutdown.", "System Closing", JOptionPane.WARNING_MESSAGE, icon);
                exit();
            }
        }
    }
    
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public static void main(String[] args) {
        new ClientTextFrame();
    }
}
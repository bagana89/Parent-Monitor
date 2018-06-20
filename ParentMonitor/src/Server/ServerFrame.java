package Server;

import static Server.Network.IMAGE_PORT;
import static Server.Network.TEXT_PORT;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ServerFrame extends JFrame {

    private final JMenuBar menuBar;
    private final JPopupMenu popup;
    private final JTabbedPane tabs;

    public static final GraphicsEnvironment GRAPHICS_ENVIRONMENT = GraphicsEnvironment.getLocalGraphicsEnvironment();
    public static final GraphicsConfiguration GRAPHICS_CONFIGURATION = GRAPHICS_ENVIRONMENT.getDefaultScreenDevice().getDefaultConfiguration();
    public static final Rectangle SCREEN_BOUNDS = GRAPHICS_ENVIRONMENT.getMaximumWindowBounds();

    private final ImageIcon icon = new ImageIcon();

    private ParentPanel selected;

    @SuppressWarnings("Convert2Lambda")
    public ServerFrame() {
        super("Parent Monitor - Server");
        super.setLayout(new BorderLayout());

        try {
            BufferedImage iconImage = ImageIO.read(ServerFrame.class.getResourceAsStream("/Images/Eye.jpg"));
            icon.setImage(iconImage);
            super.setIconImage(iconImage);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }

        final int width = SCREEN_BOUNDS.width; //was once half screen size
        final int height = SCREEN_BOUNDS.height;
        final Dimension frameArea = new Dimension(width, height);

        super.setSize(frameArea);
        super.setPreferredSize(frameArea);
        super.setMinimumSize(frameArea);
        super.setMaximumSize(frameArea);
        //super.setLocation((SCREEN_BOUNDS.width / 2) - (SCREEN_BOUNDS.width / 4), (SCREEN_BOUNDS.height / 2) - (height / 2));
        super.setResizable(false);
        super.setVisible(true);

        super.setJMenuBar(menuBar = new JMenuBar());

        JMenu file = new JMenu("File");
        JMenuItem closeAll = new JMenuItem("Close All");
        JMenuItem addClient = new JMenuItem("Add Client");

        addClient.addActionListener(new ActionListener() {
            @Override
            @SuppressWarnings("Convert2Lambda")
            public void actionPerformed(ActionEvent e) {
                String host = (String) JOptionPane.showInputDialog(ServerFrame.this, "Enter the client IP-Address or IPv4 Address:", "Enter Client Address", JOptionPane.QUESTION_MESSAGE, icon, null, null);
                if (host == null || host.isEmpty()) {
                    return;
                }
                /*
                String port = (String) JOptionPane.showInputDialog(ServerFrame.this, "Enter a port number to connect to the client: " + Quotes.surroundWithDoubleQuotes(host), "Enter Port Number", JOptionPane.QUESTION_MESSAGE, icon, null, null);
                if (port == null || port.isEmpty()) {
                    return;
                }
                 */
                //try {
                /*
                    int portNumber = Integer.parseInt(port);
                    if (portNumber <= 0) {
                        JOptionPane.showMessageDialog(ServerFrame.this, "Error: The specified port number: " + Quotes.surroundWithDoubleQuotes(port) + " is less the minimum valid port number: " + Quotes.surroundWithDoubleQuotes("1") + ".", "Invalid Port Number", JOptionPane.ERROR_MESSAGE, icon);
                        return;
                    }
                    else if (portNumber > Character.MAX_VALUE) {
                        JOptionPane.showMessageDialog(ServerFrame.this, "Error: The specified port number: " + Quotes.surroundWithDoubleQuotes(port) + " is over the maximum valid port number: " + Quotes.surroundWithDoubleQuotes("65535") + ".", "Invalid Port Number", JOptionPane.ERROR_MESSAGE, icon);
                        return;
                    }
                 */
                try {
                    Socket connectToClientText = new Socket(host, TEXT_PORT);
                    Socket connectToClientImage = new Socket(host, IMAGE_PORT);
                    ParentPanel panel = new ParentPanel(tabs, connectToClientText, connectToClientImage);

                    //each client gets a dedicated mouselistener so that the listener can cater
                    //to it directly
                    panel.addMouseListener(new MouseListener() {

                        @Override
                        public void mouseClicked(MouseEvent e) {

                        }

                        @Override
                        public void mousePressed(MouseEvent e) {

                        }

                        @Override
                        public void mouseReleased(MouseEvent e) {
                            if (e.isPopupTrigger()) {
                                popup.show(e.getComponent(), e.getX(), e.getY());
                            }
                        }

                        @Override
                        public void mouseEntered(MouseEvent e) {

                        }

                        @Override
                        public void mouseExited(MouseEvent e) {

                        }
                    });
                    tabs.addTab(panel.getName(), panel);
                }
                catch (IOException ex) {
                    JOptionPane.showMessageDialog(ServerFrame.this, "Error: Could not connect to " + host + ".", "Connection Failed", JOptionPane.ERROR_MESSAGE, icon);
                    //ex.printStackTrace(); //No need to do this here, stackTrace will be printed by ParentPanel
                    //in case of socket failure, no need to display stacktrace
                    //JOptionPane.showMessageDialog(ServerFrame.this, "Error: Could not connect to " + host + " on port: " + portNumber + "." , "Connection Failed", JOptionPane.ERROR_MESSAGE, icon);
                }
                //catch (NumberFormatException ex) {
                //JOptionPane.showMessageDialog(ServerFrame.this, "Error: Invalid port number: " + Quotes.surroundWithDoubleQuotes(port) + ".", "Invalid Port Number", JOptionPane.ERROR_MESSAGE, icon);
                //}
            }
        });

        menuBar.add(file);
        menuBar.add(closeAll);
        menuBar.add(addClient);

        Container contentPane = super.getContentPane();
        contentPane.setBackground(Color.WHITE);

        tabs = new JTabbedPane();
        tabs.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                popup.setVisible(false);
                selected = (ParentPanel) tabs.getSelectedComponent();
            }
        });
        super.add(tabs, BorderLayout.CENTER);

        System.out.println("Frame Bounds: " + super.getBounds());

        //add Popup Menu here
        popup = new JPopupMenu();

        JMenuItem close = new JMenuItem("Disconnect Client");
        JMenuItem saveScreenShot = new JMenuItem("Capture Screenshot");
        JMenuItem toggleLiveRefresh = new JMenuItem("Toggle Refresh");
        JMenuItem clientInfo = new JMenuItem("Client Info (Advanced)");

        //action listener for popups
        @SuppressWarnings("Convert2Lambda")
        ActionListener popupListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Object source = event.getSource();
                //popup responds to current selected client
                ParentPanel current = selected;
                if (current != null) {
                    if (source == close) {
                        //notify client we are closing their connection
                        current.terminate(true);
                        //tabs.remove(current); //No need to do this here, terminate takes care of it already
                    }
                    else if (source == saveScreenShot) {
                        current.saveCurrentShot();
                    }
                    else if (source == toggleLiveRefresh) {
                        current.toggleUpdate();
                    }
                    else {
                        TextFrame.showTextFrame(ServerFrame.this, icon.getImage(), "Client: [" + current.getName() + "] Updated: " + new Date(), current.getClientSystemInfo());
                    }
                }
            }
        };

        close.addActionListener(popupListener);
        saveScreenShot.addActionListener(popupListener);
        toggleLiveRefresh.addActionListener(popupListener);
        clientInfo.addActionListener(popupListener);

        close.setHorizontalTextPosition(JMenuItem.RIGHT);
        saveScreenShot.setHorizontalTextPosition(JMenuItem.RIGHT);
        toggleLiveRefresh.setHorizontalTextPosition(JMenuItem.RIGHT);
        clientInfo.setHorizontalTextPosition(JMenuItem.RIGHT);

        popup.add(close);
        popup.add(toggleLiveRefresh);
        popup.addSeparator();
        popup.add(clientInfo);

        super.setVisible(true);
        super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        System.out.println("Menu Bar Bounds: " + menuBar.getBounds());
        System.out.println("Content Pane Bounds: " + contentPane.getSize());
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public static void main(String[] args) throws IOException {
        new ServerFrame();
    }
}
package Server;

import static Server.Network.IMAGE_PORT;
import static Server.Network.TEXT_PORT;
import Util.Quotes;
import Util.ThreadSafeBoolean;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class ServerFrame extends JFrame {
    
    public static final GraphicsEnvironment GRAPHICS_ENVIRONMENT = GraphicsEnvironment.getLocalGraphicsEnvironment();
    public static final GraphicsConfiguration GRAPHICS_CONFIGURATION = GRAPHICS_ENVIRONMENT.getDefaultScreenDevice().getDefaultConfiguration();
    public static final Rectangle SCREEN_BOUNDS = GRAPHICS_ENVIRONMENT.getMaximumWindowBounds();

    private JPopupMenu popup;
    private JMenuBar menuBar;
    private JTabbedPane tabs;

    private ImageIcon icon = new ImageIcon();
    
    //These frames must be disposed when closing!
    private TextFrame connectionHistory; 
    private ScreenShotDisplayer master;

    private ParentPanel selected;
    
    private ImageBank bank = new ImageBank();

    @SuppressWarnings("Convert2Lambda")
    public ServerFrame() {
        super("Parent Monitor - Server");
        super.setLayout(new BorderLayout());

        final int width = SCREEN_BOUNDS.width; //was once half screen size
        final int height = SCREEN_BOUNDS.height;
        final Dimension frameArea = new Dimension(width, height);

        super.setSize(frameArea);
        super.setPreferredSize(frameArea);
        super.setMinimumSize(frameArea);
        super.setMaximumSize(frameArea);
        super.setResizable(false);
        
        //add Popup Menu here
        popup = new JPopupMenu();

        JMenuItem close = new JMenuItem("Disconnect Client");
        JMenuItem saveScreenShot = new JMenuItem("Capture Screenshot");
        JMenuItem showSavedScreenShots = new JMenuItem("Show Captured Screenshots");
        //JMenuItem toggleLiveRefresh = new JMenuItem("Toggle Refresh");
        JMenuItem clientInfo = new JMenuItem("Client System Info (Advanced)");
        JMenuItem punish = new JMenuItem("Shutdown Client");

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
                        String clientName = current.getName();
                        if (JOptionPane.showConfirmDialog(ServerFrame.this,
                                "Are you sure you want to disconnect " + clientName + "?", "Disconnect " + clientName + "?",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE, icon) == JOptionPane.YES_OPTION) {
                            current.close(true);
                        }
                        //tabs.remove(current); //No need to do this here, terminate takes care of it already
                    }
                    else if (source == saveScreenShot) {
                        current.saveCurrentShot(bank, master);
                    }
                    else if (source == showSavedScreenShots) {
                        if (current.takenScreenShot()) {
                            current.showSavedScreenShots();
                        }
                        else {
                            JOptionPane.showMessageDialog(ServerFrame.this, "Error: There are no captured screenshots from " + current.getName() + " to show.", "Invalid Operation", JOptionPane.ERROR_MESSAGE, icon);
                        }
                    }
                    /*
                    else if (source == toggleLiveRefresh) {
                        current.toggleUpdate();
                    }
                     */
                    else if (source == clientInfo) {
                        current.showInfo();
                    }
                    else {
                        String clientName = current.getName();
                        if (JOptionPane.showConfirmDialog(ServerFrame.this,
                                "Are you sure you want to shutdown " + clientName + "'s device?", "Shutdown " + clientName + "?",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE, icon) == JOptionPane.YES_OPTION) {
                            current.punish();
                        }
                    }
                }
            }
        };

        close.addActionListener(popupListener);
        saveScreenShot.addActionListener(popupListener);
        showSavedScreenShots.addActionListener(popupListener);
        //toggleLiveRefresh.addActionListener(popupListener);
        clientInfo.addActionListener(popupListener);
        punish.addActionListener(popupListener);

        close.setHorizontalTextPosition(JMenuItem.RIGHT);
        saveScreenShot.setHorizontalTextPosition(JMenuItem.RIGHT);
        showSavedScreenShots.setHorizontalTextPosition(JMenuItem.RIGHT);
        //toggleLiveRefresh.setHorizontalTextPosition(JMenuItem.RIGHT);
        clientInfo.setHorizontalTextPosition(JMenuItem.RIGHT);
        punish.setHorizontalTextPosition(JMenuItem.RIGHT);

        popup.add(close);
        //popup.add(toggleLiveRefresh);
        popup.add(saveScreenShot);
        popup.add(showSavedScreenShots);
        popup.addSeparator();
        popup.add(clientInfo);
        popup.add(punish);

        super.setJMenuBar(menuBar = new JMenuBar());
        
        super.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                if (JOptionPane.showConfirmDialog(ServerFrame.this,
                        "Are you sure you want to exit?\nWarning: All unsaved data will be erased.", "Exit?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE, icon) == JOptionPane.YES_OPTION) {
                    dispose();
                }
                setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            }
        });

        JMenu file = new JMenu("File");
        
        JMenuItem saveImages = new JMenuItem("Save Screenshots");
        saveImages.addActionListener(new ActionListener() {
            
            private final ThreadSafeBoolean saving = new ThreadSafeBoolean(false);
            
            //Prevent more than 1 saving operation from happening at the same time
            @Override
            public void actionPerformed(ActionEvent event) {
                if (bank.isEmpty()) {
                    JOptionPane.showMessageDialog(ServerFrame.this, "Error: There are no captured screenshots to save.", "Invalid Operation", JOptionPane.ERROR_MESSAGE, icon);
                    return;
                }
                if (saving.get()) {
                    return;
                }
                saving.set(true);
                JFileChooser save = new JFileChooser();
                save.setDialogType(JFileChooser.SAVE_DIALOG);
                save.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (save.showSaveDialog(ServerFrame.this) == JFileChooser.APPROVE_OPTION) {
                    File directoryChosen = save.getSelectedFile();
                    if (directoryChosen.isDirectory()) {
                        bank.writeToFiles(ServerFrame.this, icon, directoryChosen, saving);
                        //Resets saving to false, re-allowing access to Save again
                    }
                    else {
                        JOptionPane.showMessageDialog(ServerFrame.this, "Error: " + directoryChosen + " is not a directory!", "Invalid Directory", JOptionPane.ERROR_MESSAGE, icon);
                    }
                }
                else {
                    saving.set(false);
                }
            }
        });

        /*
        JMenuItem saveChatHistory = new JMenuItem("Save Chat History");
        saveChatHistory.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                
            }
        });
         */
        
        file.add(saveImages);
        //file.add(saveChatHistory);

        JMenuItem addClient = new JMenuItem("Connect Client");
        addClient.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                addClient.setArmed(true);
                addClient.repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                addClient.setArmed(false);
                addClient.repaint();
            }
        });
        addClient.addActionListener(new ActionListener() {
            @Override
            @SuppressWarnings("Convert2Lambda")
            public void actionPerformed(ActionEvent event) {
                String host = (String) JOptionPane.showInputDialog(ServerFrame.this, "Enter the client IP-Address or IPv4 Address:", "Enter Client Address", JOptionPane.QUESTION_MESSAGE, icon, null, null);
                if (host == null || host.isEmpty()) {
                    return;
                }
                if (selected != null) {
                    selected.setSelected(true);
                }
                TextSocket connectToClientText = new TextSocket(host, TEXT_PORT);
                if (!connectToClientText.isActive()) {
                    JOptionPane.showMessageDialog(ServerFrame.this, "Error: Could not connect to " + host + ".", "Connection Failed", JOptionPane.ERROR_MESSAGE, icon);
                    return;
                }
                ImageSocket connectToClientImage = new ImageSocket(host, IMAGE_PORT);
                if (!connectToClientImage.isActive()) {
                    JOptionPane.showMessageDialog(ServerFrame.this, "Error: Could not connect to " + host + ".", "Connection Failed", JOptionPane.ERROR_MESSAGE, icon); 
                    return;
                }
                try {
                    Date connectedTime = new Date();
                    ParentPanel panel = new ParentPanel(ServerFrame.this, connectToClientText, connectToClientImage);
                    //each client gets a dedicated mouselistener so that the listener can cater
                    //to it directly
                    panel.getSplitPane().addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseReleased(MouseEvent event) {
                            if (event.isPopupTrigger()) {
                                if (selected != null) {
                                    String clientName = selected.getName();
                                    close.setText("Disconnect " + clientName);
                                    clientInfo.setText(clientName + " System Info (Advanced)");
                                    punish.setText("Shutdown " + clientName);
                                    //No need to reset text to original
                                }
                                popup.show(event.getComponent(), event.getX(), event.getY());
                            }
                        }
                    });
                    String clientName = panel.getName();
                    tabs.addTab(clientName, panel);
                    connectionHistory.addText(clientName + " connected to Server: " + connectedTime);
                }
                catch (IOException ex) {
                    JOptionPane.showMessageDialog(ServerFrame.this, "Error: Could not connect to " + host + ".", "Connection Failed", JOptionPane.ERROR_MESSAGE, icon);
                    //No need to print stack trace here, it will be printed by ParentPanel
                    //in case of socket failure, no need to display stacktrace
                }
            }
        });
        
        JMenuItem closeAll = new JMenuItem("Disconnect All Clients");
        closeAll.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                closeAll.setArmed(true);
                closeAll.repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                closeAll.setArmed(false);
                closeAll.repaint();
            }
        });
        closeAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                int tabCount = tabs.getTabCount();
                if (tabCount == 0) {
                    JOptionPane.showMessageDialog(ServerFrame.this, "Error: There are no clients to disconnect.", "Invalid Operation", JOptionPane.ERROR_MESSAGE, icon);
                    return;
                }
                if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(ServerFrame.this, "Are you sure you want to disconnect all clients?", "Disconnect All?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, icon)) {
                    for (int index = tabCount - 1; index >= 0; --index) { //Loop backwards to prevent this index problem
                       ((ParentPanel) tabs.getComponentAt(index)).close(true); //fail loudly, cast error should NEVER happen
                        //close will reduce size of tabs by 1, ParentPanel holds a reference
                        //close also adds text to connection history
                    }
                }
            }
        });

        JMenuItem history = new JMenuItem("Connection History");
        history.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                history.setArmed(true);
                history.repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                history.setArmed(false);
                history.repaint();
            }
        });
        history.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (connectionHistory.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(ServerFrame.this, "Error: No clients have been connected to this server yet.\nSee: " + Quotes.surroundWithDoubleQuotes("Connect Client") + " to connect a client to this server.", "Invalid Operation", JOptionPane.ERROR_MESSAGE, icon);
                    return;
                }
                connectionHistory.setVisible(true);
            }
        });
        
        JMenuItem allShots = new JMenuItem("View All Screenshots");
        allShots.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                allShots.setArmed(true);
                allShots.repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                allShots.setArmed(false);
                allShots.repaint();
            }
        });
        allShots.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (bank.isEmpty()) {
                    JOptionPane.showMessageDialog(ServerFrame.this, "Error: There are no captured screenshots to show.", "Invalid Operation", JOptionPane.ERROR_MESSAGE, icon);
                    return;
                }
                master.setVisible(true);
            }
        });
        
        menuBar.add(file);
        menuBar.add(addClient);
        menuBar.add(closeAll);
        menuBar.add(history);
        menuBar.add(allShots);

        Container contentPane = super.getContentPane();
        contentPane.setBackground(Color.WHITE);

        tabs = new JTabbedPane();
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent event) {
                popup.setVisible(false);
                if (selected != null) {
                    selected.setSelected(false);
                }
                selected = (ParentPanel) tabs.getSelectedComponent(); 
                //cast does not throw NPE, the current selected component may be null
                //espically when we remove the only client left, causing a state change
                if (selected != null) {
                    selected.setSelected(true);
                }
            }
        });
        super.add(tabs, BorderLayout.CENTER);

        System.out.println("Frame Bounds: " + super.getBounds());
        
        try {
            BufferedImage iconImage = ImageIO.read(ServerFrame.class.getResourceAsStream("/Images/Eye.jpg"));
            icon.setImage(iconImage);
            connectionHistory = new TextFrame(this, iconImage, "Connection History", "", true);
            super.setIconImage(iconImage);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        
        master = new ScreenShotDisplayer(this, "All Captured Screenshots");
        
        super.setVisible(true);
        super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        System.out.println("Menu Bar Bounds: " + menuBar.getBounds());
        System.out.println("Content Pane Bounds: " + contentPane.getSize());

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
    }
    
    public JTabbedPane getTabs() {
        return tabs;
    }

    public Icon getIcon() {
        return icon;
    }
    
    public TextFrame getConnectionHistoryFrame() {
        return connectionHistory;
    }
    
    @Override
    public void dispose() {
        super.setEnabled(false);
        super.dispose(); //Make window invisible
        
        //First, disconnect all clients as we are closing
        for (int index = tabs.getTabCount() - 1; index >= 0; --index) { //Loop backwards to prevent this index problem
            ((ParentPanel) tabs.getComponentAt(index)).close(true); //fail loudly, cast error should NEVER happen
            //close will reduce size of tabs by 1, ParentPanel holds a reference
        }
        
        removeAll(); //remove all sub-components
        
        popup.removeAll();
        menuBar.removeAll();
        tabs.removeAll();
        
        popup = null;
        menuBar = null;
        tabs = null;
        
        icon = null;
        
        connectionHistory.dispose();
        connectionHistory = null;
        
        master.dispose();
        master = null;
        
        selected = null;
        bank = null;
         
        //System.exit(0); //Allow threads to clean up
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public static void main(String[] args) {
        ImageIO.setUseCache(false); //Disk operations are too slow!!!
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }
        new ServerFrame();
    }
}
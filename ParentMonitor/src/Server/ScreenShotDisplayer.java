package Server;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Date;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

//Should use similar design to parent, display images in their respective tabs
//will hold the list of saved screenshots per client
public final class ScreenShotDisplayer extends JFrame {
    
    private JTabbedPane tabs = new JTabbedPane();
    
    public ScreenShotDisplayer(ServerFrame parent, String title) {
        super(title);
        super.setIconImage(parent.getIconImage());
        
        super.setBounds(new Rectangle(parent.getX() + parent.getWidth() / 4, parent.getY() + parent.getHeight() / 3, parent.getWidth() / 2, parent.getHeight() / 2));
        super.setLocationRelativeTo(parent);
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        super.add(tabs, BorderLayout.CENTER);
        
        super.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        //Dont set visible
        //Keep this displayer alive
    }
    
    //private int count = 0; //Could've used counter instead to label files
    
    public final void addScreenShot(Date taken, ScreenShot screenShot) {
        tabs.addTab(taken.toString(), new ImagePanel(screenShot));
    }
    
    @Override
    public void dispose() {
        super.dispose();
        for (int index = 0, tabCount = tabs.getTabCount(); index < tabCount; ++index) {
            ImagePanel panel = (ImagePanel) tabs.getComponentAt(index); //fail loudly, this should always work
            panel.setEnabled(false);
            panel.image = null;
        }
        tabs.removeAll();
        tabs = null;
    }
    
    private final class ImagePanel extends JPanel {
        
        private BufferedImage image;
        
        private ImagePanel(ScreenShot shot) {
            image = shot.getImage();
            super.setToolTipText(shot.getFileName());
        }
        
        @Override
        protected void paintComponent(Graphics context) {
            super.paintComponent(context);
            context.drawImage(image, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
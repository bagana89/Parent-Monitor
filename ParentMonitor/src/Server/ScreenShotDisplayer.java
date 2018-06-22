package Server;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

//Should use similar design to parent, display images in their respective tabs
//will hold the list of saved screenshots per client
public final class ScreenShotDisplayer extends JFrame {
    
    private final JTabbedPane tabs = new JTabbedPane();
    private final List<ScreenShot> shots = new ArrayList<>();
    
    public ScreenShotDisplayer(ServerFrame parent, String clientName) {
        super(clientName);
        super.setIconImage(parent.getIconImage());
        
        super.setBounds(new Rectangle(parent.getX() + parent.getWidth() / 4, parent.getY() + parent.getHeight() / 3, parent.getWidth() / 2, parent.getHeight() / 2));
        super.setLocationRelativeTo(parent);
        
        super.add(tabs, BorderLayout.CENTER);
        
        super.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        //Dont set visible, or destroy this displayer
    }
    
    //private int count = 0;
    
    public final ScreenShot addScreenShot(String clientName, BufferedImage image) {
        Date taken = new Date();
        ScreenShot screenShot = new ScreenShot(taken, clientName + " Screenshot [" + taken.getTime() + "]", image);
        //could've used counter instead of getTime()
        tabs.addTab(taken.toString(), new ImagePanel(screenShot));
        shots.add(screenShot);
        //++count;
        return screenShot;
    }
    
    @Override
    public void dispose() {
        super.dispose();
        for (int index = 0, tabCount = tabs.getTabCount(); index < tabCount; ++index) {
            ImagePanel panel = (ImagePanel) tabs.getComponentAt(index); //fail loudly, this should always work
            panel.image = null;
            panel.setEnabled(false);
        }
        tabs.removeAll();
    }
    
    private final class ImagePanel extends JPanel {
        
        private BufferedImage image;
        
        private ImagePanel(ScreenShot shot) {
            image = shot.getImage();
            super.setToolTipText(shot.getName());
        }
        
        @Override
        protected void paintComponent(Graphics context) {
            super.paintComponent(context);
            context.drawImage(image, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
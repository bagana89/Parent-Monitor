package Server;

import static Server.Network.REQUEST_IMAGE;
import Util.StreamCloser;
import Util.ThreadSafeBoolean;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class ClientPanel extends JPanel implements Runnable {
    
    private String clientName;
    
    //stream variables
    private Socket imageChannel;
    private DataInputStream recieve;
    private PrintWriter send;
    
    private ImageRetrieverWorkerThread worker;
    
    //Rendering variables
    private BufferedImage buffer;
    private Graphics2D graphics;
    private FontRenderContext fontRenderContext;
    
    private ThreadSafeBoolean repaint = new ThreadSafeBoolean(true);
    
    private boolean terminated = false;
    
    private BufferedImage previousScreenShot;
    private ScreenShotDisplayer displayer;

    //Any IOExceptions should be thrown and passed up to the ParentPanel
    //which will pass any of its own IOExceptions to the ServerFrame, allowing
    //the server frame to display a error dialog
    @SuppressWarnings("CallToThreadStartDuringObjectConstruction")
    public ClientPanel(ServerFrame parent, String client, Socket imageStream) throws IOException {
        
        DataInputStream input;
        PrintWriter output;
        
        try {
            input = new DataInputStream(imageStream.getInputStream());
        }
        catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        }
        
        try {
            output = new PrintWriter(imageStream.getOutputStream(), true);
        }
        catch (IOException ex) {
            StreamCloser.close(input);
            ex.printStackTrace();
            throw ex;
        }
        
        imageChannel = imageStream;
        recieve = input;
        send = output;
        
        super.setBackground(Color.RED);
        
        super.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                //Everytime this component is resized, change 
                graphics = (buffer = (BufferedImage) createImage(getWidth(), getHeight())).createGraphics();
                fontRenderContext = graphics.getFontRenderContext();
            }

            @Override
            public void componentMoved(ComponentEvent e) {

            }

            @Override
            public void componentShown(ComponentEvent e) {

            }

            @Override
            public void componentHidden(ComponentEvent e) {

            }
        });
        
        displayer = new ScreenShotDisplayer(parent, clientName = client);

        new Thread(this, client + " Client Image Render Thread").start();
        (worker = new ImageRetrieverWorkerThread()).start();
    }
    
    private final class ImageRetrieverWorkerThread extends Thread {
        
        private ThreadSafeBoolean updateScreenShot = new ThreadSafeBoolean(true);
        
        private ImageRetrieverWorkerThread() {
            super(clientName + " Client Image Retriever Worker Thread");
        }
        
        @Override
        public final void run() {
            while (!terminated) {
                if (repaint.get() && updateScreenShot.get()) {
                    send.println(REQUEST_IMAGE);
                    try {
                        byte[] imageArray = new byte[recieve.readInt()];
                        recieve.readFully(imageArray);
                        previousScreenShot = ImageIO.read(new ByteArrayInputStream(imageArray));
                    }
                    catch (IOException ex) {
                        System.err.println("Failed to retreve image from client!");
                        ex.printStackTrace();
                    }
                }
            }
            //Redundant, but this is safe
            ClientPanel.this.destroy();
            updateScreenShot = null;
        }
    }
    
    public boolean isUpdating() {
        return worker.updateScreenShot.get();
    }
    
    public void setUpdate(boolean update) {
        worker.updateScreenShot.set(update);
    }
    
    public void toggleUpdate() {
        worker.updateScreenShot.invert();
    }

    public void saveCurrentShot(ImageBank bank) {
        if (previousScreenShot != null) {
            bank.addScreenShot(displayer.addScreenShot(clientName, previousScreenShot));
        }
    }
    
    public void showScreenShotDisplayer() {
        if (!displayer.isVisible()) {
            displayer.setVisible(true);
        }
    }
    
    @Override
    public void paintComponent(Graphics context)  {
        super.paintComponent(context);
        
        final int width = getWidth();
        final int height = getHeight();
        
        if (graphics == null) {
            graphics = (buffer = (BufferedImage) createImage(width, height)).createGraphics();
            fontRenderContext = graphics.getFontRenderContext();
        }

        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, width, height);
        
        //draw client user name on center top
        
        graphics.setColor(Color.WHITE);
        String display = "Client Name: " + clientName; 
        graphics.drawString(display, 
                (width / 2) - (getStringWidth(display, graphics.getFont(), fontRenderContext)) / 2, 
                getStringHeight(display, graphics.getFont(), fontRenderContext) + 10);

        if (previousScreenShot != null) {
            //Prints out max window bounds
            //System.out.println(previousScreenShot.getWidth());
            //System.out.println(previousScreenShot.getHeight());
            //System.out.println();
            
            graphics.drawImage(previousScreenShot, 0, 50, width, height - 50, null);
        }
        
        //Prints out current panel size
        //System.out.println("Width: " + width + " Height: " + height);
        
        context.drawImage(buffer, 0, 0, this);
    }
    
    //No need to notify client we are exiting, ParentFrame takes care of that
    public final void destroy() {
        if (terminated) {
            return;
        }
        
        terminated = true;
        repaint.set(false);
        
        StreamCloser.close(imageChannel);
        StreamCloser.close(recieve);
        StreamCloser.close(send);
        
        clientName = null;
        
        imageChannel = null;
        recieve = null;
        send = null;
        
        buffer = null;
        graphics = null;
        fontRenderContext = null;
        
        previousScreenShot = null;
        
        //Dont clear savedShots, we may need them later...
        
        worker = null;
        
        if (displayer != null) {
            displayer.dispose();
            displayer = null;
        }
        
        super.setEnabled(false);
        super.setVisible(false);
    }
    
    public void setRepaint(boolean shouldRepaint) {
        repaint.set(shouldRepaint);
    }

    @Override
    @SuppressWarnings("SleepWhileInLoop")
    public final void run() {
        while (!terminated) {
            if (repaint.get()) {
                repaint();
            }
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            }
            catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        //Redundant, but this is safe
        destroy();
    }

    public static float getStringWidth(String str, Font font, FontRenderContext fontRenderContext) {
        return (float) font.getStringBounds(str, fontRenderContext).getWidth();
    }

    public static float getStringHeight(String str, Font font, FontRenderContext fontRenderContext) {
        return (float) font.getStringBounds(str, fontRenderContext).getHeight();
    }
    
    private static final Map<String, Font> SAVED_FONTS = new HashMap<>(1);

    //works perfectly only when FontRenderContext is unchanged
    public static Font getFont(String text, int width, int height, FontRenderContext fontRenderContext) {
        if (SAVED_FONTS.containsKey(text)) {
            return SAVED_FONTS.get(text);
        }
        int size = 0;
        Font current = new Font("Arial", Font.BOLD, size);
        for (;;) {
            Rectangle2D currentStringBounds = current.getStringBounds(text, fontRenderContext);
            if (currentStringBounds.getWidth() <= width && currentStringBounds.getHeight() <= height) {
                Font next = new Font("Arial", Font.BOLD, ++size);
                Rectangle2D nextStringBounds = next.getStringBounds(text, fontRenderContext);
                if (nextStringBounds.getWidth() <= width && nextStringBounds.getHeight() <= height) {
                    current = next;
                    continue;
                }
                SAVED_FONTS.put(text, current);
                return current;
            }
            throw new IllegalArgumentException("Could not generate a suitable font.");
        }
    }
}
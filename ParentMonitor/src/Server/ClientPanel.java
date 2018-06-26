package Server;

import static Server.Network.REQUEST_IMAGE;
import Util.StreamCloser;
import Util.ThreadSafeBoolean;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class ClientPanel extends JPanel implements Runnable {
    
    private final ThreadSafeBoolean terminated = new ThreadSafeBoolean(false);
    private final ThreadSafeBoolean repaint = new ThreadSafeBoolean(true);

    //stream variables
    private Socket imageChannel;
    private DataInputStream recieve;
    private PrintWriter send;
    
    //Rendering variables
    private BufferedImage buffer;
    private Graphics2D graphics;

    private BufferedImage previousScreenShot;
    private ScreenShotDisplayer displayer;

    private ImageRetrieverWorkerThread worker;
    
    private String clientName;

    //private Semaphore repaintControl = new Semaphore(1, true);
    
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
            StreamCloser.close(imageStream);
            ex.printStackTrace();
            throw ex;
        }
        
        try {
            output = new PrintWriter(imageStream.getOutputStream(), true);
        }
        catch (IOException ex) {
            StreamCloser.close(imageStream);
            StreamCloser.close(input);
            ex.printStackTrace();
            throw ex;
        }
        
        imageChannel = imageStream;
        recieve = input;
        send = output;
        
        super.setBackground(Color.RED);
        
        super.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                //Everytime this component is resized, change 
                graphics = (buffer = (BufferedImage) createImage(getWidth(), getHeight())).createGraphics();
            }
        });
        
        displayer = new ScreenShotDisplayer(parent, "Screenshots Taken From: " + (clientName = client));

        new Thread(this, client + " Client Image Render Thread").start();
        (worker = new ImageRetrieverWorkerThread()).start();
    }
    
    private final class ImageRetrieverWorkerThread extends Thread {
        
        //Shared and modified by multiple threads, to avoid using ThreadSafeBoolean
        //Could make a Object to syncrhonize lock on when accessing booleans
        private ThreadSafeBoolean updateScreenShot = new ThreadSafeBoolean(true);
        
        private ImageRetrieverWorkerThread() {
            super(clientName + " Client Image Retriever Worker Thread");
        }
        
        @Override
        public final void run() {
            while (!terminated.get()) { //Loop breaks automatically AFTER close() has been called and finished execution
                if (repaint.get() && updateScreenShot.get()) {
                    send.println(REQUEST_IMAGE);
                    try {
                        byte[] imageArray = new byte[recieve.readInt()];
                        recieve.readFully(imageArray);
                        previousScreenShot = ImageIO.read(new ByteArrayInputStream(imageArray));
                    }
                    catch (IOException ex) {
                        System.err.println("Failed to retreve image from client!");
                        //If the client has been forcibly terminted on their end
                        //without sending the final exit message, such as from manual
                        //shutdown, we must take care to destroy the client on this end
                        //as well, this is taken care of in the ParentPanel
                        ex.printStackTrace();
                    }
                }
            }
            updateScreenShot = null;
            System.out.println("Image Retriever Exiting. Client Name Should Be Set to Null: " + clientName);
            //clientName should be set to null here, since close() has been called
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

    public void saveCurrentShot(ImageBank bank, ScreenShotDisplayer master) {
        if (previousScreenShot != null) {
            Date taken = new Date();
            ScreenShot screenShot = new ScreenShot(taken, clientName + " Screenshot [" + taken.getTime() + "]", previousScreenShot);
            displayer.addScreenShot(taken, screenShot);
            master.addScreenShot(taken, screenShot);
            bank.addScreenShot(screenShot);
        }
    }

    public void showScreenShotDisplayer() {
        displayer.setVisible(true);
    }

    @Override
    public void paintComponent(Graphics context)  {
        super.paintComponent(context);
        
        final int width = getWidth();
        final int height = getHeight();
        
        if (graphics == null) {
            graphics = (buffer = (BufferedImage) createImage(width, height)).createGraphics();
        }

        if (previousScreenShot != null) {
            //Prints out max window bounds
            //System.out.println(previousScreenShot.getWidth());
            //System.out.println(previousScreenShot.getHeight());
            //System.out.println();
            graphics.drawImage(previousScreenShot, 0, 0, width, height, null);
        }
        
        //Prints out current panel size
        //System.out.println("Width: " + width + " Height: " + height);
        
        context.drawImage(buffer, 0, 0, this);
    }
    
    //No need to notify client we are exiting, ParentFrame takes care of that
    public synchronized final void close() {
        if (terminated.get()) { //Lock
            return;
        }
     
        repaint.set(false);
        
        StreamCloser.close(imageChannel);
        StreamCloser.close(recieve);
        StreamCloser.close(send);
        
        imageChannel = null;
        recieve = null;
        send = null;
        
        buffer = null;
        graphics = null;
        
        previousScreenShot = null;
        displayer.dispose();
        displayer = null;
        
        worker = null;
        
        clientName = null;

        super.setEnabled(false);
        super.setVisible(false);
        
        terminated.set(true); //Unlock
    }
    
    public void setRepaint(boolean shouldRepaint) {
        repaint.set(shouldRepaint);
    }

    @Override
    @SuppressWarnings("SleepWhileInLoop")
    public final void run() {
        while (!terminated.get()) {
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
        System.out.println("Render Exiting. Client Name Should Be Set to Null: " + clientName);
        //clientName should be set to null here, since close() has been called
    }
}
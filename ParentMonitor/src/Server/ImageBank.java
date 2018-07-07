package Server;

import static Server.Network.PNG;
import Util.ThreadSafeBoolean;
import java.awt.BorderLayout;
import static java.awt.Component.LEFT_ALIGNMENT;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.event.ActionListener;
import java.awt.event.MouseMotionAdapter;
import javax.swing.ListModel;

//Saves all screenshots taken from all clients
public final class ImageBank {

    private final List<ScreenShot> screenShotList = new ArrayList<>();
    private ThreadSafeBoolean savingInProgress;

    public ImageBank() {

    }

    //Used by all instances of ClientPanel
    public void addScreenShot(ScreenShot shot) {
        screenShotList.add(shot);
    }
    
    public boolean isEmpty() {
        return screenShotList.isEmpty();
    }
    
    @SuppressWarnings({"Convert2Lambda", "ResultOfObjectAllocationIgnored"})
    public void writeToFiles(ServerFrame parent, Icon icon, File directory, ThreadSafeBoolean saving) {
        savingInProgress = saving;
        
        int entries = screenShotList.size(); //number of images to save
        String[] files = new String[entries];

        //Use an array directly for JList
        for (int index = 0; index < entries; ++index) {
            files[index] = new File(directory, screenShotList.get(index).getFileName()).getAbsolutePath();
        }
        
        new ListDisplayer(parent, files);
    }

    private class ListDisplayer extends JDialog {
        
        //Thread-Safe Boolean to indicate that the Cancel Button has been pressed or the window closed
        final ThreadSafeBoolean closed = new ThreadSafeBoolean(false);

        @SuppressWarnings({"Convert2Lambda", "UseOfObsoleteCollectionType"})
        private ListDisplayer(ServerFrame parent, String[] files) {
            super(parent, "Save Files", false);
            
            final JProgressBar progress = new JProgressBar(JProgressBar.HORIZONTAL, 0, files.length);
            progress.setValue(0);
            progress.setStringPainted(true);
            progress.setString("Progress: 0%");
            
            final JList<String> fileList = new JList<String>(files) {
                @Override
                public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
                    int row;
                    if (orientation == SwingConstants.VERTICAL && direction < 0 && (row = getFirstVisibleIndex()) != -1) {
                        Rectangle r = getCellBounds(row, row);
                        if ((r.y == visibleRect.y) && (row != 0)) {
                            Point loc = r.getLocation();
                            loc.y--;
                            int prevIndex = locationToIndex(loc);
                            Rectangle prevR = getCellBounds(prevIndex, prevIndex);

                            if (prevR == null || prevR.y >= r.y) {
                                return 0;
                            }
                            
                            return prevR.height;
                        }
                    }
                    return super.getScrollableUnitIncrement(visibleRect, orientation, direction);
                }
            };
                        
            final ListModel<String> modelList = fileList.getModel();
            
            final JButton cancel = new JButton("Cancel");
            cancel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    fileList.removeAll();
                    dispose();
                }
            });
            
            final JButton save = new JButton("Save");
            save.addActionListener(new ActionListener() {
                
                //Thread-Safe Boolean to indicate that the Save Button has been pressed
                //Changes value only once, since save button can only be pressed once
                private final ThreadSafeBoolean savePressed = new ThreadSafeBoolean(false);
                
                @Override
                public void actionPerformed(ActionEvent event) {             
                    if (savePressed.get()) {
                        return;
                    }
                    
                    savePressed.set(true);
                    
                    int[] selectedIndexes = fileList.getSelectedIndices();
                    int selectedCount = selectedIndexes.length;
                    
                    if (selectedCount == 0) {
                        //progressDialog.dispose();
                        dispose();
                        return;
                    }
                    
                    progress.setMaximum(selectedCount);

                    //Saves images in another thread to prevent blocking
                    final class ImageSaverWorkerThread extends Thread {

                        private ImageSaverWorkerThread() {
                            super("Image Saver Worker Thread");
                        }

                        @Override
                        public final void run() {
                            List<String> errorFiles = new ArrayList<>(selectedCount);
                            // Get all the selected items using the indices
                            for (int index = 0; index < selectedCount; ++index) {
                                if (closed.get()) {
                                    break;
                                }
                                int selectedIndex = selectedIndexes[index];
                                String path = modelList.getElementAt(selectedIndex);
                                try {
                                    ImageIO.write(screenShotList.get(selectedIndex).getImage(), PNG, new File(path));
                                }
                                catch (IOException ex) {
                                    errorFiles.add(path);
                                }
                                int nextValue = progress.getValue() + 1;
                                progress.setValue(nextValue);
                                progress.setString("Progress: " + (int) (100.0 * nextValue / selectedCount) + "%");
                                progress.repaint();
                            }
                            //No errors!!!
                            if (errorFiles.isEmpty()) {
                                if (closed.get()) {
                                    JOptionPane.showMessageDialog(ListDisplayer.this, progress.getValue() + "/" + selectedCount + " files saved successfully.", "Some Images Saved", JOptionPane.INFORMATION_MESSAGE, parent.getIcon());
                                }
                                else {
                                    JOptionPane.showMessageDialog(ListDisplayer.this, "All files saved successfully.", "Images Saved", JOptionPane.INFORMATION_MESSAGE, parent.getIcon());
                                }
                            }
                            //Errors!!!
                            else {
                                StringBuilder errorMessage = new StringBuilder("The following files could not be saved:\n");
                                int lastIndex = errorFiles.size() - 1;
                                for (int index = 0; index < lastIndex; ++index) {
                                    errorMessage.append(errorFiles.get(index)).append("\n");
                                }
                                errorMessage.append(errorFiles.get(lastIndex));
                                TextFrame frame = new TextFrame(ListDisplayer.this, parent.getIconImage(), "Some Images Not Saved", errorMessage.toString(), true);
                                frame.setBounds(new Rectangle(parent.getX() + parent.getWidth() / 4, parent.getY() + parent.getHeight() / 3, parent.getWidth() / 2, parent.getHeight() / 2));
                                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                                frame.setVisible(true);
                            }
                            dispose();
                        }
                    }
                    new ImageSaverWorkerThread().start(); //Start a worker thread
                }
            });
            
            fileList.addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent event) {
                    int index = fileList.locationToIndex(event.getPoint());
                    if (index > -1) {
                        String fileName = modelList.getElementAt(index);
                        fileList.setToolTipText(new File(fileName).exists() ? fileName + " already exists, selecting this will override it!" : fileName);
                    }
                }
            });

            fileList.setLayoutOrientation(JList.VERTICAL);
            fileList.setVisibleRowCount(-1);
            fileList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        save.doClick(); //emulate button click
                    }
                }
            });
            
            super.getRootPane().setDefaultButton(save); //Default selected button is save
            
            final JScrollPane listScroller = new JScrollPane(fileList);
            listScroller.setAlignmentX(LEFT_ALIGNMENT);

            final JPanel listPane = new JPanel();
            listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
            final JLabel label = new JLabel("Click On Files To Save", SwingConstants.CENTER);
            label.setLabelFor(fileList);
            listPane.add(label);
            listPane.add(Box.createRigidArea(new Dimension(0, 5)));
            listPane.add(listScroller);
            listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            //Lay out the buttons from left to right.
            final JPanel buttonPane = new JPanel();
            buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
            buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
            buttonPane.add(Box.createHorizontalGlue());
            buttonPane.add(cancel);
            buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
            buttonPane.add(save);

            //Put everything together, using the content pane's BorderLayout.
            final Container contentPane = super.getContentPane();
            contentPane.add(progress, BorderLayout.PAGE_START);
            contentPane.add(listPane, BorderLayout.CENTER);
            contentPane.add(buttonPane, BorderLayout.PAGE_END);
 
            super.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            super.setSize(parent.getWidth() / 4, parent.getHeight() / 2);
            super.setLocationRelativeTo(parent);
            super.setVisible(true);
        }
        
        @Override
        public void dispose() {
            closed.set(true);
            super.dispose();
            savingInProgress.set(false);
            savingInProgress = null; //Get rid of reference, no longer needed
        }
    }
}
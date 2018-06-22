package Server;

import static Server.Network.PNG;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public final class ImageBank {

    private final List<ScreenShot> list = new ArrayList<>();

    public ImageBank() {

    }

    //caller should specifiy name by screenshot date taken and by the client
    //whose computer provided the screenshots
    public void addScreenShot(ScreenShot shot) {
        list.add(shot);
    }

    //assume file is a directory, will need to make this in another thread
    //for many files, this operation blocks
    //should a user spam the save function, deny them, show a dialog
    //that says save operation in progress... could use JProgressBar
    @SuppressWarnings({"Convert2Lambda", "ResultOfObjectAllocationIgnored"})
    public void writeToFiles(JFrame parent, Icon icon, File directory) {
        int entries = list.size(); //number of images to save
        
        List<String> errorFiles = new ArrayList<>(entries); //list of potential files that cannot be saved
        
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(entries + 1, 1));

        Map<JCheckBox, BufferedImage> data = new HashMap<>(entries);

        //Put data into map
        for (int index = 0; index < entries; ++index) {
            ScreenShot shot = list.get(index);
            File outputFile = new File(directory, shot.getFileName());
            JCheckBox checkBox = new JCheckBox(outputFile.getAbsolutePath());
            if (outputFile.exists()) {
                checkBox.setToolTipText("Warning! The destination file already exists, selecting this will override it!");
            }
            panel.add(checkBox);
            data.put(checkBox, shot.getImage());
        }
        
        //Select All Feature
        JCheckBox selectAll = new JCheckBox("Select All");
        selectAll.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (selectAll.isSelected()) {
                    for (Iterator<JCheckBox> it = data.keySet().iterator(); it.hasNext();) {
                        it.next().setSelected(true);
                    }
                }
            }
        });
        panel.add(selectAll);

        //Assume the user knows any previous files will be overwritten if they exist
        //which is unlikely, since we use unique time stamp numbers
        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(parent, panel, "Select Files To Save", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, icon)) {
            
            //Dialog to hold progress bar
            JDialog progressDialog = new JDialog(parent, "Save Progress");
            progressDialog.setSize(parent.getWidth() / 4, parent.getHeight() / 8);
            progressDialog.setLocationRelativeTo(parent);
            progressDialog.setLayout(new GridLayout(1, 1));

            //Progress Bar
            JProgressBar progress = new JProgressBar(JProgressBar.HORIZONTAL, 0, entries);
            progress.setValue(0);
            progress.setStringPainted(true);
            progress.setString("Progress: 0%");

            if (!selectAll.isSelected()) {
                int count = 0;
                for (Iterator<JCheckBox> it = data.keySet().iterator(); it.hasNext();) {
                    if (it.next().isSelected()) {
                        ++count;
                    }
                }
                progress.setMaximum(count);
            }

            progressDialog.add(progress);
            progressDialog.setVisible(true);
               
            //Saves images in another thread to prevent blocking
            final class ImageSaverWorkerThread extends Thread {

                @SuppressWarnings("CallToThreadStartDuringObjectConstruction")
                private ImageSaverWorkerThread() {
                    super("Image Saver Worker Thread");
                    super.start();
                }

                @Override
                public final void run() {
                    final int max = progress.getMaximum();
                    for (Map.Entry<JCheckBox, BufferedImage> entry : data.entrySet()) {
                        JCheckBox checkBox = entry.getKey();
                        String path = checkBox.getText();
                        try {
                            ImageIO.write(entry.getValue(), PNG, new File(path));
                        }
                        catch (IOException ex) {
                            errorFiles.add(path);
                        }
                        int nextValue = progress.getValue() + 1;
                        progress.setValue(nextValue);
                        progress.setString("Progress: " + (int) (100.0 * nextValue / max) + "%");
                        progress.repaint();
                    }
                    //No errors!!!
                    if (errorFiles.isEmpty()) {
                        JOptionPane.showMessageDialog(parent, "All files saved successfully.", "Images Saved", JOptionPane.INFORMATION_MESSAGE, icon);
                    }
                    //Errors!!!
                    else {
                        StringBuilder errorMessage = new StringBuilder("The following files could not be saved:");
                        int lastIndex = errorFiles.size() - 1;
                        for (int index = 0; index < lastIndex; ++index) {
                            errorMessage.append(errorFiles.get(index)).append("\n");
                        }
                        errorMessage.append(errorFiles.get(lastIndex));
                        TextFrame frame = new TextFrame(parent, parent.getIconImage(), "Some Images Not Saved", errorMessage.toString(), true);
                        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        frame.setVisible(true);
                    }
                    progressDialog.dispose();
                }
            }
            
            new ImageSaverWorkerThread(); //Start a worker thread
        }
    }
}
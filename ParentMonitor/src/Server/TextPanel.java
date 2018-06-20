package Server;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.PrintWriter;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class TextPanel extends JPanel {

    private final JScrollPane scroll;
    private final JEditorPane editor;
    private final JTextField field;
    private final JButton button;
    
    private PrintWriter textOutput;

    @SuppressWarnings({"CallToThreadStartDuringObjectConstruction", "Convert2Lambda"})
    public TextPanel() {
        
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

        editor.setText("");
        editor.setEditable(false);
        scroll.setViewportView(editor);
        
        super.setLayout(new GridLayout(3, 1));
        super.add(scroll);
        super.add(field);
        super.add(button);
    }
    
    public void setOutput(PrintWriter output) {
        field.setEditable(output != null);
        textOutput = output;
    }
    
    public JEditorPane getEditorPane() {
        return editor;
    }
}
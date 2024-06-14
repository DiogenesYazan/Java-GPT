import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AIChatApp {
    private static final String GEMINI_API_KEY = "YOUR_GEMINI_API_KEY";

    public static void main(String[] args) {
        JFrame frame = new JFrame("AI Chat App");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.DARK_GRAY);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JTextField questionField = new RoundJTextField(30);
        questionField.setBackground(Color.BLACK);
        questionField.setForeground(Color.WHITE);
        questionField.setCaretColor(Color.WHITE);

        JTextArea responseArea = new JTextArea(10, 50);
        responseArea.setBackground(Color.BLACK);
        responseArea.setForeground(Color.WHITE);
        responseArea.setLineWrap(true);
        responseArea.setWrapStyleWord(true);
        responseArea.setEditable(false);

        JButton sendButton = new JButton("Send");
        sendButton.setBackground(new Color(138, 43, 226)); // Roxo claro
        sendButton.setForeground(Color.WHITE);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String question = questionField.getText();
                responseArea.setText("Loading...");
                sendToGemini(question, responseArea);
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(questionField, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        panel.add(sendButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        panel.add(new JScrollPane(responseArea), gbc);

        frame.add(panel);
        frame.setVisible(true);
    }

    private static void sendToGemini(String question, JTextArea responseArea) {
        new Thread(() -> {
            try {
                URI uri = new URI("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + GEMINI_API_KEY);
                URL url = uri.toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");

                String inputJson = "{\"contents\": [{\"parts\":[{\"text\": \"" + question + "\"}]}]}";

                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = inputJson.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                String formattedResponse = formatResponse(response.toString());
                SwingUtilities.invokeLater(() -> typewriterEffect(formattedResponse, responseArea));
            } catch (Exception ex) {
                responseArea.setText("Error: " + ex.getMessage());
            }
        }).start();
    }

    private static String formatResponse(String response) {
        int start = response.indexOf("{\"text\": \"") + 10;
        int end = response.indexOf("\"", start);
        return response.substring(start, end).replace("\\n", "\n");
    }

    private static void typewriterEffect(String text, JTextArea textArea) {
        textArea.setText("");
        Timer timer = new Timer(50, new ActionListener() {
            private int index = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (index < text.length()) {
                    textArea.append(String.valueOf(text.charAt(index)));
                    index++;
                } else {
                    ((Timer) e.getSource()).stop();
                }
            }
        });
        timer.start();
    }
}

class RoundJTextField extends JTextField {
    private Shape shape;

    public RoundJTextField(int size) {
        super(size);
        setOpaque(false); // As suggested by @AVD in comment.
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
        super.paintComponent(g);
    }

    @Override
    protected void paintBorder(Graphics g) {
        g.setColor(getForeground());
        g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 15, 15);
    }

    @Override
    public boolean contains(int x, int y) {
        if (shape == null || !shape.getBounds().equals(getBounds())) {
            shape = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15);
        }
        return shape.contains(x, y);
    }
}
